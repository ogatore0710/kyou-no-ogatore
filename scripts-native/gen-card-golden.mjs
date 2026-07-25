#!/usr/bin/env node
// ネイティブ移植 Step 0: 記録カードの決定的ロジックの中間値ゴールデンを採取する（マスタープラン§6 Step 0・
// §2-4）。drawCard()自体はCanvas描画（ブラウザAPI）なのでNode単体では動かせないため、既存
// scripts/smoke.js と同じpuppeteer基盤（ローカルpython3サーバー+ヘッドレスChrome）でindex.htmlを
// 実際にロードし、本番の関数（cardPatternFor/ensureRotAssign等）をそのままpage.evaluateで呼び出して
// 中間値（テーマ/レア/pos/日付インデックス）を回収する。ロジックの再実装はしない（複製によるズレを避ける）。
//
// rotAssign初期状態の仕様（査読指摘で明記が必要とされた点）: 本スクリプトは実行のたびlocalStorageを
// 空にしてから開始する＝ensureRotAssign()が「空localStorage→dates配列を順に辿ってlegacyRotPosで
// バックフィル」する経路（index.html ensureRotAssign）を通る。これを仕様として固定する。
//
// 採取対象の日付: スナップショット基準日(2026-07-25)を終端とする55日間(2026-06-01〜2026-07-25)を
// 連続記録としてseedする。この範囲はCARD_IMG_FROM(2026-07-14)・CARD_THEMES_V2_FROM(2026-07-13)の
// 境界日を自然に含み、かつ55日分あるのでMILESTONES(3/4/7/14/21/30/50日目)の大半にも実際に到達する。
//
// 出力: scripts-native/out/card-golden.json
// 使い方: node scripts-native/gen-card-golden.mjs
import fs from "node:fs";
import net from "node:net";
import http from "node:http";
import path from "node:path";
import { spawn } from "node:child_process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url); // puppeteer-core(CommonJS)を読むためだけに使う
const REPO = path.resolve(__dirname, "..");
const OUT = path.join(__dirname, "out", "card-golden.json");

function findChrome() {
  const home = process.env.HOME || "";
  const candidates = [
    process.env.SMOKE_CHROME,
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    path.join(home, "Applications/Google Chrome.app/Contents/MacOS/Google Chrome"),
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
  ].filter(Boolean);
  for (const p of candidates) {
    try { fs.accessSync(p, fs.constants.X_OK); return p; } catch (e) { /* next */ }
  }
  console.error("[gen-card-golden] Chrome が見つかりませんでした。SMOKE_CHROME=/path/to/Chrome で指定してください。");
  process.exit(2);
}
function getFreePort(preferred) {
  return new Promise((resolve) => {
    const srv = net.createServer();
    srv.once("error", () => {
      const srv2 = net.createServer();
      srv2.listen(0, "127.0.0.1", () => { const p = srv2.address().port; srv2.close(() => resolve(p)); });
    });
    srv.listen(preferred, "127.0.0.1", () => { srv.close(() => resolve(preferred)); });
  });
}
function startServer(port) {
  return spawn("python3", ["-m", "http.server", String(port), "--bind", "127.0.0.1", "--directory", REPO], { stdio: ["ignore", "ignore", "ignore"] });
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
      req.on("error", () => { if (Date.now() > deadline) reject(new Error("server did not start")); else setTimeout(probe, 150); });
      req.on("timeout", () => req.destroy());
    })();
  });
}

// 連続55日(2026-06-01〜2026-07-25)の日付文字列配列を生成(固定断面。実行日時に依存しない)
function buildDateRange() {
  const dates = [];
  const d = new Date(Date.UTC(2026, 5, 1)); // 2026-06-01
  const end = new Date(Date.UTC(2026, 6, 25)); // 2026-07-25
  while (d.getTime() <= end.getTime()) {
    dates.push(d.toISOString().slice(0, 10));
    d.setUTCDate(d.getUTCDate() + 1);
  }
  return dates;
}

async function main() {
  const chromePath = findChrome();
  const puppeteer = require("puppeteer-core");
  const port = await getFreePort(8899);
  const server = startServer(port);
  const killServer = () => { try { server.kill("SIGKILL"); } catch (e) { /* already dead */ } };
  try {
    await waitForServer(port, 15000);
    const browser = await puppeteer.launch({ executablePath: chromePath, headless: "new", args: ["--no-sandbox"] });
    try {
      const page = await browser.newPage();
      page.setDefaultTimeout(15000);
      await page.goto(`http://127.0.0.1:${port}/index.html`, { waitUntil: "load" });

      const dateRange = buildDateRange();
      // streak2をseedする(rotAssignは意図的にセットしない=空状態でensureRotAssignのバックフィル経路を通す)
      await page.evaluate((dates) => {
        localStorage.clear();
        localStorage.setItem("kyono_streak2", JSON.stringify({ dates, count: dates.length, total: dates.length }));
      }, dateRange);
      await page.reload({ waitUntil: "load" });

      const result = await page.evaluate((dates) => {
        const rot = ensureRotAssign(); // 空localStorage→バックフィル経路を通した後のrotAssign
        const st = getStreakData();
        const off = Math.max(0, (st.total || 0) - st.dates.length);
        const out = [];
        for (const ds of dates) {
          const idx = st.dates.indexOf(ds);
          const effTotal = idx >= 0 ? idx + 1 + off : st.total;
          const dateIdx = Math.floor((new Date(ds).getTime() + 9 * 3600 * 1000) / 86400000);
          const pat = cardPatternFor(ds, effTotal, dateIdx);
          const milestone = MILESTONES.includes(effTotal);
          out.push({
            ds, dateIdx, effTotal, milestone,
            isImgEra: dateIdx >= CARD_IMG_FROM,
            isThemeV2Era: dateIdx >= CARD_THEMES_V2_FROM,
            pat: pat ? { tier: pat.tier || null, name: pat.name || null, key: pat.key || null } : null,
            rotAssignPos: Object.prototype.hasOwnProperty.call(rot, ds) ? rot[ds] : null,
          });
        }
        return { CARD_IMG_FROM, CARD_THEMES_V2_FROM, MILESTONES, cases: out };
      }, dateRange);

      const manifest = {
        note: "rotAssign初期状態=空localStorage(legacyRotPosバックフィル経路)。streak2.datesは2026-06-01〜2026-07-25の連続55日をseed。",
        seedDateRangeStart: dateRange[0],
        seedDateRangeEnd: dateRange[dateRange.length - 1],
        CARD_IMG_FROM: result.CARD_IMG_FROM,
        CARD_THEMES_V2_FROM: result.CARD_THEMES_V2_FROM,
        MILESTONES: result.MILESTONES,
        cases: result.cases,
      };
      fs.mkdirSync(path.dirname(OUT), { recursive: true });
      fs.writeFileSync(OUT, JSON.stringify(manifest, null, 2));
      const milestoneCount = result.cases.filter((c) => c.milestone).length;
      const imgEraCount = result.cases.filter((c) => c.isImgEra).length;
      console.log(`card-golden.json 生成: ${result.cases.length}日分（節目${milestoneCount}件・画像方式時代${imgEraCount}件）`);
    } finally {
      await browser.close();
    }
  } finally {
    killServer();
  }
}

main().catch((e) => { console.error(e); process.exit(1); });
