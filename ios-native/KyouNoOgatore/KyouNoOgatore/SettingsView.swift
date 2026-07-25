//
//  SettingsView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「index.html じまん/声/とどくメーター/
//  おやすみ券/エクスポート・インポート」行): 設定UI(Android版SettingsScreen.ktと同一ロジック。
//  index.html:798-846「続ける設定」カードの1:1移植)。テーマ/文字サイズ/エクスポート・インポート
//  いずれもStep3で移植済みのRecordStoreキー(theme/bigtext)・KyonoTransferを呼ぶだけ。
//
//  実装範囲の注記: MaterialTheme相当(色配色)をtheme設定に応じてアプリ全体へ即座に反映する処理
//  (ダークモードの実見た目)は本ステップのスコープ外として見送った(Android版と同じ判断。
//  検収基準に含まれず、データ契約の正しさとは独立した表示上の作り込みのため)。

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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Button("◀ もどる", action: onBack)
                Text("続ける設定").font(.title2.bold())

                Text("画面のみため").font(.headline)
                HStack {
                    ForEach([("auto", "じどう"), ("light", "ライト"), ("dark", "ダーク")], id: \.0) { v, label in
                        Button(label) { theme = v; store.set("theme", v) }
                            .buttonStyle(.borderedProminent)
                            .tint(theme == v ? Color(red: 0.42, green: 0.31, blue: 0.65) : Color(red: 0.91, green: 0.89, blue: 0.96))
                    }
                }

                Text("もじの大きさ").font(.headline)
                HStack {
                    ForEach([(false, "ふつう"), (true, "大きめ")], id: \.0) { v, label in
                        Button(label) { bigtext = v; store.set("bigtext", v) }
                            .buttonStyle(.borderedProminent)
                            .tint(bigtext == v ? Color(red: 0.42, green: 0.31, blue: 0.65) : Color(red: 0.91, green: 0.89, blue: 0.96))
                    }
                }

                Text("📦 記録のひっこし").font(.headline)
                Button("📦 記録をコピーする") {
                    let str = KyonoTransfer.buildExportString(store)
                    exportText = str
                    UIPasteboard.general.string = str
                }
                if let exportText {
                    Text("クリップボードにコピーしました。下のテキストは長押しでも選択できます:").font(.caption)
                    TextEditor(text: .constant(exportText)).frame(height: 120).border(Color.gray.opacity(0.3))
                }

                Text("よみこみ").font(.headline)
                TextField("KYONO1:... をここに貼りつけ", text: $importInput).textFieldStyle(.roundedBorder)
                Button("📥 よみこむ") { confirmImport = true }
                if let importMessage { Text(importMessage) }
            }
            .padding(16)
        }
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
