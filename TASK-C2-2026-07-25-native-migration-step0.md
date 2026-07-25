# タスク（C2/appdev向け）— ネイティブ移植 Step 0（断面固定・フィクスチャ/ゴールデン採取）

## 背景

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`をalan5が検収しOK（破綻・見落としなし。内部査読3レンズの指摘も確認済み）。マスタープラン§6の実装ステップに沿って、Sonnetによる量産移植を開始する。このタスクはその最初の一歩=**Step 0のみ**。

**Step 0はスクリプト・データ抽出作業のみで、本人のGUI操作もSwift/Kotlinコードも不要**（Xcodeプロジェクト作成等の本人操作ゲートはStep 1から。今回は対象外）。

## やること

`NATIVE-MIGRATION-MASTERPLAN-2026-07-25.md`の**§6 Step 0**セクションをそのまま実行する（作業内容・検収基準ともに同セクションが正本。以下は要点の再掲のみ）。

1. タグ`native-base-2026-07-25`付与・`ios-native/BASELINE.md`にコミットハッシュ記録。`ios-native/` `android-native/` `scripts-native/`を新設
2. `scripts-native/`配下にスクリプトを作成・実行:
   - `gen-safety-kb.mjs`（soudan-kb.js→soudan-kb.json）
   - `gen-safety-fixtures.mjs`（redflag-safety-test.mjsの非exportローカル配列群+safety-fixes.raw.jsonから111件のsafety-fixtures.jsonを生成。§3-4手順1）
   - `verify-fixtures.mjs`（soudan-kb.json+safety-fixtures.jsonのみを入力にnorm/crisisHit/redFlagHit/redFlagKindをNode実行→111/111一致のリプレイ検証）
   - `verify-kb-sync.mjs`（data.mjs⇔soudan-kb.jsのredFlags.kw/stateKw/crisis/answer文面のdeep-equal照合）
   - `gen-catalog.mjs`（videos.js/obu-feed.js→JSON）
   - normゴールデン採取（合成濁点・半角カナ・絵文字の3系統+§3-3の連結マッチ敵対ケース1〜2件。JS実出力を正として`node -e`で採取）
   - `gen-card-golden.mjs`（puppeteer・既存scripts/smoke.js基盤流用。rotAssign初期状態=空localStorageを仕様として明示指定した上で、過去30日+CARD_IMG_FROM前後境界日の中間値を採取）
   - `gen-export-fixture.mjs`（puppeteerでlocalStorageに既知状態をseed注入→buildExportString実出力をfixture化）

## 検収基準（マスタープラン§6 Step 0と同一）

- [ ] safety-fixtures.jsonが111件・`node redflag-safety-test.mjs`は111/111緑のまま
- [ ] リプレイ検証（verify-fixtures.mjs）がsoudan-kb.json+safety-fixtures.jsonのみで111/111一致
- [ ] verify-kb-sync.mjsでdata.mjsとsoudan-kb.jsが完全一致（**不一致ならその時点でalan5へ報告し、指示を待つこと。build-data.mjsの再生成はWeb側の変更に当たるため独断で行わない**）
- [ ] norm-golden.jsonに3系統+連結マッチ敵対ケースが含まれ、期待値がJS実出力
- [ ] card-golden.json（過去30日+境界日・rotAssign初期状態明記）とexport-fixture.json+期待値JSONが存在
- [ ] `npm test`442緑・`git status`でWeb配信対象ファイル（index.html/app-*.js/soudan-kb.js/videos.js/sw.js/manifest.json等）に変更なし

## やらないこと

- Step 1以降（Xcodeプロジェクト作成・Swift/Kotlin実装）は着手しない。Step 0完了→alan5への報告のみ
- Web版（PWA）側の配信ファイルは一切変更しない（`ios-native/` `android-native/` `scripts-native/`とBASELINE.mdのみ新設）
- verify-kb-sync.mjsで不一致が出た場合、build-data.mjsを勝手に再実行しない（Web側の生成物に触るため。alan5に報告し判断を仰ぐ）

## 報告

Step 0完了時、ドア配達で以下を含めること:
- 検収基準6項目それぞれのPASS/FAIL
- verify-kb-sync.mjsの結果（一致/不一致。不一致なら差分の要約）
- 消費トークンの概算
