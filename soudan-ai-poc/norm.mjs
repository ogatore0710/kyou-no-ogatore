// 相談室の正規化・赤旗判定・候補インテント絞り込み。
// index.html の sdNorm / sdRedFlagHit / sdScoreIntents を忠実移植（挙動を一致させる）。
import { KB } from "./data.mjs";

// index.html:2709 sdNorm と同一
export function norm(s) {
  s = String(s == null ? "" : s).toLowerCase();
  s = s.replace(/[Ａ-Ｚａ-ｚ０-９]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0xfee0));
  s = s.replace(/[ァ-ヶ]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0x60));
  return s.replace(/[^0-9a-zぁ-ゖー一-鿿々]/g, "");
}

// index.html:2842 sdRedFlagHit と同一（n は正規化済み前提）
export function redFlagHit(n) {
  const rf = KB.redFlags;
  if (!rf || !Array.isArray(rf.kw)) return false;
  n = n.replace(/寝転|ねころ|寝ころ|ねっころ|寝っこ/g, ""); // index.html sdRedFlagHit と同期
  for (const k0 of rf.kw) {
    const k = norm(k0);
    if (k.length >= 2 && n.indexOf(k) >= 0) return true;
  }
  return false;
}

// index.html sdRedFlagKind と同一（n は正規化済み前提）。赤旗ヒット後にどちらの文面(answer=症状系/answerState=状態系)を
// 返すかの判定。stateKw(妊娠・術後等の状態語=kwの部分集合)にしかヒットしていなければ"state"、症状語が1つでも
// ヒットすれば"symptom"(安全側優先。両方ヒット時も症状系を優先)。
export function redFlagKind(n) {
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

// index.html:3229 sdCrisisHit と同一（n は正規化済み前提。redFlagHitと違い「寝転」除去は無い）
export function crisisHit(n) {
  const c = KB.crisis;
  if (!c || !Array.isArray(c.kw)) return false;
  for (const k0 of c.kw) {
    const k = norm(k0);
    if (k.length >= 2 && n.indexOf(k) >= 0) return true;
  }
  return false;
}

// index.html:2847 sdScoreIntents と同一（n は正規化済み前提）
export function scoreIntents(n) {
  const out = [];
  for (const it of KB.intents) {
    let score = 0, firstHit = -1;
    (it.kw || []).forEach((k0, j) => {
      const k = norm(k0);
      if (k && n.indexOf(k) >= 0) {
        score += k.length;
        if (firstHit < 0) firstHit = j;
      }
    });
    if (score > 0) out.push({ it, score, firstHit });
  }
  out.sort((a, b) => (b.score - a.score) || (a.firstHit - b.firstHit));
  return out;
}

export function redFlagAnswer(n) {
  const rf = KB.redFlags || {};
  const kind = redFlagKind(n || "");
  const mitate = (kind === "state" && rf.answerState) ? rf.answerState : (rf.answer || "");
  return {
    empathy: rf.empathy || "その状態はまず専門家にみてもらうのが安心だよ。",
    mitate,
    keizoku: "",
    videoId: null,
    needsReferral: true,
    source: "redflag",
  };
}
