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
    // ページ共通の配線（メインpage・7c用の使い捨てpageの両方から呼ぶ）。
    // consoleErrors/externalWarnsは外側のクロージャ変数を共有するので、使い捨てpageのエラーも
    // 最終確認(8-コンソールエラー総数0)に正しく合算される。
    async function wirePage(p) {
      p.setDefaultTimeout(10000);
      await p.setViewport({ width: 390, height: 844 }); // スマホ想定
      // SMOKE_BLOCK_EXTERNAL=1: 外部リソース(fonts/ytimg等)を即時abortする。プロキシ環境（クラウド
      // セッション等）では外部リクエストがエラーにならず黙って固まり、loadイベントが10秒を超えて
      // 全ステップがNavigation timeoutで連鎖失敗するため。アプリは外部失敗を警告扱いのオフライン
      // 耐性設計なので、遮断＝オフライン相当のテストになる。Mac等の通常実行では指定不要（挙動不変）。
      if (process.env.SMOKE_BLOCK_EXTERNAL === "1") {
        await p.setRequestInterception(true);
        p.on("request", (req) => {
          if (isExternal(req.url())) req.abort().catch(() => {});
          else req.continue().catch(() => {});
        });
      }
      p.on("dialog", (d) => d.accept().catch(() => {}));
      p.on("pageerror", (err) => {
        consoleErrors.push("[" + currentStep + "] pageerror: " + String(err && err.message ? err.message : err));
      });
      p.on("console", (msg) => {
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
    }
    const page = await browser.newPage();
    await wirePage(page);

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

    // 1c. カレンダー登録カード（2026-07-16にオンボQ3直後の吹き出しとして導入→2026-07-17「はじめの1本
    //     ガイド」設計でPO承認のうえ移設）: いまはQ3直後には出ず、オンボ完走後の「1日目クリア」
    //     （きょうやった！markDone成功）の直後に#calAskへ一度だけ出る。あさ/おふろ上がり/ねるまえの
    //     各アンカーで正しい時刻のICS/Googleカレンダーリンクが実際に生成されること、タップしても
    //     ページ遷移しないこと、リロード後は二度と出ない（kyono_calseenガード）こと、そして既存の
    //     「つづける設定」側(#icsLink/#gcalLink)が壊れていないこと（回帰）を実測で確認する
    await step("1c-カレンダー登録カード（1日目クリア後の#calAsk・移設後）", async () => {
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
        await tapObChip2("大きめ");       // Q0（bigtext。Q1にも同名「ふつう」があるため一意な「大きめ」でタップ）
        await tapObChip2("ふつう");       // Q1（stiff=ふつう。かたさチェックquizルートではなくtodayルートで
                                          // アンカー→#calAskの動線だけをシンプルに見たいための選択）
        await tapObChip2("とくにない");   // Q2（worry=none → soudanに振られずtodayルートへ）
        await tapObChip2(c.anchorChip);   // Q3（アンカー実保存・最終問なのでルーティングCTAが出る）
        // Q3直後にカレンダー案内の吹き出しは出ない（移設済み・obLog内にcalAsk関連idが無いこと）ことを確認
        const bubbleGone = await page.evaluate(() => !document.getElementById("calAskIcsLink") && !document.getElementById("obIcsLink"));
        if (!bubbleGone) throw new Error(c.anchorChip + ": オンボQ3直後にまだカレンダー案内の吹き出しが出ている（移設漏れ）");
        await tapObChip2("きょうの1本を見る"); // todayルートのCTA。obGo()経由でwelcomeが閉じてhomeへ
        await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
        await visible("#home");
        // まだ#calAskは空（1日目クリア前）
        const calAskBefore = await page.evaluate(() => (document.getElementById("calAsk").innerHTML || "").trim());
        if (calAskBefore !== "") throw new Error(c.anchorChip + ": きょうやった！を押す前から#calAskに何か出ている");
        await visible("#doneBtn");
        await page.$eval("#doneBtn", (el) => el.scrollIntoView({ block: "center" }));
        await page.waitForFunction(() => !document.getElementById("doneBtn").disabled, { timeout: 5000 });
        await page.click("#doneBtn"); // markDone() 成功 → total=1 → #calAskへ一度だけ描画される
        await page.waitForFunction(() => document.getElementById("doneBtn").disabled, { timeout: 8000 });
        await page.waitForFunction(() => (document.getElementById("calAsk").innerHTML || "").trim() !== "", { timeout: 12000 });
        const info = await page.evaluate(() => ({
          lead: (document.getElementById("calAskLead") || {}).textContent,
          ics: (document.getElementById("calAskIcsLink") || {}).href,
          gcal: (document.getElementById("calAskGcalLink") || {}).href,
          note: (document.getElementById("calAskNote") || {}).textContent,
        }));
        if (info.lead.indexOf("あしたも同じじかんに会おう") === -1) throw new Error(c.anchorChip + ": #calAskのリード文言が設計どおりでない (" + info.lead + ")");
        const icsDecoded = decodeURIComponent(info.ics || "");
        if (!info.ics || info.ics.indexOf("data:text/calendar") !== 0) throw new Error(c.anchorChip + ": #calAskのICSリンクがdata:text/calendarで始まっていない");
        // 2026-07-17 実機バグ修正: DTSTART/datesはハードコードされた過去日(20260701)ではなく
        // 「今日」(todayStr())を使う必要がある。過去日固定だとGoogleカレンダーが単発予定として
        // 扱ってしまう不具合が実機で確認されたため、再発防止として動的な今日日付を検証する。
        const todayDigits = await page.evaluate(() => todayStr().replace(/-/g, ""));
        if (icsDecoded.indexOf("DTSTART:20260701T") !== -1) {
          throw new Error(c.anchorChip + ": #calAskのICSにハードコードされた過去日(20260701)が残っている（todayStr()参照に修正したはず）");
        }
        if (icsDecoded.indexOf("DTSTART:" + todayDigits + "T" + c.tm) === -1) {
          throw new Error(c.anchorChip + ": #calAskのICS日時が今日(" + todayDigits + ")+" + c.tm + "になっていない (" + icsDecoded.slice(0, 200) + ")");
        }
        if (icsDecoded.indexOf("RRULE:FREQ=DAILY") === -1) {
          throw new Error(c.anchorChip + ": #calAskのICSにRRULE:FREQ=DAILYが含まれていない（毎日繰り返しが壊れている）");
        }
        if (!info.gcal || info.gcal.indexOf("dates=20260701T") !== -1) {
          throw new Error(c.anchorChip + ": #calAskのGoogleカレンダーリンクにハードコードされた過去日(20260701)が残っている");
        }
        if (!info.gcal || info.gcal.indexOf("dates=" + todayDigits + "T" + c.tm) === -1) {
          throw new Error(c.anchorChip + ": #calAskのGoogleカレンダー日時が今日(" + todayDigits + ")+" + c.tm + "になっていない (" + info.gcal + ")");
        }
        if (info.gcal.indexOf("recur=RRULE:FREQ=DAILY") === -1) {
          throw new Error(c.anchorChip + ": #calAskのGoogleカレンダーリンクにrecur=RRULE:FREQ=DAILYが含まれていない");
        }
        if (info.note.indexOf("つづける設定") === -1) throw new Error(c.anchorChip + ": #calAskの注記が設計どおりでない (" + info.note + ")");
        if (c.tap) {
          // タップしてもページ遷移しない（ダウンロードリンク）ことを確認
          const before = page.url();
          await page.click("#calAskIcsLink");
          const after = page.url();
          if (before !== after) throw new Error("カレンダーボタンのタップでページ遷移してしまった");
        }
      }
      // 最後（ねるまえ）のケースのまま: リロードしても#calAskはもう出ない（kyono_calseenガード・二度目なし）
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      const calAskAfterReload = await page.evaluate(() => (document.getElementById("calAsk").innerHTML || "").trim());
      if (calAskAfterReload !== "") throw new Error("リロード後も#calAskに残っている（もう一度出てしまっている＝kyono_calseenガードが効いていない）");
      const calseen = await page.evaluate(() => localStorage.getItem("kyono_calseen"));
      if (calseen !== "1") throw new Error("kyono_calseenが1になっていない (" + calseen + ")");
      // 回帰確認: マイ記録タブの「つづける設定」側(#icsLink/#gcalLink)が#calAsk移設の影響で壊れていない
      await page.click("#tab-history");
      await visible("#history");
      const myRecord = await page.evaluate(() => ({
        ics: document.getElementById("icsLink").href,
        anchorNow: document.getElementById("anchorNow").textContent,
      }));
      const myIcsDecoded = decodeURIComponent(myRecord.ics);
      const myTodayDigits = await page.evaluate(() => todayStr().replace(/-/g, ""));
      if (myRecord.ics.indexOf("data:text/calendar") !== 0) throw new Error("マイ記録タブの#icsLinkが壊れている");
      if (myIcsDecoded.indexOf("DTSTART:20260701T") !== -1) {
        throw new Error("マイ記録タブの#icsLinkにハードコードされた過去日(20260701)が残っている");
      }
      if (myIcsDecoded.indexOf("DTSTART:" + myTodayDigits + "T213000") === -1) {
        throw new Error("マイ記録タブの#icsLinkが今日(" + myTodayDigits + ")+ねるまえ(21:30)を反映していない (" + myIcsDecoded.slice(0, 200) + ")");
      }
      if (myRecord.anchorNow.indexOf("寝るまえ") === -1) throw new Error("マイ記録タブのanchorNowがねるまえになっていない (" + myRecord.anchorNow + ")");
      // 日付を跨いだ場合の回帰確認: Date.nowを+2日ずらしてrenderIcs()を再実行し、
      // #icsLink/#gcalLinkの日付がtodayStr()の新しい値に追従して更新されることを確認
      // （renderIcs()が起動時の日付を握ったまま固定化されていないか＝再発防止）。
      const rollover = await page.evaluate(() => {
        const real = Date.now;
        Date.now = function () { return real() + 2 * 86400000; };
        try {
          const expected = todayStr().replace(/-/g, "");
          renderIcs();
          const ics = decodeURIComponent(document.getElementById("icsLink").href);
          const gcal = document.getElementById("gcalLink").href;
          return { expected: expected, ics: ics, gcal: gcal };
        } finally { Date.now = real; }
      });
      if (rollover.ics.indexOf("DTSTART:" + rollover.expected + "T") === -1) {
        throw new Error("日付を+2日ずらしてもrenderIcs()のICS日付が新しい今日(" + rollover.expected + ")に更新されない (" + rollover.ics.slice(0, 200) + ")");
      }
      if (rollover.gcal.indexOf("dates=" + rollover.expected + "T") === -1) {
        throw new Error("日付を+2日ずらしてもrenderIcs()のGoogleカレンダーリンクが新しい今日(" + rollover.expected + ")に更新されない (" + rollover.gcal + ")");
      }
      // 後始末: ページに残ったDate.now上書きの影響が後続ステップに漏れないよう、明示的にrenderIcs()を
      // 通常時刻で再実行しておく（上のfinallyでDate.now自体は既に復元済みだが、念のため描画も戻す）
      await page.evaluate(() => { renderIcs(); });
      // 後続ステップ用にクリーンな状態へ戻す（このテストで作った記録/fd/calseenを持ち越さない）。
      // 注意: #obSkipBtn(obSkip()→obClose())は内部でhistory.state.obが立っていればhistory.back()を呼ぶ経路。
      // このスモークテストは1ページを使い回すため、ここまでの多数のpushState(オンボ/かたさチェック等)で
      // history本来のスタックがかなり深くなっており、back()が意図しない古いエントリ(例:マイ記録タブ)へ
      // 着地しうる（実測で再現・アプリ本体のバグではなくテストの使い回しに起因）。そのため、ここでは
      // back()を使わない経路（CTAボタン経由のobGo()。obClose(true)でback()を意図的にスキップする設計）で
      // クリーンな状態に戻す
      async function tapObChip2b(text) {
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
      await page.evaluate(() => localStorage.clear());
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      await page.click("#tab-guide");
      await visible("#obReenterLink");
      await page.click("#obReenterLink");
      await page.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"));
      await tapObChip2b("大きめ");
      await tapObChip2b("ふつう");
      await tapObChip2b("とくにない");
      await tapObChip2b("きめてない");
      await tapObChip2b("きょうの1本を見る");
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
      await visible("#ckBtn");
      // このtodayルート通過ではじめの1本ガイド(kyono_fd="go")が立つが、後続の「2」はガイド非対象の
      // 素の結果画面を検証する意図のテストのため、ここだけ剥がしてクリーンな前提に揃える
      await page.evaluate(() => localStorage.removeItem("kyono_fd"));
      return "あさ(07:30・タップ後も継続)/おふろ上がり(20:30)/ねるまえ(21:30)の3アンカーで、1日目クリア直後の#calAskがICS/Googleカレンダーの時刻を正しく反映、オンボQ3直後には出ない(移設確認)、リロード後は二度と出ない(kyono_calseenガード)、マイ記録タブ#icsLink/anchorNowの回帰も確認";
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

    // 6f. 背面スクロールロック(position:fixed+top:-scrollY方式・iOS15以前の「overflow:hiddenだけでは背面が
    //     指でスクロールできてしまう」既知バグの対策)を実機相当(ヘッドレスChrome)で実測する。
    //     相談室シート・カード図鑑ともopen時にscrollYを記憶してbodyへposition:fixed+top:-scrollYを適用し、
    //     close時にインラインstyleを解除してwindow.scrollToで元の位置へ正確に復元することを確認する。
    await step("6f-背面スクロールロック(position:fixed+top:-scrollY)", async () => {
      if (!soudanKbExists) return "SKIP扱い: soudan-kb.js未着（相談室シート側は検証不可）";
      // ---- 相談室シート ----
      await page.click("#tab-home");
      await visible("#home");
      await page.evaluate(() => window.scrollTo(0, 220)); // 少しスクロールした状態を作ってから開く
      const sdScrollBefore = await page.evaluate(() => window.scrollY);
      if (sdScrollBefore < 100) throw new Error("事前スクロールが効いていない (scrollY=" + sdScrollBefore + ")");
      await visible("#soudanCard");
      // page.click()は要素を自動でscrollIntoViewしてしまい、直前にwindow.scrollToで作った検証用の
      // スクロール位置が上書きされてしまうため、ここは素のclick()をevaluate経由で発火する
      await page.evaluate(() => { document.querySelector("#soudanCard .sec-head").click(); });
      await page.waitForFunction(() => !document.getElementById("soudanSheet").classList.contains("hidden"));
      const sdLocked = await page.evaluate(() => ({
        position: document.body.style.position,
        top: document.body.style.top,
        width: document.body.style.width,
        hasClass: document.body.classList.contains("sd-lock"),
      }));
      if (sdLocked.position !== "fixed") throw new Error("相談室オープン時にbody.style.positionがfixedでない (" + sdLocked.position + ")");
      if (sdLocked.top !== "-" + sdScrollBefore + "px") throw new Error("body.style.topがscrollYと不一致 (" + sdLocked.top + " / scrollY=" + sdScrollBefore + ")");
      if (sdLocked.width !== "100%") throw new Error("body.style.widthが100%でない (" + sdLocked.width + ")");
      if (!sdLocked.hasClass) throw new Error("sd-lockクラスが付いていない");
      await page.click(".sd-close");
      await page.waitForFunction(() => document.getElementById("soudanSheet").classList.contains("hidden"));
      const sdAfter = await page.evaluate(() => ({ position: document.body.style.position, top: document.body.style.top, scrollY: window.scrollY }));
      if (sdAfter.position !== "" || sdAfter.top !== "") throw new Error("相談室クローズ後もインラインstyleが残っている (position=" + sdAfter.position + " top=" + sdAfter.top + ")");
      if (sdAfter.scrollY !== sdScrollBefore) throw new Error("相談室クローズ後にscrollYが復元されていない (" + sdScrollBefore + "→" + sdAfter.scrollY + ")");

      // ---- カード図鑑モーダル ----
      await page.click("#tab-history");
      await visible("#history");
      await page.evaluate(() => window.scrollTo(0, 150));
      const dexScrollBefore = await page.evaluate(() => window.scrollY);
      if (dexScrollBefore < 50) throw new Error("図鑑オープン前のスクロールが効いていない (scrollY=" + dexScrollBefore + ")");
      await page.evaluate(() => { document.querySelector('#dexBannerCard button[onclick="openDex()"]').click(); });
      await visible("#dexModal");
      const dexLocked = await page.evaluate(() => ({ position: document.body.style.position, top: document.body.style.top }));
      if (dexLocked.position !== "fixed") throw new Error("図鑑オープン時にbody.style.positionがfixedでない (" + dexLocked.position + ")");
      if (dexLocked.top !== "-" + dexScrollBefore + "px") throw new Error("図鑑オープン時のtopがscrollYと不一致 (" + dexLocked.top + " / scrollY=" + dexScrollBefore + ")");
      await page.click(".dex-close");
      await page.waitForFunction(() => document.getElementById("dexModal").classList.contains("hidden"));
      const dexAfter = await page.evaluate(() => ({ position: document.body.style.position, scrollY: window.scrollY }));
      if (dexAfter.position !== "") throw new Error("図鑑クローズ後もposition:fixedが残っている (" + dexAfter.position + ")");
      if (dexAfter.scrollY !== dexScrollBefore) throw new Error("図鑑クローズ後にscrollYが復元されていない (" + dexScrollBefore + "→" + dexAfter.scrollY + ")");

      // ---- 通常時(モーダル未オープン)のスクロールは今まで通り効くこと ----
      await page.click("#tab-home");
      await visible("#home");
      await page.evaluate(() => window.scrollTo(0, 80));
      const normal = await page.evaluate(() => ({ scrollY: window.scrollY, position: getComputedStyle(document.body).position }));
      if (normal.scrollY !== 80) throw new Error("モーダル未使用時に通常スクロールができていない (scrollY=" + normal.scrollY + ")");
      if (normal.position === "fixed") throw new Error("モーダル未使用時にbodyがposition:fixedのままになっている");
      await page.evaluate(() => window.scrollTo(0, 0));

      return "相談室シート/カード図鑑ともopen時body.position=fixed+top=-scrollY(" + sdScrollBefore + "px/" + dexScrollBefore + "px)・close時に元のscrollYへ復元・通常時のスクロールは影響なしを実測確認";
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

    // 7b. 「きょうの1本」タップで実際に見た動画IDがdaylogに残る（AUDIT-MEMO低優先項目の回帰確認）
    //     ケースA: タップした動画がおすすめと別IDでもdaylogにはタップ側が残る
    //     ケースB: タップなしで「きょうやった！」→従来どおりおすすめ動画IDにフォールバック
    //     あわせて、既存の「きょうやった？」ナッジ(checkDoneNudge)がタップの動画ID記録を消してしまわないことも確認
    await step("7b-タップ動画IDのdaylog反映（おすすめと別ID／フォールバック／ナッジ非干渉）", async () => {
      const result = await page.evaluate(() => {
        const real = Date.now;
        try {
          // ---- ケースA ----
          Date.now = function () { return real() + 2 * 86400000; };
          renderHome();
          const dayA = todayStr();
          const recA = currentTodayId();
          const otherId = Object.keys(V).map((k) => V[k].id).find((id) => id !== recA);
          if (!otherId) throw new Error("catalogに別動画candidateが見つからない");
          const tv = document.getElementById("todayVideo");
          const fakeA = document.createElement("a");
          fakeA.className = "video";
          fakeA.href = "https://www.youtube.com/watch?v=" + otherId;
          fakeA.target = "_blank"; fakeA.rel = "noopener";
          tv.appendChild(fakeA);
          // 実際にYouTubeへ遷移させない（テスト用のfakeAだけpreventDefault、既存の動画リンクの挙動には触れない）
          const guard = (ev) => { if (ev.target === fakeA) ev.preventDefault(); };
          document.addEventListener("click", guard, true);
          fakeA.dispatchEvent(new MouseEvent("click", { bubbles: true, cancelable: true }));
          document.removeEventListener("click", guard, true);
          fakeA.remove();
          const pendingVideoAfterTap = sessionStorage.getItem("kyono_pendingNudgeVideo");
          // 「戻ってきた」ナッジチェックを挟む。旧kyono_pendingNudgeは消えるが、動画IDキーは影響を受けないはず
          checkDoneNudge();
          const pendingNudgeAfterCheck = sessionStorage.getItem("kyono_pendingNudge");
          const pendingVideoAfterCheck = sessionStorage.getItem("kyono_pendingNudgeVideo");
          markDone();
          const dlA = (JSON.parse(localStorage.getItem("kyono_daylog") || "{}"))[dayA];

          // ---- ケースB: タップせず「きょうやった！」だけ→フォールバック ----
          Date.now = function () { return real() + 3 * 86400000; };
          renderHome();
          const dayB = todayStr();
          const recB = currentTodayId();
          markDone();
          const dlB = (JSON.parse(localStorage.getItem("kyono_daylog") || "{}"))[dayB];

          return {
            dayA, recA, otherId, pendingVideoAfterTap, pendingNudgeAfterCheck, pendingVideoAfterCheck,
            dlA: dlA ? dlA.v : null,
            dayB, recB, dlB: dlB ? dlB.v : null,
          };
        } finally { Date.now = real; }
      });
      if (!result.pendingVideoAfterTap) throw new Error("タップでkyono_pendingNudgeVideoが記録されていない");
      if (JSON.parse(result.pendingVideoAfterTap).v !== result.otherId) throw new Error("記録された動画IDがタップしたものと不一致");
      if (result.pendingNudgeAfterCheck !== null) throw new Error("checkDoneNudge後もkyono_pendingNudgeが残っている（従来のナッジ仕様が壊れている）");
      if (!result.pendingVideoAfterCheck) throw new Error("checkDoneNudgeでkyono_pendingNudgeVideoまで消えてしまっている（ナッジと動画記録が独立していない）");
      if (result.dlA !== result.otherId) throw new Error("daylogにタップした動画IDが残っていない (記録=" + result.dlA + " 期待=" + result.otherId + ")");
      if (result.dlA === result.recA) throw new Error("daylogがおすすめ動画IDのままになっている（タップ優先が効いていない）");
      if (result.dlB !== result.recB) throw new Error("タップなしのフォールバックでおすすめ動画IDが記録されていない (記録=" + result.dlB + " 期待=" + result.recB + ")");
      return "タップ動画(" + result.otherId + ")がdaylogに反映／ナッジ後も動画ID保持／タップなしはおすすめ(" + result.recB + ")にフォールバック";
    });

    // 7c. ホーム画面に追加ポップアップ(#a2hsModal)の端末/ブラウザ分岐（実機UA差し替えで7シナリオを検証）
    //     メインpageのUAは書き換えず、シナリオごとに使い捨てpageをwirePage()で配線して検証する
    //     （UA/standalone/beforeinstallpromptのevaluateOnNewDocumentはpage生存中ずっと残るため、
    //     シナリオ間で汚染しないよう毎回新しいpageを使い捨てる）。
    await step("7c-ホーム画面追加ポップアップの端末分岐（iOS Safari/iOS Chrome/Android/standalone/デスクトップ/アプリ内ブラウザ）", async () => {
      const UA_IOS_SAFARI = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
      const UA_IOS_CHROME = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/125.0.6422.80 Mobile/15E148 Safari/604.1";
      const UA_ANDROID = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36";
      const UA_DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
      const UA_ANDROID_LINE = UA_ANDROID + " Line/12.7.0";

      async function freshPage(opts) {
        opts = opts || {};
        const p = await browser.newPage();
        await wirePage(p);
        await p.setUserAgent(opts.ua);
        if (opts.standalone) {
          await p.evaluateOnNewDocument(() => {
            try { Object.defineProperty(navigator, "standalone", { get: () => true, configurable: true }); } catch (e) { /* ignore */ }
          });
        }
        // window.__a2hsEventをアクセサ化してテスト用の値に固定する。
        // 理由: この配信環境(python http.serverのlocalhost+有効なmanifest+SW登録済み)はChrome自身が
        // 実インストール可能と判定し、本物のネイティブbeforeinstallpromptイベントを発火させてしまう。
        // 実測で確認済み: 本物のイベントはprompt()を持つ本物のAPI形状のため型チェックはすり抜けるが、
        // page.evaluateの非トラステッドクリックからprompt()を呼んでもuserChoiceが解決されずテストが
        // ハングする（実ブラウザの「ユーザー操作必須」仕様のため）。アプリ自身のheadスクリプトが
        // window.__a2hsEventへ代入する前（evaluateOnNewDocumentは全スクリプトより先に実行される）に
        // アクセサ化しておき、代入(=本物のイベントの上書き含む)を無視してテスト用の固定値だけを
        // 返すようにすることで、環境依存の実イベント発火タイミングに左右されない決定的なテストにする。
        await p.evaluateOnNewDocument((fakeEnabled) => {
          const forced = fakeEnabled
            ? { prompt: function () { window.__a2hsPromptCalled = true; return Promise.resolve(); }, userChoice: Promise.resolve({ outcome: "accepted" }) }
            : null;
          Object.defineProperty(window, "__a2hsEvent", {
            configurable: true,
            get: function () { return forced; },
            set: function () { /* 実ブラウザ由来のbeforeinstallpromptによる上書きを無視 */ },
          });
        }, !!opts.bip);
        await p.goto(url, { waitUntil: "load" });
        await p.evaluate(() => localStorage.clear()); // 同一プロファイル内の他シナリオ分の記録を消し、フレッシュ起動を再現
        await p.reload({ waitUntil: "load" });
        await p.waitForFunction(() => window.__kyonoBoot === true, { timeout: 8000 });
        return p;
      }
      function waitPopupOrGuide(p) {
        return p.waitForFunction(() => {
          const w = document.getElementById("welcome"), m = document.getElementById("a2hsModal");
          return (w && !w.classList.contains("hidden")) || (m && !m.classList.contains("hidden"));
        }, { timeout: 6000 });
      }
      const notes = [];

      // 1) iOS Safari → 共有ボタンからの案内、ボタンは「あとで」のみ
      {
        const p = await freshPage({ ua: UA_IOS_SAFARI });
        await waitPopupOrGuide(p);
        const kind = await p.$eval("#a2hsModal", (el) => el.getAttribute("data-a2hs-kind"));
        if (kind !== "ios-safari") throw new Error("iOS Safariでkindがios-safariでない (実測=" + kind + ")");
        const welcomeHidden = await p.$eval("#welcome", (el) => el.classList.contains("hidden"));
        if (!welcomeHidden) throw new Error("iOS Safari: a2hsModal表示中にwelcomeも同時に見えている");
        await p.click("#a2hsBtns button"); // 「あとで」
        await p.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"), { timeout: 5000 });
        await p.close();
        notes.push("iOS Safari→共有ボタン案内→あとで→はじめてガイド");
      }

      // 2) iOS Chrome(CriOS) → 「Safariで開く必要がある」案内
      {
        const p = await freshPage({ ua: UA_IOS_CHROME });
        await waitPopupOrGuide(p);
        const kind = await p.$eval("#a2hsModal", (el) => el.getAttribute("data-a2hs-kind"));
        if (kind !== "ios-other") throw new Error("iOS Chromeでkindがios-otherでない (実測=" + kind + ")");
        await p.click("#a2hsBtns button"); // 「あとで」
        await p.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"), { timeout: 5000 });
        await p.close();
        notes.push("iOS Chrome(CriOS)→Safariで開く案内→あとで→はじめてガイド");
      }

      // 3) Android + beforeinstallprompt発火保持あり → 「ホーム画面に追加する」タップでprompt()実行
      {
        const p = await freshPage({ ua: UA_ANDROID, bip: true });
        await waitPopupOrGuide(p);
        const kind = await p.$eval("#a2hsModal", (el) => el.getAttribute("data-a2hs-kind"));
        if (kind !== "android-prompt") throw new Error("Android+bip保持でkindがandroid-promptでない (実測=" + kind + ")");
        const btnCount = await p.$$eval("#a2hsBtns button", (a) => a.length);
        if (btnCount !== 2) throw new Error("Android+bip保持でボタンが2個(インストール/あとで)でない (実測=" + btnCount + ")");
        await p.evaluate(() => {
          const btns = document.querySelectorAll("#a2hsBtns button");
          for (const b of btns) { if (b.textContent.indexOf("ホーム画面に追加する") !== -1) { b.click(); return; } }
        });
        await p.waitForFunction(() => window.__a2hsPromptCalled === true, { timeout: 5000 });
        await p.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"), { timeout: 5000 });
        await p.close();
        notes.push("Android(beforeinstallprompt保持)→「ホーム画面に追加する」→prompt()実行確認→はじめてガイド");
      }

      // 4) Android（beforeinstallprompt未発火）→ 「⋮」メニュー案内。戻る操作でも進行不能にならないことも確認
      {
        const p = await freshPage({ ua: UA_ANDROID });
        await waitPopupOrGuide(p);
        const kind = await p.$eval("#a2hsModal", (el) => el.getAttribute("data-a2hs-kind"));
        if (kind !== "android-menu") throw new Error("Android未発火でkindがandroid-menuでない (実測=" + kind + ")");
        await p.goBack(); // ボタンを押さず戻る操作で閉じても、必ずはじめてガイドへ進む（進行不能防止の必須要件）
        await p.waitForFunction(() => document.getElementById("a2hsModal").classList.contains("hidden"), { timeout: 5000 });
        await p.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"), { timeout: 5000 });
        await p.close();
        notes.push("Android(未発火)→メニュー案内→戻る操作で閉じても必ずはじめてガイドへ進行(スタック防止)");
      }

      // 5) すでにstandalone起動中 → ポップアップなしで直接はじめてガイド
      {
        const p = await freshPage({ ua: UA_ANDROID, standalone: true });
        await p.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"), { timeout: 6000 });
        const modalTouched = await p.$eval("#a2hsModal", (el) => !el.classList.contains("hidden") || el.hasAttribute("data-a2hs-kind"));
        if (modalTouched) throw new Error("standalone起動中なのにa2hsModalが出た");
        await p.close();
        notes.push("standalone起動中→a2hsModalなしで直接はじめてガイド");
      }

      // 6) デスクトップUA → ポップアップなしで直接はじめてガイド（「ホーム画面」の概念がスマホ前提のため）
      {
        const p = await freshPage({ ua: UA_DESKTOP });
        await p.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"), { timeout: 6000 });
        const modalTouched = await p.$eval("#a2hsModal", (el) => !el.classList.contains("hidden") || el.hasAttribute("data-a2hs-kind"));
        if (modalTouched) throw new Error("デスクトップUAなのにa2hsModalが出た");
        await p.close();
        notes.push("デスクトップUA→a2hsModalなしで直接はじめてガイド");
      }

      // 7) アプリ内ブラウザ(LINE) → 従来どおりポップアップ・はじめてガイドとも出さない（既存仕様の回帰確認）
      {
        const p = await freshPage({ ua: UA_ANDROID_LINE });
        await new Promise((r) => setTimeout(r, 2200)); // スプラッシュ無し(0.6秒)+ポップアップ判定の猶予を待っても出ないことを確認
        const state = await p.evaluate(() => ({
          welcomeHidden: document.getElementById("welcome").classList.contains("hidden"),
          modalHidden: document.getElementById("a2hsModal").classList.contains("hidden"),
          boot: window.__kyonoBoot === true,
        }));
        if (!state.boot) throw new Error("LINEアプリ内UAで起動マーカーが立っていない");
        if (!state.welcomeHidden) throw new Error("LINEアプリ内UAなのにwelcomeが表示された（既存仕様の回帰）");
        if (!state.modalHidden) throw new Error("LINEアプリ内UAなのにa2hsModalが表示された");
        await p.close();
        notes.push("アプリ内ブラウザ(LINE)→従来どおりポップアップ/はじめてガイドとも非表示(回帰なし)");
      }

      return notes.join(" / ");
    });

    // 7d. はじめの1本ガイド（2026-07-17・Fable設計・PO承認済み）: オンボ完走(quizルート)→かたさチェック完走→
    //     結果画面が①だけの導線になる→①タップで新規タブが開く→戻ってきた体でcheckDoneNudge()相当を発火→
    //     結果画面表示中は#rDoneNudge→ボタンでホームへ→doneBtnが強調→「きょうやった！」→固定cheer文言/
    //     メモplaceholder/#calAsk表示→kyono_fd=1→ホーム「あなた用」が通常の3本表示に自動復帰、という
    //     一連の流れを実機で確認する。あわせて既存ユーザー（ガイド対象外）の「もう一回チェックする」が
    //     従来どおりであること（回帰・最重要）、soudanルートがガイド対象外であることも確認する。
    await step("7d-はじめの1本ガイド（オンボ→結果画面①強調→タップ→復帰ナッジ→記録→カレンダー→3本表示に復帰）", async () => {
      async function tapObChip4(text) {
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
      await page.evaluate(() => localStorage.clear());
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      await page.click("#tab-guide");
      await visible("#obReenterLink");
      await page.click("#obReenterLink");
      await page.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"));
      await tapObChip4("大きめ");         // Q0
      await tapObChip4("ガチガチかも");   // Q1(stiff=hard) → quizルート確定
      await tapObChip4("とくにない");     // Q2(worry=none) → かたさチェックQ5(悩み)はスキップされず5問のまま
      await tapObChip4("きめてない");     // Q3(anchor=free)
      await tapObChip4("かたさチェックをはじめる"); // quizルートCTA。obGo()がここでkyono_fd="go"をセットする
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
      await visible("#quiz");
      const fdAfterObGo = await page.evaluate(() => localStorage.getItem("kyono_fd"));
      if (fdAfterObGo !== '"go"') throw new Error("obGo()通過後にkyono_fd=\"go\"がセットされていない (" + fdAfterObGo + ")");
      // 5問とも先頭の選択肢で完走（全カテゴリ0点→タイプ=しなやかネコ=rx未固定の分岐も一緒に確認できる）
      for (let i = 0; i < 5; i++) {
        await page.waitForFunction((n) => {
          const el = document.getElementById("qnum");
          return el && el.textContent.indexOf("Q" + n) === 0;
        }, {}, i + 1);
        await page.waitForSelector("#opts .opt:not([disabled])", { visible: true });
        await page.click("#opts .opt");
      }
      await visible("#result");

      // ---- 確認1: 結果画面が①だけの能動ガイド導線になっていること ----
      const r1 = await page.evaluate(() => ({
        rxHead: (document.getElementById("rxHead") || {}).innerHTML || "",
        rxListHtml: (document.getElementById("rxList") || {}).innerHTML || "",
        heroVideoCount: document.querySelectorAll("#rxList .fd-hero .video").length,
        totalVideoCount: document.querySelectorAll("#rxList .video").length,
        rotateHidden: (document.getElementById("rRotateNote") || {}).classList.contains("hidden"),
        tourBtnHidden: (document.getElementById("rTourBtn") || {}).classList.contains("hidden"),
      }));
      if (r1.rxHead.indexOf("まずはこの1本から") === -1 || r1.rxHead.indexOf("②③はあしたからでOKだよ") === -1) {
        throw new Error("rxHeadがガイド用文言になっていない (" + r1.rxHead + ")");
      }
      if (r1.heroVideoCount !== 1) throw new Error(".fd-hero内の動画が1本でない (実測" + r1.heroVideoCount + "本)");
      if (r1.totalVideoCount !== 3) throw new Error("結果画面の動画総数が3本でない=②③が消えている懸念 (実測" + r1.totalVideoCount + "本)");
      if (r1.rxListHtml.indexOf("きょうはこれ1本でOK！") === -1) throw new Error("①のラベルが「きょうはこれ1本でOK！」になっていない");
      if (r1.rxListHtml.indexOf("タップするとYouTubeがひらくよ") === -1) throw new Error("オガトレの一言吹き出しが出ていない");
      if (r1.rxListHtml.indexOf("3本つづけて再生する") !== -1) throw new Error("guide中なのに「3本つづけて再生する」ボタンが出ている");
      if (!r1.rotateHidden) throw new Error("guide中なのにrotate-note(3日ごと入れ替わり注記)が隠れていない");
      if (!r1.tourBtnHidden) throw new Error("guide中なのにrTourBtn(つづき:使い方ツアーへ)が隠れていない");

      // ---- 確認2(タップ検知): ①の動画リンクを実際にクリック→新規タブが開く→pendingNudge系がセットされる ----
      // 新規タブが開くと(target=_blank)、実機同様このページはdocument.hidden=trueになり、タブを閉じて
      // フォーカスが戻ると本物のvisibilitychangeでrefreshDay()→checkDoneNudge()が自動発火することを実測で
      // 確認済み。そのためpendingNudge系のセットは「新規タブが実際に開いてフォーカスを奪う前」の時点
      // （page.click()が返った直後・popup検出を待つ前）で読み取る。読み取り後にpopupを閉じることで、
      // 実際のvisibilitychangeを経由した自動checkDoneNudge()発火（=よりリアルな「アプリに戻った」経路）を
      // 妨げない。
      const popupWait = new Promise((resolve) => {
        browser.once("targetcreated", (t) => { t.page().then(resolve).catch(() => resolve(null)); });
        setTimeout(() => resolve(null), 4000);
      });
      const heroId = await page.evaluate(() => {
        const a = document.querySelector("#rxList .fd-hero a.video");
        return a && (a.getAttribute("href").match(/[?&]v=([\w-]{11})/) || [])[1];
      });
      await page.click("#rxList .fd-hero a.video");
      const pendingAfterTap = await page.evaluate(() => ({
        nudge: sessionStorage.getItem("kyono_pendingNudge"),
        video: sessionStorage.getItem("kyono_pendingNudgeVideo"),
      }));
      if (!pendingAfterTap.nudge) throw new Error("#result内の①タップでkyono_pendingNudgeがセットされていない（#result専用タップ検知IIFEの不備）");
      if (!pendingAfterTap.video || JSON.parse(pendingAfterTap.video).v !== heroId) throw new Error("kyono_pendingNudgeVideoがタップした①の動画IDと一致しない");
      const popup = await popupWait;
      if (!popup) throw new Error("①タップで新規タブ(target=_blank)が開かなかった");
      const popupUrl = popup.url();
      await popup.close().catch(() => {});
      if (!heroId || popupUrl.indexOf(heroId) === -1) throw new Error("開いた新規タブのURLがタップした動画IDと一致しない (" + popupUrl + " / " + heroId + ")");

      // ---- 確認3(復帰ナッジ): 結果画面が表示中のまま「アプリに戻った」状態を再現→#rDoneNudgeが出る ----
      // 新規タブを閉じてフォーカスが戻ると本物のvisibilitychangeでcheckDoneNudge()が自動発火することがある
      // （実測で確認済み・環境依存の可能性があるため）。ここではさらに明示的にcheckDoneNudge()を呼んで
      // 確実性を担保する（既に自動発火済みならkyono_pendingNudgeは既に消えており、早期returnの単なる
      // no-opになるだけで害はない＝どちらの経路でも同じ結果になることを利用した決定的なテスト）。
      await page.evaluate(() => { checkDoneNudge(); });
      await page.waitForFunction(() => !document.getElementById("rDoneNudge").classList.contains("hidden"), { timeout: 5000 });
      const rDoneNudgeText = await page.$eval("#rDoneNudge", (el) => el.innerText);
      if (rDoneNudgeText.indexOf("おかえりなさい") === -1 || rDoneNudgeText.indexOf("ストレッチできた") === -1) {
        throw new Error("#rDoneNudgeの文言が設計どおりでない (" + rDoneNudgeText + ")");
      }
      if (rDoneNudgeText.indexOf("1日目の記録をつけにいく") === -1) throw new Error("#rDoneNudgeのボタン文言が設計どおりでない");
      // 結果画面表示中はホーム側cheerを触っていないこと（早期return・相互排他の確認）
      const cheerUntouched = await page.evaluate(() => (document.getElementById("cheer") || {}).innerHTML === "");
      if (!cheerUntouched) throw new Error("結果画面表示中なのにホームの#cheerが書き換わっている（分岐が相互排他になっていない）");

      // ---- 確認4: #rDoneNudgeのボタン→ホームへ→doneBtnが強調される ----
      await page.click("#rDoneNudgeBtn");
      await page.waitForFunction(() => !document.getElementById("home").classList.contains("hidden"), { timeout: 5000 });
      await page.waitForFunction(() => document.getElementById("doneBtn").classList.contains("nudge-pulse"), { timeout: 5000 });

      // ---- 確認4a(2026-07-17追加): checkDoneNudge()のタイミング検知に依存しない持続的な案内。
      // guide中・きょうの記録がまだの間は#doneBtn直上の#fdDoneStaticNudgeが常に見えていること ----
      const staticNudgeBeforeDone = await page.evaluate(() => ({
        hidden: document.getElementById("fdDoneStaticNudge").classList.contains("hidden"),
        text: document.getElementById("fdDoneStaticNudge").textContent,
      }));
      if (staticNudgeBeforeDone.hidden) throw new Error("guide中・未記録なのに#fdDoneStaticNudgeが隠れている（持続的な記録案内が出ていない）");
      if (staticNudgeBeforeDone.text.indexOf("ここを押してね") === -1) throw new Error("#fdDoneStaticNudgeの文言が設計どおりでない (" + staticNudgeBeforeDone.text + ")");

      // ---- 確認4b: ホーム「あなた用」がまだ①だけの導線になっていること ----
      // renderToday()はstate.modeが一度でも真値になると以後それを使い回す(既存仕様・本機能とは無関係の
      // 既存の挙動)。初回起動時のrenderHome()がクイズ未完了時点で既にstate.modeをasa/yoruへ確定させて
      // いるため、ここでは実ユーザーと同じ操作(segMineタップ)で明示的に「あなた用」へ切り替えて確認する。
      await visible("#segMine");
      await page.click("#segMine");
      await page.waitForFunction(() => document.getElementById("segMine").classList.contains("on"), { timeout: 5000 });
      const mineBefore = await page.evaluate(() => ({
        html: document.getElementById("todayVideo").innerHTML,
        videoCount: document.querySelectorAll("#todayVideo .video").length,
      }));
      if (mineBefore.html.indexOf("きょうはこれ1本でOK！") === -1) throw new Error("guide中なのにホーム「あなた用」のバッジがガイド用になっていない");
      if (mineBefore.videoCount !== 1) throw new Error("guide中のホーム「あなた用」が1本でない (実測" + mineBefore.videoCount + "本)");
      if (mineBefore.html.indexOf("②と③はあしたからでだいじょうぶ") === -1) throw new Error("guide中のホーム「あなた用」に②③注記が無い");
      if (mineBefore.html.indexOf("連続再生") !== -1) throw new Error("guide中なのにホーム「あなた用」に連続再生リンクが出ている");

      // ---- 確認5: 「きょうやった！」→固定cheer文言・メモplaceholder変更・#calAsk表示 ----
      await page.$eval("#doneBtn", (el) => el.scrollIntoView({ block: "center" }));
      await page.click("#doneBtn");
      await page.waitForFunction(() => (document.getElementById("cheer") || {}).innerHTML !== "", { timeout: 5000 });
      const afterDone = await page.evaluate(() => ({
        cheer: document.getElementById("cheer").innerHTML,
        placeholder: document.getElementById("memoInput").placeholder,
        calAskLead: (document.getElementById("calAskLead") || {}).textContent,
        fd: localStorage.getItem("kyono_fd"),
      }));
      if (afterDone.cheer.indexOf("🎉 1日目クリア！ナイスご自愛！") === -1) throw new Error("cheerが固定文言「1日目クリア」になっていない (" + afterDone.cheer + ")");
      if (afterDone.cheer.indexOf("きょうのひとことをどうぞ") === -1) throw new Error("cheerにメモ促し文言が無い");
      if (afterDone.cheer.indexOf("「使い方」タブ") === -1) throw new Error("cheerに使い方タブ案内が無い");
      if (afterDone.placeholder !== "例: 肩がかるくなった気がする😊") throw new Error("メモ欄のplaceholderがguide用に変わっていない (" + afterDone.placeholder + ")");
      if (!afterDone.calAskLead || afterDone.calAskLead.indexOf("あしたも同じじかんに会おう") === -1) throw new Error("#calAskがguide完了後に表示されていない");
      if (afterDone.fd !== "1") throw new Error("kyono_fdが完了値1になっていない (" + afterDone.fd + ")");

      // ---- 確認5b(2026-07-17追加): 記録完了で持続的な案内(#fdDoneStaticNudge)が消えること ----
      const staticNudgeGone = await page.evaluate(() => document.getElementById("fdDoneStaticNudge").classList.contains("hidden"));
      if (!staticNudgeGone) throw new Error("記録後も#fdDoneStaticNudgeが表示されたまま（fdActive()false/今日記録済みで消えるはず）");

      // ---- 確認5c(2026-07-17追加): cheer後に「使い方ツアーを見る」ボタンが出て、タップで実際にツアーが始まること ----
      await visible("#cheerTourBtn");
      const tourBtnText = await $text("#cheerTourBtn");
      if (tourBtnText.indexOf("使い方ツアーを見る") === -1) throw new Error("#cheerTourBtnの文言が設計どおりでない (" + tourBtnText + ")");
      await visible("#cheerTourSkipBtn"); // 「あとで」的なスキップ手段も確保されていること
      await page.click("#cheerTourBtn");
      await page.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"), { timeout: 5000 });
      await page.waitForFunction(() => (document.getElementById("obLog") || {}).innerHTML !== undefined && document.getElementById("obLog").children.length > 0, { timeout: 8000 });
      const tourStarted = await page.evaluate(() => ({
        welcomeHidden: document.getElementById("welcome").classList.contains("hidden"),
        obLogHasContent: document.getElementById("obLog").children.length > 0,
      }));
      if (tourStarted.welcomeHidden) throw new Error("「使い方ツアーを見る」タップ後もwelcome(オンボ/ツアーのシート)が開いていない");
      if (!tourStarted.obLogHasContent) throw new Error("「使い方ツアーを見る」タップ後にobOpenTour()のツアー内容(#obLog)が始まっていない");
      // ツアーを閉じてホームへ戻す（後続ステップへの影響を避ける）。obClose()の既定引数だと
      // history.back()を呼ぶ経路になるが、このスモークテストは1ページを使い回すため
      // history本来のスタックが深く、back()が意図しない古いエントリへ着地しうる（他ステップの
      // コメント済みの既知の注意点と同じ）。obClose(true)でback()を意図的にスキップする。
      await page.evaluate(() => { obClose(true); });
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"), { timeout: 5000 });
      await page.click("#tab-home");
      await visible("#home");

      // ---- 確認6: fd完了後はホーム「あなた用」が自動的に通常の3本表示へ戻る ----
      await page.evaluate(() => { renderHome(); });
      const mineAfter = await page.evaluate(() => ({
        html: document.getElementById("todayVideo").innerHTML,
        videoCount: document.querySelectorAll("#todayVideo .video").length,
      }));
      if (mineAfter.html.indexOf("きょうのあなた用") === -1) throw new Error("fd完了後もホーム「あなた用」がガイド表示のまま (バッジ文言不一致)");
      if (mineAfter.videoCount !== 3) throw new Error("fd完了後の「あなた用」が3本表示に戻っていない (実測" + mineAfter.videoCount + "本)");
      if (mineAfter.html.indexOf("連続再生") === -1) throw new Error("fd完了後に連続再生リンクが復活していない");

      // ---- 確認7: リロード後、#calAskは二度と出ない ----
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      const calAskAfterReload2 = await page.evaluate(() => (document.getElementById("calAsk").innerHTML || "").trim());
      if (calAskAfterReload2 !== "") throw new Error("リロード後も#calAskが残っている（もう一度出てしまっている）");

      // ---- 確認8(回帰・最重要): 既存ユーザー(ガイド対象外)の「もう一回チェックする」は従来どおり ----
      await visible("#ckBtn");
      const ckLabel = await $text("#ckBtn");
      if (ckLabel.indexOf("もう一回チェックする") === -1) throw new Error("既存ユーザーなのに#ckBtnが「もう一回チェックする」表示になっていない");
      await page.click("#ckBtn");
      for (let i = 0; i < 5; i++) {
        await page.waitForFunction((n) => {
          const el = document.getElementById("qnum");
          return el && el.textContent.indexOf("Q" + n) === 0;
        }, {}, i + 1);
        await page.waitForSelector("#opts .opt:not([disabled])", { visible: true });
        await page.click("#opts .opt");
      }
      await visible("#result");
      const regress = await page.evaluate(() => ({
        rxHead: (document.getElementById("rxHead") || {}).innerHTML || "",
        rxListHtml: (document.getElementById("rxList") || {}).innerHTML || "",
        rotateHidden: (document.getElementById("rRotateNote") || {}).classList.contains("hidden"),
      }));
      if (regress.rxHead.indexOf("おすすめの3本") === -1) throw new Error("回帰: 既存ユーザーのrxHeadがguide文言のままになっている (" + regress.rxHead + ")");
      if (regress.rxListHtml.indexOf("fd-hero") !== -1) throw new Error("回帰: 既存ユーザーの結果画面に.fd-heroが残っている");
      if (regress.rxListHtml.indexOf("3本つづけて再生する") === -1) throw new Error("回帰: 既存ユーザーで「3本つづけて再生する」ボタンが消えている");
      if (regress.rotateHidden) throw new Error("回帰: 既存ユーザーでrotate-noteが隠れたままになっている");
      await page.click("#tab-home");
      await visible("#home");

      // ---- 確認9: soudanルートはガイド対象外（kyono_fdをセットしない） ----
      let soudanNote = "SKIP扱い: soudan-kb.js未着のためsoudanルート自体が発生しない(today側にフォールバックする既存仕様)";
      if (soudanKbExists) {
        await page.evaluate(() => localStorage.clear());
        await page.reload({ waitUntil: "load" });
        await visible("#home");
        await page.click("#tab-guide");
        await visible("#obReenterLink");
        await page.click("#obReenterLink");
        await page.waitForFunction(() => !document.getElementById("welcome").classList.contains("hidden"));
        await tapObChip4("大きめ");        // Q0
        await tapObChip4("やわらかい");    // Q1(stiff=soft・quizルートを回避)
        await tapObChip4("肩こり・首");    // Q2(worry=katakoriで実質的な悩みあり) → soudanルートへ
        await tapObChip4("きめてない");    // Q3
        await tapObChip4("相談室で聞いてみる"); // soudanルートCTA
        await page.waitForFunction(() => !document.getElementById("soudanSheet").classList.contains("hidden"), { timeout: 8000 });
        const fdAfterSoudan = await page.evaluate(() => localStorage.getItem("kyono_fd"));
        if (fdAfterSoudan) throw new Error("soudanルートなのにkyono_fdがセットされている (" + fdAfterSoudan + ")＝設計の対象外条件が守られていない");
        soudanNote = "soudanルート実行→kyono_fd未セットを確認(対象外)";
        try { await page.evaluate(() => { if (typeof closeSoudan === "function") closeSoudan(true); }); } catch (e) { /* ignore */ }
      }
      // このステップが本スイート最後の機能テストのため（残るは8のコンソールエラー集計のみ・状態非依存）、
      // 後続ステップ用のクリーンアップは行わない。#obSkipBtnはhistory.state.obが立っていればhistory.back()
      // する経路で、この1ページを使い回すスモークテストではここまでの大量のpushStateでhistoryスタックが
      // 深くなっており、back()が意図しない古いエントリへ着地しうる（実測で確認済み・アプリのバグではない）
      // ため、状態非依存の最終ステップでは意図的にこの経路を避けている。

      return "オンボ完走(quizルート)→結果画面①強調(fd-hero/バッジ/オガトレ一言/3本つづけて再生ボタン非表示/rotate-note非表示/rTourBtn非表示)→①タップで新規タブ+pendingNudge系セット→checkDoneNudge()で#rDoneNudge表示(ホームcheerは不可侵)→ボタンでホームへ+doneBtn強調→#fdDoneStaticNudge常時案内を確認→きょうやった！で固定cheer文言/メモplaceholder/#calAsk表示/#fdDoneStaticNudge消失→#cheerTourBtnタップでobOpenTour()実起動を確認→kyono_fd=1→あなた用が3本表示に復帰→リロード後も#calAsk再出現なし→既存ユーザーの再チェックは回帰なし→" + soudanNote;
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
