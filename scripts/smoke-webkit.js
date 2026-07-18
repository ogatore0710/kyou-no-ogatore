#!/usr/bin/env node
// WebKit実機スモークテスト: ユーザーの大半を占めるiPhone(Safari)相当のレンダリングエンジン(WebKit)で
// クリティカルパスを検証する。scripts/smoke.js（puppeteer-core=Chromium・24ステップ）の完全移植ではなく、
// 「Safari系エンジンでしか踏めない道」に絞ったサブセット版（記録カード生成のフォント待ちレース等）。
// 使い方: npm install（初回のみ・playwright-coreはdevDependency）→ npx playwright-core install webkit
//        （他マシンで初回実行するときに必要。ブラウザ本体は ~/Library/Caches/ms-playwright に入る）
//        → npm run smoke:webkit
// 環境変数: SMOKE_WEBKIT_PATH=WebKit実行ファイルのパス（既定の自動検出を上書き）
//           SMOKE_ROOT=配信ルートの上書き（既定はリポジトリ直下）
//           SMOKE_WEBKIT_REQUIRED=1 を立てると「未導入/起動失敗」を通常のスキップ(exit 0)ではなく
//           失敗(exit 1)にする（CI用。見せかけグリーンの防止。ローカルでは未設定=従来どおりスキップ）
// 注意: 静的チェックは scripts/qa.js（npm test）。Chromiumでの網羅的スモークは scripts/smoke.js（npm run smoke）。
//       こちらはWebKit固有の非互換・実挙動レースを狙い撃ちする専用コマンドで、アプリ本体は一切変更しない。
const fs = require("fs");
const net = require("net");
const http = require("http");
const path = require("path");
const { spawn } = require("child_process");
const { webkit } = require("playwright-core");

const REPO = path.resolve(__dirname, "..");
const ROOT = process.env.SMOKE_ROOT ? path.resolve(process.env.SMOKE_ROOT) : REPO;
const SHOT_DIR = path.join(REPO, ".smoke-webkit"); // .gitignore対象（even-syncに拾わせない）
const PREFERRED_PORT = 8802; // scripts/smoke.js(8801)と衝突しないよう別ポート
const REQUIRED = process.env.SMOKE_WEBKIT_REQUIRED === "1";

// ---- WebKit実行ファイル検出（本体ダウンロードはしない。Playwrightキャッシュの既存ブラウザを使う） ----
// 候補の優先順: 明示指定 → playwright-core自身の解決（Linux/macOSどちらの`npx playwright-core install`
// 配置にも追従する。既定パスはPlaywrightのバージョン間で変わりうるため、これが最も壊れにくい） →
// このマシンで過去に実測した既定パス（playwright-core側の解決が失敗した場合の最終フォールバック）
function findWebkit() {
  const home = process.env.HOME || "";
  const candidates = [process.env.SMOKE_WEBKIT_PATH];
  try { candidates.push(webkit.executablePath()); } catch (e) { /* 未導入時など。次の候補へ */ }
  candidates.push(path.join(home, "Library/Caches/ms-playwright/webkit-2311/Playwright.app/Contents/MacOS/Playwright"));
  for (const p of candidates.filter(Boolean)) {
    try { fs.accessSync(p, fs.constants.X_OK); return p; } catch (e) { /* 次の候補へ */ }
  }
  return null;
}

// ---- 空きポート確保 ----
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

