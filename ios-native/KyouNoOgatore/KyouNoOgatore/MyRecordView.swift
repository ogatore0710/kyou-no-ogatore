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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("マイ記録").font(.title2.bold())

                // ---- カレンダー(index.html:renderCal相当。§6 Step5b検収基準1) ----
                HStack {
                    Button("◀") { if month == 1 { month = 12; year -= 1 } else { month -= 1 } }
                    Spacer()
                    // Text(verbatim:)必須: 素朴なText("\(year)年...")はLocalizedStringKeyの
                    // 数値補間経由でロケール依存の桁区切り(2,026年のような表記)が入ってしまうため
                    // (実機検証で発見)。
                    Text(verbatim: "\(year)年\(month)月").font(.headline)
                    Spacer()
                    Button("▶") { if month == 12 { month = 1; year += 1 } else { month += 1 } }
                }
                HStack {
                    ForEach(["日", "月", "火", "水", "木", "金", "土"], id: \.self) { w in
                        Text(w).frame(maxWidth: .infinity)
                    }
                }
                calendarGrid

                Text(verbatim: "おやすみ券 のこり\(freezeLeft)枚")

                Text("とどくメーター").font(.headline)
                Text(reachList.last.map { "いまの記録: 段位\($0.lv)" } ?? "まだ記録なし")
                HStack {
                    ForEach(1...5, id: \.self) { lv in
                        Button("\(lv)") {
                            RecordLogic.setReach(store, lv: lv, now: Date())
                            reachList = RecordLogic.getReach(store)
                            reachMsg = "記録しました！"
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                if let reachMsg { Text(reachMsg) }

                Button("カレンダーに登録する") {
                    connectCalendar { ok in
                        calendarMsg = ok ? "カレンダーに追加しました" : "カレンダーへの追加が許可されませんでした"
                    }
                }
                .buttonStyle(.borderedProminent)
                if let calendarMsg { Text(calendarMsg) }
            }
            .padding(20)
        }
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
                                Text(verbatim: "\(day)")
                                    .frame(maxWidth: .infinity, minHeight: 32)
                                    .foregroundStyle(isFuture ? .gray : .primary)
                                    .background(isDone ? Color(red: 0.61, green: 0.87, blue: 0.79) : Color.clear)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(isToday ? Color(red: 0.90, green: 0.42, blue: 0.60) : .clear, lineWidth: 2))
                            } else {
                                Color.clear.frame(maxWidth: .infinity, minHeight: 32)
                            }
                        }
                    }
                }
            }
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
