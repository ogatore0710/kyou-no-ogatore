# 発注: TestFlightビルド8の再開（中断からの再実行）

発注元: alan5（本人の常設GO「もんだい起きなければテストフライトまで仕上げて」2026-07-30夜）
背景: 先代appdevが2026-07-31 未明、ビルド番号bump commit(`870162a`)直後のアーカイブ〜アップロード中にセッション消滅。**Apple側にはビルド8は未着**（alan5がASC API `/v1/builds` 実照会で確認済み・最新はビルド7=VALID）。中途半端にアップロードされたビルドは存在しないので、まっさらに再実行してよい。

## 0. まず現状確認（成果が全部commit済みであることを自分の目で見る）

```bash
cd ~/Claude/kyou-no-ogatore
git log --oneline -15   # 昨夜の実装群(第1〜3波・ボタン・10分TTL・月修正)が全部載っていること
grep -n "CURRENT_PROJECT_VERSION" ios-native/KyouNoOgatore/KyouNoOgatore.xcodeproj/project.pbxproj | head -4
# → 4箇所とも 8 になっているはず(bumpは先代がcommit済み。7のままなら870162aを確認)
npm test                # 全green(459+ checks)
```

実装内容の把握は各REPORT（wave1〜3・button-standard-migration・soudan-10min-memory・segmoon-icon-fix）を読めば足りる。**再実装は一切不要。ビルドだけやる。**

## 1. アーカイブ→エクスポート→アップロード

ビルド7と同じ手順（`REPORT-C2-2026-07-30-testflight-build7.md`と、初回セットアップの`REPORT-C2-2026-07-28-testflight-*.md`群が正本）。署名・ExportOptions・altool/ASC APIキー（`~/Claude/ogatore-hub/secrets/asc-api.json`）も全部既存のまま使える。

## 2. ASC側の設定

- `version=8` が `processingState=VALID` になるまで待つ
- 既存内部グループ **`3b3f7a0b-3063-451d-acdd-404432f08a76`** に紐付け（**新規グループ作成禁止**）
- ⚠️ ビルド7の前例: アップロード直後のグループ紐付けは**Apple側インデックス遅延で404が20分続いた**。慌てず時間を置いて再試行
- `MARKETING_VERSION` は 1.0 のまま／App Store公開メタデータ（appStoreVersions・スクショ・説明文・審査提出）には触らない

## 3. whatsNew(ja)「テストする内容」— 以下をそのまま設定

```
①「きょうやった！」のあと ねぎらいの言葉が見えるようになりました 記録カードはひと呼吸おいて出ます（記念日・季節・レアのカードは ぽんとはずんで登場）
②ホームの「きょうの1本」に「あなた用」「あさ」「よる」の切りかえがつきました
③相談室の会話が消えなくなりました とじても続きから 10分以内ならアプリを再起動しても残っています
④ボタンの押しごこちを直しました 押しかけて指をずらせばキャンセルできます
⑤はじめての使い方を直しました 会話が自動で上まで上がる・押すボタンはいつも下に見える・練習の文字を減らしました
⑥部位（腰・肩など）や時間帯をえらぶボタンにイラストがつきました
⑦こまかい直しいろいろ：設定の「もどる」が来た画面にもどる・おしらせ時間の見出しと並び・暗いテーマで読めなかった文字・「この日の動画」が記録されるように（きょうから）・ひとことにっきに昔のメモの見かた案内 など
```

## 4. 報告

`REPORT-C2-2026-07-31-testflight-build8.md` にビルド7と同じ検収形式で（version=8/VALID・グループ紐付け・whatsNew設定・作業中に起きたことを正直に）。commit+ドア配達。
