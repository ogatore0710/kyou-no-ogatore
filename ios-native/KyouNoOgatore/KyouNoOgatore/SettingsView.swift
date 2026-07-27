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
import UIKit
import EventKit
import RecordCore

// 設定画面「やるタイミング」欠落修正タスク(TASK-C2-2026-07-26-settings-missing-items.md):
// index.html:1974-1979 ANCHORSの1:1移植(ラベル+カレンダー通知の既定時刻)。キー(asa/furo/neru/free)
// 自体はOnboardingViews.swiftのanchor質問と共有(§1-2手写し禁止対象の機械抽出データではなく、
// 短い固定UI文言なのでオンボ文言等と同じくUI copyとして直接記述)。
private struct AnchorInfo { let key: String; let label: String; let defaultHour: Int; let defaultMinute: Int }
private let settingsAnchors: [AnchorInfo] = [
    AnchorInfo(key: "asa", label: "朝おきてそのまま", defaultHour: 7, defaultMinute: 30),
    AnchorInfo(key: "furo", label: "おふろ上がりに", defaultHour: 20, defaultMinute: 30),
    AnchorInfo(key: "neru", label: "寝るまえふとんの上で", defaultHour: 21, defaultMinute: 30),
    AnchorInfo(key: "free", label: "きめてない・そのつど", defaultHour: 20, defaultMinute: 0),
]

struct SettingsView: View {
    let store: RecordStore
    let onBack: () -> Void

