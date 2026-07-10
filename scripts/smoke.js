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
    browser = await puppeteer.launch({
      executablePath: chromePath,
      headless: true,
      args: ["--no-first-run", "--no-default-browser-check", "--disable-extensions", "--hide-scrollbars"],
    });
    const page = await browser.newPage();
    page.setDefaultTimeout(10000);
    await page.setViewport({ width: 390, height: 844 }); // スマホ想定
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

    // 1. フレッシュ起動（新規プロファイル＝localStorage空）→ ホーム描画・コンソールエラー0
    await step("1-フレッシュ起動でホーム描画", async () => {
      await page.goto(url, { waitUntil: "load" });
      await visible("#home");
      const boot = await page.evaluate(() => window.__kyonoBoot === true);
      if (!boot) throw new Error("__kyonoBoot が立っていない（起動スクリプト未完走）");
      const fresh = await page.evaluate(() => localStorage.length);
      return "boot marker OK / localStorage初期キー数=" + fresh;
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

    // 3. きょうやった！→日数増加→メモ保存→記録カード生成
    await step("3-きょうやった！で日数が増える", async () => {
      await page.click("#tab-home");
      await visible("#doneBtn");
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
        () => document.getElementById("sdChips").textContent.indexOf("べつの悩み") !== -1, { timeout: 8000 }
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
