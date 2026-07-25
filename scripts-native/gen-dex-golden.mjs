#!/usr/bin/env node
// ネイティブ移植 Step 7a: 図鑑(getDexStatus)の中間値ゴールデンを採取する（マスタープラン§6 Step 7a）。
// getDexStatus()はDOM操作を含まないプレーンなJS関数だが、index.html内のトップレベルスコープに
// 定義されており単体では切り出せないため、gen-card-golden.mjsと同じpuppeteer基盤で実ページを
// ロードし、本番の関数をそのままpage.evaluateで呼び出して結果を回収する(ロジックの再実装はしない)。
//
// rotAssign初期状態・streak2のseed範囲はgen-card-golden.mjsと完全に同一(2026-06-01〜2026-07-25の
// 連続55日)にして、同じ断面から両方のゴールデンを再現できるようにする。この範囲は多くの
// normal/rareカードのgot=trueへ実際に到達するため、図鑑のロック/アンロック判定の検証に十分な母数になる。
//
// 出力: scripts-native/out/dex-golden.json
// 使い方: node scripts-native/gen-dex-golden.mjs
import fs from "node:fs";
import net from "node:net";
import http from "node:http";
import path from "node:path";
import { spawn } from "node:child_process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const REPO = path.resolve(__dirname, "..");
const OUT = path.join(__dirname, "out", "dex-golden.json");

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
  console.error("[gen-dex-golden] Chrome が見つかりませんでした。SMOKE_CHROME=/path/to/Chrome で指定してください。");
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
      await page.evaluate((dates) => {
        localStorage.clear();
        localStorage.setItem("kyono_streak2", JSON.stringify({ dates, count: dates.length, total: dates.length }));
      }, dateRange);
      await page.reload({ waitUntil: "load" });

      const result = await page.evaluate(() => {
        const d = getDexStatus();
        const slim = (items) => items.map((it) => ({
          tier: it.tier, key: it.key ?? null, name: it.name, got: it.got,
        }));
        return {
          toku: slim(d.toku), season: slim(d.season), rare: slim(d.rare), normal: slim(d.normal),
        };
      });

      const manifest = {
        note: "rotAssign初期状態=空localStorage(legacyRotPosバックフィル経路)。streak2.datesは2026-06-01〜2026-07-25の連続55日をseed(gen-card-golden.mjsと同一断面)。",
        seedDateRangeStart: dateRange[0],
        seedDateRangeEnd: dateRange[dateRange.length - 1],
        ...result,
      };
      fs.mkdirSync(path.dirname(OUT), { recursive: true });
      fs.writeFileSync(OUT, JSON.stringify(manifest, null, 2));
      const gotCount = [...result.toku, ...result.season, ...result.rare, ...result.normal].filter((x) => x.got).length;
      const totalCount = result.toku.length + result.season.length + result.rare.length + result.normal.length;
      console.log(`dex-golden.json 生成: toku=${result.toku.length} season=${result.season.length} rare=${result.rare.length} normal=${result.normal.length} (got ${gotCount}/${totalCount})`);
    } finally {
      await browser.close();
    }
  } finally {
    killServer();
  }
}

main().catch((e) => { console.error(e); process.exit(1); });
