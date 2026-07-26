//
//  MyRecordView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 5b(マスタープラン§6 Step 5b): マイ記録(カレンダー42マス・おやすみ券・
//  とどくメーター・カレンダー連携)。Android版(MainActivity.kt MyRecordScreen)と同一ロジックの
//  SwiftUI実装。判定・集計ロジックはCalendarLogic/RecordLogic(Step3/5b・RecordCore)の
//  純粋関数を呼ぶだけで、ここでは再実装しない。
//
//  LazyVerticalGrid相当のSwiftUI Gridも同様に「1ヶ月最大42マス」程度なら問題なく動くが、
//  Android版とロジック・見た目を対応させるため、ここも素朴なVStack+HStack(Column+Row)で組む
//  (masterplan §1-4のLazyVerticalGrid禁じ手はAndroid固有の制約だが、両OSの実装方針を揃える)。
//
//  カレンダー連携(index.html:2001 renderIcs相当)はEventKitで日次リマインダーを追加する。
//  Android版のIntent委譲(権限不要)と異なり、EventKitは書き込み権限が必要(iOS 17+の
//  write-onlyアクセス。Info.plistにNSCalendarsWriteOnlyAccessUsageDescriptionを追加済み)。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:403-415 .cal/.cal .d.done/.cal .d.today/.bar(おやすみ券進捗)の1:1移植。

import SwiftUI
import EventKit
import RecordCore

struct MyRecordView: View {
    let store: RecordStore

    @State private var streak: RecordLogic.StreakData
    @State private var doneDates: Set<String>
    private let today: String
    @State private var year: Int
    @State private var month: Int
    @State private var reachList: [RecordLogic.ReachEntry]
    @State private var reachMsg: String?
    @State private var calendarMsg: String?
    private let freezeLeft: Int

    init(store: RecordStore) {
        self.store = store
        let s = RecordLogic.loadStreak(store)
        _streak = State(initialValue: s)
        _doneDates = State(initialValue: Set(s.dates))
        let now = Date()
        today = RecordLogic.todayStr(now: now)
        let c = Calendar.current.dateComponents([.year, .month], from: now)
        _year = State(initialValue: c.year!)
        _month = State(initialValue: c.month!)
        _reachList = State(initialValue: RecordLogic.getReach(store))
        freezeLeft = RecordLogic.freezeLeft(store, now: now)
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            MyRecordContentView(
                year: $year, month: $month, reachList: $reachList, reachMsg: $reachMsg,
                calendarMsg: $calendarMsg, doneDates: doneDates, today: today, freezeLeft: freezeLeft,
                store: store, onConnectCalendar: connectCalendar
            )
        }
    }

    // index.html:2001 renderIcs/saveIcsTime相当。Web版はICSファイルダウンロード/Googleカレンダー
    // リンクだが、ネイティブはEventKitで実際のカレンダーへ日次リマインダーを追加する
    // (マスタープラン§2-1「icstimeはEventKit/カレンダーIntentに接続」)。write-onlyアクセスのみ要求し、
    // 既存の予定は読み取らない(Android版のIntent委譲=権限不要、と設計思想を揃えた最小権限)。
    private func connectCalendar(completion: @escaping (Bool) -> Void) {
        let store = EKEventStore()
        store.requestWriteOnlyAccessToEvents { granted, _ in
            DispatchQueue.main.async {
                guard granted else { completion(false); return }
                let event = EKEvent(eventStore: store)
                event.title = "きょうのオガトレ（1本だけ）"
                event.notes = "ストレッチの時間です"
                var comps = Calendar.current.dateComponents([.year, .month, .day], from: Date())
                comps.hour = 20; comps.minute = 0
                let start = Calendar.current.date(from: comps) ?? Date()
                event.startDate = start
                event.endDate = start.addingTimeInterval(10 * 60)
                event.recurrenceRules = [EKRecurrenceRule(recurrenceWith: .daily, interval: 1, end: nil)]
                event.calendar = store.defaultCalendarForNewEvents
                do {
                    try store.save(event, span: .thisEvent)
                    completion(true)
                } catch {
                    completion(false)
                }
            }
        }
    }
}

