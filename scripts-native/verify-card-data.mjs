// ネイティブ移植 Step 4: card-data.json + card-golden.json だけを入力に cardPatternFor 相当を
// Node上で再実装・再生し、card-golden.jsonの55件と一致するか確認する(verify-fixtures.mjsと同じ
// 「実装を先にNodeで検証してからSwift/Kotlinへ移植する」二段構え)。
// 使い方: node scripts-native/verify-card-data.mjs
import { readFileSync } from "node:fs";

const data = JSON.parse(readFileSync(new URL("./out/card-data.json", import.meta.url), "utf8"));
const golden = JSON.parse(readFileSync(new URL("./out/card-golden.json", import.meta.url), "utf8"));

function legacyRotPos(dateIdx) {
  return data.CARD_ROT_ORDER[((dateIdx % 50) + 50) % 50];
}
function cardFromRotPos(pos) {
  if (pos < data.NORMAL_CARDS.length) {
    const t = data.NORMAL_CARDS[pos];
    return { tier: "normal", name: t.name, key: null };
  }
  const r = data.RARE_CARDS[pos - data.NORMAL_CARDS.length];
  return { tier: "rare", name: r.name, key: r.key };
}
function cardSeasonPick(ds) {
  const mmdd = Number(ds.slice(5, 7) + ds.slice(8, 10));
  for (const s of data.SEASON_CARDS) if (s.ws && mmdd >= s.ws && mmdd <= s.we) return { tier: "season", name: s.name, key: s.key };
  return null;
}
function dateIdxOf(ds) {
  return Math.floor((new Date(ds).getTime() + 9 * 3600 * 1000) / 86400000);
}
// rotAssign初期状態=空localStorage(Step0の仕様どおり)からのensureRotAssign相当
function ensureRotAssign(dates, total) {
  const rot = {};
  const off = Math.max(0, (total || 0) - dates.length);
  const sorted = dates.slice().sort();
  sorted.forEach((ds, i) => {
    const di = dateIdxOf(ds);
    if (di < data.CARD_IMG_FROM) return;
    const eff = i + 1 + off;
    if (data.TOKU_CARDS[eff]) return;
    if (data.MILESTONES.includes(eff)) return;
    if (cardSeasonPick(ds)) return;
    rot[ds] = legacyRotPos(di);
  });
  return rot;
}
function cardRotPick(ds, rot) {
  let pos = rot[ds];
  if (pos == null) {
    const seq = Object.keys(rot).length;
    pos = data.CARD_ROT_ORDER[seq % 50];
    rot[ds] = pos;
  }
  return cardFromRotPos(pos);
}
function cardPatternFor(ds, effTotal, dateIdx, rot) {
  if (dateIdx < data.CARD_IMG_FROM) return null;
  const tk = data.TOKU_CARDS[effTotal];
  if (tk) return { tier: "toku", name: tk.name, key: tk.key };
  if (data.MILESTONES.includes(effTotal)) return null;
  const sp = cardSeasonPick(ds);
  if (sp) return sp;
  return cardRotPick(ds, rot);
}

// card-golden.jsonのnote通り: streak2.datesは2026-06-01〜2026-07-25の連続55日(=cases自体)。
// rotAssignは空から出発(ensureRotAssignで一括バックフィル)。
const dates = golden.cases.map((c) => c.ds);
const total = dates.length;
const rot = ensureRotAssign(dates, total);

let mismatches = [];
golden.cases.forEach((c, i) => {
  const effTotal = i + 1;
  const dateIdx = dateIdxOf(c.ds);
  const milestone = data.MILESTONES.includes(effTotal);
  const isImgEra = dateIdx >= data.CARD_IMG_FROM;
  const isThemeV2Era = dateIdx >= data.CARD_THEMES_V2_FROM;
  const pat = cardPatternFor(c.ds, effTotal, dateIdx, rot);
  const rotAssignPos = isImgEra && pat && (pat.tier === "normal" || pat.tier === "rare") ? rot[c.ds] : null;

  const got = { dateIdx, effTotal, milestone, isImgEra, isThemeV2Era, pat: pat ? { tier: pat.tier, name: pat.name, key: pat.key } : null, rotAssignPos };
  const want = { dateIdx: c.dateIdx, effTotal: c.effTotal, milestone: c.milestone, isImgEra: c.isImgEra, isThemeV2Era: c.isThemeV2Era, pat: c.pat, rotAssignPos: c.rotAssignPos };
  if (JSON.stringify(got) !== JSON.stringify(want)) {
    mismatches.push({ ds: c.ds, got, want });
  }
});

if (mismatches.length) {
  console.error(`不一致 ${mismatches.length}/${golden.cases.length} 件`);
  console.error(JSON.stringify(mismatches, null, 2));
  process.exit(1);
}
console.log(`card-golden.json ${golden.cases.length}/${golden.cases.length} 件一致`);
