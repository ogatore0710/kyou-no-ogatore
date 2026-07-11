// 相談室 赤旗の穴4件を soudan-kb.js に反映（本人承認済み前提）。
// redFlags.kw への追加＋「転倒」除去、zutsuu.kw から吐き気除去、koureisha.kw に予防語追加。
// index.html のエンジン1行(寝転除去)は別途 Edit で入れる。
import { readFileSync, writeFileSync } from "node:fs";
import { norm } from "./norm.mjs";

const kbUrl = new URL("../soudan-kb.js", import.meta.url);
let text = readFileSync(kbUrl, "utf8");
const KB0 = new Function(text + "\nreturn SOUDAN_KB;")();
const fixes = JSON.parse(readFileSync(new URL("./safety-fixes.raw.json", import.meta.url), "utf8")).result.fixes;

const addRedflag = [];
for (const f of fixes) for (const k of f.addRedflagKw || []) if (!addRedflag.includes(k)) addRedflag.push(k);
const removeRedflag = ["転倒"];
const removeFromZutsuu = ["吐き気", "はきけ"];
const addKoureisha = ["転倒予防", "転倒防止", "転ばない", "転びやすく"];

function tokens(arrContent) { return arrContent.match(/"(?:[^"\\]|\\.)*"/g) || []; }
function raw(tok) { return JSON.parse(tok); }

// --- redFlags.kw ---
{
  // 実配列は [ の直後が引用符。先頭コメントの kw:[...] を誤爆しないよう \s*" を要求
  const re = /(redFlags:\s*\{\s*kw:\s*\[)(\s*"[^\]]*)(\])/;
  const mm = text.match(re);
  if (!mm) throw new Error("redFlags.kw が見つからない");
  let toks = tokens(mm[2]);
  const existing = new Set(toks.map((t) => norm(raw(t))));
  toks = toks.filter((t) => !removeRedflag.includes(raw(t)));
  for (const k of addRedflag) if (!existing.has(norm(k))) toks.push(JSON.stringify(k));
  text = text.replace(re, "$1" + toks.join(",") + "$3");
}

// --- 指定インテントの kw を編集 ---
function editIntentKw(id, { remove = [], add = [] }) {
  const re = new RegExp('(\\{ id: "' + id + '",[\\s\\S]*?kw: \\[)([^\\]]*)(\\])');
  const mm = text.match(re);
  if (!mm) throw new Error(id + " の kw が見つからない");
  let toks = tokens(mm[2]);
  const existing = new Set(toks.map((t) => norm(raw(t))));
  toks = toks.filter((t) => !remove.includes(raw(t)));
  for (const k of add) if (!existing.has(norm(k))) toks.push(JSON.stringify(k));
  text = text.replace(re, "$1" + toks.join(",") + "$3");
}
editIntentKw("zutsuu", { remove: removeFromZutsuu });
editIntentKw("koureisha", { add: addKoureisha });

writeFileSync(kbUrl, text);

// 検証: 構文＋差分
const KB1 = new Function(readFileSync(kbUrl, "utf8") + "\nreturn SOUDAN_KB;")();
const rf0 = KB0.redFlags.kw.length, rf1 = KB1.redFlags.kw.length;
console.log(`redFlags.kw: ${rf0} → ${rf1} (追加${addRedflag.length}・除去${removeRedflag.length})`);
console.log(`zutsuu.kw: ${KB0.intents.find((i) => i.id === "zutsuu").kw.length} → ${KB1.intents.find((i) => i.id === "zutsuu").kw.length}`);
console.log(`koureisha.kw: ${KB0.intents.find((i) => i.id === "koureisha").kw.length} → ${KB1.intents.find((i) => i.id === "koureisha").kw.length}`);
console.log("転倒 除去確認:", KB1.redFlags.kw.includes("転倒") ? "❌まだある" : "✅");
console.log("吐き気 zutsuu除去確認:", KB1.intents.find((i) => i.id === "zutsuu").kw.includes("吐き気") ? "❌" : "✅");
