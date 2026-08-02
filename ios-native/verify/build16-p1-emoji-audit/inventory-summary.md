# P-1: Apple絵文字全廃(UI文面) 全数grep棚卸し

## 対象範囲
- iOS: `KyouNoOgatore/`, `RecordCore/Sources`, `CardCore/Sources`, `WidgetCore/Sources`
- Android: `app/src/main/java/jp/ogatore/kyouno/`
- 除外(対象外・タスク文どおり): `QuizArt.swift`/`.kt`(かたさチェック図解のCanvas描画注釈)、
  `CardRenderer.swift`/`.kt`・`BragCardRenderer.swift`/`.kt`(記録カード画像内の描画文字)
- 除外(コメント行): `//`始まりの行は対象外(表示されないコード内メモのため)
- 判定基準: U+1F300-1FAFF・U+2600-26FF・U+2700-27BF・U+1F1E6-1F1FF(国旗)・U+2B00-2BFF・
  variation selector U+FE0F。ただし✓✔✕✖(U+2713,2714,2715,2716)は装飾絵文字ではなく
  UI機能記号(クリア/閉じるボタン等の実体そのもの)のため除外

## 結果
- 削除対象ヒット: 265行(iOS 133行・Android 132行、ミラー実装なので概ね対）
- 全数削除完了(全数再走査で残存0件を確認。詳細は`removal-log.txt`)

## 意図的に残した4箇所(全数把握済み・すべて別タスクで対応)
| ファイル:行 | 内容 | 理由 |
|---|---|---|
| `KyouNoOgatoreApp.swift:289` / `MainActivity.kt:500` | 相談FABの💬 | P-2でCanvas線画アイコンへ差し替え予定(削除ではなく置換) |
| `GuideData.swift:78` / `GuideData.kt:92` | FAQ内「📖 図鑑をひらく」の文言 | A部でボタン名自体が現在の実配置に合わせて全面更新されるため、二重編集を避けP-1では未着手 |

## 副作用の修正
- `WidgetLogicTest.kt`・`KyonoWidgetRenderTest.kt`(Android)、`WidgetStateCalculatorTests.swift`(iOS):
  絵文字を含む期待文字列を、対応する実装(`WidgetLogic.kt`/`WidgetStateCalculator.swift`)の
  変更後の文字列に合わせて更新(テスト自体は変更なし・期待値の文字列のみ追従)

## 実装メモ(自己チェック)
削除は「絵文字の直前直後の空白」も含めて自然な文として読めるよう処理:
- 先頭装飾(`"📖 見出し"`のような開き引用符直後)は絵文字+続く半角スペースをまとめて削除
- 文末(`"〜ですね✅"`のような閉じ引用符直前)は絵文字+直前の半角スペースをまとめて削除
- 文中(`"AとB🎉Cが"`のような自然文の途中)は絵文字のみ削除し、既存の助詞・読点による
  区切りをそのまま残す(不要な空白の二重化・欠落なし)
- 日本語の鉤括弧「」の直後にある絵文字も上記の開き引用符と同じ扱いで手作業補正(4箇所)
- 全265行の変更後、インデント(行頭の空白幅)が変更前後で完全一致することを機械的に検証
