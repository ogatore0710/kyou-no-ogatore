# D7 画面の向きを縦固定 完了報告

## やったこと
- Android: `AndroidManifest.xml`の`MainActivity`に`android:screenOrientation="portrait"`追加。
- iOS: `project.pbxproj`の`INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone`をPortrait
  のみに変更(Debug/Release両方)。`_iPad`キーは無変更(スコープ外指示どおり)。

## 検証
- Android: エミュレータで`settings put system accelerometer_rotation 0`+
  `user_rotation 1`(強制横向き)を送っても画面が縦のまま変わらないことをスクリーンショットで
  確認。
- iOS: ビルド後のInfo.plistを直接確認。`UISupportedInterfaceOrientations~iphone`は
  `Portrait`のみ、`~ipad`は`Portrait/PortraitUpsideDown/LandscapeLeft/LandscapeRight`の
  4方向のまま(無変更)であることを確認。

## 回帰
- Android: `assembleDebug test --rerun-tasks` → 267件・失敗0(D5バッチ完了時と同数、
  今回の変更で増減なし)。
- iOS: シミュレータ向け・実機宛(`generic/platform=iOS` + `-allowProvisioningUpdates`)
  ビルド両方成功。

## D5との関係
縦固定にしても、システムのフォントサイズ変更・言語変更・プロセス再生成では引き続き
Activity/シーンが再生成されるため、D5(`screen`/相談室会話/クイズ回答途中の
`rememberSaveable`化)は無駄にならない。

以上でD7完了。次はTASK-C2-2026-07-28-testflight-internal.mdへ進む。
