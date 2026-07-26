//
//  SettingsView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「index.html じまん/声/とどくメーター/
//  おやすみ券/エクスポート・インポート」行): 設定UI(Android版SettingsScreen.ktと同一ロジック。
//  index.html:798-846「続ける設定」カードの1:1移植)。テーマ/文字サイズ/エクスポート・インポート
//  いずれもStep3で移植済みのRecordStoreキー(theme/bigtext)・KyonoTransferを呼ぶだけ。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: KyonoCard/KyonoSectionHeader(Clockアイコン)/KyonoSegmentedControlへ作り替え。

import SwiftUI
import RecordCore

struct SettingsView: View {
    let store: RecordStore
    let onBack: () -> Void

    @State private var theme: String
    @State private var bigtext: Bool
    @State private var exportText: String?
    @State private var importInput = ""
    @State private var importMessage: String?
    @State private var confirmImport = false

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        _theme = State(initialValue: store.get("theme", default: "auto"))
        _bigtext = State(initialValue: store.get("bigtext", default: true))
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            content
        }
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                KyonoLineButton("◀ もどる", action: onBack)

                KyonoCard {
                    KyonoSectionHeader(icon: .clock, title: "続ける設定", fill: Color(hex: 0xDFF5F2))
                    Spacer().frame(height: 8)

                    KyonoBodyText("画面のみため")
                    KyonoSegmentedControl(
                        options: [("auto", "じどう"), ("light", "ライト"), ("dark", "ダーク")],
                        selected: theme,
                        onSelect: { v in theme = v; store.set("theme", v) }
                    )

                    Spacer().frame(height: 12)
                    KyonoBodyText("もじの大きさ")
                    KyonoSegmentedControl(
                        options: [(false, "ふつう"), (true, "大きめ")],
                        selected: bigtext,
                        onSelect: { v in bigtext = v; store.set("bigtext", v) }
                    )

                    Spacer().frame(height: 20)
                    Text("📦 記録のひっこし").font(.kyono(.black900, size: 16))
                    Spacer().frame(height: 10)
                    KyonoLineButton("📦 記録をコピーする") {
                        let str = KyonoTransfer.buildExportString(store)
                        exportText = str
                        UIPasteboard.general.string = str
                    }
                    if let exportText {
                        Spacer().frame(height: 8)
                        Text("クリップボードにコピーしました。下のテキストは長押しでも選択できます:").font(.kyono(.bold700, size: 12))
                        TextEditor(text: .constant(exportText)).frame(height: 120).border(Color.gray.opacity(0.3))
                    }

                    Spacer().frame(height: 16)
                    Text("よみこみ").font(.kyono(.black900, size: 16))
                    Spacer().frame(height: 8)
                    TextField("KYONO1:... をここに貼りつけ", text: $importInput).textFieldStyle(.roundedBorder)
                    Spacer().frame(height: 8)
                    KyonoLineButton("📥 よみこむ") { confirmImport = true }
                    if let importMessage {
                        Spacer().frame(height: 8)
                        Text(importMessage).font(.kyono(.bold700, size: 15))
                    }
                }
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
        .alert("いまの記録の上に書きかえるよ", isPresented: $confirmImport) {
            Button("書きかえる") {
                do {
                    try KyonoTransfer.importString(importInput.trimmingCharacters(in: .whitespacesAndNewlines), into: store)
                    theme = store.get("theme", default: "auto")
                    bigtext = store.get("bigtext", default: true)
                    importMessage = "よみこみました！"
                } catch {
                    importMessage = "読みこめませんでした（文字列が壊れているかも）"
                }
            }
            Button("やめる", role: .cancel) {}
        } message: {
            Text("だいじょうぶ？")
        }
    }
}
