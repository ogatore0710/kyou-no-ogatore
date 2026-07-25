// ネイティブ移植 Step 4: cardRand(mulberry32・index.html:2674)の生出力をJS実行環境からそのまま
// 採取する。card-golden.jsonはcardPatternForの解決結果(pat/rotAssignPos等)しか記録しておらず、
// CARD_IMG_FROM以降の画像方式カードはcardRandを一切使わないため、cardRand自体のビット単位の
// 正しさ(Math.imul/>>>のUInt32折り返し)を検証するゴールデンが別途必要(norm-golden.jsonと同じ理由)。
// 出力: scripts-native/out/card-rand-golden.json
// 使い方: node scripts-native/gen-card-rand-golden.mjs
import { writeFileSync, mkdirSync } from "node:fs";

function cardRand(seed) {
  let s = seed >>> 0;
  return function () {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// 実運用で実際に使われる範囲(dateIdx)+境界値+桁あふれを起こしうる大きい値を混ぜる
const SEEDS = [0, 1, 20605, 20647, 20648, 20659, 4294967295, 2147483647, 2147483648];
const cases = SEEDS.map((seed) => {
  const rnd = cardRand(seed);
  const values = Array.from({ length: 8 }, () => rnd());
  return { seed, values };
});

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/card-rand-golden.json", import.meta.url), JSON.stringify(cases, null, 2));
console.log(`card-rand-golden.json 生成: ${cases.length}シード × 8値`);
