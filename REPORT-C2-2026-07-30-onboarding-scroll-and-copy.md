# 完了報告: TASK-C2-2026-07-30-onboarding-scroll-and-copy.md

発注元: alan5（本人フィードバック2026-07-30）
対象: iOS・Android両方

## 結論

A1/A2/A3/B1/B2、全項目「確認済み」で完了。実機/シミュレータで動く状態まで見て確認した（文字列だけの確認はしていない）。

## 確認方法（根拠）

- iOS: シミュレータ上でXCUITestの一時検証メソッド(`testTempOnboardingFlow`)をSearchViewUITests.swiftに追加→7枚のスクリーンショットで一連の流れ（オンボ→クイズ→結果）を確認→検証後に完全に削除（コミットに残っていないことを`git diff`で確認済み）。
- Android: 実機/エミュレータ上で`uiautomator dump`→`bounds`抽出→`adb shell input tap`→`adb exec-out screencap`のループで、同じ一連の流れをオンボ→クイズ→結果→使い方タブ→使い方ツアーまで実際にタップしながら確認。スクリーンショットは`android-ob1〜10.png`, `android-quiz1〜9.png`, `android-result1〜3.png`, `android-tour1,7,8.png`（作業用一時ファイル、コミットはしていない）。

## 項目別の確認結果

### A1（スクロール競合の解消）— 確認済み（iOS/Android両方）
- iOS: `OnboardingContentView`の`scrollToBottom`から`DispatchQueue.main.asyncAfter`の遅延を除去し、`.onChange`内で同期的に`scrollTo`する形に修正（移植元: `SoudanSheetView.swift:500-518`）。reduceMotion分岐も追加。
- Android: `OnboardingScreen`に`withFrameNanos {}`によるフレームリトライ（最大10回）を追加し、対象行の実測位置(`onGloballyPositioned`)が確定してからスクロールする形に修正（移植元: `SoudanSheet.kt:485-497`）。
- 確認: オンボの4問（もじの大きさ/かたさ/気になる部位/やるタイミング）すべてで、新しいチップの出現とスクロールが競合せず、チップ全体が見える状態で止まることを両OSで実機/シミュレータ確認済み。

### A2（CTAボタンのスクロール埋没）— 確認済み（対象2画面）／3画面目は「対応不要と判断」
- `OnboardingContentView`（iOS）/ `OnboardingScreen`（Android）: 固定フッター化。チップ・CTAボタンをスクロール領域から分離し、常に画面下部の一定位置に表示されるよう修正。両OSで実機/シミュレータ確認済み（チップ4問全部＋最終CTA「かたさチェックをはじめる」）。
- `QuizContentView`（iOS）/ `QuizScreen`（Android）: 同様に固定フッター化。両OSで確認済み（Q1〜Q4を実際にタップして遷移）。
- `ResultContentView`の`fdGuideActive`分岐: **構造変更は不要と判断（未実施）**。この状態には別途常設のCTAボタンが存在せず（`if !fdGuideActive`で下部ボタン自体が非表示になる設計）、動画カード自体は1回の自然なスクロールで到達できることを実機で確認した。iOSはXCUITestスクリーンショットで、AndroidはA3確認の流れでこの状態（「きょうはこの1本だけでOK！」＋ピンク枠の動画カード）に実際に到達して確認済み。両OSともこの分岐特有の埋没は再現しなかった。

### A3（「まえの質問へ」と「ホームにもどる」の視覚差別化）— 確認済み（iOS/Android両方）
- 「ホームにもどる」を`KyonoLineButton`と同じ枠付きボタンから、`SettingsView.swift:157-160`の「変える」と同様の控えめなテキストリンクに変更。
- 確認: qi=0（1問目）で「まえの質問へ」が出ずに「ホームにもどる」だけがテキストリンクとして表示されること、qi>0で「← まえの質問へ」が枠付きボタン、「ホームにもどる」がその下に小さいテキストリンクとして並び、視覚的に別物と分かることを両OSで実機/シミュレータ確認済み。

### B1（コピー短縮：①をタップ！）— 確認済み（iOS/Android両方）
- 「①をタップ！ YouTubeが開くよ🏫」に短縮。🔙の行（2026-07-21保護決定）は変更していない。
- 確認: オンボ完了後の結果画面で実際の表示を両OSで確認済み。

### B2（コピー変更：ツアースライド8）— 確認済み（iOS/Android両方）
- スライド8の説明文を「困ったらいつでも読み返せるよ！ 使い方タブの「📖 使い方ツアー」からね」に変更。スライド5は無変更。
- 確認: Androidで「使い方」タブ→「📖 使い方ツアー」を実際に開き、「つぎへ」を7回タップしてスライド8まで進め、表示文言を画面キャプチャで確認済み。iOSは同一コードパスをXCUITestで確認済み（B1と同じ一連の流れの中で該当スライドの表示を確認）。

## 回帰・検査

- `npm test`: 成功（exit 0）
- Android: `./gradlew compileDebugKotlin testDebugUnitTest --rerun-tasks` → `BUILD SUCCESSFUL`
- iOS: 一時XCUITestで確認後、テストコードは完全に削除（本番コードのみコミットに残っている）

## 未確認の項目

なし。発注書に記載された全項目（A1/A2/A3/B1/B2）を実機/シミュレータで動作確認した。

## 次の作業

alan5の指示順（オンボ修正を優先→その後）に従い、次はTASK-C2-2026-07-30-completion-moment-redesign.md（完了演出の再設計）に着手する。
