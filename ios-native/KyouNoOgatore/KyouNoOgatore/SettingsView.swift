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
struct AnchorInfo { let key: String; let label: String; let defaultHour: Int; let defaultMinute: Int }
let settingsAnchors: [AnchorInfo] = [
    AnchorInfo(key: "asa", label: "朝おきてそのまま", defaultHour: 7, defaultMinute: 30),
    AnchorInfo(key: "furo", label: "おふろ上がりに", defaultHour: 20, defaultMinute: 30),
    AnchorInfo(key: "neru", label: "寝るまえふとんの上で", defaultHour: 21, defaultMinute: 30),
    AnchorInfo(key: "free", label: "きめてない・そのつど", defaultHour: 20, defaultMinute: 0),
]

// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §3: index.html:2001-2020 renderIcs()の
// 「anchor別の既定＋保存済みicstimeを必ず反映」の1:1移植。設定画面・マイ記録画面の両方の
// カレンダー登録ボタンから共通で使う(以前はマイ記録側だけhour=20,minute=0固定だった)。
func icsTimeFor(_ store: RecordStore) -> (Int, Int) {
    let anchor: String? = store.get("anchor", default: nil)
    let anchorInfo = settingsAnchors.first { $0.key == anchor }
    let savedIcsTime: String? = store.get("icstime", default: nil)
    let parts = savedIcsTime?.split(separator: ":").compactMap { Int($0) }
    let hour = (parts?.count ?? 0) > 0 ? parts![0] : (anchorInfo?.defaultHour ?? settingsAnchors.last!.defaultHour)
    let minute = (parts?.count ?? 0) > 1 ? parts![1] : (anchorInfo?.defaultMinute ?? settingsAnchors.last!.defaultMinute)
    return (hour, minute)
}

