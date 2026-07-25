// ネイティブ移植 Step 0: redflag-safety-test.mjs のインライン60ケース + safety-fixes.raw.jsonの51ケースから
// 単一の safety-fixtures.json（111件）を生成する（マスタープラン§3-4手順1）。
//
// 抽出方法（重要・査読で指摘された「パース誤りで期待値が反転しても検出できない」への対策）:
// インライン60ケースは redflag-safety-test.mjs 内の非exportローカルconst配列
// （extra/chest/crisis/newFlags2026_07_14/round2_2026_07_14/round3_2026_07_14/attack2026_07_20/redFlagKindCases）
// であり import では取れない。正規表現でテキストを読み取る「手書きパーサ」は書かない。代わりに、
// 元ファイルのソースを実際にJSエンジンで実行し（import行と安全チェックのfor文だけを無害化）、
// 配列リテラルの値そのものをJSパーサ自身に解決させてから回収する。文字列の書き写し・正規表現の
// 取りこぼしが原因の期待値反転が起こりえない（コメント内の似た文字列を誤って拾う、といった事故もない）。
//
// 出力: scripts-native/out/safety-fixtures.json
// 形式: [{input, expect: "refer"|"normal"|"crisis"|"crisis-negative"|"state"|"symptom"}, ...] 111件
// 使い方: node scripts-native/gen-safety-fixtures.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

const testSrc = readFileSync(new URL("../soudan-ai-poc/redflag-safety-test.mjs", import.meta.url), "utf8");

// import文とKB読み込み・アサーションforループ群を無害化するため、実行前に以下を除去/置換する:
// 1. `import { ... } from "./norm.mjs";` → 何もしない4関数のスタブに置換（配列宣言には一切影響しない）
// 2. `const fixes = JSON.parse(readFileSync(...))...` の行 → 削除（safety-fixes.raw.jsonは別途直接読む）
// 3. 末尾の `fails.forEach(...)` / `console.log(...)` / `process.exit(...)` → 削除（実行しない）
// これらは「対象を除去/無害化するだけ」で、8個の配列宣言・その中身には一切触れない。
let src = testSrc;
// 冒頭2本のimport文(node:fs / ./norm.mjs)はどちらもトップレベルの`import ... from "...";`一行完結の
// 形なので、行頭からセミコロンまでを丸ごと削る(この形以外のimportが増えたら気づけるよう複数マッチではなく
// 行単位で明示的に2つとも消す)
src = src.replace(/^import\s*\{\s*readFileSync\s*\}\s*from\s*["']node:fs["'];\s*$/m, "");
src = src.replace(/^import\s*\{[^}]*\}\s*from\s*["']\.\/norm\.mjs["'];\s*$/m, "");
src = src.replace(/^const fixes = JSON\.parse\(readFileSync\([^;]*;\s*$/m, "");
// fixesを消費する`for (const f of fixes){...}`ブロックも(fixesが未定義になるため)無害化する。
// safety-fixes.raw.json由来の51件は本スクリプトが別途JSON.parseで直接読むので、ここでの処理は不要。
src = src.replace(/for \(const f of fixes\) \{[\s\S]*?\n\}\n/, "");
const stub =
  'function norm(s){return String(s==null?"":s);}\n' +
  "function redFlagHit(){return false;}\n" +
  "function crisisHit(){return false;}\n" +
  "function redFlagKind(){return null;}\n";
// fails.forEach以降(結果出力・プロセス終了)は配列抽出に不要なので丸ごと切り落とす
const tailIdx = src.indexOf("fails.forEach(");
if (tailIdx === -1) throw new Error("想定していた末尾(fails.forEach)が見つからない。redflag-safety-test.mjsの構造が変わっていないか確認して");
src = src.slice(0, tailIdx);

const ARRAY_NAMES = [
  "extra",
  "chest",
  "crisis",
  "newFlags2026_07_14",
  "round2_2026_07_14",
  "round3_2026_07_14",
  "attack2026_07_20",
  "redFlagKindCases",
];
const returnStmt = "\nreturn {" + ARRAY_NAMES.join(",") + "};\n";
const fn = new Function(stub + src + returnStmt);
const arrays = fn();

for (const name of ARRAY_NAMES) {
  if (!Array.isArray(arrays[name])) throw new Error(`配列 ${name} の抽出に失敗（undefined or not array）`);
}

const fixtures = [];
function pushBoolCases(arr, whenTrue, whenFalse) {
  for (const [input, want] of arr) fixtures.push({ input, expect: want ? whenTrue : whenFalse });
}
pushBoolCases(arrays.extra, "refer", "normal");
pushBoolCases(arrays.chest, "refer", "normal");
pushBoolCases(arrays.crisis, "crisis", "crisis-negative");
pushBoolCases(arrays.newFlags2026_07_14, "refer", "normal");
pushBoolCases(arrays.round2_2026_07_14, "refer", "normal");
pushBoolCases(arrays.round3_2026_07_14, "refer", "normal");
pushBoolCases(arrays.attack2026_07_20, "refer", "normal");
for (const [input, want] of arrays.redFlagKindCases) {
  if (want !== "state" && want !== "symptom") throw new Error(`redFlagKindCasesの期待値が想定外: ${want}`);
  fixtures.push({ input, expect: want });
}

const inlineCount = fixtures.length;
if (inlineCount !== 60) throw new Error(`インライン抽出件数が60でない(実測${inlineCount})。配列の増減がないか確認して`);

// safety-fixes.raw.json（referCases/normalCases・51件）はJSON.parseで直接読む(パース事故のリスクなし)
const fixesRaw = JSON.parse(readFileSync(new URL("../soudan-ai-poc/safety-fixes.raw.json", import.meta.url), "utf8")).result.fixes;
let fixesCount = 0;
for (const f of fixesRaw) {
  for (const c of f.referCases || []) { fixtures.push({ input: c, expect: "refer" }); fixesCount++; }
  for (const c of f.normalCases || []) { fixtures.push({ input: c, expect: "normal" }); fixesCount++; }
}
if (fixesCount !== 51) throw new Error(`safety-fixes.raw.json由来の件数が51でない(実測${fixesCount})`);

if (fixtures.length !== 111) throw new Error(`合計件数が111でない(実測${fixtures.length})`);

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/safety-fixtures.json", import.meta.url), JSON.stringify(fixtures, null, 2));
console.log(`safety-fixtures.json 生成: インライン${inlineCount}件 + raw.json${fixesCount}件 = 合計${fixtures.length}件`);
