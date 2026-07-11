// 相談室 赤旗の穴4件（夜間痛/発熱漢字形/転倒口語/吐き気頭痛）の回帰テスト。
// Fable設計の referCases→受診 / normalCases→通常 を固定。寝転誤爆回避も含む。
// 使い方: node soudan-ai-poc/redflag-safety-test.mjs
import { readFileSync } from "node:fs";
import { norm, redFlagHit } from "./norm.mjs";

const fixes = JSON.parse(readFileSync(new URL("./safety-fixes.raw.json", import.meta.url), "utf8")).result.fixes;

let pass = 0, fail = 0;
const fails = [];
for (const f of fixes) {
  for (const c of f.referCases || []) {
    if (redFlagHit(norm(c))) pass++; else { fail++; fails.push(`[${f.key}] 受診になるべき→ならず: ${c}`); }
  }
  for (const c of f.normalCases || []) {
    if (!redFlagHit(norm(c))) pass++; else { fail++; fails.push(`[${f.key}] 巻き込むな→受診に: ${c}`); }
  }
}
// 寝転の誤爆回避（エンジン1行の固定）
const extra = [
  ["寝転んでできるストレッチはありますか", false],
  ["ねころんでやりたい", false],
  ["寝転がろうとして転んだ", true],
  ["昨日転んで腰を打った", true],
];
for (const [c, want] of extra) {
  if (redFlagHit(norm(c)) === want) pass++; else { fail++; fails.push(`[寝転] ${c} → 期待:${want ? "受診" : "通常"}`); }
}

fails.forEach((m) => console.log("❌ " + m));
console.log(`赤旗の穴4件+寝転: ${pass}/${pass + fail} pass` + (fail ? " ❌" : " ✅"));
process.exit(fail ? 1 : 0);