    @State private var theme: String
    @State private var bigtext: Bool
    @State private var exportText: String?
    @State private var importInput = ""
    @State private var importMessage: String?
    @State private var confirmImport = false
    @State private var anchor: String?
    @State private var showAnchorPicker = false
    @State private var icsHour: Int
    @State private var icsMinute: Int
    @State private var icsMessage: String?

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        _theme = State(initialValue: store.get("theme", default: "auto"))
        _bigtext = State(initialValue: store.get("bigtext", default: true))
        let savedAnchor: String? = store.get("anchor", default: nil)
        _anchor = State(initialValue: savedAnchor)
        let anchorInfo = settingsAnchors.first { $0.key == savedAnchor }
        // index.html:2003 renderIcs()のdef計算(未設定時はfree扱い)+保存済みicstimeがあればそちらを優先。
        let savedIcsTime: String? = store.get("icstime", default: nil)
        let parts = savedIcsTime?.split(separator: ":").compactMap { Int($0) }
        _icsHour = State(initialValue: (parts?.count ?? 0) > 0 ? parts![0] : (anchorInfo?.defaultHour ?? settingsAnchors.last!.defaultHour))
        _icsMinute = State(initialValue: (parts?.count ?? 0) > 1 ? parts![1] : (anchorInfo?.defaultMinute ?? settingsAnchors.last!.defaultMinute))
    }

    private var icsDateBinding: Binding<Date> {
        Binding(
            get: {
                var c = DateComponents(); c.hour = icsHour; c.minute = icsMinute
                return Calendar.current.date(from: c) ?? Date()
            },
            set: { newDate in
                let c = Calendar.current.dateComponents([.hour, .minute], from: newDate)
                icsHour = c.hour ?? icsHour
                icsMinute = c.minute ?? icsMinute
                store.set("icstime", String(format: "%02d:%02d", icsHour, icsMinute))
            }
        )
    }

    // index.html:2001系のカレンダーIntent(MyRecordView.connectCalendarと同じ設計判断・§2-1準拠)。
    // 時刻を指定できるようパラメータ化。
    private func addToAppleCalendar(hour: Int, minute: Int, completion: @escaping (Bool) -> Void) {
        let eventStore = EKEventStore()
        eventStore.requestWriteOnlyAccessToEvents { granted, _ in
            DispatchQueue.main.async {
                guard granted else { completion(false); return }
                let event = EKEvent(eventStore: eventStore)
                event.title = "きょうのオガトレ（1本だけ）"
                event.notes = "ストレッチの時間です"
                var comps = Calendar.current.dateComponents([.year, .month, .day], from: Date())
                comps.hour = hour; comps.minute = minute
                let start = Calendar.current.date(from: comps) ?? Date()
                event.startDate = start
                event.endDate = start.addingTimeInterval(10 * 60)
                event.recurrenceRules = [EKRecurrenceRule(recurrenceWith: .daily, interval: 1, end: nil)]
                event.calendar = eventStore.defaultCalendarForNewEvents
                do {
                    try eventStore.save(event, span: .thisEvent)
                    completion(true)
                } catch {
                    completion(false)
                }
            }
        }
    }

    // index.html:2020 gcalLink組み立ての1:1移植。終了時刻の丸め方(分+10を59で頭打ち・時をまたがない)
    // もWeb版の実装をそのまま踏襲。
    private func openGoogleCalendar(hour: Int, minute: Int) {
        let icsDate = RecordLogic.todayStr(now: Date()).replacingOccurrences(of: "-", with: "")
        let startTm = String(format: "%02d%02d00", hour, minute)
        let endMinute = min(59, minute + 10)
        let endTm = String(format: "%02d%02d00", hour, endMinute)
        let text = "きょうのオガトレ（1本だけ）".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let urlStr = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=\(text)&dates=\(icsDate)T\(startTm)/\(icsDate)T\(endTm)&recur=RRULE:FREQ=DAILY"
        if let url = URL(string: urlStr) { UIApplication.shared.open(url) }
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
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

                    // index.html:800 「やるタイミング: 現在値 変える」の1:1移植。Home側のanchorCard
                    // (いつやる派？の再選択UI)は別タスク(home-structure-fix.md)で明示的にスコープ外と
                    // されており未実装のため、「変える」の遷移先はこの画面内のインライン選択に留めた
                    // (「設定画面に変更導線を追加する」という本タスクの要求は満たしつつ、未実装のHome
                    // 機能への依存を避ける判断)。
                    HStack(alignment: .lastTextBaseline, spacing: 8) {
                        (Text("やるタイミング: ")
                            + Text(settingsAnchors.first { $0.key == anchor }?.label ?? "未設定").fontWeight(.black))
                            .kyonoFont(.bold700, size: 15).foregroundColor(.primary)
                        Text("変える")
                            .kyonoFont(.black900, size: 14).foregroundColor(Color(hex: 0x177065))
                            .onTapGesture { showAnchorPicker.toggle() }
                    }
                    if showAnchorPicker {
                        Spacer().frame(height: 8)
                        VStack(spacing: 6) {
                            ForEach(settingsAnchors, id: \.key) { info in
                                KyonoGhostButton(info.label) {
                                    store.set("anchor", info.key)
                                    anchor = info.key
                                    showAnchorPicker = false
                                }
                            }
                        }
                    }
                    Spacer().frame(height: 16)

                    KyonoBodyText("画面のみため")
                    KyonoSegmentedControl(
                        options: [("auto", "じどう"), ("light", "ライト"), ("dark", "ダーク")],
                        selected: theme,
                        onSelect: { v in theme = v; store.set("theme", v) }
                    )
                    // TASK-C2-2026-07-27-settings-clipboard-import-and-hints.md: index.html:807の1:1移植。
                    Spacer().frame(height: 6)
                    Text("「じどう」は夜（19時〜朝5時）やスマホがダーク設定のとき暗くなります")
                        .kyonoFont(.bold700, size: 12).foregroundColor(.secondary)

                    Spacer().frame(height: 12)
                    KyonoBodyText("もじの大きさ")
                    KyonoSegmentedControl(
                        options: [(false, "ふつう"), (true, "大きめ")],
                        selected: bigtext,
                        onSelect: { v in bigtext = v; store.set("bigtext", v) }
                    )

                    // index.html:809-816 カレンダーのおしらせ時間+Apple/Googleカレンダー登録ボタンの
                    // 1:1移植。「毎日自動でアプリがひらく設定（iPhone）」(index.html:818-827・
                    // Shortcutsアプリの自動化案内)はPWAが通知を送れないことへのiOS限定の回避策で
                    // ネイティブには元から無関係な問題のため移植しない(タスク指示どおり)。マイ記録
                    // タブの既存「📅 カレンダーに登録する」(時刻指定なし・簡易版)とは別物として両方残す。
                    Spacer().frame(height: 20)
                    KyonoBodyText("カレンダーのおしらせ時間")
                    Spacer().frame(height: 6)
                    DatePicker("", selection: icsDateBinding, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                        .datePickerStyle(.compact)
                    Spacer().frame(height: 10)
                    KyonoLineButton("📅 Appleカレンダーに入れる") {
                        addToAppleCalendar(hour: icsHour, minute: icsMinute) { ok in
                            icsMessage = ok ? nil : "カレンダーへの追加が許可されませんでした"
                        }
                    }
                    Spacer().frame(height: 8)
                    KyonoLineButton("📅 Googleカレンダーに入れる") { openGoogleCalendar(hour: icsHour, minute: icsMinute) }
                    Spacer().frame(height: 6)
                    Text("スマホのカレンダーが毎日その時間に知らせてくれます").kyonoFont(.bold700, size: 12)
                    if let icsMessage {
                        Text(icsMessage).kyonoFont(.bold700, size: 12).foregroundColor(Color(hex: 0xE56A9A))
                    }

                    Spacer().frame(height: 20)
                    Text("📦 記録のひっこし").kyonoFont(.black900, size: 16)
                    Spacer().frame(height: 10)
                    KyonoLineButton("📦 記録をコピーする") {
                        let str = KyonoTransfer.buildExportString(store)
                        exportText = str
                        UIPasteboard.general.string = str
                    }
                    if let exportText {
                        Spacer().frame(height: 8)
                        Text("クリップボードにコピーしました。下のテキストは長押しでも選択できます:").kyonoFont(.bold700, size: 12)
                        TextEditor(text: .constant(exportText)).frame(height: 120).border(Color.gray.opacity(0.3))
                    }

                    Spacer().frame(height: 16)
                    Text("よみこみ").kyonoFont(.black900, size: 16)
                    Spacer().frame(height: 8)
                    // TASK-C2-2026-07-27-settings-clipboard-import-and-hints.md: index.html:839,2067-2074
                    // importFromClipboard()の1:1移植。高齢者・デジタル機器が苦手な方向けに、長押しコピー→
                    // 貼り付けという操作をボタン1つで完結させる(2026-07-19 Fableレビュー対応と同じ意図)。
                    // 読み取れない/空のときはWeb版と同趣旨のメッセージで下の手動欄へフォールバックする。
                    KyonoPrimaryButton("📋 コピーした記録を自動で読みこむ") {
                        let text = UIPasteboard.general.string ?? ""
                        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            importMessage = "クリップボードが空みたい\nコピーできているか確認してね"
                        } else {
                            importInput = text.trimmingCharacters(in: .whitespacesAndNewlines)
                            confirmImport = true
                        }
                    }
                    Spacer().frame(height: 6)
                    Text("うまくいかないときは 下のわくに手で貼り付けてね").kyonoFont(.bold700, size: 12).foregroundColor(.secondary)
                    Spacer().frame(height: 8)
                    TextField("KYONO1:... をここに貼りつけ", text: $importInput).textFieldStyle(.roundedBorder)
                    Spacer().frame(height: 8)
                    KyonoLineButton("📥 よみこむ") { confirmImport = true }
                    if let importMessage {
                        Spacer().frame(height: 8)
                        Text(importMessage).kyonoFont(.bold700, size: 15)
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
