// B2調査用(2026-07-29): Web版drawCard()を実ブラウザで呼び、cardImgのdata URLをそのまま
// PNGとして保存する。ネイティブ版(CardCore/CardRenderer)と同じ条件(total=33,count=5)で
// 描画し、行間を並べて目で確認するため。
const fs = require("fs");
const path = require("path");
const http = require("http");
const puppeteer = require("puppeteer-core");

const REPO = "/Users/ryunosuke/Claude/kyou-no-ogatore";
const OUT = "/private/tmp/claude-501/-Users-ryunosuke-Claude-kyou-no-ogatore/e86261c8-edff-4623-ac2a-59e23f311837/scratchpad";
const PORT = 8843;
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
  const server = await serve();
  const browser = await puppeteer.launch({
    executablePath: CHROME, headless: "new",
    args: ["--no-sandbox", "--force-device-scale-factor=1"],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 500, height: 500 });
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
  await new Promise((r) => setTimeout(r, 1500));

  const dataUrl = await page.evaluate(() => {
    return new Promise((resolve) => {
      if (typeof drawCard === "function") {
        drawCard();
        setTimeout(() => resolve(document.getElementById("cardImg").src), 300);
      } else {
        resolve(null);
      }
    });
  });

  if (!dataUrl) {
    console.error("drawCard() not found or cardImg empty");
    process.exit(1);
  }
  const base64 = dataUrl.replace(/^data:image\/png;base64,/, "");
  fs.writeFileSync(path.join(OUT, "web_card_total33_count5.png"), Buffer.from(base64, "base64"));
  console.log("saved web_card_total33_count5.png");

  await browser.close();
  server.close();
})().catch((e) => { console.error("ERR", e.message, e.stack); process.exit(1); });
