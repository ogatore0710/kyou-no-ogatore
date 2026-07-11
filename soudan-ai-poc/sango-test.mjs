// 産後の線引き 回帰テスト（エビデンス: 産褥期6-8週・1ヶ月健診で軽い運動解禁・帝王切開は術後長め）。
// 受診(急性/早期) / 通常＋注意(慢性・時期不明) / 通常(産後無関係) の3分類を固定。
// 使い方: node soudan-ai-poc/sango-test.mjs
import { readFileSync } from "node:fs";
import { norm } from "./norm.mjs";

const KB = new Function(readFileSync(new URL("../soudan-kb.js", import.meta.url), "utf8") + "\nreturn SOUDAN_KB;")();
const RF = KB.redFlags.kw.map(norm).filter((k) => k.length >= 2);
const refer = (n) => RF.some((k) => n.indexOf(k) >= 0);
const sango = (n) => n.indexOf("産後") >= 0 || n.indexOf("出産後") >= 0;
function cls(raw) { const n = norm(raw); if (refer(n)) return "受診"; return sango(n) ? "通常＋注意" : "通常"; }

const CASES = [
  // 早期・急性 → 受診
  ["産後3週間で腰が痛い", "受診"], ["産後1ヶ月、まだ悪露がある", "受診"], ["出産直後で骨盤が痛い", "受診"],
  ["産褥期で恥骨が痛い", "受診"], ["産後すぐだけど肩こり", "受診"], ["退院したばかりで腰が", "受診"],
  ["帝王切開の傷がまだ痛い", "受診"], ["帝王切開で産んで2ヶ月お腹に力が入らない", "受診"], ["産後まもなくて体がだるい", "受診"],
  ["産後、大量出血があった", "受診"], ["悪露が止まらない", "受診"],
  // 妊娠 → 受診（不変）
  ["妊娠中で腰がつらい", "受診"],
  // 慢性・時期不明 → 通常＋注意
  ["産後から肩こりがひどい", "通常＋注意"], ["産後半年で腰が痛い", "通常＋注意"], ["産後1年、骨盤がゆるい", "通常＋注意"],
  ["産後3ヶ月 抱っこで背中バキバキ", "通常＋注意"], ["産後の骨盤ケアしたい", "通常＋注意"],
  // 産後無関係 → 通常（注意なし）
  ["デスクワークで肩こり", "通常"], ["階段で膝が痛い", "通常"], ["2歳児の抱っこで肩が", "通常"],
];

let pass = 0, fail = 0;
for (const [raw, want] of CASES) {
  const got = cls(raw);
  const ok = got === want; ok ? pass++ : fail++;
  if (!ok) console.log(`❌ 期待:${want} 実際:${got} ← ${raw}`);
}
console.log(`産後の線引き: ${pass}/${CASES.length} pass` + (fail ? " ❌" : " ✅"));
process.exit(fail ? 1 : 0);
