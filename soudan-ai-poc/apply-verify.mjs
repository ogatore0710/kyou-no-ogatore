// 採用kwを実ファイルへ反映する前に、実KBで二重検証する。
// ① 280ケースで baseline→final を再現 & 既存正着地の非退行
// ② 全インテントの代表kwが自分に着地し続けるか（追加kwが既存を奪ってないか）
import { readFileSync } from "node:fs";
import { norm } from "./norm.mjs";

const KB = new Function(readFileSync(new URL("../soudan-kb.js", import.meta.url), "utf8") + "\nreturn SOUDAN_KB;")();
const result = JSON.parse(readFileSync(new URL("./loop-result.json", import.meta.url), "utf8"));
const keptKw = result.keptKw || {};
const regset = result.regressionSet || [];

const RF = ((KB.redFlags && KB.redFlags.kw) || []).map(norm).filter((k) => k.length >= 2);
function redFlag(n) { return RF.some((k) => n.indexOf(k) >= 0); }

function makeIntents(withAdds) {
  return KB.intents.map((it) => {
    const kw = (it.kw || []).slice();
    if (withAdds && keptKw[it.id]) for (const k of keptKw[it.id]) if (!kw.some((x) => norm(x) === norm(k))) kw.push(k);
    return { id: it.id, kw };
  });
}
function land(intents, text) {
  const n = norm(text);
  if (redFlag(n)) return "__rf__";
  let best = null, bs = 0, bf = 1e9;
  for (const it of intents) {
    let sc = 0, fh = -1;
    it.kw.forEach((k0, j) => { const k = norm(k0); if (k && n.indexOf(k) >= 0) { sc += k.length; if (fh < 0) fh = j; } });
    if (sc > 0 && (sc > bs || (sc === bs && fh < bf))) { bs = sc; bf = fh; best = it.id; }
  }
  return best;
}

const base = makeIntents(false);
const fin = makeIntents(true);

// ① 280ケース
let baseOk = 0, finOk = 0, regress = [];
for (const c of regset) {
  const b = land(base, c.text) === c.expectId;
  const f = land(fin, c.text) === c.expectId;
  if (b) baseOk++;
  if (f) finOk++;
  if (b && !f) regress.push(c.text);
}
console.log(`① 280ケース: baseline ${baseOk}/${regset.length}(${Math.round(baseOk / regset.length * 100)}%) → final ${finOk}/${regset.length}(${Math.round(finOk / regset.length * 100)}%)`);
console.log(`   既存正着地の非退行(baseline○→final✗): ${regress.length}件` + (regress.length ? " ⚠ " + regress.slice(0, 5).join(" / ") : " ✅"));

// ② 全インテント代表kw自着地チェック（追加で既存を奪ってないか）
let selfBreak = [];
for (const it of KB.intents) {
  const rep = (it.kw || [])[0];
  if (!rep) continue;
  const b = land(base, rep) === it.id;
  const f = land(fin, rep) === it.id;
  if (b && !f) selfBreak.push(`${it.id}(${rep})→${land(fin, rep)}`);
}
console.log(`② 代表kw自着地の非退行: ${selfBreak.length}件` + (selfBreak.length ? " ⚠ " + selfBreak.join(" / ") : " ✅"));

// 追加kwの内訳
const totalKw = Object.values(keptKw).reduce((a, v) => a + v.length, 0);
console.log(`\n採用kw: ${totalKw}語 / ${Object.keys(keptKw).length}インテント`);