// ---- 配信サーバー（python3 -m http.server を子プロセス起動。scripts/smoke.jsと同方式） ----
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
  const webkitPath = findWebkit();
  if (!webkitPath) {
    console.log("[smoke-webkit] WebKit実行ファイルが見つかりませんでした。" + (REQUIRED ? "SMOKE_WEBKIT_REQUIRED=1のため失敗にします。" : "このマシンではスキップします。"));
    console.log("  既定パス: ~/Library/Caches/ms-playwright/webkit-2311/Playwright.app/Contents/MacOS/Playwright");
    console.log("  導入するには: npx playwright-core install webkit");
    console.log("  別パスを使う場合は環境変数 SMOKE_WEBKIT_PATH=/path/to/Playwright を指定してください。");
    process.exit(REQUIRED ? 1 : 0);
  }

  const port = await getFreePort(PREFERRED_PORT);
  const server = startServer(port);
  const killServer = () => { try { server.kill("SIGKILL"); } catch (e) { /* already dead */ } };
  process.on("exit", killServer);
  process.on("SIGINT", () => { killServer(); process.exit(130); });
  process.on("SIGTERM", () => { killServer(); process.exit(143); });

  const url = "http://127.0.0.1:" + port + "/index.html";
  const soudanKbExists = fs.existsSync(path.join(ROOT, "soudan-kb.js"));
  let browser = null;
  const results = [];       // {name, ok, detail}
  const consoleErrors = []; // 失敗扱いのコンソールエラー（ローカル起因＋JS例外）
  const externalWarns = []; // 外部リソース(fonts/ytimg等)の読み込み失敗は警告のみ（オフライン耐性）
  let currentStep = "setup";

  function isExternal(u) {
    return /^https?:\/\//.test(u) && u.indexOf("127.0.0.1") === -1 && u.indexOf("localhost") === -1;
  }

  await waitForServer(port, 10000);
  // ブラウザの起動失敗は、既定では「未導入(パス不在)」と同じ扱いでスキップ(exit 0)にする。
  // 実機で確認済み: 未導入時と違い、キャッシュのWebKit実行ファイル自体は存在してもOS側の
  // 私有WebKit.framework(SPI)とのバイナリ非互換で起動プロセスがdyldレベルで即死するケースがある
  // （例: 2026-07-18実測、macOS 26.5.1でwebkit-2311が`dyld: Symbol not found:
  // _OBJC_CLASS_$__WKBrowserContext`で起動不能。--forceで再ダウンロードしても同じ症状＝キャッシュ
  // 破損ではなくOSのWebKit private SPIとの真の非互換）。これはテストコードでもアプリ本体でも
  // 直しようがない環境要因のため、CI/npm testの流れを止めないようにここもスキップ扱いにする
  // （ローカルmac向けの既定動作）。ただしSMOKE_WEBKIT_REQUIRED=1（CI向け）のときは
  // 「見せかけのグリーン」を避けるため、起動失敗をそのまま失敗(exit 1)として伝播させる。
  try {
    browser = await webkit.launch({ executablePath: webkitPath, headless: true });
  } catch (launchErr) {
    killServer();
    console.log("[smoke-webkit] WebKitの起動に失敗しました。" + (REQUIRED ? "SMOKE_WEBKIT_REQUIRED=1のため失敗にします。" : "このマシンではスキップします（exit 0）。"));
    console.log("  実行ファイルは存在しますが、OS側のWebKit私有API(SPI)と非互換の可能性があります。");
    console.log("  詳細: " + String(launchErr && launchErr.message ? launchErr.message : launchErr).split("\n")[0]);
    console.log("  対処案: 対応するmacOSバージョンで実行する／`npx playwright-core install webkit`で");
    console.log("  再取得しても改善しない場合はplaywright-coreの更新が必要な可能性があります（司令塔判断）。");
    process.exit(REQUIRED ? 1 : 0);
  }

  try {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 } }); // スマホ想定
    const page = await context.newPage();
    page.setDefaultTimeout(10000);
    page.on("dialog", (d) => d.accept().catch(() => {}));
    page.on("pageerror", (err) => {
      consoleErrors.push("[" + currentStep + "] pageerror: " + String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const loc = msg.location() || {};
      const at = loc.url || "";
      const text = msg.text();
      if (isExternal(at) && /Failed to load resource|net::ERR|error loading/i.test(text)) {
        externalWarns.push("[" + currentStep + "] " + text + " (" + at + ")");
      } else if (!soudanKbExists && at.indexOf("soudan-kb.js") !== -1) {
        externalWarns.push("[" + currentStep + "] soudan-kb.js未着による404（想定内）: " + text);
      } else {
        consoleErrors.push("[" + currentStep + "] console.error: " + text + (at ? " (" + at + ")" : ""));
      }
    });

    const $text = (sel) => page.$eval(sel, (el) => el.textContent.trim());
    const visible = (sel) => page.waitForSelector(sel, { state: "visible" });
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

    // 1. フレッシュ起動→はじめてガイド(#welcome)表示→スキップ→ホーム
    await step("1-フレッシュ起動でwelcome表示→スキップでホーム", async () => {
      await page.goto(url, { waitUntil: "load" });
      await visible("#home");
      const boot = await page.evaluate(() => window.__kyonoBoot === true);
      if (!boot) throw new Error("__kyonoBoot が立っていない（起動スクリプト未完走）");
      await page.waitForFunction(
        () => { const w = document.getElementById("welcome"); return w && !w.classList.contains("hidden"); },
        null, { timeout: 5000 }
      );
      await page.click("#obSkipBtn");
      await page.waitForFunction(() => document.getElementById("welcome").classList.contains("hidden"));
      const onboarded = await page.evaluate(() => localStorage.getItem("kyono_onboarded"));
      if (!onboarded) throw new Error("スキップ後に kyono_onboarded が立っていない");
      return "boot marker OK / welcome表示→スキップ→onboarded=" + onboarded;
    });

    // 2. リロードしてもwelcomeは二度と出ない（kyono_onboardedガード）
    await step("2-はじめてガイドはリロードしても二度と出ない", async () => {
      await page.reload({ waitUntil: "load" });
      await visible("#home");
      const hidden = await page.evaluate(() => document.getElementById("welcome").classList.contains("hidden"));
      if (!hidden) throw new Error("リロード後にwelcomeが再表示されてしまった");
      return "リロード後もwelcomeはhiddenのまま";
    });

    // 3. かたさチェック5問を実ボタンクリックで完走→タイプ名が出る結果画面
    await step("3-かたさチェック完走と結果表示", async () => {
      await visible("#ckBtn");
      await page.click("#ckBtn");
      for (let i = 0; i < 5; i++) {
        await page.waitForFunction(
          (n) => {
            const el = document.getElementById("qnum");
            return el && el.textContent.indexOf("Q" + n) === 0;
          }, i + 1
        );
        await page.waitForSelector("#opts .opt:not([disabled])", { state: "visible" });
        await page.click("#opts .opt"); // 各問とも先頭の選択肢（決定的）
      }
      await visible("#result");
      const name = await $text("#rName");
      if (!name) throw new Error("結果画面にタイプ名が出ていない");
      return "タイプ=" + name;
    });

    // 4. ホームで「きょうやった！」→通算表示が更新される
    await step("4-きょうやった！で通算表示が更新される", async () => {
      await page.click("#tab-home");
      await visible("#doneBtn");
      await page.$eval("#doneBtn", (el) => el.scrollIntoView({ block: "center" }));
      const before = await $text("#streakNum");
      await page.click("#doneBtn");
      await page.waitForFunction(
        (prev) => document.getElementById("streakNum").textContent.trim() !== prev, before
      );
      const after = await $text("#streakNum");
      if (Number(after) !== Number(before) + 1) throw new Error("通算日数が " + before + "→" + after + "（+1でない）");
      return "通算 " + before + "日→" + after + "日";
    });

    // 5. メモ入力→保存
    await step("5-メモ入力→保存", async () => {
      await visible("#memoInput");
      await page.fill("#memoInput", "WebKitスモークテスト良好");
      await page.click("#memoBtn");
      await page.waitForFunction(() => {
        const el = document.getElementById("memoSaved");
        return el && el.textContent.indexOf("のこしました") !== -1;
      });
      const saved = await page.evaluate(() => {
        try { return (JSON.parse(localStorage.getItem("kyono_memos")) || {}); } catch (e) { return {}; }
      });
      const vals = Object.keys(saved).map((k) => saved[k]);
      if (vals.indexOf("WebKitスモークテスト良好") === -1) throw new Error("localStorageのメモに反映されていない");
      return "memoSaved表示・localStorage反映を確認";
    });

    // 6. 記録カード生成（本丸）: カード画像(canvas→img data URL)が10秒以内に非空で生成される。
    //    cardMaking（「カードをつくってます…」）が消えることも確認する（WebKitでのフォント待ちレースの実挙動検証）。
    await step("6-記録カード生成（10秒以内・cardMaking解消）", async () => {
      await page.$eval("#makeCardBtn", (el) => el.scrollIntoView({ block: "center" }));
      const t0 = Date.now();
      await page.click("#makeCardBtn");
      await visible("#cardModal");
      await page.waitForFunction(() => {
        const img = document.getElementById("cardImg");
        return img && img.src.indexOf("data:image/png") === 0 && img.src.length > 10000;
      }, null, { timeout: 10000 });
      const elapsedMs = Date.now() - t0;
      const makingHidden = await page.evaluate(() => {
        const mk = document.getElementById("cardMaking");
        return !mk || mk.classList.contains("hidden");
      });
      if (!makingHidden) throw new Error("カード画像は出たが#cardMakingが消えていない（つくってます…が残ったまま）");
      const len = await page.$eval("#cardImg", (img) => img.src.length);
      await page.click("#cardModal button.btn-line"); // とじる
      await page.waitForFunction(() => document.getElementById("cardModal").classList.contains("hidden"));
      return "data URL長=" + len + " (>10000) / 生成まで" + elapsedMs + "ms(<10000ms) / cardMaking解消OK";
    });

    // 7. 相談室を開く→カテゴリチップをタップ→回答吹き出しが表示される
    await step("7-相談室ゴールデンフロー", async () => {
      await page.click("#tab-home");
      await visible("#home");
      if (!soudanKbExists) {
        const hidden = await page.$eval("#soudanCard", (el) => el.classList.contains("hidden"));
        if (!hidden) throw new Error("KB未着なのに入口カードが表示されている");
        return "SKIP扱い: soudan-kb.js未着（入口カードが隠れていることは確認）";
      }
      await visible("#soudanCard");
      await page.click("#soudanCard .sec-head");
      await page.waitForFunction(() => !document.getElementById("soudanSheet").classList.contains("hidden"));
      await page.waitForFunction(() => document.querySelectorAll("#sdChips button").length > 0);
      const chipText = await page.evaluate(() => {
        const list = Array.prototype.slice.call(document.querySelectorAll("#sdChips button"));
        const c = list.filter((b) => b.textContent.indexOf("肩") !== -1)[0] || list[0];
        const t = c.textContent.trim();
        c.click();
        return t;
      });
      await page.waitForFunction(
        () => document.querySelectorAll("#sdLog .sd-row.oga .video").length >= 1, null, { timeout: 8000 }
      );
      const ogaCount = await page.$$eval("#sdLog .sd-row.oga:not(.sd-typing)", (a) => a.length);
      if (ogaCount < 1) throw new Error("回答吹き出しが出ていない");
      // シートを閉じておく（開いたままだと固定オーバーレイが後続ステップのタブクリックを吸ってしまう）
      await page.goBack();
      await page.waitForFunction(() => document.getElementById("soudanSheet").classList.contains("hidden"));
      await visible("#home");
      return "チップ「" + chipText + "」タップ→回答吹き出し" + ogaCount + "件を確認→シートを閉じてホームへ";
    });

    // 8. 動画を探すタブでキーワード検索→結果が1件以上
    await step("8-動画を探すタブでキーワード検索", async () => {
      await page.click("#tab-search");
      await visible("#search");
      await page.fill("#q", "朝");
      await page.waitForFunction(() => {
        const el = document.getElementById("hitCount");
        return el && /\d+本/.test(el.textContent) && parseInt(el.textContent, 10) > 0;
      }, null, { timeout: 5000 });
      const hitCount = await $text("#hitCount");
      const shown = await page.$$eval("#vlist .video", (a) => a.length);
      if (shown < 1) throw new Error("検索結果カードが1件も描画されていない（表示件数=" + shown + "）");
      return "キーワード「朝」→" + hitCount + "・カード表示" + shown + "件";
    });

    // 最終確認: 全ステップを通してコンソールエラー総数が0であること
    // （各stepは自分の担当区間で既に検知済みだが、区間外＝setup/wiring由来の取りこぼしがないか最終念押し）
    if (consoleErrors.length !== 0) {
      results.push({ name: "9-コンソールエラー総数0", ok: false, detail: consoleErrors.join(" / ") });
      console.error("FAIL 9-コンソールエラー総数0 — " + consoleErrors.length + "件: " + consoleErrors.join(" / "));
    } else {
      results.push({ name: "9-コンソールエラー総数0", ok: true, detail: "0件" });
      console.log("PASS 9-コンソールエラー総数0 — 0件");
    }
  } catch (err) {
    console.error("[smoke-webkit] 致命的エラー: " + (err && err.stack ? err.stack : err));
    results.push({ name: "fatal", ok: false, detail: String(err && err.message ? err.message : err) });
  } finally {
    if (browser) { try { await browser.close(); } catch (e) { /* noop */ } }
    killServer();
  }

  const failed = results.filter((r) => !r.ok);
  console.log("");
  console.log("==== smoke-webkit 結果 " + (results.length - failed.length) + "/" + results.length + " PASS ====");
  if (externalWarns.length) {
    console.log("(外部リソース警告 " + externalWarns.length + "件・オフライン耐性のため失敗扱いにはしない)");
  }
  if (failed.length) {
    console.log("失敗:");
    failed.forEach((r) => console.log("  - " + r.name + ": " + r.detail));
    process.exit(1);
  }
  process.exit(0);
}

main().catch((err) => {
  console.error("[smoke-webkit] 予期しない例外: " + (err && err.stack ? err.stack : err));
  process.exit(1);
});
