#!/usr/bin/env node
// ネイティブ移植 Step 0: エクスポート/インポート契約のfixtureを採取する（マスタープラン§6 Step 0・
// 査読指摘「Step 3検収の元ネタ供給が宙に浮いていた」への対応）。
// scripts/smoke.js と同じpuppeteer基盤でindex.htmlをロードし、localStorageに既知の kyono_* 状態を
// 直接注入してから、本番の buildExportString() をそのまま呼んで実出力を回収する（ロジックは複製しない）。
// 得られた "KYONO1:..." 文字列は、このスクリプト自身がNode側で独立にデコードして
// seedした値と一致することも確認する（エンコード契約の自己検証）。
//
// 出力:
//   scripts-native/out/export-fixture.json         — 実際のbuildExportString()出力(KYONO1:...文字列)
//   scripts-native/out/export-fixture-expected.json — 期待値(count/total/キー集合など。Step 3検収で使う)
// 使い方: node scripts-native/gen-export-fixture.mjs
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
const OUT_DIR = path.join(__dirname, "out");

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
  console.error("[gen-export-fixture] Chrome が見つかりませんでした。SMOKE_CHROME=/path/to/Chrome で指定してください。");
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

// 既知seedデータ。実データを持つキー(記録・カード等)+ネイティブが使わない未使用キー2つ(a2hs2/homehint_next)を
// あえて混ぜ、「未知キーもインポート/エクスポートでパススルー保全されること」(マスタープラン§2-2 4行目)の
// 検証材料にする。20キー(cnt上限50に対して余裕あり)。
const SEED = {
  streak2: { dates: ["2026-07-20", "2026-07-21", "2026-07-22", "2026-07-23", "2026-07-24", "2026-07-25"], count: 6, total: 6 },
  daylog: { "2026-07-25": { v: "CyWthETY73s", t: "テスト動画", c: 6 } },
  memos: { "2026-07-25": "エクスポートfixtureのテストメモ" },
  reach: [{ d: "2026-07-25", lv: 4 }],
  type: { key: "momo", worry: "katakori", at: "2026-07-25" },
  freeze2: { used: [], month: "2026-07" },
  chapters: 1,
  rotAssign: { "2026-07-20": 5 },
  fd: 1,
  fdday: "2026-07-25",
  tourpend: 0,
  tourseen: 1,
  calseen: 1,
  onboarded: 1,
  mode_manual: { m: "mine", d: "2026-07-25" },
  anchor: "asa",
  theme: "auto",
  bigtext: true,
  // 以下2つはネイティブ側では未使用の想定キー(A2HS導線関連)。インポート/エクスポートで
  // 消えずに往復することを確認する対象(マスタープラン§2-2「未知キーはパススルー保全」)。
  a2hs2: 1,
  homehint_next: 7,
};

async function main() {
  const chromePath = findChrome();
  const puppeteer = require("puppeteer-core");
  const port = await getFreePort(8898);
  const server = startServer(port);
  const killServer = () => { try { server.kill("SIGKILL"); } catch (e) { /* already dead */ } };
  try {
    await waitForServer(port, 15000);
    const browser = await puppeteer.launch({ executablePath: chromePath, headless: "new", args: ["--no-sandbox"] });
    try {
      const page = await browser.newPage();
      page.setDefaultTimeout(15000);
      await page.goto(`http://127.0.0.1:${port}/index.html`, { waitUntil: "load" });

      await page.evaluate((seed) => {
        localStorage.clear();
        for (const k in seed) localStorage.setItem("kyono_" + k, JSON.stringify(seed[k]));
      }, SEED);
      await page.reload({ waitUntil: "load" });

      const exportString = await page.evaluate(() => buildExportString());

      // ---- Node側で独立デコードして自己検証(エンコード契約の確認。buildExportString内の実装は複製しない
      //      ＝index.htmlの実装をそのまま呼んだ「結果の文字列」を、標準API(atob/decodeURIComponent)だけで
      //      読み戻せることを確認するだけ) ----
      if (!exportString.startsWith("KYONO1:")) throw new Error(`prefix不一致: ${exportString.slice(0, 20)}`);
      const decoded = JSON.parse(decodeURIComponent(escape(atob(exportString.slice(7)))));
      if (decoded.v !== 1) throw new Error(`v不一致: ${decoded.v}`);
      const gotKeys = Object.keys(decoded.data).sort();
      const wantKeys = Object.keys(SEED).map((k) => "kyono_" + k).sort();
      if (JSON.stringify(gotKeys) !== JSON.stringify(wantKeys)) {
        throw new Error(`キー集合が一致しない\n want=${JSON.stringify(wantKeys)}\n got =${JSON.stringify(gotKeys)}`);
      }
      for (const k of Object.keys(SEED)) {
        const got = JSON.parse(decoded.data["kyono_" + k]);
        if (JSON.stringify(got) !== JSON.stringify(SEED[k])) {
          throw new Error(`値が一致しない(kyono_${k})\n want=${JSON.stringify(SEED[k])}\n got =${JSON.stringify(got)}`);
        }
      }

      const expected = {
        note: "gen-export-fixture.mjsでseedした既知状態の期待値。Step 3検収(kyono-store.json往復インポート)で使う。",
        streak2_count: SEED.streak2.count,
        streak2_total: SEED.streak2.total,
        daylog_keyCount: Object.keys(SEED.daylog).length,
        keys: Object.keys(SEED).map((k) => "kyono_" + k).sort(),
        keyCount: Object.keys(SEED).length,
        passThroughOnlyKeys: ["kyono_a2hs2", "kyono_homehint_next"],
        seed: SEED,
      };

      fs.mkdirSync(OUT_DIR, { recursive: true });
      fs.writeFileSync(path.join(OUT_DIR, "export-fixture.json"), JSON.stringify({ exportString }, null, 2));
      fs.writeFileSync(path.join(OUT_DIR, "export-fixture-expected.json"), JSON.stringify(expected, null, 2));
      console.log(`export-fixture.json 生成: ${exportString.length}文字 / キー${expected.keyCount}個・自己デコード検証OK`);
    } finally {
      await browser.close();
    }
  } finally {
    killServer();
  }
}

main().catch((e) => { console.error(e); process.exit(1); });
