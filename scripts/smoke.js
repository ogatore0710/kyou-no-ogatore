#!/usr/bin/env node
// 実機スモークテスト: ヘッドレスChrome（puppeteer-core）でゴールデンフローを自動実行する
// 使い方: npm install（初回のみ）→ npm run smoke
// 前提: Mac に Google Chrome がインストールされていること（ブラウザ本体はダウンロードしない）
// 環境変数: SMOKE_CHROME=Chrome実行ファイルのパス（自動検出を上書き）
//           SMOKE_ROOT=配信ルートの上書き（既定はリポジトリ直下。壊したコピーの検知確認用）
// 注意: 静的チェックは scripts/qa.js（npm test）。こちらは実行時バグ専用の独立コマンド。
const fs = require("fs");
const net = require("net");
const http = require("http");
const path = require("path");
const { spawn } = require("child_process");

const REPO = path.resolve(__dirname, "..");
const ROOT = process.env.SMOKE_ROOT ? path.resolve(process.env.SMOKE_ROOT) : REPO;
const SHOT_DIR = path.join(REPO, ".smoke"); // .gitignore対象（even-syncに拾わせない）
const PREFERRED_PORT = 8801;

// ---- Chrome検出（本体ダウンロードはしない） ----
function findChrome() {
  const home = process.env.HOME || "";
  const candidates = [
    process.env.SMOKE_CHROME,
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    path.join(home, "Applications/Google Chrome.app/Contents/MacOS/Google Chrome"),
    "/Applications/Google Chrome Beta.app/Contents/MacOS/Google Chrome Beta",
    "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
  ].filter(Boolean);
  for (const p of candidates) {
    try { fs.accessSync(p, fs.constants.X_OK); return p; } catch (e) { /* 次の候補へ */ }
  }
  console.error("[smoke] Chrome が見つかりませんでした。");
  console.error("  Google Chrome を /Applications にインストールするか、");
  console.error("  環境変数 SMOKE_CHROME=/path/to/Chrome を指定してください。");
  console.error("  例: SMOKE_CHROME='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' npm run smoke");
  process.exit(2);
}

// ---- 空きポート確保（8801優先・使用中なら任意の空きポート） ----
function getFreePort(preferred) {
  return new Promise((resolve) => {
    const srv = net.createServer();
    srv.once("error", () => {
      const srv2 = net.createServer();
      srv2.listen(0, "127.0.0.1", () => {
        const p = srv2.address().port;
        srv2.close(() => resolve(p));
      });
    });
    srv.listen(preferred, "127.0.0.1", () => {
      srv.close(() => resolve(preferred));
    });
  });
}

// ---- 配信サーバー（python3 -m http.server を子プロセス起動） ----
function startServer(port) {
  const child = spawn("python3", ["-m", "http.server", String(port), "--bind", "127.0.0.1", "--directory", ROOT], {
    stdio: ["ignore", "ignore", "ignore"],
  });
  return child;
}
function waitForServer(port, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  return new Promise((resolve, reject) => {
    (function probe() {
      const req = http.get({ host: "127.0.0.1", port, path: "/index.html", timeout: 1000 }, (res) => {
        res.resume();
        if (res.statusCode === 200) resolve();
        else if (Date.now() > deadline) reject(new Error("server returned " + res.statusCode));
        else setTimeout(probe, 150);
      });
      req.on("error", () => {
        if (Date.now() > deadline) reject(new Error("server did not start on port " + port));
        else setTimeout(probe, 150);
      });
      req.on("timeout", () => req.destroy());
    })();
  });
}

