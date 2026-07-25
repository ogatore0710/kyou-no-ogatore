// ネイティブ移植 Step 0: リプレイ検証（マスタープラン§3-4手順1）。
// 生成済みの out/soudan-kb.json と out/safety-fixtures.json 「だけ」を入力に、
// norm/crisisHit/redFlagHit/redFlagKind の4アルゴリズムをここで再実行し、111/111一致を確認する。
// soudan-ai-poc/norm.mjs は import { KB } from "./data.mjs" を経由するため
// 「本番soudan-kb.jsを実際に検証していない」問題があった(査読で発覚)。本スクリプトは
// data.mjsを一切importせず、soudan-kb.jsから生成したsoudan-kb.jsonだけを正として検証することで、
// (a)gen-safety-fixtures.mjsの抽出が正しいこと (b)soudan-kb.jsonの生成が正しいこと の両方を
// 実挙動で一度に固定する。
//
// アルゴリズム本体はsoudan-ai-poc/norm.mjsの実装を意図的に複製している(KBの取得元だけが違う)。
// これは重複ではなく「証明対象を切り替えるための独立実装」であり、Step 0の目的そのもの。
//
// 使い方: node scripts-native/verify-fixtures.mjs
import { readFileSync } from "node:fs";

const KB = JSON.parse(readFileSync(new URL("./out/soudan-kb.json", import.meta.url), "utf8"));
const fixtures = JSON.parse(readFileSync(new URL("./out/safety-fixtures.json", import.meta.url), "utf8"));

// ---- norm.mjs の4関数を、KB取得元だけ差し替えて忠実複製 ----
function norm(s) {
  s = String(s == null ? "" : s).toLowerCase();
  s = s.replace(/[Ａ-Ｚａ-ｚ０-９]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0xfee0));
  s = s.replace(/[ァ-ヶ]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0x60));
  return s.replace(/[^0-9a-zぁ-ゖー一-鿿々]/g, "");
}
function redFlagHit(n) {
  const rf = KB.redFlags;
  if (!rf || !Array.isArray(rf.kw)) return false;
  n = n.replace(/寝転|ねころ|寝ころ|ねっころ|寝っこ/g, "");
  for (const k0 of rf.kw) {
    const k = norm(k0);
    if (k.length >= 2 && n.indexOf(k) >= 0) return true;
  }
  return false;
}
function redFlagKind(n) {
  const rf = KB.redFlags;
  if (!rf || !Array.isArray(rf.kw)) return null;
  const stateSet = new Set((rf.stateKw || []).map(norm));
  n = n.replace(/寝転|ねころ|寝ころ|ねっころ|寝っこ/g, "");
  let stateHit = false;
  for (const k0 of rf.kw) {
    const k = norm(k0);
    if (k.length >= 2 && n.indexOf(k) >= 0) {
      if (stateSet.has(k)) stateHit = true;
      else return "symptom";
    }
  }
  return stateHit ? "state" : null;
}
function crisisHit(n) {
  const c = KB.crisis;
  if (!c || !Array.isArray(c.kw)) return false;
  for (const k0 of c.kw) {
    const k = norm(k0);
    if (k.length >= 2 && n.indexOf(k) >= 0) return true;
  }
  return false;
}

let pass = 0, fail = 0;
const fails = [];
for (const { input, expect } of fixtures) {
  const n = norm(input);
  let ok;
  switch (expect) {
    case "refer": ok = redFlagHit(n) === true; break;
    case "normal": ok = redFlagHit(n) === false; break;
    case "crisis": ok = crisisHit(n) === true; break;
    case "crisis-negative": ok = crisisHit(n) === false; break;
    case "state": ok = redFlagHit(n) === true && redFlagKind(n) === "state"; break;
    case "symptom": ok = redFlagHit(n) === true && redFlagKind(n) === "symptom"; break;
    default: throw new Error(`未知のexpect種別: ${expect}`);
  }
  if (ok) pass++; else { fail++; fails.push(`[${expect}] ${input}`); }
}

fails.forEach((m) => console.log("❌ " + m));
console.log(`リプレイ検証(soudan-kb.json+safety-fixtures.jsonのみを入力): ${pass}/${pass + fail} pass` + (fail ? " ❌" : " ✅"));
process.exit(fail ? 1 : 0);