struct SettingsView: View {
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §4: ダークモード非対応のハードコード色
    // (0xFFFFFF/0xF2EADB/0xDFF5F2/0x177065/0xE56A9A)をテーマ変数に差し替えるために追加
    // (Android版SettingsScreen.ktはcolors.card/colors.line/colors.tealSoft/colors.tealInkで
    // 対応済みだったが、iOSだけ固定色のままダークで白いチップが浮いていた)。
    @Environment(\.kyonoColors) private var colors
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
    @State private var notifEnabled: Bool
    // GO-G9(5視点ワンループ): 「よみこむ」実行前の状態を1件だけプロセス内メモリに退避し、実行直後の
    // 一定時間だけ「さっきの状態にもどす」を出す(永続化はしない・RecordLogicの計算には一切触れない
    // コピー退避のみ)。
    @State private var preImportSnapshot: [String: String]?
    @State private var showImportUndo = false
    // TASK-C2-2026-08-01-build15-subtraction9.md #4: カレンダー・通知一式を開閉式に(引き算)。既定は閉。
    @State private var notifSectionExpanded = false

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        _theme = State(initialValue: store.get("theme", default: "auto"))
        _bigtext = State(initialValue: store.get("bigtext", default: true))
        let savedAnchor: String? = store.get("anchor", default: nil)
        _anchor = State(initialValue: savedAnchor)
        // index.html:2003 renderIcs()のdef計算(未設定時はfree扱い)+保存済みicstimeがあればそちらを優先。
        let (savedHour, savedMinute) = icsTimeFor(store)
        // TASK-C2-2026-07-27-local-notifications.md §2-2(本人指示): 時刻ピッカーを15分刻みに変更。
        // 既に保存済みのicstimeが15分刻みでない場合は、表示・保存とも最も近い15分に丸める
        // (既定値はすべて15分刻みなので影響なし)。
        _icsHour = State(initialValue: savedHour)
        _icsMinute = State(initialValue: min(45, ((savedMinute + 7) / 15) * 15))
        _notifEnabled = State(initialValue: store.get("notif_enabled", default: false))
    }

    private static let minuteOptions = [0, 15, 30, 45]

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
        // iOSスワイプもどり導線追加タスク(EdgeSwipeBack.swift参照): アコーディオン状態を持たない画面。
        .edgeSwipeBack(onBack: onBack)
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                KyonoLineButton("◀ もどる", action: onBack)

                KyonoCard {
                    KyonoSectionHeader(icon: .clock, title: "続ける設定", fill: colors.tealSoft)
                    Spacer().frame(height: 8)

                    // index.html:800 「やるタイミング: 現在値 変える」の1:1移植。Home側のanchorCard
                    // (いつやる派？の再選択UI)は別タスク(home-structure-fix.md)で明示的にスコープ外と
                    // されており未実装のため、「変える」の遷移先はこの画面内のインライン選択に留めた
                    // (「設定画面に変更導線を追加する」という本タスクの要求は満たしつつ、未実装のHome
                    // 機能への依存を避ける判断)。
                    HStack(alignment: .lastTextBaseline, spacing: 8) {
                        (Text("やるタイミング: ")
                            + Text(settingsAnchors.first { $0.key == anchor }?.label ?? "未設定").fontWeight(.black))
                            .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                        // GO-G3(5視点ワンループ): 最小タップ領域44pt/48ptの確保(見た目は変えず当たり判定のみ拡張)。
                        Text("変える")
                            .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                            .padding(.vertical, 12)
                            .onTapGesture { showAnchorPicker.toggle() }
                    }
                    if showAnchorPicker {
                        Spacer().frame(height: 8)
                        VStack(spacing: 6) {
                            ForEach(settingsAnchors, id: \.key) { info in
                                // TASK-C2-2026-07-30-icon-system-addendum-chips.md ③: オンボの
                                // anchor質問(asa/furo/neru/free)と同じキー・同じ絵を再利用する
                                // (この画面用に新しく描き起こさない)。KyonoGhostButtonはアイコン
                                // 引数を持たないため、同じ見た目(colors.tealSoft/tealInk・
                                // kyonoButtonRadius)をこの場でだけ組む。
                                HStack(spacing: 10) {
                                    if let url = Bundle.main.url(forResource: "chip-\(info.key)", withExtension: "png"),
                                       let uiImage = UIImage(contentsOfFile: url.path) {
                                        Image(uiImage: uiImage).resizable().scaledToFit().frame(width: 28, height: 28)
                                    }
                                    Text(info.label).kyonoFont(.black900, size: 15).foregroundColor(colors.tealInk)
                                }
                                .padding(.horizontal, 18).padding(.vertical, 16)
                                .frame(maxWidth: .infinity)
                                .background(colors.tealSoft)
                                .cornerRadius(kyonoButtonRadius)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    store.set("anchor", info.key)
                                    anchor = info.key
                                    showAnchorPicker = false
                                    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6:
                                    // index.html:2001-2020 renderIcs()は呼ばれるたびにanchor別の
                                    // 既定を反映するが、以前はinit時の1回きりで「やるタイミング」を
                                    // 変えても表示中の時刻が追従しなかった。保存済みicstimeがまだ無い
                                    // (=本人が時刻を明示的に選んでいない)間だけ、anchor変更にあわせて
                                    // 既定時刻を追従させる。
                                    let savedIcsTime: String? = store.get("icstime", default: nil)
                                    if savedIcsTime == nil {
                                        icsHour = info.defaultHour
                                        icsMinute = min(45, ((info.defaultMinute + 7) / 15) * 15)
                                    }
                                }
                            }
                        }
                    }
                    Spacer().frame(height: 16)

                    KyonoBodyText("画面のみため")
                    KyonoSegmentedControl(
                        // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:804-805
                        // ラベル「明るい/暗い」の1:1移植。
                        options: [("auto", "じどう"), ("light", "明るい"), ("dark", "暗い")],
                        selected: theme,
                        onSelect: { v in theme = v; store.set("theme", v) }
                    )
                    // TASK-C2-2026-07-27-settings-clipboard-import-and-hints.md: index.html:807の1:1移植。
                    Spacer().frame(height: 6)
                    Text("「じどう」は夜（19時〜朝5時）やスマホがダーク設定のとき暗くなります")
                        .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)

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
                    // UX13案・案9(2026-07-30): 見出しが「カレンダーの」おしらせ時間を名乗りつつ、
                    // 実際は通知機能とも共有する時刻だったため「おしらせの時間」に改め、時刻ピッカー→
                    // 「毎日のおしらせ」トグル→カレンダー登録ボタンの順に並び替え(通知だけ使いたい人が
                    // カレンダー連携の説明を挟まず自分の通知時刻にたどり着けるように)。要素の追加削除は無し。
                    Spacer().frame(height: 20)
                    // TASK-C2-2026-08-01-build15-subtraction9.md #4: カレンダー・通知一式を隣接の
                    // FAQ開閉(GuideView.swift)と同じ様式(見出しタップで開閉・▾/▴)で畳む。既定は閉。
                    // 閉じていても状態がわかるよう、見出し行に現在時刻/オンオフの要約を残す。
                    HStack {
                        KyonoBodyText("おしらせの時間")
                        Spacer()
                        Text(notifEnabled ? "\(String(format: "%02d", icsHour)):\(String(format: "%02d", icsMinute)) オン" : "オフ")
                            .kyonoFont(.bold700, size: 13).foregroundColor(notifEnabled ? colors.tealInk : colors.sub)
                        Text(notifSectionExpanded ? "▴" : "▾").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { notifSectionExpanded.toggle() }
                    if notifSectionExpanded {
                    Spacer().frame(height: 6)
                    // TASK-C2-2026-07-27-local-notifications.md §2-2(本人指示): 時刻ピッカーを
                    // 15分刻み(:00/:15/:30/:45の4択)に変更。Android版のDropdownMenu2つ構成と
                    // 同じ考え方でMenu2つに置き換える(単一のDatePickerだと分の刻みを制御できない)。
                    HStack(spacing: 8) {
                        Menu {
                            ForEach(0..<24, id: \.self) { h in
                                Button("\(String(format: "%02d", h))時") {
                                    icsHour = h
                                    store.set("icstime", String(format: "%02d:%02d", icsHour, icsMinute))
                                    DailyNotifications.resync(store: store)
                                }
                            }
                        } label: {
                            Text("\(String(format: "%02d", icsHour))時")
                                .kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                                .padding(.horizontal, 14).padding(.vertical, 8)
                                .background(RoundedRectangle(cornerRadius: 12).fill(colors.card))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 2))
                        }
                        .accessibilityLabel("時を選ぶ、現在\(String(format: "%02d", icsHour))時")
                        Menu {
                            ForEach(Self.minuteOptions, id: \.self) { m in
                                Button("\(String(format: "%02d", m))分") {
                                    icsMinute = m
                                    store.set("icstime", String(format: "%02d:%02d", icsHour, icsMinute))
                                    DailyNotifications.resync(store: store)
                                }
                            }
                        } label: {
                            Text("\(String(format: "%02d", icsMinute))分")
                                .kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                                .padding(.horizontal, 14).padding(.vertical, 8)
                                .background(RoundedRectangle(cornerRadius: 12).fill(colors.card))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 2))
                        }
                        .accessibilityLabel("分を選ぶ、現在\(String(format: "%02d", icsMinute))分")
                    }

                    // TASK-C2-2026-07-27-local-notifications.md: 「毎日のおしらせ」トグル。既定オフ・
                    // オンにした瞬間だけ許可ダイアログを出す(1日目クリア時のインライン提案とは別経路。
                    // どちらも同じnotif_enabledに収束する。Android版SettingsScreen.ktと同一仕様)。
                    Spacer().frame(height: 20)
                    HStack(alignment: .center) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("毎日のおしらせ").kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                            Text("上の時間に、一言だけやわらかくお知らせします(その日すでに記録していれば出ません)")
                                .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                        }
                        Spacer()
                        // GO-G10(5視点ワンループ): 他の設定は文字つきセグメントで状態を伝えているのに、
                        // ここだけ色と位置だけだった。「オン/オフ」の文字を併記する。
                        Text(notifEnabled ? "オン" : "オフ")
                            .kyonoFont(.black900, size: 13)
                            .foregroundColor(notifEnabled ? colors.tealInk : colors.sub)
                        Toggle("", isOn: Binding(
                            get: { notifEnabled },
                            set: { on in
                                if on {
                                    DailyNotifications.requestAuthorization { granted in
                                        if granted {
                                            store.set("notif_enabled", true)
                                            notifEnabled = true
                                            DailyNotifications.resync(store: store)
                                        }
                                    }
                                } else {
                                    store.set("notif_enabled", false)
                                    notifEnabled = false
                                    DailyNotifications.cancelAll()
                                }
                            }
                        )).labelsHidden()
                    }

                    Spacer().frame(height: 20)
                    KyonoLineButton("Appleカレンダーに入れる") {
                        addToAppleCalendar(hour: icsHour, minute: icsMinute) { ok in
                            icsMessage = ok ? nil : "カレンダーへの追加が許可されませんでした"
                        }
                    }
                    Spacer().frame(height: 8)
                    KyonoLineButton("Googleカレンダーに入れる") { openGoogleCalendar(hour: icsHour, minute: icsMinute) }
                    Spacer().frame(height: 6)
                    Text("スマホのカレンダーが毎日その時間に知らせてくれます").kyonoFont(.bold700, size: 12)
                    if let icsMessage {
                        Text(icsMessage).kyonoFont(.bold700, size: 12).foregroundColor(colors.pinkInk)
                    }
                    }

                    Spacer().frame(height: 20)
                    // アイコン方針(I) 新規描き起こしバッチ1(2026-07-30): この見出しと直下のボタンは
                    // 同じ📦だったため、同じ`.exportBox`を再利用する(見出しは削除しない・付ける)。
                    HStack(spacing: 6) {
                        KyonoIconGlyph(icon: .exportBox, fill: .clear, accent: colors.pink).frame(width: 18, height: 18)
                        Text("記録のひっこし").kyonoFont(.black900, size: 16)
                    }
                    Spacer().frame(height: 10)
                    KyonoLineButton("記録をコピーする", icon: .exportBox) {
                        let str = KyonoTransfer.buildExportString(store)
                        exportText = str
                        UIPasteboard.general.string = str
                    }
                    if let exportText {
                        Spacer().frame(height: 8)
                        // UI/UXパリティ監査GO-10(2026-07-28): index.html:835 「この文字を長押しで
                        // コピーしてね」の1:1移植。ネイティブは自動コピーという機能差があるためWeb版と
                        // 文言はそのまま同じにはできないが、句点・コロンで終わる説明文調ではなく
                        // Web版と同じカジュアルな文体に寄せる。
                        Text("クリップボードにコピーしたよ 下の文字は長押しでも選べるよ").kyonoFont(.bold700, size: 12)
                        TextEditor(text: .constant(exportText)).frame(height: 120).border(Color.gray.opacity(0.3))
                    }
                    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:837の1:1移植。
                    // コピーして終わり→保存されないまま機種変、が起きないよう保存先を促す。
                    Spacer().frame(height: 6)
                    Text("コピーした文字はLINEの「じぶん専用トーク」やメモアプリに貼っておくと安心です")
                        .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)

                    Spacer().frame(height: 16)
                    Text("よみこみ").kyonoFont(.black900, size: 16)
                    Spacer().frame(height: 8)
                    // TASK-C2-2026-07-27-settings-clipboard-import-and-hints.md: index.html:839,2067-2074
                    // importFromClipboard()の1:1移植。高齢者・デジタル機器が苦手な方向けに、長押しコピー→
                    // 貼り付けという操作をボタン1つで完結させる(2026-07-19 Fableレビュー対応と同じ意図)。
                    // 読み取れない/空のときはWeb版と同趣旨のメッセージで下の手動欄へフォールバックする。
                    KyonoPrimaryButton("コピーした記録を自動で読みこむ", icon: .clipboardPaste) {
                        let text = UIPasteboard.general.string ?? ""
                        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            importMessage = "クリップボードが空みたい\nコピーできているか確認してね"
                        } else {
                            importInput = text.trimmingCharacters(in: .whitespacesAndNewlines)
                            confirmImport = true
                        }
                    }
                    Spacer().frame(height: 6)
                    Text("うまくいかないときは 下のわくに手で貼り付けてね").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                    Spacer().frame(height: 8)
                    TextField("KYONO1:... をここに貼りつけ", text: $importInput).textFieldStyle(.roundedBorder)
                    Spacer().frame(height: 8)
                    KyonoLineButton("よみこむ", icon: .importTray) {
                        // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: index.html:2082-2084
                        // importData()の空欄チェックの1:1移植。以前は空欄でも確認ダイアログへ進み、
                        // base64/prefixエラーの「文字列が壊れているかも」という誤解を招くメッセージが
                        // 出ていた。
                        if importInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            importMessage = "コピーした文字を上のわくに貼ってから「よみこむ」を押してね"
                        } else {
                            confirmImport = true
                        }
                    }
                    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:843の1:1移植。
                    Spacer().frame(height: 6)
                    Text("機種変更のときは 前のスマホで「コピー」→ 新しいスマホで「よみこむ」")
                        .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                    if let importMessage {
                        Spacer().frame(height: 8)
                        Text(importMessage).kyonoFont(.bold700, size: 15)
                    }
                    // GO-G9(5視点ワンループ): 「よみこむ」実行直後だけ出る取り消し導線。
                    if showImportUndo {
                        Spacer().frame(height: 6)
                        KyonoLineButton("さっきの状態にもどす") {
                            if let snapshot = preImportSnapshot {
                                let current = store.allRawKyonoEntries
                                for k in current.keys where snapshot[k] == nil { store.removeRaw(k) }
                                for (k, v) in snapshot { store.setRaw(k, v) }
                                theme = store.get("theme", default: "auto")
                                bigtext = store.get("bigtext", default: true)
                            }
                            importMessage = "さっきの状態にもどしました"
                            showImportUndo = false
                            preImportSnapshot = nil
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
        .alert("いまの記録の上に書きかえるよ", isPresented: $confirmImport) {
            Button("書きかえる") {
                do {
                    // GO-G9(5視点ワンループ): 上書き前の状態を1件だけ退避してからimportする。
                    let snapshot = store.allRawKyonoEntries
                    try KyonoTransfer.importString(importInput.trimmingCharacters(in: .whitespacesAndNewlines), into: store)
                    theme = store.get("theme", default: "auto")
                    bigtext = store.get("bigtext", default: true)
                    importMessage = "よみこみました！"
                    preImportSnapshot = snapshot
                    showImportUndo = true
                    Task {
                        try? await Task.sleep(nanoseconds: 15_000_000_000)
                        showImportUndo = false
                    }
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
