import http from "http";
import fs from "fs";
import path from "path";
import puppeteer from "puppeteer-core";

const REPO = "/Users/ryunosuke/Claude/kyou-no-ogatore";
const MIME = {".html":"text/html",".js":"text/javascript",".css":"text/css",".json":"application/json",".png":"image/png",".jpg":"image/jpeg",".svg":"image/svg+xml"};
const server = http.createServer((req,res)=>{
  let p = decodeURIComponent(req.url.split("?")[0]);
  if(p==="/") p="/index.html";
  const fp = path.join(REPO, p);
  fs.readFile(fp, (err,data)=>{
    if(err){ res.writeHead(404); res.end("no"); return; }
    const ext = path.extname(fp);
    res.writeHead(200, {"Content-Type": MIME[ext]||"application/octet-stream"});
    res.end(data);
  });
});
await new Promise(r=>server.listen(8802,r));

const chromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const browser = await puppeteer.launch({executablePath: chromePath, headless: "new"});
const page = await browser.newPage();
await page.setViewport({width:390, height:660});
page.on("pageerror", e=>console.log("PAGEERROR", e.message));
await page.goto("http://localhost:8802/index.html", {waitUntil:"domcontentloaded"});
await new Promise(r=>setTimeout(r,500));

// 相談室を開き、長いmitateを持つintentへ直接答えさせる（onboardingスキップ）
const result = await page.evaluate(async ()=>{
  try{ localStorage.setItem("kyono_seen_welcome","1"); }catch(e){}
  openSoudan();
  await new Promise(r=>setTimeout(r,300));
  // カテゴリタブを「からだの部位で」以外に切り替えてみて2段になり得るか幅を見る
  const catBox = document.getElementById("sdCatRow");
  sdRenderChips();
  await new Promise(r=>setTimeout(r,50));
  const catRect = catBox.getBoundingClientRect();
  const kb = sdKb();
  // 最も長いmitateを持つintentを探す
  let longest = null, longestLen = 0;
  for(const it of kb.intents){
    if(it.mitate && it.mitate.length > longestLen){ longestLen = it.mitate.length; longest = it; }
  }
  window.__reduced = true; // アニメ待ちを避ける
  sdAnswerIntent(longest, null);
  return { catHeight: catRect.height, longestId: longest.id, longestLen };
});
console.log("catRow height(px):", result.catHeight, "(2段なら約72px以上のはず)");
console.log("longest mitate intent:", result.longestId, result.longestLen, "chars");

// 中間状態: 長いmitate吹き出しが「現時点でのログ末尾」として現れた瞬間を捕まえて、
// その冒頭が可視領域内にあるか確認する（タイピングドット出現前の一瞬を狙う）
let midCheck = null;
for(let i=0;i<100;i++){
  midCheck = await page.evaluate(()=>{
    const log = document.getElementById("sdLog");
    const rows = [...log.querySelectorAll(".sd-row.oga:not(.sd-typing)")];
    const last = rows[rows.length-1];
    if(!last || last.textContent.length<=140) return null;
    const r=last.getBoundingClientRect(), lr=log.getBoundingClientRect();
    return { topRel: r.top-lr.top, height: r.height, logVisible: lr.height, text: last.textContent.slice(0,30) };
  });
  if(midCheck) break;
  await new Promise(r=>setTimeout(r,30));
}
console.log("mitate行が末尾として現れた瞬間の可視性:", JSON.stringify(midCheck));
if(midCheck){
  const mitateTopVisible = midCheck.topRel >= -2 && midCheck.topRel < midCheck.logVisible;
  console.log(mitateTopVisible ? "OK: mitateの冒頭も可視領域内" : "NG: mitateの冒頭が可視領域外");
}

// sdPendingが0に戻り、タイピング中インジケータが消えるまで待つ（最長8秒）
for(let i=0;i<80;i++){
  const done = await page.evaluate(()=>{
    return (typeof sdPending!=="undefined"?sdPending:0)===0 &&
      !document.querySelector("#sdLog .sd-typing");
  });
  if(done) break;
  await new Promise(r=>setTimeout(r,100));
}


const scrollInfo = await page.evaluate(()=>{
  const log = document.getElementById("sdLog");
  const rows = log.querySelectorAll(".sd-row.oga:not(.sd-typing)");
  const last = rows[rows.length-1];
  const lastRect = last.getBoundingClientRect();
  const logRect = log.getBoundingClientRect();
  return {
    scrollTop: log.scrollTop, scrollHeight: log.scrollHeight, clientHeight: log.clientHeight,
    lastRowTopRelativeToLog: lastRect.top - logRect.top,
    lastRowBottomRelativeToLog: lastRect.bottom - logRect.top,
    lastRowHeight: lastRect.height,
    logVisibleHeight: logRect.height,
    lastRowText: last.textContent.slice(0,40)
  };
});
console.log(JSON.stringify(scrollInfo, null, 2));

const topVisible = scrollInfo.lastRowTopRelativeToLog >= -2 && scrollInfo.lastRowTopRelativeToLog < scrollInfo.logVisibleHeight;
console.log(topVisible ? "OK: 最新コメントの冒頭が可視領域内" : "NG: 最新コメントの冒頭が可視領域外(画面上に見えていない)");

await browser.close();
server.close();
process.exit(topVisible?0:1);
