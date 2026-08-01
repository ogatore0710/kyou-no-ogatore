# 完了報告: ビルド14 A部(即修正4件)

`TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md` A部の4件すべて実装・検証完了。
iOS/Android双方に適用済み。ビルドはこの報告のゲート通過後に着手します。

## A-1: 5択質問の色被りを解消(5色目追加)

4色パレット(154/48/28/320)の巡回(`i % 4`)により、5択の質問(オンボの「いちばん
気になるのは？」等)で1番目と5番目が同色になっていた(本人スクショ: 肩こり・首と
とくにないが同じ緑)。既存4色の色相分散に馴染む5色目(青系・色相約200)を追加し、
参照側も`palette.count`基準のインデックスに変更(4色でも5色でも自動対応)。ライト/
ダーク両方・iOS/Android両方に適用。

証拠: `ios-native/verify/build14-a1-5color/`, `android-native/verify/build14-a1-5color/`
(5択設問で5色すべてが別色になることを確認)

## A-2: チャット末尾の固定CTAが最後の吹き出しを隠す修正

「かたさチェックをはじめる」等の固定フッターCTAが、最後のbot吹き出しに被って
読めない不具合を修正。スクロールコンテンツの下端にCTA高さぶん(約100pt/dp)の
インセットを確保し、自動スクロール着地位置もこの分だけ手前で止まるように変更。
両OS。

証拠: `ios-native/verify/build14-a2-cta-inset/`, `android-native/verify/build14-a2-cta-inset/`
(最後の吹き出しがCTAに隠れず全文読めることを確認)

## A-3: 練習ブロックの見出し文言変更

fdGuide初回練習ブロック(結果画面)の見出し「きょうはこの1本だけでOK！」を
「きょうは練習してみよう」に変更。動画カード内バッジ「きょうはこれ1本でOK！」は
対象外・据え置き(別要素・別文言のため誤って変更していないことをgrepで確認済み)。
両OS。

## A-4: ダークでアイコンが消えている不具合を根本修正(全数点検)

使い方タブ・マイ記録タブの手描き風Canvasアイコンが、ダークで背景に沈んでほぼ
見えなくなっていた不具合。

**原因調査の結果、alan5の「当たり」どおりでした**: 全アイコン共通の描画部品
`KyonoIconGlyph`の輪郭線が、呼び出し側(`KyonoSectionHeader`等)がfill/accentを
正しくテーマ変数で渡していたにもかかわらず、線そのものだけ常に固定値`0x3A3A35`
(墨色)でストロークされていました。これはタブバーアイコンで過去に一度直したのと
同じ欠落(`tabbarStrokeOff`)が、汎用アイコン部品側には未適用のまま残っていた
ものです。呼び出し側(GuideView/MyRecordView等)は変更不要でした。

`KyonoIconGlyph`内の固定色を、テーマ環境から取得した`colors.ink`(ライト:
0x3A3A35/ダーク:0xF2EDE1)に置き換えました。

**使い方タブ・マイ記録タブの全アイコンを両テーマで棚卸し**した結果、この1箇所の
修正で全アイコンが解消することを確認しました(個別に色指定していた箇所は無く、
すべて共通部品`KyonoIconGlyph`経由だったため)。

証拠: `ios-native/verify/build14-a4-dark-icons/`, `android-native/verify/build14-a4-dark-icons/`
(使い方タブの目次見出しカード・FAQ導線ボタン、マイ記録タブの続けた記録・カード
図鑑、それぞれダークで全アイコンが判読できることを確認)

## 検証

各タスクのコミット時にすべて実施済み:
```
npm test → 459 checks green(各段階で確認)
Android ./gradlew testDebugUnitTest assembleDebug → BUILD SUCCESSFUL
iOS xcodebuild build(generic/platform=iOS Simulator) → BUILD SUCCEEDED
```
一時XCUITest・pbxproj編集は既存作法どおり検証後に削除・`git diff --stat`で0行を
確認してからコミット。

## コミット一覧

```
dd24b8b fix: 5択質問の色被りを解消(5色目を追加)(A-1・A-2のCTAインセットも同時)
bbb3788 test: A-2チャット末尾CTAインセット修正の検証証拠を追加
d9ee77d fix: 練習ブロック見出し文言を変更(A-3)
5b4bb35 fix: ダークでアイコンが消える不具合を根本修正(使い方・マイ記録タブ全数)(A-4)
```

## 検収チェック

- [x] A-1: 5色すべて別色・alan5ダークスクショ検分用の証拠あり
- [x] A-2: 最後の吹き出しが全文読める
- [x] A-3: 見出し文言変更・バッジは据え置き確認
- [x] A-4: 使い方・マイ記録タブの全アイコンをダークで棚卸し・alan5検分用の証拠あり
- [x] iOS/Android両OS適用
- [x] npm test 459 checks green
- [x] Android/iOSビルド成功
- [ ] alan5ゲート → ビルド14着手(13→14・既存グループ・公開メタデータ不可触・
      sw.js版数上げない・ASC裏取り報告)。whatsNewはalan5が渡す予定

B部(Fable5視点全体監査)は別報告(`REPORT-C2-2026-08-01-5lens-audit.md`)にて。

以上、A部4件すべて完了です。ゲートよろしくお願いします。
