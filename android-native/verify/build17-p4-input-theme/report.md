# build17 P-4: ライトモードで入力欄が黒いまま — 修正報告(Android)

## 原因
`OutlinedTextField`は既定で透明な背景を持ち、`colors`を明示指定しないと
端末のシステム側ダーク/ライト設定に追従する既定スタイルが透けて見える。
アプリ内テーマ(`kyono_theme`)はシステム設定とは独立した自前の値のため、
両者が食い違う(例: システムはダーク、アプリ内は「明るい」)とき、対象の
入力欄だけシステム側の暗い背景のまま浮いて見えていた。

## 修正した3箇所(発注書指定に対応)
| 画面 | ファイル | 内容 |
|---|---|---|
| 相談室チャット入力欄 | `SoudanSheet.kt:818` | `OutlinedTextFieldDefaults.colors(focusedContainerColor = colors.card, ...)`を明示指定 |
| 設定「コピーした記録を読みこむ」貼り付け欄 | `SettingsScreen.kt:522` | 同上 |
| 設定 エクスポート欄(読み取り専用) | `SettingsScreen.kt:470` | 同上(`disabled*`も含めて指定) |

## 全数棚卸し(grep `TextField(`/`OutlinedTextField(` — Android全8箇所)
| ファイル:行 | 状態 | 備考 |
|---|---|---|
| `BragScreen.kt:103`(日数入力) | 既に安全 | `TextFieldDefaults.colors`で`colors.card`指定済み |
| `BragScreen.kt:125`(検索) | 既に安全 | 同上 |
| `GuideScreen.kt:412`(FAQ検索) | 既に安全 | 同上 |
| `SettingsScreen.kt:470`(エクスポート欄) | **修正済み** | 上記 |
| `SettingsScreen.kt:522`(インポート貼り付け欄) | **修正済み** | 上記(発注書指定の3箇所目) |
| `MainActivity.kt:1696`(ホーム ひとことメモ) | 既に安全 | `TextFieldDefaults.colors`で`colors.card`指定済み |
| `SoudanSheet.kt:818`(相談室入力) | **修正済み** | 上記 |
| `SearchScreen.kt:298`(動画検索) | 既に安全 | 同上 |

iOSと異なりAndroidの「ひとことメモ」(`MainActivity.kt:1696`)は元から`TextFieldDefaults.colors`で
`colors.card`が指定済みで安全だった(iOSは未指定で破損していた)。発注書指定の3箇所のうち
Androidで実際に壊れていたのは相談室入力欄と設定2箇所(貼り付け欄+エクスポート欄)の計3箇所。

## 検証(自分で確認済み)
- `adb shell settings put secure ui_night_mode 2` + `cmd uimode night yes`でシステムレベルの
  ダークモードを強制しつつ、アプリ内`kyono_theme`は`"light"`固定という食い違い条件を再現。
- `01-soudan-input-fixed.png` — 相談室入力欄が黒くならず、ライトテーマの白/クリーム地で描画
- `02-settings-import-field-fixed.png` — 設定の貼り付け欄が同条件で黒くならず描画
- エクスポート欄は同一修正パターンのためコード確認のみ(未確認)。ビルド・テストは成功、
  目視での崩れなし。
