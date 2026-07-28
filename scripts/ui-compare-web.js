// UI/UX比較用: Web版PWAを Androidエミュレータと同一解像度で撮る。
// 使い方: node scripts/ui-compare-web.js  → .uiux-compare/web/*.png
// ネイティブ側は adb screencap で同じタブを撮り、左右に並べて見比べる。
// 目的: 「Web版に追いついていない」を感覚でなく画像で詰めるため(2026-07-28 本人指摘)。
// 解像度はエミュレータ(1080x2400 / density 420 = 411x914dp @2.625)に合わせてある。
// 目的: ネイティブ版と1:1で並べて、見た目の差を目で確かめるため。
const http = require("http");
const fs = require("fs");
const path = require("path");
const puppeteer = require("puppeteer-core");

const REPO = "/Users/ryunosuke/Claude/kyou-no-ogatore";
const OUT = path.join(REPO, ".uiux-compare", "web"); // .gitignore対象
const PORT = 8842;
const CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const MIME = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css",
  ".json": "application/json", ".png": "image/png", ".jpg": "image/jpeg",
  ".svg": "image/svg+xml", ".woff2": "font/woff2", ".ico": "image/x-icon" };

function serve() {
  return new Promise((res) => {
    const s = http.createServer((req, rep) => {
      let p = decodeURIComponent(req.url.split("?")[0]);
      if (p === "/") p = "/index.html";
      const f = path.join(REPO, p);
      if (!f.startsWith(REPO) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) {
        rep.writeHead(404); return rep.end("nf");
      }
      rep.writeHead(200, { "Content-Type": MIME[path.extname(f)] || "application/octet-stream" });
      fs.createReadStream(f).pipe(rep);
    });
    s.listen(PORT, () => res(s));
  });
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const server = await serve();
  const browser = await puppeteer.launch({
    executablePath: CHROME, headless: "new",
    args: ["--no-sandbox", "--force-device-scale-factor=2.625", "--hide-scrollbars"],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 411, height: 914, deviceScaleFactor: 2.625, isMobile: true, hasTouch: true });

  // 記録あり・オンボ済みの状態を仕込む(ネイティブ側と条件を揃える)
  await page.goto(`http://localhost:${PORT}/index.html`, { waitUntil: "networkidle2" });
  await page.evaluate(() => {
    const today = new Date(Date.now() - 3 * 3600 * 1000);
    const iso = (d) => d.toISOString().slice(0, 10);
    const dates = [];
    for (let i = 5; i >= 1; i--) dates.push(iso(new Date(today.getTime() - i * 86400000)));
    localStorage.setItem("kyono_onboarded", "true");
    localStorage.setItem("kyono_streak2", JSON.stringify({ dates, count: 5, total: 33 }));
    localStorage.setItem("kyono_bigtext", "true");
  });
  await page.reload({ waitUntil: "networkidle2" });
  await new Promise((r) => setTimeout(r, 2500));

  const tabs = [["home", "ホーム"], ["history", "マイ記録"], ["search", "動画を探す"], ["guide", "使い方"]];
  for (const [id, label] of tabs) {
    await page.evaluate((t) => { if (typeof switchTab === "function") switchTab(t); }, id);
    await new Promise((r) => setTimeout(r, 1800));
    await page.evaluate(() => window.scrollTo(0, 0));
    await new Promise((r) => setTimeout(r, 400));
    await page.screenshot({ path: path.join(OUT, `web-${id}.png`) });
    console.log("撮影:", label, "→", `web-${id}.png`);
  }

  await browser.close();
  server.close();
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