private struct MyRecordContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Binding var year: Int
    @Binding var month: Int
    @Binding var reachList: [RecordLogic.ReachEntry]
    @Binding var reachMsg: String?
    @Binding var calendarMsg: String?
    let doneDates: Set<String>
    let today: String
    let freezeLeft: Int
    let store: RecordStore
    let onConnectCalendar: (@escaping (Bool) -> Void) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                KyonoCard {
                    KyonoSectionHeader(icon: .calendarCheck, title: "マイ記録", fill: colors.pinkSoft, accent: colors.pink)
                    Spacer().frame(height: 12)
                    // ---- カレンダー(index.html:renderCal相当。§6 Step5b検収基準1) ----
                    HStack {
                        KyonoGhostButton("◀") { if month == 1 { month = 12; year -= 1 } else { month -= 1 } }
                            .frame(maxWidth: 60)
                        Spacer()
                        // Text(verbatim:)必須: 素朴なText("\(year)年...")はLocalizedStringKeyの
                        // 数値補間経由でロケール依存の桁区切り(2,026年のような表記)が入ってしまうため
                        // (実機検証で発見)。
                        Text(verbatim: "\(year)年\(month)月").font(.kyono(.black900, size: 16)).foregroundColor(colors.ink)
                        Spacer()
                        KyonoGhostButton("▶") { if month == 12 { month = 1; year += 1 } else { month += 1 } }
                            .frame(maxWidth: 60)
                    }
                    Spacer().frame(height: 8)
                    HStack {
                        ForEach(["日", "月", "火", "水", "木", "金", "土"], id: \.self) { w in
                            Text(w).font(.kyono(.black900, size: 12)).foregroundColor(colors.sub)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    calendarGrid
                }

                KyonoCard {
                    Text("🎫 おやすみ券").font(.kyono(.black900, size: 16)).foregroundColor(colors.ink)
                    Spacer().frame(height: 6)
                    Text(verbatim: "おやすみ券 のこり\(freezeLeft)枚").font(.kyono(.bold700, size: 15)).foregroundColor(colors.sub)
                    Spacer().frame(height: 8)
                    // index.html:414-415 .bar/.bar>div(teal系グラデーションの進捗バー)の1:1移植。
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(colors.line)
                            Capsule().fill(colors.teal)
                                .frame(width: geo.size.width * min(1, max(0, CGFloat(freezeLeft) / 3)))
                        }
                    }
                    .frame(height: 14)
                }

                KyonoCard {
                    KyonoSectionHeader(icon: .mountainCheck, title: "とどくメーター", fill: colors.yellowSoft)
                    Spacer().frame(height: 8)
                    Text(reachList.last.map { "いまの記録: 段位\($0.lv)" } ?? "まだ記録なし").font(.kyono(.bold700, size: 15)).foregroundColor(colors.sub)
                    Spacer().frame(height: 8)
                    // index.html:504-506 .reach-row(5列グリッド)/.reach-btn/.reach-btn.on(teal-strong塗り)の1:1移植。
                    HStack(spacing: 6) {
                        ForEach(Array(zip(1...5, ["ひざ", "すね", "足首", "つま先", "ゆか"])), id: \.0) { lv, label in
                            let on = reachList.last?.lv == lv
                            // 見た目パリティ第2弾 §3: タップ領域44pt以上ルールの再確認(既存ルール=HANDOFF.md)。
                            // Web版の13px paddingのままだと44ptをわずかに割り込むため、見た目(padding値)は
                            // 変えずminHeightで下限だけ確保する。
                            Text(label)
                                .font(.kyono(.black900, size: 14))
                                .foregroundColor(on ? .white : colors.sub)
                                .frame(maxWidth: .infinity, minHeight: 44)
                                .padding(.vertical, 13)
                                .background(on ? colors.tealStrong : colors.card)
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(on ? colors.tealStrong : colors.line, lineWidth: 2))
                                .cornerRadius(12)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    RecordLogic.setReach(store, lv: lv, now: Date())
                                    reachList = RecordLogic.getReach(store)
                                    reachMsg = "記録しました！"
                                }
                        }
                    }
                    if let reachMsg { Spacer().frame(height: 6); Text(reachMsg).font(.kyono(.bold700, size: 15)).foregroundColor(colors.teal) }
                }

                KyonoLineButton("📅 カレンダーに登録する") {
                    onConnectCalendar { ok in
                        calendarMsg = ok ? "カレンダーに追加しました" : "カレンダーへの追加が許可されませんでした"
                    }
                }
                if let calendarMsg { Text(calendarMsg).font(.kyono(.bold700, size: 15)).foregroundColor(colors.pink) }
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }

    private var calendarGrid: some View {
        let leading = CalendarLogic.firstWeekday(year: year, month: month)
        let days = CalendarLogic.daysInMonth(year: year, month: month)
        let rows = (leading + days + 6) / 7
        return VStack(spacing: 4) {
            ForEach(0..<rows, id: \.self) { r in
                HStack(spacing: 4) {
                    ForEach(0..<7, id: \.self) { c in
                        let day = r * 7 + c - leading + 1
                        Group {
                            if day >= 1 && day <= days {
                                let ds = CalendarLogic.dateString(year: year, month: month, day: day)
                                let isDone = doneDates.contains(ds)
                                let isToday = ds == today
                                let isFuture = ds > today
                                // index.html:406-409,413 .cal .d/.d.done(teal-strong塗り)/.d.today(pink枠)/.d.mute
                                Text(verbatim: "\(day)")
                                    .font(.kyono(.bold700, size: 15))
                                    .frame(maxWidth: .infinity, minHeight: 32)
                                    .foregroundStyle(isDone ? .white : (isFuture ? Color(hex: 0xD5CFBE) : colors.ink))
                                    .background(isDone ? colors.tealStrong : Color.clear)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(isToday ? colors.pink : .clear, lineWidth: 2.5))
                            } else {
                                Color.clear.frame(maxWidth: .infinity, minHeight: 32)
                            }
                        }
                    }
                }
            }
        }
    }
}
