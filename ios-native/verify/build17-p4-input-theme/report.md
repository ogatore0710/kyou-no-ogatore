# build17 P-4: ライトモードで入力欄が黒いまま — 修正報告(iOS)

## 原因
`TextField`/`OutlinedTextField`のうち、明示的な`colors`/`background`指定を持たないものは
端末のシステム側ダーク/ライト設定に追従する既定スタイルを使う。アプリ内テーマ(`kyono_theme`)は
システム設定とは独立した自前の値のため、両者が食い違う(例: システムはダーク、アプリ内は
「明るい」)とき、対象の入力欄だけシステム側の暗い背景のまま浮いて見えていた。

## 修正した3箇所(発注書指定)
| 画面 | ファイル | 内容 |
|---|---|---|
| ホーム ひとことメモ | `HomeView.swift:1060` | `colors.card`塗り+`colors.line`枠線の自前スタイルへ |
| 相談室チャット入力欄 | `SoudanSheetView.swift:728` | 同上 |
| 設定「コピーした記録を読みこむ」貼り付け欄 | `SettingsView.swift:407` | 同上 |

## 全数棚卸し(grep `TextField(`/`TextEditor(` — iOS全8箇所)
| ファイル:行 | 状態 | 備考 |
|---|---|---|
| `BragView.swift:104`(日数入力) | 既に安全 | `colors.card`塗り済み |
| `BragView.swift:122`(検索) | 既に安全 | 同上 |
| `GuideView.swift:342`(FAQ検索) | 既に安全 | 同上 |
| `HomeView.swift:1060`(メモ) | **修正済み** | 上記 |
| `SoudanSheetView.swift:728`(相談室入力) | **修正済み** | 上記 |
| `SearchView.swift:287`(動画検索) | 既に安全 | 外側HStackに`colors.card`塗り済み |
| `SettingsView.swift:367`(エクスポート`TextEditor`) | **追加で発見・修正** | `TextEditor`は既定でシステム背景を自前描画するため`.background()`が隠れる欠陥。`.scrollContentBackground(.hidden)`で解除後`colors.card`塗り |
| `SettingsView.swift:407`(インポート貼り付け欄) | **修正済み** | 上記(発注書指定の3箇所目) |

発注書指定の3箇所に加え、`SettingsView.swift`のエクスポート欄(`TextEditor`)も同症状だったため
棚卸しで発見し合わせて修正。それ以外の5箇所は元から`colors.card`塗りで安全だった。

## 検証(自分で確認済み)
- シミュレータをシステムレベルでダークモードに強制(`xcrun simctl ui ... appearance dark`)しつつ、
  アプリ内`kyono_theme`は`"light"`固定という、報告時と同じ食い違い条件を再現。
- XCUITestで実機同等の描画を撮影:
  - `01-soudan-input-fixed.png` — 相談室入力欄が黒くならず、ライトテーマの白/クリーム地で描画
  - `02-settings-import-field-fixed.png` — 設定の貼り付け欄が同条件で黒くならず描画
- ホームのメモ欄・設定のエクスポート欄は同一修正パターンのためコード確認のみ(未確認)。
  ビルドは成功、目視での崩れなし。
