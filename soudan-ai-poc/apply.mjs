// 採用kwを ../soudan-kb.js の各インテントの kw 配列末尾に追記する（検証済み前提）。
import { readFileSync, writeFileSync } from "node:fs";
import { norm } from "./norm.mjs";

const kbUrl = new URL("../soudan-kb.js", import.meta.url);
let text = readFileSync(kbUrl, "utf8");
const KB = new Function(text + "\nreturn SOUDAN_KB;")();
const keptKw = JSON.parse(readFileSync(new URL("./" + (process.env.RESULT || "loop-result.json"), import.meta.url), "utf8")).keptKw || {};

const existing = {};
for (const it of KB.intents) existing[it.id] = new Set((it.kw || []).map(norm));

let applied = 0, intentsTouched = 0, misses = [];
for (const [id, kws] of Object.entries(keptKw)) {
  const add = kws.filter((k) => !existing[id].has(norm(k)));
  if (!add.length) continue;
  const idEsc = id.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const re = new RegExp('(\\{ id: "' + idEsc + '",[\\s\\S]*?kw: \\[[^\\]]*)\\]');
  if (!re.test(text)) { misses.push(id); continue; }
  const tail = "," + add.map((k) => JSON.stringify(k)).join(",");
  text = text.replace(re, (m, p1) => p1 + tail + "]");
  applied += add.length; intentsTouched++;
}

writeFileSync(kbUrl, text);
console.log(`反映: ${applied}語 / ${intentsTouched}インテント` + (misses.length ? " ⚠ 未反映id: " + misses.join(",") : ""));

// 反映後のファイルが構文的に読めるか & 語数増加を確認
const KB2 = new Function(readFileSync(kbUrl, "utf8") + "\nreturn SOUDAN_KB;")();
const before = KB.intents.reduce((a, it) => a + (it.kw || []).length, 0);
const after = KB2.intents.reduce((a, it) => a + (it.kw || []).length, 0);
console.log(`kw総数: ${before} → ${after} (+${after - before})`);
