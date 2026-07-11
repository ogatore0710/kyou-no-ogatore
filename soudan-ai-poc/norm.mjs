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

export function redFlagAnswer() {
  const rf = KB.redFlags || {};
  return {
    empathy: rf.empathy || "その状態はまず専門家にみてもらうのが安心だよ。",
    mitate: rf.answer || "",
    keizoku: "",
    videoId: null,
    needsReferral: true,
    source: "redflag",
  };
}
