// 相談室マッチャの精度メーター（検査体系の第4層）。
// 凍結した難問セット（accuracy-set.json・loop1+loop2で445ケース）を現行 soudan-kb.js に通し、
// 期待インテントへの着地率を数字で出す。閾値未満なら非ゼロ終了（CI/回帰用）。
// 使い方: node soudan-ai-poc/accuracy-test.mjs
import { readFileSync } from "node:fs";
import { norm } from "./norm.mjs";

const THRESHOLD = 0.83; // これを下回ったら失敗（kw変更で精度が落ちたら気づける）

const KB = new Function(readFileSync(new URL("../soudan-kb.js", import.meta.url), "utf8") + "\nreturn SOUDAN_KB;")();
const SET = JSON.parse(readFileSync(new URL("./accuracy-set.json", import.meta.url), "utf8"));
const RF = ((KB.redFlags && KB.redFlags.kw) || []).map(norm).filter((k) => k.length >= 2);

function land(text) {
  const n = norm(text);
  if (RF.some((k) => n.indexOf(k) >= 0)) return "__rf__";
  let best = null, bs = 0, bf = 1e9;
  for (const it of KB.intents) {
    let sc = 0, fh = -1;
    (it.kw || []).forEach((k0, j) => { const k = norm(k0); if (k && n.indexOf(k) >= 0) { sc += k.length; if (fh < 0) fh = j; } });
    if (sc > 0 && (sc > bs || (sc === bs && fh < bf))) { bs = sc; bf = fh; best = it.id; }
  }
  return best;
}

let ok = 0;
const miss = [];
for (const c of SET) {
  if (land(c.text) === c.expectId) ok++;
  else miss.push(`「${c.text.slice(0, 24)}」→期待:${c.expectId}`);
}
const pct = ok / SET.length;
console.log(`相談室 精度メーター: ${ok}/${SET.length} (${Math.round(pct * 100)}%)  閾値${Math.round(THRESHOLD * 100)}%`);
if (miss.length && process.argv.includes("-v")) miss.slice(0, 30).forEach((m) => console.log("  ✗ " + m));
if (pct < THRESHOLD) { console.log("❌ 閾値未満"); process.exit(1); }
console.log("✅ pass");