// ---- 本体 ----
async function main() {
  const chromePath = findChrome();
  const puppeteer = require("puppeteer-core");
  const port = await getFreePort(PREFERRED_PORT);
  const server = startServer(port);
  const killServer = () => { try { server.kill("SIGKILL"); } catch (e) { /* already dead */ } };
  process.on("exit", killServer);
  process.on("SIGINT", () => { killServer(); process.exit(130); });
  process.on("SIGTERM", () => { killServer(); process.exit(143); });

  const url = "http://127.0.0.1:" + port + "/index.html";
  // 相談室の知識ベース（dev64担当・別ファイル）。未着の間は相談室ステップをスキップ扱いにし、
  // soudan-kb.jsの404だけは警告扱いにする（アプリ本体はtypeofガードで壊れない設計）
  const soudanKbExists = fs.existsSync(path.join(ROOT, "soudan-kb.js"));
  let browser = null;
  const results = [];       // {name, ok, detail}
  const consoleErrors = []; // 失敗扱いのコンソールエラー（ローカル起因＋JS例外）
  const externalWarns = []; // 外部リソース(fonts/ytimg等)の読み込み失敗は警告のみ（オフライン耐性）
  let currentStep = "setup";

  function isExternal(u) {
    return /^https?:\/\//.test(u) && u.indexOf("127.0.0.1") === -1 && u.indexOf("localhost") === -1;
  }

  try {
    await waitForServer(port, 10000);
    // SMOKE_NO_SANDBOX=1: root実行のコンテナ環境（クラウドセッション等）向け。
    // Chromiumはroot+サンドボックスで起動拒否するため。Mac等の通常実行では指定不要（挙動不変）。
    const extraArgs = process.env.SMOKE_NO_SANDBOX === "1" ? ["--no-sandbox", "--disable-setuid-sandbox"] : [];
    browser = await puppeteer.launch({
      executablePath: chromePath,
      headless: true,
      args: ["--no-first-run", "--no-default-browser-check", "--disable-extensions", "--hide-scrollbars"].concat(extraArgs),
    });
    const page = await browser.newPage();
    page.setDefaultTimeout(10000);
    await page.setViewport({ width: 390, height: 844 }); // スマホ想定
    // SMOKE_BLOCK_EXTERNAL=1: 外部リソース(fonts/ytimg等)を即時abortする。プロキシ環境（クラウド
    // セッション等）では外部リクエストがエラーにならず黙って固まり、loadイベントが10秒を超えて
    // 全ステップがNavigation timeoutで連鎖失敗するため。アプリは外部失敗を警告扱いのオフライン
    // 耐性設計なので、遮断＝オフライン相当のテストになる。Mac等の通常実行では指定不要（挙動不変）。
    if (process.env.SMOKE_BLOCK_EXTERNAL === "1") {
      await page.setRequestInterception(true);
      page.on("request", (req) => {
        if (isExternal(req.url())) req.abort().catch(() => {});
        else req.continue().catch(() => {});
      });
    }
    page.on("dialog", (d) => d.accept().catch(() => {}));
    page.on("pageerror", (err) => {
      consoleErrors.push("[" + currentStep + "] pageerror: " + String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const loc = msg.location() || {};
      const at = loc.url || "";
      const text = msg.text();
      if (isExternal(at) && /Failed to load resource|net::ERR/.test(text)) {
        externalWarns.push("[" + currentStep + "] " + text + " (" + at + ")");
      } else if (!soudanKbExists && at.indexOf("soudan-kb.js") !== -1) {
        externalWarns.push("[" + currentStep + "] soudan-kb.js未着による404（想定内）: " + text);
      } else {
        consoleErrors.push("[" + currentStep + "] console.error: " + text + (at ? " (" + at + ")" : ""));
      }
    });

    const $text = (sel) => page.$eval(sel, (el) => el.textContent.trim());
    const visible = (sel) => page.waitForSelector(sel, { visible: true });
    async function shot(name) {
      try {
        fs.mkdirSync(SHOT_DIR, { recursive: true });
        const file = path.join(SHOT_DIR, name.replace(/[^\w-]/g, "_") + ".png");
        await page.screenshot({ path: file, fullPage: true });
        return file;
      } catch (e) { return null; }
    }
    async function step(name, fn) {
      currentStep = name;
      const before = consoleErrors.length;
      try {
        const detail = await fn();
        if (consoleErrors.length > before) {
          throw new Error("コンソールエラー検出: " + consoleErrors.slice(before).join(" / "));
        }
        results.push({ name, ok: true, detail: detail || "" });
        console.log("PASS " + name + (detail ? " — " + detail : ""));
      } catch (err) {
        const file = await shot(name);
        results.push({ name, ok: false, detail: String(err.message || err) + (file ? " → " + file : "") });
        console.error("FAIL " + name + " — " + String(err.message || err) + (file ? " (screenshot: " + file + ")" : ""));
      }
    }

    // 1. フレッシュ起動（新規プロファイル＝localStorage空）→ はじめてガイド(#welcome)が出る→スキップでホーム・コンソールエラー0
    await step("1-フレッシュ起動でwelcome表示→スキップでホーム", async () => {
      await page.goto(url, { waitUntil: "load" });
      await visible("#home");
      const boot = await page.evaluate(() => window.__kyonoBoot === true);
      if (!boot) throw new Error("__kyonoBoot が立っていない（起動スクリプト未完走）");
      const fresh = await page.evaluate(() => localStorage.length);
      // ブラウザ起動（スプラッシュ無し）では約600ms後にシートイン
      await page.waitForFunction(
        () => { const w = document.getElementById("welcome"); return w && !w.classList.contains("hidden"); },
        { timeout: 5000 }
      );
      await shot("1-welcome-open");
      await page.click("#obSkipBtn");
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
      const onboarded = await page.evaluate(() => localStorage.getItem("kyono_onboarded"));
      if (!onboarded) throw new Error("スキップ後に kyono_onboarded が立っていない");
      return "boot marker OK / localStorage初期キー数=" + fresh + " / welcome表示→スキップ→onboarded=" + onboarded;
    });

    // 1b. オンボーディング一巡: 使い方タブの再入場リンクから起動→Q1〜Q3実タップ→anchor実保存→ルーティングでかたさチェックへ
    await step("1b-オンボーディング一巡（再入場→Q1〜ルーティング）", async () => {
      await page.click("#tab-guide");
      await visible("#obReenterLink");
      await page.click("#obReenterLink");
      await page.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"));
      // 台本の吹き出しは擬似タイピングで順に出るため、目当てのチップが出るまで待って押す
      async function tapObChip(text) {
        await page.waitForFunction((t) => {
          const btns = document.querySelectorAll("#obChips button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1) return true; }
          return false;
        }, { timeout: 8000 }, text);
        await page.evaluate((t) => {
          const btns = document.querySelectorAll("#obChips button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1) { btns[i].click(); return; } }
        }, text);
      }
      // 2026-07-12 オンボ4問化（Q0もじの大きさ新設・本人フィードバック）に追従:
      // 「大きめ（いまのまま）」を選ぶ（Q1にも同名チップ「ふつう」があるため一意な「大きめ」でタップ）
      await tapObChip("大きめ");         // Q0: もじの大きさ → bigtext既定(true)のまま
      await tapObChip("ガチガチかも");   // Q1: 硬い → ルーティングはかたさチェック
      await tapObChip("肩こり・首");     // Q2
      await tapObChip("朝おきて");       // Q3 → setAnchor("asa") 実保存
      await page.waitForFunction(() => localStorage.getItem("kyono_anchor") === '"asa"', { timeout: 8000 });
      await shot("1b-onboarding-chat");
      await tapObChip("かたさチェックをはじめる"); // ルーティングCTA
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
      await visible("#quiz");
      const qn = await $text("#qnum");
      if (qn.indexOf("Q1") !== 0) throw new Error("かたさチェックQ1に遷移していない (" + qn + ")");
      await page.click("#tab-home");
      await visible("#home");
      return "Q0大きめ→Q1ガチガチ→Q2肩こり・首→Q3朝(anchor=asa保存)→CTAでかたさチェックQ1へ→ホーム復帰";
    });

    // 1c. オンボーディングのカレンダー登録カード（2026-07-16・唯一の再来訪装置をオンボに接続）:
    //     Q3「いつやる派？」の直後にobCalendarBubble()が自動で挟まり、あさ/おふろ上がり/ねるまえの
    //     各アンカーで正しい時刻のICS/Googleカレンダーリンクが実際に生成されること、タップしても
    //     （あさ）タップしなくても（おふろ上がり・ねるまえ）自動で次（Q3が最終問のためルーティング
    //     CTA）へ進むこと、最後はかたさチェックへの遷移が壊れていないこと、そして既存の「つづける
    //     設定」側(#icsLink/#gcalLink)がオンボ側の変更で壊れていないこと（回帰）を実測で確認する
    await step("1c-オンボーディングのカレンダー登録カード（各アンカー実測+回帰）", async () => {
      async function tapObChip2(text) {
        await page.waitForFunction((t) => {
          const btns = document.querySelectorAll("#obChips button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1) return true; }
          return false;
        }, { timeout: 8000 }, text);
        await page.evaluate((t) => {
          const btns = document.querySelectorAll("#obChips button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1) { btns[i].click(); return; } }
        }, text);
      }
      const cases = [
        { anchorChip: "朝おきて", tm: "073000", tap: true },
        { anchorChip: "おふろ上がり", tm: "203000", tap: false },
        { anchorChip: "寝るまえ", tm: "213000", tap: false },
      ];
      for (const c of cases) {
        await page.evaluate(() => localStorage.clear());
        await page.reload({ waitUntil: "load" });
        await visible("#home");
        await page.click("#tab-guide");
        await visible("#obReenterLink");
        await page.click("#obReenterLink");
        await page.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"));
        await tapObChip2("大きめ");       // Q0
        await tapObChip2("ガチガチかも"); // Q1（ルーティング=かたさチェックに固定）
        await tapObChip2("肩こり・首");   // Q2
        await tapObChip2(c.anchorChip);   // Q3（アンカー実保存）
        await page.waitForFunction(() => !!document.getElementById("obIcsLink"), { timeout: 8000 });
        const hrefs = await page.evaluate(() => ({
          ics: document.getElementById("obIcsLink").href,
          gcal: document.getElementById("obGcalLink").href,
        }));
        const icsDecoded = decodeURIComponent(hrefs.ics);
        if (hrefs.ics.indexOf("data:text/calendar") !== 0) throw new Error(c.anchorChip + ": ICSリンクがdata:text/calendarで始まっていない");
        if (icsDecoded.indexOf("DTSTART:20260701T" + c.tm) === -1) {
          throw new Error(c.anchorChip + ": ICSの時刻が" + c.tm + "になっていない (" + icsDecoded.slice(0, 200) + ")");
        }
        if (hrefs.gcal.indexOf("dates=20260701T" + c.tm) === -1) {
          throw new Error(c.anchorChip + ": Googleカレンダーリンクの時刻が" + c.tm + "になっていない (" + hrefs.gcal + ")");
        }
        if (c.tap) {
          // タップしてもページ遷移せず（ダウンロードリンク）、その後の質問フローも壊れないことを確認
          const before = page.url();
          await page.click("#obIcsLink");
          const after = page.url();
          if (before !== after) throw new Error("カレンダーボタンのタップでページ遷移してしまった");
        }
        // タップの有無に関わらず、自動的に次（Q3が最終問なのでルーティングCTA）まで進む
        await page.waitForFunction(() => {
          const box = document.getElementById("obChips");
          return box && box.textContent.indexOf("かたさチェックをはじめる") !== -1;
        }, { timeout: 10000 });
      }
      // 最後（ねるまえ）のまま実際にかたさチェックへ進み、案内後も質問フローが継続することを確認
      await tapObChip2("かたさチェックをはじめる");
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
      await visible("#quiz");
      const qn = await $text("#qnum");
      if (qn.indexOf("Q1") !== 0) throw new Error("カレンダー案内のあともかたさチェックQ1に遷移できていない (" + qn + ")");
      // 回帰確認: マイ記録タブの「つづける設定」側(#icsLink/#gcalLink)がオンボ側の変更で壊れていない
      await page.click("#tab-home");
      await visible("#home");
      await page.click("#tab-history");
      await visible("#history");
      const myRecord = await page.evaluate(() => ({
        ics: document.getElementById("icsLink").href,
        anchorNow: document.getElementById("anchorNow").textContent,
      }));
      const myIcsDecoded = decodeURIComponent(myRecord.ics);
      if (myRecord.ics.indexOf("data:text/calendar") !== 0) throw new Error("マイ記録タブの#icsLinkが壊れている");
      if (myIcsDecoded.indexOf("DTSTART:20260701T213000") === -1) {
        throw new Error("マイ記録タブの#icsLinkがねるまえ(21:30)を反映していない (" + myIcsDecoded.slice(0, 200) + ")");
      }
      if (myRecord.anchorNow.indexOf("寝るまえ") === -1) throw new Error("マイ記録タブのanchorNowがねるまえになっていない (" + myRecord.anchorNow + ")");
      await page.click("#tab-home");
      await visible("#home");
      return "あさ(07:30・タップ後も継続)/おふろ上がり(20:30)/ねるまえ(21:30)の3アンカーでICS/Googleカレンダーの時刻一致を実測、タップ有無いずれも自動で次へ継続、マイ記録タブ#icsLink/anchorNowの回帰も確認";
    });

    // 2. かたさチェック5問を実ボタンクリックで完走 → タイプ名・アイコン・処方3本
    await step("2-かたさチェック完走と結果表示", async () => {
      await visible("#ckBtn");
      await page.click("#ckBtn");
      for (let i = 0; i < 5; i++) {
        await page.waitForFunction(
          (n) => {
            const el = document.getElementById("qnum");
            return el && el.textContent.indexOf("Q" + n) === 0;
          }, {}, i + 1
        );
        await page.waitForSelector("#opts .opt:not([disabled])", { visible: true });
        await page.click("#opts .opt"); // 各問とも先頭の選択肢（決定的）
      }
      await visible("#result");
      const name = await $text("#rName");
      if (!name) throw new Error("結果画面にタイプ名が出ていない");
      const icon = await page.evaluate(() => {
        const box = document.getElementById("rIllust");
        if (!box) return "none";
        const img = box.querySelector("img");
        if (img) return img.complete && img.naturalWidth > 0 ? "img-ok" : "img-broken";
        const svg = box.querySelector("svg");
        return svg && svg.childElementCount > 0 ? "svg-ok" : "none";
      });
      if (icon !== "img-ok" && icon !== "svg-ok") throw new Error("タイプアイコンが描画されていない (" + icon + ")");
      const rxCount = await page.$$eval("#rxList .video", (a) => a.length);
      if (rxCount !== 3) throw new Error("処方が3本でない (実測 " + rxCount + "本)");
      return "タイプ=" + name + " / アイコン=" + icon + " / 処方=" + rxCount + "本";
    });

    // 2b. オンボQ2「いちばん気になるのは？」→かたさチェックQ5「いちばんの悩みは？」の二度聞き解消
    //     （2026-07-17・仕様判断待ち項目、本人YES=Q5スキップで承認済み）。
    //     ①オンボで実質的な悩み（肩こり）に回答→直後のかたさチェックが4問構成になりQ5が出ないこと、
    //     結果画面のworryExtraにオンボの悩み(katakori)がちゃんと反映されること、「戻る」で壊れないこと。
    //     ②オンボで「とくにない」を選んだ場合はQ5がスキップされず通常通り5問出ること（回帰・除外確認）。
    //     ③オンボを経由しない単独起動（使い方タブ「チェックをはじめる」）は従来どおり5問のままなこと（回帰）。
    await step("2b-オンボQ2の悩みでかたさチェックQ5を二重に聞かない", async () => {
      async function tapObChip3(text) {
        await page.waitForFunction((t) => {
          const btns = document.querySelectorAll("#obChips button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1) return true; }
          return false;
        }, { timeout: 8000 }, text);
        await page.evaluate((t) => {
          const btns = document.querySelectorAll("#obChips button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1) { btns[i].click(); return; } }
        }, text);
      }
      async function runOnboardingToQuiz(worryChip) {
        await page.evaluate(() => localStorage.clear());
        await page.reload({ waitUntil: "load" });
        await visible("#home");
        await page.click("#tab-guide");
        await visible("#obReenterLink");
        await page.click("#obReenterLink");
        await page.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"));
        await tapObChip3("大きめ");          // Q0
        await tapObChip3("ガチガチかも");    // Q1（ルーティング=かたさチェックに固定）
        await tapObChip3(worryChip);         // Q2（ここが本題）
        await tapObChip3("きめてない");      // Q3
        await tapObChip3("かたさチェックをはじめる"); // ルーティングCTA
        await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
        await visible("#quiz");
      }

      // ① 肩こり・首 → 4問構成（Q5=悩み質問が出ない）
      await runOnboardingToQuiz("肩こり・首");
      let qn = await $text("#qnum");
      if (qn !== "Q1 / 4") throw new Error("①4問構成になっていない (実測 " + qn + ")");
      let dotCount = await page.$$eval("#dots .dot", (a) => a.length);
      if (dotCount !== 4) throw new Error("①進捗ドットが4個でない (実測 " + dotCount + "個)");
      // Q1「まえの質問へ」は非表示、回答して次に進んだらQ2で表示される（既存挙動の回帰確認）
      const backHiddenAtQ1 = await page.$eval("#qBackBtn", (b) => b.classList.contains("hidden"));
      if (!backHiddenAtQ1) throw new Error("①Q1で「まえの質問へ」が表示されている（既存仕様に反する）");
      await page.click("#opts .opt");
      await page.waitForFunction(() => document.getElementById("qnum").textContent === "Q2 / 4");
      // 「まえの質問へ」でQ1に戻れること（回帰）→ Q1へ戻ったら再度Q1の回答をやり直して進める
      await page.click("#qBackBtn");
      await page.waitForFunction(() => document.getElementById("qnum").textContent === "Q1 / 4");
      await page.click("#opts .opt");
      await page.waitForFunction(() => document.getElementById("qnum").textContent === "Q2 / 4");
      // 残り3問（Q2〜Q4=momo以降/ashiまで）を進めて完走。Q5(悩み)は一度も出現しないまま結果画面へ到達するはず
      for (let i = 2; i <= 4; i++) {
        await page.waitForFunction((n) => document.getElementById("qnum").textContent === "Q" + n + " / 4", {}, i);
        await page.waitForSelector("#opts .opt:not([disabled])", { visible: true });
        await page.click("#opts .opt");
      }
      await visible("#result");
      const worryExtraHtml1 = await page.$eval("#worryExtra", (el) => el.innerHTML);
      if (worryExtraHtml1.indexOf("肩こりさんへ") === -1) {
        throw new Error("①結果画面のworryExtraにオンボの悩み(肩こり)が反映されていない (" + worryExtraHtml1.slice(0, 120) + ")");
      }

      // ② とくにない → Q5スキップ対象外。従来どおり5問（Q5=悩み質問あり）で完走できること
      await runOnboardingToQuiz("とくにない");
      qn = await $text("#qnum");
      if (qn !== "Q1 / 5") throw new Error("②「とくにない」なのに5問構成になっていない (実測 " + qn + ")");
      for (let i = 1; i <= 4; i++) {
        await page.waitForFunction((n) => document.getElementById("qnum").textContent === "Q" + n + " / 5", {}, i);
        await page.waitForSelector("#opts .opt:not([disabled])", { visible: true });
        await page.click("#opts .opt");
      }
      await page.waitForFunction(() => document.getElementById("qnum").textContent === "Q5 / 5");
      const q5Title = await $text("#qtitle");
      if (q5Title.indexOf("いちばんの悩み") === -1) throw new Error("②Q5(悩み質問)が出ていない (" + q5Title + ")");
      await page.click("#opts .opt"); // Q5に自分で回答
      await visible("#result");

      // ③ オンボを経由しない単独起動（ホームの「チェックをはじめる」#ckBtn）は従来どおり5問のまま（回帰）
      await page.evaluate(() => localStorage.clear());
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      await visible("#obSkipBtn"); // フレッシュ状態ではオンボが自動起動するので、まずスキップ
      await page.click("#obSkipBtn");
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
      await visible("#ckBtn");
      await page.click("#ckBtn");
      await visible("#quiz");
      qn = await $text("#qnum");
      if (qn !== "Q1 / 5") throw new Error("③単独起動が5問構成でない=回帰 (実測 " + qn + ")");
      dotCount = await page.$$eval("#dots .dot", (a) => a.length);
      if (dotCount !== 5) throw new Error("③単独起動の進捗ドットが5個でない=回帰 (実測 " + dotCount + "個)");

      return "①肩こり選択→4問構成(Q5非出現)・戻る操作OK・結果に肩こり反映 ②とくにない→5問のまま(Q5あり) ③単独起動は5問のまま(回帰なし)";
    });

    // 3. きょうやった！→日数増加→メモ保存→記録カード生成
    await step("3-きょうやった！で日数が増える", async () => {
      await page.click("#tab-home");
      await visible("#doneBtn");
      // ホーム上部が伸びるとdoneBtnの中心が固定タブバーの真裏に落ち、page.clickがタブバーを
      // 叩いてしまう（scrollIntoViewIfNeededは「画面内」だと動かない）。中央へ寄せてから押す
      await page.$eval("#doneBtn", (el) => el.scrollIntoView({ block: "center" }));
      const before = await $text("#streakNum");
      await page.click("#doneBtn");
      await page.waitForFunction(
        (prev) => document.getElementById("streakNum").textContent.trim() !== prev, {}, before
      );
      const after = await $text("#streakNum");
      if (Number(after) !== Number(before) + 1) throw new Error("日数が " + before + "→" + after + "（+1でない）");
      const disabled = await page.$eval("#doneBtn", (b) => b.disabled);
      if (!disabled) throw new Error("押下後もdoneBtnが再度押せてしまう");
      return "通算 " + before + "日→" + after + "日 / ボタンは完了状態に変化";
    });
    await step("3b-メモ保存", async () => {
      await visible("#memoInput");
      await page.type("#memoInput", "スモークテスト良好");
      await page.click("#memoBtn");
      await page.waitForFunction(() => {
        const el = document.getElementById("memoSaved");
        return el && el.textContent.indexOf("のこしました") !== -1;
      });
      const saved = await page.evaluate(() => {
        try { return (JSON.parse(localStorage.getItem("kyono_memos")) || {}); } catch (e) { return {}; }
      });
      const vals = Object.keys(saved).map((k) => saved[k]);
      if (vals.indexOf("スモークテスト良好") === -1) throw new Error("localStorageのメモに反映されていない");
      return "memoSaved表示・localStorage反映を確認";
    });
    await step("3c-記録カード生成（cardImg data URL）", async () => {
      // #doneBtnと同じ理由：固定タブバーに隠れて誤クリックしないよう中央へ寄せてから押す
      await page.$eval("#makeCardBtn", (el) => el.scrollIntoView({ block: "center" }));
      await page.click("#makeCardBtn");
      await visible("#cardModal");
      await page.waitForFunction(() => {
        const img = document.getElementById("cardImg");
        return img && img.src.indexOf("data:image/png") === 0 && img.src.length > 10000;
      }, { timeout: 15000 });
      const len = await page.$eval("#cardImg", (img) => img.src.length);
      await page.click("#cardModal button.btn-line"); // とじる
      await page.waitForFunction(() => document.getElementById("cardModal").classList.contains("hidden"));
      return "data URL長=" + len + " (>10000)";
    });

    // 4. 5タブ巡回（各セクション描画・コンソールエラー0）
    await step("4-5タブ巡回", async () => {
      const tabs = [["guide", "guide"], ["history", "history"], ["playlists", "playlists"], ["search", "search"], ["home", "home"]];
      const seen = [];
      for (const pair of tabs) {
        await page.click("#tab-" + pair[0]);
        await visible("#" + pair[1]);
        const info = await page.$eval("#" + pair[1], (sec) => ({
          kids: sec.childElementCount,
          textLen: sec.innerText.replace(/\s+/g, "").length,
        }));
        if (info.kids === 0 || info.textLen < 20) {
          throw new Error(pair[1] + " セクションが空（子要素" + info.kids + "・本文" + info.textLen + "字）");
        }
        seen.push(pair[1] + "(" + info.kids + "子)");
      }
      return seen.join(" ");
    });

    // 5. ダークモード切替→戻し
    await step("5-ダークモード切替と復帰", async () => {
      await page.click("#tab-history");
      await visible("#th-dark");
      await page.click("#th-dark");
      await page.waitForFunction(() => document.body.classList.contains("dark"));
      await page.click("#th-light");
      await page.waitForFunction(() => !document.body.classList.contains("dark"));
      await page.click("#th-auto");
      await page.waitForFunction(() => document.getElementById("th-auto").classList.contains("on"));
      const pref = await page.evaluate(() => localStorage.getItem("kyono_theme"));
      if (pref !== '"auto"') throw new Error("themeが auto に戻っていない (" + pref + ")");
      return "dark適用→light解除→auto復帰を確認";
    });

    // 6. オガトレ通信FABを開く→閉じる
    await step("6-オガトレ通信FAB開閉", async () => {
      await page.click("#tab-home");
      await visible("#obuFab");
      await page.click("#obuFab");
      await page.waitForFunction(() => !document.getElementById("obuModal").classList.contains("hidden"));
      const bodyLen = await page.$eval("#obuBody", (el) => el.innerText.replace(/\s+/g, "").length);
      if (bodyLen < 10) throw new Error("obuBodyが空（本文" + bodyLen + "字）");
      await page.click('#obuModal button[onclick="closeObu()"]');
      await page.waitForFunction(() => document.getElementById("obuModal").classList.contains("hidden"));
      return "開いて本文" + bodyLen + "字→閉じるを確認";
    });

    // 6b. オガトレ相談室（ボトムシート版）: 入口カード→シートが開く→チップで回答（吹き出し+動画カード）
    //     →ページスクロール不動＆ログ内部スクロールの確認→赤旗ワード→戻る操作でシートが閉じる
    //     KB（soudan-kb.js）未着のときはスキップ扱い（入口カードが隠れていることだけ確認してpass）
    await step("6b-相談室ゴールデンフロー", async () => {
      await page.click("#tab-home");
      await visible("#home");
      if (!soudanKbExists) {
        const hidden = await page.$eval("#soudanCard", (el) => el.classList.contains("hidden"));
        if (!hidden) throw new Error("KB未着なのに入口カードが表示されている");
        return "SKIP扱い: soudan-kb.js未着（入口カードが隠れていることは確認）";
      }
      await visible("#soudanCard");
      await page.click("#soudanCard .sec-head"); // カード上部（チップ以外）をタップしてシートを開く
      await page.waitForFunction(() => !document.getElementById("soudanSheet").classList.contains("hidden"));
      await page.waitForFunction(() => document.querySelectorAll("#sdChips button").length > 0);
      // シートが開いた時点の背面ページ位置を基準に「チャット操作でページが1pxも動かない」ことを実測する
      const scrollBefore = await page.evaluate(() => window.pageYOffset);
      const chipText = await page.evaluate(() => {
        const list = Array.prototype.slice.call(document.querySelectorAll("#sdChips button"));
        const c = list.filter((b) => b.textContent.indexOf("肩") !== -1)[0] || list[0];
        const t = c.textContent.trim();
        c.click();
        return t;
      });
      await page.waitForFunction(
        () => document.querySelectorAll("#sdLog .sd-row.oga .video").length >= 1, { timeout: 8000 }
      );
      const counts = await page.evaluate(() => ({
        user: document.querySelectorAll("#sdLog .sd-row.user").length,
        oga: document.querySelectorAll("#sdLog .sd-row.oga:not(.sd-typing)").length,
        video: document.querySelectorAll("#sdLog .sd-row.oga .video").length,
      }));
      if (counts.user < 1) throw new Error("相談（右吹き出し）が出ていない");
      if (counts.oga < 2) throw new Error("回答吹き出しが2個未満 (実測 " + counts.oga + ")");
      // 回答の吹き出し出し切り（続き質問チップに切り替わる）まで待つ。表示中は送信ガードが効くため
      await page.waitForFunction(
        // 2026-07-12 相談室テンポ調整（3個目以降の吹き出し間隔が倍・上限3200ms・本人承認）に追従: 8000→16000
        () => document.getElementById("sdChips").textContent.indexOf("べつの悩み") !== -1, { timeout: 16000 }
      );
      // スクロール根治の確認①: 回答表示までの間、ページ全体のスクロール位置が1pxも動いていない
      const scrollAfter = await page.evaluate(() => window.pageYOffset);
      if (scrollAfter !== scrollBefore) {
        throw new Error("ページスクロールが動いた (" + scrollBefore + "→" + scrollAfter + ")");
      }
      // 赤旗ワードを自由入力→赤旗回答（sd-red）が出る
      await page.type("#sdInput", "げきつう");
      await page.click("#sdSendBtn");
      await page.waitForFunction(
        () => document.querySelectorAll("#sdLog .sd-row.sd-red").length >= 1, { timeout: 8000 }
      );
      // スクロール根治の確認②: 新しい吹き出しでログ自身が最下部まで内部スクロールしている
      const logState = await page.evaluate(() => {
        const log = document.getElementById("sdLog");
        return { top: log.scrollTop, gap: log.scrollHeight - log.clientHeight - log.scrollTop };
      });
      if (logState.gap > 40) throw new Error("ログが最下部まで追従していない (残り" + logState.gap + "px)");
      await shot("6b-soudan-chat");
      // 戻る操作（ブラウザバック）でシートが閉じてホームに戻る
      await page.goBack();
      await page.waitForFunction(() => document.getElementById("soudanSheet").classList.contains("hidden"));
      await visible("#home");
      return "チップ「" + chipText + "」→回答" + counts.oga + "吹き出し・動画" + counts.video + "枚→ページ不動(" + scrollAfter + "px)→赤旗回答→戻るでシート閉";
    });

    // 6c. 相談室×かたさタイプ連携（dev66 パートB）:
    //     タイプ無し→入口チップの初回オープン（挨拶後にキュー消化）＋「かたさチェック」導線チップ→startQuiz
    //     タイプあり(momo)→結果画面の逆導線→タイプ挨拶＋「あなたのタイプの定番」バッジ・導線チップは出ない
    await step("6c-相談室×かたさタイプ連携", async () => {
      if (!soudanKbExists) return "SKIP扱い: soudan-kb.js未着";
      const FLAVOR_RE = /モモンガ|トビラ|ダチョウ|ペンギン|ロボットさん|しなやかネコ|ネコさんの/;
      // ---- タイプ無し ----
      await page.evaluate(() => localStorage.removeItem("kyono_type"));
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      await visible("#soudanCard");
      // 入口カードのチップ（intent指定つきオープン）。初回オープンは挨拶表示→キュー消化の順に流れる
      await page.evaluate(() => {
        const btns = document.querySelectorAll("#soudanCardChips button");
        for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf("肩") !== -1) { btns[i].click(); return; } }
        btns[0].click();
      });
      await page.waitForFunction(() => !document.getElementById("soudanSheet").classList.contains("hidden"));
      await page.waitForFunction(
        () => document.querySelectorAll("#sdLog .sd-row.user").length >= 1, { timeout: 8000 }
      ); // 指定インテントの相談（右吹き出し）が出る=初回オープンでもチップが無反応にならない
      await page.waitForFunction(
        // 2026-07-12 相談室テンポ調整（3個目以降の吹き出し間隔が倍・上限3200ms・本人承認）に追従: 8000→16000
        () => document.getElementById("sdChips").textContent.indexOf("べつの悩み") !== -1, { timeout: 16000 }
      );
      const noType = await page.evaluate((reSrc) => ({
        quizChip: document.getElementById("sdChips").textContent.indexOf("かたさチェック") !== -1,
        flavor: new RegExp(reSrc).test(document.getElementById("sdLog").textContent),
      }), FLAVOR_RE.source);
      if (!noType.quizChip) throw new Error("タイプ無しなのに「かたさチェック」導線チップが出ていない");
      if (noType.flavor) throw new Error("タイプ無しなのにタイプ挨拶が出ている");
      await shot("6c-soudan-no-type");
      // 導線チップ→シートが閉じてかたさチェックQ1へ
      await page.evaluate(() => {
        const btns = document.querySelectorAll("#sdChips button");
        for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf("かたさチェック") !== -1) { btns[i].click(); return; } }
      });
      await page.waitForFunction(() => document.getElementById("soudanSheet").classList.contains("hidden"));
      await visible("#quiz");
      const qn1 = await $text("#qnum");
      if (qn1.indexOf("Q1") !== 0) throw new Error("導線チップからかたさチェックQ1に遷移していない (" + qn1 + ")");
      // ---- タイプあり(momo=つっぱりモモンガ) ----
      await page.evaluate(() => {
        localStorage.setItem("kyono_type", JSON.stringify({ key: "momo", worry: "none", at: "2026-01-01" }));
      });
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      await page.evaluate(() => showResult(state.type)); // 結果画面（チェック済みの人の再訪と同じ導線）
      await visible("#rSoudanLink");
      const linkText = await $text("#rSoudanLink");
      if (linkText.indexOf("相談室") === -1) throw new Error("結果画面に相談室への逆導線が出ていない");
      await page.evaluate(() => { document.querySelector("#rSoudanLink a").click(); });
      await page.waitForFunction(() => !document.getElementById("soudanSheet").classList.contains("hidden"));
      await page.waitForFunction(
        // 2026-07-12 相談室テンポ調整（3個目以降の吹き出し間隔が倍・上限3200ms・本人承認）に追従: 8000→16000
        () => document.getElementById("sdChips").textContent.indexOf("べつの悩み") !== -1, { timeout: 16000 }
      );
      const typed = await page.evaluate(() => ({
        user: document.querySelectorAll("#sdLog .sd-row.user").length,
        flavor: document.getElementById("sdLog").textContent.indexOf("モモンガ") !== -1,
        badge: document.getElementById("sdLog").textContent.indexOf("あなたのタイプの定番") !== -1,
        quizChip: document.getElementById("sdChips").textContent.indexOf("かたさチェック") !== -1,
      }));
      if (typed.user < 1) throw new Error("逆導線からの相談（右吹き出し）が出ていない");
      if (!typed.flavor) throw new Error("タイプ挨拶（モモンガ）が回答に出ていない");
      if (!typed.badge) throw new Error("「あなたのタイプの定番」バッジが動画に付いていない");
      if (typed.quizChip) throw new Error("タイプありなのに「かたさチェック」導線チップが出ている");
      await shot("6c-soudan-momo-type");
      await page.click(".sd-close");
      await page.waitForFunction(() => document.getElementById("soudanSheet").classList.contains("hidden"));
      await page.click("#tab-home");
      await visible("#home");
      return "タイプ無し: 入口チップ初回OK・導線チップ→Q1／タイプあり(momo): 逆導線→挨拶+定番バッジ・導線チップ非表示";
    });

    // 6d. 2週間プラン（処方箋モード・dev67）: 相談室から開始→mine枠専有→日替わりローテ→markDone整合→完走→解除
    //     日付偽装: Date.nowを+N日ずらす（todayStr/dayIndexとも同じ時計を見るため、日数もローテも一貫してずれる）
    await step("6d-2週間プラン（開始→日替わり→完走→解除）", async () => {
      if (!soudanKbExists) return "SKIP扱い: soudan-kb.js未着";
      // ---- 開始: 肩こり回答→「📅 2週間プランにする」チップ→確認吹き出し→はじめる ----
      // 6cの相談ログ・チップがシートに残っている。古いチップに印を付け、katakori回答の
      // 完了後に再描画された「新しい」plan-chipだけを待つ（回答表示中に古いチップを
      // 掴むと、planChipTapが表示中ガードで無反応=タイムアウトになる）
      await page.evaluate(() => {
        const olds = document.querySelectorAll("#sdChips button");
        for (let i = 0; i < olds.length; i++) olds[i].setAttribute("data-smoke-old", "1");
        openSoudan("katakori");
      });
      await page.waitForFunction(() => !document.getElementById("soudanSheet").classList.contains("hidden"));
      await page.waitForFunction(() => !!document.querySelector("#sdChips .plan-chip:not([data-smoke-old])"), { timeout: 8000 });
      await page.evaluate(() => { document.querySelector("#sdChips .plan-chip:not([data-smoke-old])").click(); });
      async function tapLogBtn(text) {
        await page.waitForFunction((t) => {
          const btns = document.querySelectorAll("#sdLog button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1 && !btns[i].disabled) return true; }
          return false;
        }, { timeout: 8000 }, text);
        await page.evaluate((t) => {
          const btns = document.querySelectorAll("#sdLog button");
          for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf(t) !== -1 && !btns[i].disabled) { btns[i].click(); return; } }
        }, text);
      }
      await tapLogBtn("はじめる！");
      await page.waitForFunction(() => {
        try { const p = JSON.parse(localStorage.getItem("kyono_plan")); return !!(p && p.intentId === "katakori" && p.videos.length >= 2 && p.days === 14); } catch (e) { return false; }
      }, { timeout: 8000 });
      await tapLogBtn("きょうの1本を見にいく");
      await page.waitForFunction(() => document.getElementById("soudanSheet").classList.contains("hidden"));
      const day1 = await page.evaluate(() => {
        const badge = document.querySelector("#todayVideo .badge");
        const link = document.querySelector("#todayVideo a.video");
        return {
          badge: badge ? badge.textContent : "",
          id: link ? (link.href.split("v=")[1] || "") : "",
          planTitle: document.getElementById("planTitle").textContent,
          cardHidden: document.getElementById("planCard").classList.contains("hidden"),
        };
      });
      if (day1.badge.indexOf("プラン1日目/14") !== 0) throw new Error("mine枠バッジがプラン表示でない (" + day1.badge + ")");
      if (day1.cardHidden || day1.planTitle.indexOf("1/14日") === -1) throw new Error("ホームのプランカードが1/14日を示していない (" + day1.planTitle + ")");
      await shot("6d-plan-day1");
      // ---- 日替わり(+1日) → バッジ2日目・動画はローテ次番。markDoneのdaylogにはプラン動画IDが残る ----
      const day2 = await page.evaluate(() => {
        const real = Date.now;
        Date.now = function () { return real() + 86400000; };
        try {
          renderHome();
          const badge = document.querySelector("#todayVideo .badge").textContent;
          const id = document.querySelector("#todayVideo a.video").href.split("v=")[1] || "";
          markDone(); // プラン中の「きょうやった！」— daylogに残るIDをトレース
          const ds = todayStr();
          const dl = JSON.parse(localStorage.getItem("kyono_daylog") || "{}");
          return { badge: badge, id: id, videos: JSON.parse(localStorage.getItem("kyono_plan")).videos, daylogV: dl[ds] ? dl[ds].v : "(なし)" };
        } finally { Date.now = real; }
      });
      if (day2.badge.indexOf("プラン2日目/14") !== 0) throw new Error("+1日でバッジが2日目にならない (" + day2.badge + ")");
      const i1 = day2.videos.indexOf(day1.id);
      if (i1 === -1) throw new Error("1日目の動画がプラン動画リストにない (" + day1.id + ")");
      if (day2.id !== day2.videos[(i1 + 1) % day2.videos.length]) throw new Error("+1日の動画がローテ次番でない (" + day1.id + "→" + day2.id + ")");
      if (day2.daylogV !== day2.id) throw new Error("プラン中のdaylogにプラン動画IDが残っていない (daylog=" + day2.daylogV + ")");
      // ---- 完走(+15日=15日目) → 初回ホーム描画でお祝いカード・kyono_plan削除 ----
      const grad = await page.evaluate(() => {
        const real = Date.now;
        Date.now = function () { return real() + 15 * 86400000; };
        try {
          renderHome();
          return {
            doneVisible: !document.getElementById("planDoneCard").classList.contains("hidden"),
            txt: document.getElementById("planDoneText").textContent,
            planLeft: localStorage.getItem("kyono_plan"),
          };
        } finally { Date.now = real; }
      });
      if (!grad.doneVisible || grad.txt.indexOf("完走") === -1) throw new Error("完走お祝いカードが出ていない");
      if (grad.planLeft !== "null") throw new Error("完走後にkyono_planが削除されていない (" + grad.planLeft + ")");
      await shot("6d-plan-graduate");
      // ---- もう2週間つづける → プラン再開(1/14) → 「やめる」(confirm自動OK)で解除・責めない文言 ----
      await page.evaluate(() => {
        const btns = document.querySelectorAll("#planDoneCard button");
        for (let i = 0; i < btns.length; i++) { if (btns[i].textContent.indexOf("もう2週間") !== -1) { btns[i].click(); return; } }
      });
      await page.waitForFunction(() => {
        try { const p = JSON.parse(localStorage.getItem("kyono_plan")); return !!(p && p.intentId === "katakori"); } catch (e) { return false; }
      });
      const again = await $text("#planTitle");
      if (again.indexOf("1/14日") === -1) throw new Error("再開プランが1日目から始まっていない (" + again + ")");
      await page.evaluate(() => { planQuit(); }); // confirmはdialogハンドラが自動OK
      await page.waitForFunction(() => localStorage.getItem("kyono_plan") === "null");
      const quit = await page.evaluate(() => ({
        msgVisible: !document.getElementById("planQuitMsg").classList.contains("hidden"),
        cardVisible: !document.getElementById("planCard").classList.contains("hidden"),
      }));
      if (!quit.msgVisible || !quit.cardVisible) throw new Error("解除後の「またいつでも組めるよ」文言が出ていない");
      return "開始(肩こり3本)→+1日でローテ次番+daylog=プラン動画→+15日で完走お祝い+plan削除→再開1/14→やめるで解除文言";
    });

    // 6e. とどくメーター(#reach)でFAB(相談室/オガトレ通信)が隠れるか(2026-07-15実測監査で発見した重なりバグの再発防止)
    //     実測(390x844)でreach-rowの5番目のボタンがFAB2段と大きく重なりタップ不能に近い状態だったため、
    //     quizと同様にreachでもFABを隠す修正をした。ホームに戻るとFABが復帰することも合わせて確認する
    await step("6e-とどくメーターでFABが隠れる（重なりバグの再発防止）", async () => {
      await page.click("#tab-history");
      await visible("#history");
      await page.click('#reachCard button[onclick="navTo(\'reach\')"]');
      await visible("#reach");
      const hiddenOnReach = await page.evaluate(() => ({
        bodyClass: document.body.classList.contains("fabs-hide"),
        soudanDisplay: getComputedStyle(document.getElementById("soudanFab")).display,
        obuDisplay: getComputedStyle(document.getElementById("obuFab")).display,
      }));
      if (!hiddenOnReach.bodyClass) throw new Error("reachでbody.fabs-hideが付いていない");
      if (hiddenOnReach.soudanDisplay !== "none") throw new Error("reachで相談室FABが非表示になっていない (display=" + hiddenOnReach.soudanDisplay + ")");
      if (hiddenOnReach.obuDisplay !== "none") throw new Error("reachでオガトレ通信FABが非表示になっていない (display=" + hiddenOnReach.obuDisplay + ")");
      await page.click("#tab-home");
      await visible("#home");
      const shownOnHome = await page.evaluate(() => ({
        bodyClass: document.body.classList.contains("fabs-hide"),
        obuDisplay: getComputedStyle(document.getElementById("obuFab")).display,
      }));
      if (shownOnHome.bodyClass) throw new Error("ホームに戻ってもfabs-hideが残っている");
      if (shownOnHome.obuDisplay === "none") throw new Error("ホームに戻ってもオガトレ通信FABが復帰していない");
      return "reachでFAB(相談室・オガトレ通信とも)非表示→ホーム復帰で再表示を確認";
    });

    // 7. 破損データ耐性: kyono_streak2に不正文字列→リロードで白画面にならない
    await step("7-破損データ耐性（kyono_streak2）", async () => {
      await page.evaluate(() => localStorage.setItem("kyono_streak2", "{oops!!broken"));
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      const state = await page.evaluate(() => ({
        boot: window.__kyonoBoot === true,
        textLen: document.body.innerText.replace(/\s+/g, "").length,
        streak: (document.getElementById("streakNum") || { textContent: "" }).textContent.trim(),
      }));
      if (!state.boot) throw new Error("リロード後に起動マーカーが立たない");
      if (state.textLen < 100) throw new Error("画面がほぼ空（本文" + state.textLen + "字）＝白画面相当");
      if (!/^\d+$/.test(state.streak)) throw new Error("streakNumが数字でない (" + state.streak + ")");
      return "リロード後も描画OK / streakNum=" + state.streak + "（既定値へフォールバック）";
    });

    // 8. 最終確認: コンソールエラー総数0
    currentStep = "8-最終確認";
    if (consoleErrors.length === 0) {
      results.push({ name: "8-コンソールエラー総数0", ok: true, detail: "0件" });
      console.log("PASS 8-コンソールエラー総数0 — 0件");
    } else {
      results.push({ name: "8-コンソールエラー総数0", ok: false, detail: consoleErrors.length + "件" });
      console.error("FAIL 8-コンソールエラー総数0 — " + consoleErrors.length + "件:");
      for (const line of consoleErrors) console.error("  " + line);
    }
  } catch (fatal) {
    results.push({ name: "smoke全体", ok: false, detail: "致命的エラー: " + String(fatal && fatal.message ? fatal.message : fatal) });
    console.error("FAIL smoke全体 — " + String(fatal && fatal.message ? fatal.message : fatal));
  } finally {
    if (browser) { try { await browser.close(); } catch (e) { /* ignore */ } }
    killServer();
  }

  const failed = results.filter((r) => !r.ok);
  console.log("");
  if (externalWarns.length) {
    console.log("(参考) 外部リソースの読み込み失敗 " + externalWarns.length + "件は警告扱い（オフライン耐性のため失敗に数えない）:");
    for (const w of externalWarns.slice(0, 5)) console.log("  " + w);
  }
  console.log("smoke結果: " + (results.length - failed.length) + "/" + results.length + " pass");
  if (failed.length) {
    console.error("失敗ステップ:");
    for (const f of failed) console.error("  ✗ " + f.name + " — " + f.detail);
    process.exit(1);
  }
  console.log("全ステップpass🌱");
}

main().catch((err) => {
  console.error("[smoke] 予期しないエラー: " + String(err && err.stack ? err.stack : err));
  process.exit(1);
});
