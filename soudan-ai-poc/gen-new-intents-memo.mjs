import { writeFileSync, readFileSync } from "node:fs";
const props = JSON.parse(readFileSync(new URL("./new-intents.raw.json", import.meta.url), "utf8")).proposals;
let m = "# 相談室 新インテント 4件 提案（要・本人監修 / 2026-07-11）\n\n";
m += "> 精度ループ＋総点検で炙り出した未着地トピックを、Fableが監修レディに設計→alanが現行KBで検証。\n";
m += "> 全4件 safety:true（受診導線あり）＝**追加は本人監修のあと**。動画は全部 videos.js 実在確認済み。\n\n";
m += "## 検証まとめ\n";
m += "- 動画4本すべて実在。referCases も狙い通り着地（危険例は既存の赤旗が先に拾って受診案内）。\n";
m += "- 445ケース回帰: 377→376（唯一の差は『指がこわばって』が新②へ再ルーティング＝より正しい・設計も移管推奨）。\n";
m += "- bare『腕』等も既存を奪わない（腕が上がらない→四十肩・二の腕→ninoude・膝の外→hiza 等 全て保持）。\n\n---\n";
for (const p of props) {
  m += "\n## " + p.cluster + "  （id: `" + p.id + "` / " + p.decision + "）\n";
  m += "**チップ**: " + p.chip + "\n\n";
  m += "**共感**: " + p.empathy + "\n\n";
  m += "**見立て**（←ここが本人監修の主対象）:\n> " + p.mitate + "\n\n";
  m += "**続け方**: " + p.keizoku + "\n\n";
  m += "**動画**: `" + p.videoId + "` " + (p.videoTitle || "") + "\n\n";
  if (p.safety) m += "**⚠ 受診の線引き（エビデンス）**: " + (p.safetyNote || "") + "\n\n";
  m += "**kw**: " + p.kw.map((k) => "`" + k + "`").join(" ") + "\n\n";
  m += "**設計理由（要点）**: " + (p.rationale || "").slice(0, 500) + "\n\n---\n";
}
m += "\n## 監修してほしい点\n";
m += "1. 4件の新インテント、この内容（特に見立てと受診の線引き）で追加してOK?\n";
m += "2. ②指の関節: 既存 yubimukumi から「指がこわば」を本インテントへ移管する運用でOK?（こわばり=関節系／むくみ=指輪・手のむくみ系に整理）\n";
m += "3. ③頭皮: 複合相談『デスクワークの夕方に頭のてっぺん』は現状 deskwork 着地。許容? それとも touhi 優先にする?\n";
m += "4. 各インテントの動画1本の割り当て、これでいい?（設計理由に3本構成の推奨あり）\n";
writeFileSync(new URL("../NEW-INTENTS-PROPOSAL.md", import.meta.url), m);
console.log("NEW-INTENTS-PROPOSAL.md 生成: " + m.length + "字 / " + props.length + "件");
