#!/usr/bin/env node
// 図鑑コピー変更のヘッドレス実測検証（テーザー差異・フレーバー表示・toku/seasonヒント不変・横スクロールなし）
const fs = require("fs");
const net = require("net");
const http = require("http");
const path = require("path");
const { spawn } = require("child_process");

const REPO = "/Users/ryunosuke/Claude/kyou-no-ogatore";

function findChrome() {
  const home = process.env.HOME || "";
  const candidates = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    path.join(home, "Applications/Google Chrome.app/Contents/MacOS/Google Chrome"),
  ];
  for (const p of candidates) { try { fs.accessSync(p, fs.constants.X_OK); return p; } catch (e) {} }
  throw new Error("Chrome not found");
}
function getFreePort() {
  return new Promise((resolve) => {
    const srv = net.createServer();
    srv.listen(0, "127.0.0.1", () => { const p = srv.address().port; srv.close(() => resolve(p)); });
  });
}
function startServer(port) {
  return spawn("python3", ["-m", "http.server", String(port), "--bind", "127.0.0.1", "--directory", REPO], { stdio: ["ignore", "ignore", "ignore"] });
}
function waitForServer(port, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  return new Promise((resolve, reject) => {
    (function probe() {
      const req = http.get({ host: "127.0.0.1", port, path: "/index.html", timeout: 1000 }, (res) => { res.resume(); if (res.statusCode === 200) resolve(); else retry(); });
      req.on("error", retry); req.on("timeout", () => req.destroy());
      function retry() { if (Date.now() > deadline) reject(new Error("server timeout")); else setTimeout(probe, 150); }
    })();
  });
}

async function main() {
  const puppeteer = require("puppeteer-core");
  const port = await getFreePort();
  const server = startServer(port);
  process.on("exit", () => { try { server.kill("SIGKILL"); } catch (e) {} });
  await waitForServer(port, 8000);
  const browser = await puppeteer.launch({ executablePath: findChrome(), headless: "new", args: ["--window-size=430,900"] });
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 390, height: 844 });
    const url = "http://127.0.0.1:" + port + "/index.html";
    await page.goto(url, { waitUntil: "load" });
    await page.waitForFunction(() => window.__kyonoBoot === true, { timeout: 10000 });

    // ---- (a) 未取得レア2枚のティーザーが互いに異なる ----
    const teaseCheck = await page.evaluate(() => {
      const d = getDexStatus();
      const notGot = d.rare.filter(r => !r.got);
      const first2 = notGot.slice(0, 2);
      return { count: notGot.length, hints: first2.map(r => ({ key: r.key, hint: r.hint })) };
    });
    console.log("teaseCheck:", JSON.stringify(teaseCheck, null, 2));

    // ---- (b) 取得済みカードに名前+フレーバーが表示される(記録を偽装) ----
    const gotCheck = await page.evaluate(() => {
      // rare_neko = NORMAL_CARDS.length(20) + 0 = 20
      const rot = { "2026-01-01": 20 };
      localStorage.setItem("kyono_rotAssign", JSON.stringify(rot));
      const d = getDexStatus();
      const nekoItem = d.rare.find(r => r.key === "rare_neko");
      const html = dexCellHtml(nekoItem);
      return { got: nekoItem.got, flavor: nekoItem.flavor, name: nekoItem.name, html };
    });
    console.log("gotCheck:", JSON.stringify(gotCheck, null, 2));

    // ---- (c) 未取得toku/seasonのカウントダウン/日付ヒントが従来どおり ----
    const tokuSeasonCheck = await page.evaluate(() => {
      const d = getDexStatus();
      const toku = d.toku.filter(t => !t.got).slice(0, 2).map(t => t.hint);
      const season = d.season.filter(s => !s.got).slice(0, 2).map(s => s.hint);
      return { toku, season };
    });
    console.log("tokuSeasonCheck:", JSON.stringify(tokuSeasonCheck, null, 2));

    // ---- normal未取得の新共通ヒント ----
    const normalCheck = await page.evaluate(() => {
      const d = getDexStatus();
      const notGot = d.normal.filter(n => !n.got);
      return { count: notGot.length, sample: notGot.slice(0, 2).map(n => n.hint) };
    });
    console.log("normalCheck:", JSON.stringify(normalCheck, null, 2));

    // ---- (d) 図鑑モーダルの横スクロールなし（複数tierをgot状態にしてflavorが多く表示された状態で確認） ----
    const modalScrollCheck = await page.evaluate(() => {
      // 多くのレアをgot状態にしてflavor行を大量表示させ、レイアウト崩れを厳しめに検証
      const rot = {};
      for (let i = 0; i < 20; i++) rot["2026-01-" + String(i + 1).padStart(2, "0")] = 20 + i; // 全レアgot
      for (let i = 0; i < 20; i++) rot["2026-02-" + String(i + 1).padStart(2, "0")] = i; // 全ノーマルgot
      localStorage.setItem("kyono_rotAssign", JSON.stringify(rot));
      // toku達成のためtotalを1900に
      localStorage.setItem("kyono_streak2", JSON.stringify({ dates: [], count: 0, total: 1900 }));
      // season: 各season keyの期間内日付をdatesに1件ずつ入れてgot化
      const st = { dates: [], count: 0, total: 1900 };
      openDex();
      const modal = document.getElementById("dexModal");
      const body = document.getElementById("dexBody");
      return {
        modalScrollWidth: modal.scrollWidth,
        modalClientWidth: modal.clientWidth,
        bodyScrollWidth: body.scrollWidth,
        bodyClientWidth: body.clientWidth,
        boxScrollWidth: document.getElementById("dexModalBox").scrollWidth,
        boxClientWidth: document.getElementById("dexModalBox").clientWidth,
        summary: document.getElementById("dexSummary").textContent,
      };
    });
    console.log("modalScrollCheck:", JSON.stringify(modalScrollCheck, null, 2));

    // ---- バナー(56px幅4枚)のレイアウト実測 ----
    const bannerCheck = await page.evaluate(() => {
      renderDexBanner();
      const wrap = document.getElementById("dexBannerSamples");
      const cells = Array.from(wrap.querySelectorAll(".dex-cell"));
      return {
        wrapScrollWidth: wrap.scrollWidth,
        wrapClientWidth: wrap.clientWidth,
        cellCount: cells.length,
        cells: cells.map(c => {
          const hint = c.querySelector(".dex-hint");
          const rect = c.getBoundingClientRect();
          return {
            name: c.querySelector(".dex-name").textContent,
            hintText: hint ? hint.textContent : null,
            hintScrollHeight: hint ? hint.scrollHeight : null,
            cellWidth: rect.width,
            hintScrollWidth: hint ? hint.scrollWidth : null,
            hintClientWidth: hint ? hint.clientWidth : null,
          };
        }),
      };
    });
    console.log("bannerCheck:", JSON.stringify(bannerCheck, null, 2));

    await browser.close();
  } catch (e) {
    await browser.close();
    throw e;
  }
}

main().catch(e => { console.error(e); process.exit(1); });
