//
//  HomeView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 5a(マスタープラン§6 Step 5a): ホーム・記録フロー・チュートリアルフラグ機械の
//  実UI。Android版(MainActivity.kt HomeScreen)と同一ロジックのSwiftUI実装。RecordStore/RecordLogic/
//  HomeLogic/CardLottery/CardRenderer(Step2-4で作成済みの決定的ロジックパッケージ)を呼ぶだけに徹し、
//  判定ロジックの再実装は一切しない(masterplan §3-2/§2-4と同じ「判定はロジック層のみ」の原則)。
//
//  ⚠️ 未配線の注記(alan5への報告どおり): SafetyCore/RecordCore/CardCore(Step2-4で作成したローカル
//  Swift Package)をKyouNoOgatoreアプリターゲットの依存関係として追加するには、
//  Xcode > File > Add Package Dependencies > Add Local... のGUI操作が必要。project.pbxprojの
//  パッケージ依存記述を手編集でのXcodeプロジェクトファイル破損リスクを避けるため、あえて
//  Sonnetでは触らずGUI操作に委ねる(過去のXcode自動生成.gitlink化事故の教訓と同じ判断・§1-4)。
//  マスタープラン§6 Step5aの検収基準どおり、iOS側はこの時点ではビルド確認・実行確認をせず、
//  Android版(実タップ確認済み)とのロジック同一性のコードレビューのみを行う対象としてこのファイルを置く。
//
//  Step5aのスコープ(§6検収基準4件に絞っている): 動画カタログ本体・2週間プラン・カレンダー・
//  オンボ/ツアーUIはStep5b/5c/7aの範囲でありここには含めない(Android版と同じスコープ判断)。

import Combine
import SwiftUI
import UIKit
import WidgetKit
import RecordCore
import CardCore
import SafetyCore

private let CHEERS = [
    "ナイスご自愛🎉", "がんばったね！おつかれさまでした✨", "その数分が体を変えます💪",
    "イタ気持ちいい できました？😊", "体は正直！ちゃんと応えてくれますよ✨", "昨日の自分より1ミリ前へ🌱",
]

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:2124 QUOTES
// (45件)の1:1移植。手写し禁止(§1-2)のためindex.htmlから機械抽出した値をそのまま貼り付けている
// (Android版MainActivity.ktと同一のリスト。移植元はAndroid側で使ったのと同じ抽出スクリプト出力)。
private let QUOTES = [
    "体がガチガチでもだいじょうぶ", "頑張ろうね", "がんばったね おつかれさまでした",
    "痛気持ちいいところで止めましょうね", "腹筋は無理に使わなくていいですよ", "きつい方は足首を触ってくださいね",
    "呼吸 止めないでね", "昨日より1ミリ前に進んでたらOK", "休むのもストレッチのうちです",
    "3・2・1 はい おつかれさまでした", "体は正直！ちゃんと応えてくれます", "『できない』は『のびしろ』の別名です",
    "1日1本で十分です", "続けてるあなたがいちばんすごい", "お膝もいたわってあげてくださいね",
    "反動はつけなくて だいじょうぶですよ", "息を吐くと ゆるみますよ〜", "伸びてる場所を 感じてみてくださいね",
    "体がかたい日もあります そういう日もOK", "ゆっくりでだいじょうぶ 競争じゃないですから", "痛いのは がんばりすぎのサインです",
    "お風呂あがりは ゴールデンタイムです", "肩の力 ふっと抜いてみましょう", "続けてる人から 変わっていきます",
    "30秒が 体を変えていきます", "きのうのあなたより きょうのあなた", "深呼吸ひとつぶんの よゆうを",
    "固まったら ほぐせばいいんです", "首はやさしく いたわってあげて", "のびるって 気持ちいいですね〜",
    "サボっても再開したら それがいちばんえらい", "体があったまってる夜が ねらい目です", "「なんか調子いいかも」を見逃さないで",
    "ストレッチに 遅すぎることはないです", "こわばりは すこしずつ返していきましょう", "手が届かなくても 気持ちは届いてます",
    "姿勢がいいと 呼吸もふかくなりますよ", "がんばりやさんほど 休むのが仕事です", "伸ばした分だけ 楽になっていきます",
    "あしたの体は きょうつくられます", "完璧じゃなくていい つづくのがいちばん", "気持ちいい〜って 声に出してOKです",
    "体を大切にする時間 えらいです", "ひざは軽く曲げても いいですからね", "また明日も 待ってますね",
]

// index.html:1708 dayIndex()の1:1移植(現在時刻+6時間オフセットの日数カウンタ)。
private func dayIndex(_ now: Date) -> Int {
    Int((now.timeIntervalSince1970 * 1000 + 6 * 3600 * 1000) / 86400000)
}

// TASK-C2-2026-07-29-ux-audit-G.md G1: index.html:1528-1529 TODAY_ASA/TODAY_YORUの1:1移植
// (「きょうの1本」がタイプ未判定・プラン非実行時に日替わりで出す既定10本)。キーはV(index.html:1460)の
// キーであり、quizVideoKeyToId(OnboardingViews.swift・診断結果の3本おすすめで既に移植済み)を
// 再利用してYouTube動画IDへ変換する(同じ変換表を二重に持たない)。
private let TODAY_ASA = ["asa10", "asaGachi5", "asa9shi", "asaBaki9", "asa10kesen", "ogaRadio6", "asa5", "asa3", "honki9", "nagomi7"]
private let TODAY_YORU = ["yoru9umi", "yoru9ice", "yoru12kai", "jukusui9", "yoru15", "jiritsu10", "neochi10", "ofuro20", "ofuro6", "ashisuki"]

// index.html:1690 autoMode()の1:1移植(4時〜17時未満はあさ、それ以外はよる)。
private func autoTodayMode(_ now: Date) -> String {
    let hour = Calendar.current.component(.hour, from: now)
    return (4..<17).contains(hour) ? "asa" : "yoru"
}

struct HomeView: View {
    private let store: RecordStore
    let onStartTour: (Bool) -> Void
    let onOpenQuiz: () -> Void
    let onShowResult: (String) -> Void
    let onOpenSoudan: (String?) -> Void
    let onOpenMyRecord: () -> Void
    let onOpenSettings: () -> Void
    // TASK-C2-2026-07-27-behavior-parity-audit.md §B: index.html:4392-4393
    // scrollIntoView(todayVideo)の1:1移植用フラグ。
    var scrollToTodayPending: Binding<Bool> = .constant(false)
    // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C補足: rDoneNudgeBtn(結果画面)
    // 経由でHomeへ来たときも、通常の動画復帰と同じくshowDoneNudgeを立てる。
    var pendingDoneNudge: Binding<Bool> = .constant(false)

    @Environment(\.kyonoColors) private var colors
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    // iOS画面まるごとズーム第2段階(2026-07-29): 共有部品(第1段階)だけでなく、この画面自身が
    // 持つリテラルpadding/frame/cornerRadius/lineWidthにも1.18倍を適用する(フォントサイズは
    // .kyonoFont()側で既に掛かっているため対象外・第1段階と同じ除外理由)。
    @Environment(\.kyonoBigText) private var bigText
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }

    // ---- 永続状態(RecordStore経由でkyono-store.jsonへ) ----
    @State private var streak: RecordLogic.StreakData
    @State private var fd: String?
    @State private var fdday: String?
    @State private var plan: SdPlanData?
    @State private var typeResult: QuizTypeResult?
    // TASK-C2-2026-07-29-ux-audit-G.md G2: index.html:2028 welcomeCheck()のwb_seen・
    // index.html:2043 renderRecheck()のrecheck_seenの1:1移植(どちらも「1回見せたら消える」永続フラグ)。
    @State private var wbSeen: String
    @State private var recheckSeen: String
    // 2週間プラン完走お祝いカード欠落修正タスク(TASK-C2-2026-07-27-plan-completion-celebration.md):
    // index.html:1757-1759 planFinishedCache/planCelebratedの1:1移植(プロセス内メモリのみ・§2-3)。
    @State private var planFinishedCache: PlanFinishedCache?
    @State private var planCelebrated = false
    // TASK-C2-2026-07-27-offline-banner.md: index.html:4064-4080 envBanner(オフライン案内)の1:1移植。
    @StateObject private var networkMonitor = NetworkMonitor()

    // ---- プロセス内メモリ状態(§2-3: sessionStorage相当。永続化しない) ----
    @State private var lastDay: String
    @State private var pendingNudgeDate: String?
    @State private var showDoneNudge = false
    @State private var cheerText: String?
    // UI/UXパリティ監査GO-1(2026-07-28): app-record.js:133-139 節目カードの中身(ms!=nil分岐)。
    // 部品(CardDataLoader.shared.MSのd/t/m/q・KyonoConfetti)はあったが、ホーム画面のmarkDone
    // ハンドラから一度も接続されていなかった欠落を修正する(Android版HomeScreenと同一設計)。
    @State private var milestoneInfo: MilestoneInfo?
    // app-record.js:132 launchConfetti(ms?105:70)の1:1移植。同じcountの連続タップでも必ず
    // 再生させるため、単調増加するconfettiTriggerを.id()に使って毎回新規のKyonoConfettiとして
    // 張り替える(PlanDoneCardViewのshowConfettiOnceと違い、Homeでは同じViewが繰り返し使われるため)。
    @State private var confettiTrigger: Int?
    @State private var confettiCount = 70
    @State private var cardResult: TodayCardResult?
    @State private var doneBtnScale: CGFloat = 1
    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-record.js:196-208 fdCardNudge/fd-breatheと
    // app-record.js:140-149 1日目クリア時のcheer差し替え(fd-cardpop)の1:1移植。
    @State private var fdCardNudgeVisible = false
    @State private var fdCelebrationVisible = false
    @State private var makeCardBtnBreatheScale: CGFloat = 1
    // TASK-C2-2026-07-27-local-notifications.md: 1日目クリアの場面で出す「あしたも
    // おしらせしようか？」の提案(プロセス内メモリのみ・§2-3)。
    @State private var showNotifPrompt = false

    @Environment(\.scenePhase) private var scenePhase

    init(
        store: RecordStore, onStartTour: @escaping (Bool) -> Void, onOpenQuiz: @escaping () -> Void,
        onShowResult: @escaping (String) -> Void, onOpenSoudan: @escaping (String?) -> Void,
        onOpenMyRecord: @escaping () -> Void, onOpenSettings: @escaping () -> Void,
        scrollToTodayPending: Binding<Bool> = .constant(false),
        pendingDoneNudge: Binding<Bool> = .constant(false)
    ) {
        self.store = store
        self.onStartTour = onStartTour
        self.onOpenQuiz = onOpenQuiz
        self.onShowResult = onShowResult
        self.onOpenSoudan = onOpenSoudan
        self.onOpenSettings = onOpenSettings
        self.onOpenMyRecord = onOpenMyRecord
        self.scrollToTodayPending = scrollToTodayPending
        self.pendingDoneNudge = pendingDoneNudge
        let s = RecordLogic.loadStreak(store)
        _streak = State(initialValue: s)
        _fd = State(initialValue: store.get("fd", default: nil))
        _fdday = State(initialValue: store.get("fdday", default: nil))
        _lastDay = State(initialValue: RecordLogic.todayStr(now: Date()))
        _plan = State(initialValue: store.get("plan", default: nil))
        _typeResult = State(initialValue: store.get("type", default: nil))
        _wbSeen = State(initialValue: store.get("wb_seen", default: ""))
        _recheckSeen = State(initialValue: store.get("recheck_seen", default: ""))
    }

    private var today: String { RecordLogic.todayStr(now: Date()) }
    private var did: Bool { streak.dates.contains(today) }
    // index.html:2028 welcomeCheck()の1:1移植。streak.datesは記録順に追記されるだけで並べ替えない
    // (Web版のst.dates[st.dates.length-1]と同じ前提)ため、最後の要素をそのまま最終記録日として使う。
    private var showWelcomeBack: Bool {
        guard !streak.dates.isEmpty, !did, let last = streak.dates.last else { return false }
        return RecordLogic.daysBetween(last, today) >= 3 && wbSeen != today
    }
    private func closeWelcomeBack() {
        wbSeen = today
        store.set("wb_seen", today)
    }
    // index.html:2043 renderRecheck()の1:1移植。
    private var showRecheck: Bool {
        guard let at = typeResult?.at else { return false }
        return RecordLogic.daysBetween(at, today) >= 14 && recheckSeen != at
    }
    private func dismissRecheck() {
        guard let at = typeResult?.at else { return }
        recheckSeen = at
        store.set("recheck_seen", at)
    }
    // index.html:2049 goRecheck()の1:1移植。Web版のnavTo('reach')相当は、ネイティブでは「とどく
    // メーター」がMyRecordタブ内にインライン移植済み(KyouNoOgatoreApp.swift参照)のため、
    // 独立画面へは遷移させずMyRecordを開く。
    private func goRecheck() {
        dismissRecheck()
        onOpenMyRecord()
    }
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §1: app-record.js:48の1:1移植。
    private var streakBrokenNow: Bool { !did && RecordLogic.streakBrokenNow(store, streak, now: Date()) }
    private var fdFocusOn: Bool { HomeLogic.fdFocusHomeActive(fd: fd, streakTotal: streak.total, fdday: fdday, today: today) }
    private var checked: Bool { typeResult != nil && quizTypes[typeResult!.key] != nil }

    // TASK-C2-2026-07-27-auto-theme-time-rule.md: app-env.js:60 setInterval(refreshDay,60000)の
    // 1:1移植。従来はscenePhaseの.active復帰でしか日付またぎを見ていなかったため、開いたまま
    // 深夜0時をまたいだ場合に表示が更新されなかった(Android版checkRefreshDay()と同じ抜け)。
    private func checkRefreshDay() {
        let r = HomeLogic.refreshDay(now: Date(), lastDay: lastDay)
        if r.dayChanged {
            lastDay = r.today
            streak = RecordLogic.loadStreak(store)
            fd = store.get("fd", default: nil)
            fdday = store.get("fdday", default: nil)
        }
        if HomeLogic.shouldShowDoneNudge(pendingNudgeDate: pendingNudgeDate, today: r.today, streakDates: streak.dates) {
            showDoneNudge = true
        }
        pendingNudgeDate = nil
    }

    // TASK-C2-2026-07-29-ux-audit-G.md G1: 動画タップ時の共通処理(旧・仮実装のボタンにあった
    // pendingNudge復帰導線をそのまま引き継ぐ)。
    private func openTodayVideo(_ urlString: String) {
        pendingNudgeDate = RecordLogic.todayStr(now: Date())
        if let url = URL(string: urlString) {
            UIApplication.shared.open(url)
        }
    }

    private let dayTicker = Timer.publish(every: 60, on: .main, in: .common).autoconnect()

    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §2: index.html:2718
    // closeCard()→fdTourMaybeStart()の1:1移植。Web版はどう閉じても(とじるボタン・スワイプ下ろし・
    // 背景タップ)必ずfdTourMaybeStart()を呼ぶため、カードを閉じる経路を1箇所にまとめて両方から使う
    // (以前は「とじる」ボタンのactionにしかこのロジックが無かった)。
    private func closeCardAndMaybeStartTour() {
        cardResult = nil
        let tourpend: Bool = store.get("tourpend", default: false)
        let tourseen: Bool = store.get("tourseen", default: false)
        if tourpend && !tourseen {
            store.set("tourpend", false)
            store.set("tourseen", true)
            fdCardNudgeVisible = false
            // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §B):
            // index.html:4293 setTimeout(obOpenTour,350)の1:1移植。カードモーダルの閉じるアニメーションが
            // 視覚的に完了してからツアーを開始する。
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                onStartTour(true)
            }
        }
    }

    // KyonoThemeでの配色解決はRootView(KyouNoOgatoreApp.swift)側で行う(タブバー・FABとも共通の
    // 配色を1箇所で解決するため。二重ラップを避ける)。
    var body: some View {
        ZStack {
            homeContent
            // UI/UXパリティ監査GO-1(2026-07-28): index.html:1919-1942 launchConfetti()は
            // position:fixedの全画面canvasなので、homeContentの上に重ねる。confettiTriggerを
            // .id()に使い、同じcountの連続タップでも必ず新規Viewとして張り替えて再生させる
            // (PlanDoneCardViewのKyonoConfettiと同じ部品。§D reduceMotion時は不発火)。
            if let confettiTrigger, !reduceMotion {
                KyonoConfetti(count: confettiCount)
                    .id(confettiTrigger)
                    .allowsHitTesting(false)
            }
        }
    }

    // Step7bで導線ボタンを5件追加し画面高さを超えるようになったため、Android版HomeScreenの
    // .verticalScroll追加と同じ理由でScrollViewを使う(スクロールが無いと下部ボタンに到達できない)。
    // ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md):
    // index.html #home(602行〜)のカード積み重ね構成の1:1移植(Android版HomeScreenと同一ロジック)。
    private var homeContent: some View {
        ScrollViewReader { proxy in
        ScrollView {
        VStack(spacing: 16 * zoom) {
            // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94 .logoの1:1移植をKyonoAppHeaderへ
            // 共通化(マイ記録/動画を探す/使い方の3タブにも同じ部品を展開する)。
            KyonoAppHeader()

            // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:602-603
            // .qbubble(カードの外・chara-hitokoto.pngアバター+日替わりひとこと)の1:1移植。
            // pendingVideoReturnActive()相当(showDoneNudge)のときだけ「おかえりなさい」に差し替える
            // (旧来の別カードdoneNudgeCardは廃止しqbubble1本に統合)。
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 4 * zoom) {
                    Text(showDoneNudge ? "おかえりなさい" : "きょうのひとこと")
                        .kyonoFont(.black900, size: 11).foregroundColor(colors.sub)
                    Text(showDoneNudge
                        ? "おわったら下の「きょうやった！」を押してね✅"
                        : "「\(QUOTES[((dayIndex(Date()) % QUOTES.count) + QUOTES.count) % QUOTES.count])」")
                        .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                }
                .padding(.horizontal, 14 * zoom).padding(.vertical, 10 * zoom)
                .background(
                    RoundedRectangle(cornerRadius: 16 * zoom).fill(colors.card)
                        .overlay(RoundedRectangle(cornerRadius: 16 * zoom).stroke(colors.line, lineWidth: 1.5 * zoom))
                )
                Spacer()
                KyonoCharaImage(name: "chara-hitokoto").frame(height: 44 * zoom)
            }

            // TASK-C2-2026-07-27-offline-banner.md: index.html:4064-4080 envBanner(オフライン案内)の
            // 1:1移植。YouTubeアプリ内ブラウザ脱出案内等のA2HS/PWA固有の他用途は移植対象外(§2-2)なので、
            // 単純に「オフラインなら表示・オンラインなら非表示」でよい(Web版のenvBannerPrevHTML退避は不要)。
            if networkMonitor.isOffline {
                Text("いま電波がないみたい📡 動画を見るには電波が必要だよ（「きょうやった！」の記録はつけられるよ）")
                    .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(9)
                    .padding(.horizontal, 12 * zoom).padding(.vertical, 10 * zoom)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 14 * zoom).fill(colors.yellowSoft)
                            .overlay(RoundedRectangle(cornerRadius: 14 * zoom).stroke(colors.yellow, lineWidth: 1.5 * zoom))
                    )
            }

            // TASK-C2-2026-07-29-ux-audit-G.md G2: index.html:606-611 #welcomeBack(welcomeCheck())の
            // 1:1移植。HomeView既存の`showDoneNudge`(動画から戻った直後の「おかえりなさい」)とは別物
            // (あちらはqbubbleの見出し差し替えのみ・こちらは3日以上あいた復帰を祝う専用カード)。
            if showWelcomeBack {
                KyonoGradientCard(gradient: .mint) {
                    KyonoCharaImage(name: "chara").frame(width: 84, height: 84)
                        .frame(maxWidth: .infinity, alignment: .center)
                    // Text連結(+)はText型のみ許容するため、ここだけ.font(.kyono(...))を直接使う
                    // (HomeView.swift既存の停滞期はげまし文言と同じ理由)。
                    (Text("おかえりなさい！また会えてうれしいです🌱").font(.kyono(.black900, size: 17))
                        + Text("\n\n休んでも習慣はこわれません\n体は数日で取り返せます\n").font(.kyono(.bold700, size: 15))
                        + Text("通算\(streak.total)日は残っています").font(.kyono(.black900, size: 15)))
                        .foregroundColor(colors.ink)
                    KyonoPrimaryButton("ゆるっと再開する") { closeWelcomeBack() }
                    KyonoGhostButton("いまの体でかたさチェック") { closeWelcomeBack(); onOpenQuiz() }
                }
            }
            // index.html:612-615 #recheckCard(renderRecheck())の1:1移植。かたさチェックから14日後に
            // 「とどくメーター」での再測定に誘う(ネイティブに独立したreach画面は無く、Web版navTo('reach')
            // 相当はMyRecordタブ内にインライン移植済みのため、そちらへ遷移させる)。
            if showRecheck {
                KyonoGradientCard(gradient: .mint) {
                    Text("チェックから2週間たったよ🌱\n前屈 どこまで届くようになった？")
                        .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(9)
                    KyonoPrimaryButton("とどくメーターで測ってみる") { goRecheck() }
                    KyonoGhostButton("あとで") { dismissRecheck() }
                }
            }

            if !checked {
                CkCard(full: true, typeResult: typeResult, onStartQuiz: onOpenQuiz, onShowResult: onShowResult)
                SoudanCard(onOpenSoudan: onOpenSoudan)
            }

            // index.html:654-664 #todayCard(きょうの1本)相当。TASK-C2-2026-07-29-ux-audit-G.md G1:
            // 「押すとYouTubeのトップページが開くだけ」の仮実装を、renderToday()の1:1移植へ差し替える
            // (プラン優先→タイプ判定→あさ/よる自動判定の順。セグメント切替UIは「最低ライン」の
            // 注記どおり第2段へ送る=いまは自動選出のみ)。
            if !fdFocusOn {
                KyonoCard {
                    KyonoSectionTitle("きょうの1本", icon: .play)
                    TodayVideoSection(plan: plan, typeResult: typeResult, onVideoTap: openTodayVideo)
                }
                // TASK-C2-2026-07-27-behavior-parity-audit.md §B: index.html:4392-4393
                // scrollIntoView(todayVideo)のスクロール先識別子。
                .id("todayCard")
            } else {
                KyonoBodyText("🌱 はじめの1本ガイド中")
            }

            // index.html:1781 renderPlanCard相当(相談室から発行した14日プランの進捗表示)。Web版DOM順
            // (index.html:664 todayCardの直後・streakCardの直前)に合わせて位置を修正。
            if let plan {
                PlanProgressCardView(
                    store: store, plan: plan, onCleared: { self.plan = nil },
                    onFinished: { cache in planFinishedCache = cache }
                )
            }
            // 2週間プラン完走お祝いカード欠落修正タスク(TASK-C2-2026-07-27-plan-completion-celebration.md):
            // index.html:678-684 #planDoneCardの1:1移植。planと独立させる(finishedになった瞬間に
            // plan=nilで消えてしまわないよう、専用のキャッシュ状態から描画する)。
            if let cache = planFinishedCache {
                PlanDoneCardView(
                    cache: cache, alreadyCelebrated: planCelebrated,
                    onCelebrate: { planCelebrated = true },
                    onPlanAgain: {
                        // index.html:1817 planAgain()の1:1移植。state.mode/mode_manualはネイティブに
                        // 「きょうの1本」モード切替の仕組み自体が無い(§2-2的な既存スコープ判断)ため
                        // 対応するstore書き込みは行わない。
                        let newPlan = SdPlanData(intentId: cache.intentId, label: cache.label, videos: cache.videos, start: today, days: cache.days)
                        store.set("plan", newPlan)
                        plan = newPlan
                        planFinishedCache = nil
                    },
                    onStartQuiz: { planFinishedCache = nil; onOpenQuiz() },
                    onClose: { planFinishedCache = nil }
                )
            }

            // index.html:686 #streakCard(続けた日数・通算)相当。
            KyonoCard {
                KyonoSectionTitle("続けた日数（通算）", icon: .calendarCheck)
                KyonoStreakText(streak.total, streakCount: streak.count, brokenNow: streakBrokenNow)
                // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #home):
                // index.html:693 #fdDoneStaticNudge(はじめの1本ガイド中・未記録のときだけ出す常時案内)の
                // 1:1移植。HomeLogic.fdActive(fd/streakTotalのみ・fdday条件なし)をそのまま使う。
                if HomeLogic.fdActive(fd: fd, streakTotal: streak.total) && !did {
                    Text("動画を見おわったら、ここを押してね👇")
                        .kyonoFont(.black900, size: 14).foregroundColor(colors.pink)
                        .multilineTextAlignment(.center).frame(maxWidth: .infinity)
                }
                // UI/UXパリティ監査GO-8(2026-07-28): index.html:382 .done-btn.did
                // (背景グレー・影なし・文字縮小)の1:1移植。
                KyonoPrimaryButton(did ? "きょうの分は完了！おつかれさまでした😊" : "きょうやった！", enabled: !did, flatWhenDisabled: true) {
                    guard !did else { return }
                    // 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §3): Web版には無い
                    // ネイティブならではの上乗せとして、主要アクションに軽いハプティクスを追加
                    // (情報構造・文言・並び順はWeb版のまま変更しない「仕上げ方」のみの改善)。
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    // app-record.js:100-102 guide判定(fdフラグを1へ立てる前に読む)の1:1移植。
                    let wasGuide = fd == "go"
                    RecordLogic.markDone(store, now: Date())
                    streak = RecordLogic.loadStreak(store)
                    // TASK-C2-2026-07-27-local-notifications.md: 記録のたびに次回通知を予約し直す
                    // (今日はもう記録済みなので、次は翌日以降の分に自動でずれる)。
                    DailyNotifications.resync(store: store)
                    // GO-H1(ホーム画面ウィジェット): 記録した瞬間にサマリを書き出し、ウィジェットを
                    // 即時更新する(発注書§4)。RecordStore本体には触れない片道の書き出しのみ。
                    WidgetSummaryWriter.write(store: store)
                    WidgetCenter.shared.reloadAllTimelines()
                    let ms = CardDataLoader.shared.MS.first { $0.d == streak.total }
                    // §2-4許容箇所: markDoneのcheer選択のみ乱数OK。withAnimationはindex.html:311-312
                    // cpop(.3s ease-out)の1:1移植(下のtransitionと対で挿入時のポップ演出になる)。
                    // app-record.js:133-149: 節目とは重ならない前提(通算1日目=guideの唯一の発生
                    // タイミングはMSの最小値3より前)だが、念のため節目表示を優先する構造にしてある
                    // (このelse ifは節目でないときだけ通る)。
                    if let ms {
                        fdCelebrationVisible = false
                        cheerText = nil
                        withAnimation(.easeOut(duration: 0.3)) { milestoneInfo = ms }
                    } else if wasGuide {
                        withAnimation(.easeOut(duration: 0.5)) { fdCelebrationVisible = true }
                        cheerText = nil
                        milestoneInfo = nil
                        // 1日目クリアの場面で通知の許可を提案する(まだ有効化していないときだけ)。
                        // 起動直後・オンボ中には出さない(この分岐自体が1日目クリア後にしか
                        // 到達しないため自然に満たされる)。
                        if !store.get("notif_enabled", default: false) {
                            showNotifPrompt = true
                        }
                    } else {
                        fdCelebrationVisible = false
                        milestoneInfo = nil
                        withAnimation(.easeOut(duration: 0.3)) { cheerText = CHEERS.randomElement() }
                    }
                    // UI/UXパリティ監査GO-1: app-record.js:132 launchConfetti(ms?105:70)の1:1移植。
                    if !reduceMotion {
                        confettiCount = ms != nil ? 105 : 70
                        confettiTrigger = (confettiTrigger ?? 0) + 1
                    }
                    if wasGuide {
                        store.set("fd", "1")
                        fd = "1"
                        // app-record.js:107 markDone内でtourpend=1相当。実際の起動はカードモーダルを
                        // 閉じた「区切り」でcardCloseBtn側が拾う(fdTourMaybeStart相当)。
                        store.set("tourpend", true)
                        fdCardNudgeVisible = true
                    }
                    cardResult = renderTodayCard(store: store, streak: streak, ds: today)
                }
                .scaleEffect(doneBtnScale)
                .id("doneBtn")
                // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A): index.html:384
                // .done-btn.nudge-pulse(doneNudgePulse 0.7s×2回・scale 1↔1.045)の1:1移植。
                // 動画から戻ってきてshowDoneNudgeが立った瞬間だけ2回パルスして気づかせる。
                .onChange(of: showDoneNudge) { _, newValue in
                    guard newValue else { return }
                    withAnimation(.easeInOut(duration: 0.35)) { doneBtnScale = 1.045 }
                    withAnimation(.easeInOut(duration: 0.35).delay(0.35)) { doneBtnScale = 1 }
                    withAnimation(.easeInOut(duration: 0.35).delay(0.7)) { doneBtnScale = 1.045 }
                    withAnimation(.easeInOut(duration: 0.35).delay(1.05)) { doneBtnScale = 1 }
                    // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C:
                    // index.html:4006-4013の1:1移植。ボタンが画面外だとパルスに気づけないため、
                    // 画面中央へ寄せる。HomeViewが表示されている時点でWeb版のcurrentSection==="home"
                    // 条件は常に成立している(Home以外の画面ではこのViewごと非表示のため)。
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                        if reduceMotion {
                            proxy.scrollTo("doneBtn", anchor: .center)
                        } else {
                            withAnimation { proxy.scrollTo("doneBtn", anchor: .center) }
                        }
                    }
                }
                if fdCelebrationVisible {
                    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-record.js:140-149 1日目クリア時の
                    // cheer差し替え(fd-cardpop=fdPop .5s cubic-bezier(.34,1.56,.64,1)バウンド付き
                    // ポップイン)の1:1移植。
                    VStack(alignment: .leading, spacing: 6 * zoom) {
                        Text("🎉 1日目クリア！ナイスご自愛！")
                            .kyonoFont(.black900, size: 16).foregroundColor(colors.pink)
                        HStack {
                            Spacer()
                            KyonoCharaImage(name: "card-sample").frame(width: 140 * zoom, height: 140 * zoom)
                            Spacer()
                        }
                        Text("きょうの記録が1まい目のカードになったよ ためると図鑑がうまっていく📖")
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                        Text("よかったら下に✍️きょうのひとことをどうぞ からだの感じをひとことでOK（あとからでもいいよ）")
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                    }
                    // §D: index.html:214-220 fd-cardpopはprefers-reduced-motion:no-preference時のみ発火する。
                    .transition(
                        reduceMotion
                            ? .opacity.animation(.easeOut(duration: 0))
                            : .scale(scale: 0).combined(with: .opacity).animation(.timingCurve(0.34, 1.56, 0.64, 1, duration: 0.5))
                    )
                }
                // TASK-C2-2026-07-27-local-notifications.md §4: 1日目クリアの場面で初めて許可
                // ダイアログを出す(起動直後・オンボ中には出さない)。断られてもしつこく再提案しない
                // (この分岐は1日目クリア=fd=="go"のときにしか到達しないため、自然に一度きりになる)。
                if showNotifPrompt {
                    VStack(alignment: .leading, spacing: 8 * zoom) {
                        Text("あしたも おしらせしようか？").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                        HStack {
                            KyonoGhostButton("ううん") { showNotifPrompt = false }
                            KyonoPrimaryButton("うん！") {
                                DailyNotifications.requestAuthorization { granted in
                                    if granted {
                                        store.set("notif_enabled", true)
                                        DailyNotifications.resync(store: store)
                                    }
                                    showNotifPrompt = false
                                }
                            }
                        }
                    }
                    .padding(.top, 4 * zoom)
                }
                if let cheerText {
                    // 挙動パリティ監査タスク §A: index.html:311-312 cpop(scale .85→1・opacity .4→1・
                    // .3s ease-out)の1:1移植。応援メッセージがポップして出る演出が欠落していたため追加。
                    KyonoBodyText(cheerText)
                        .transition(.scale(scale: 0.85).combined(with: .opacity))
                }
                // UI/UXパリティ監査GO-1(2026-07-28): app-record.js:133-139 節目カードの中身
                // (ms!=nil分岐)の1:1移植。cheerTextと同じ#cheer要素への差し込みなので、同じ
                // cpop(scale .85→1・opacity .4→1・.3s ease-out)演出を使う(fd-cardpopの
                // 弾むバウンドとは別物)。
                if let milestoneInfo {
                    VStack(alignment: .leading, spacing: 0) {
                        Text("🎉 \(milestoneInfo.t)！（通算\(streak.total)日）")
                            .kyonoFont(.black900, size: 16).foregroundColor(colors.pink)
                        if !milestoneInfo.m.isEmpty {
                            Spacer().frame(height: 4 * zoom)
                            Text(milestoneInfo.m).kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                        }
                        if !milestoneInfo.q.isEmpty {
                            Spacer().frame(height: 8 * zoom)
                            VStack(alignment: .leading, spacing: 2 * zoom) {
                                Text("💬 せんぱいの声").kyonoFont(.black900, size: 13).foregroundColor(colors.teal)
                                Text(milestoneInfo.q.hasSuffix("（先輩の声）")
                                    ? String(milestoneInfo.q.dropLast("（先輩の声）".count))
                                    : milestoneInfo.q)
                                    .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            }
                            .padding(.horizontal, 12 * zoom).padding(.vertical, 9 * zoom)
                            .background(colors.bg)
                            .overlay(RoundedRectangle(cornerRadius: 12 * zoom).stroke(colors.line, lineWidth: 1.5 * zoom))
                            .clipShape(RoundedRectangle(cornerRadius: 12 * zoom))
                        }
                        Spacer().frame(height: 10 * zoom)
                        HStack {
                            Spacer()
                            KyonoCharaImage(name: "chara-crown").frame(width: 72 * zoom, height: 72 * zoom)
                            Spacer()
                        }
                        if !CardDataLoader.shared.MILESTONE_MSG_VIDEO.isEmpty {
                            Spacer().frame(height: 10 * zoom)
                            KyonoGhostButton("尾形さんからお祝いメッセージ") {
                                if let url = URL(string: "https://www.youtube.com/watch?v=\(CardDataLoader.shared.MILESTONE_MSG_VIDEO)") {
                                    UIApplication.shared.open(url)
                                }
                            }
                        }
                    }
                    .transition(.scale(scale: 0.85).combined(with: .opacity))
                }
                // 全画面完全性監査タスク #home: index.html:697-701 #memoRow(ひとことメモ入力欄)の1:1移植。
                // きょう記録済みのときだけ表示し、RecordLogic.saveMemo(既存の純粋関数)を呼ぶだけに徹する
                // (判定・データ構造は変更しない)。
                if did {
                    HomeMemoRow(store: store, today: today)
                }
                // 全画面完全性監査タスク #home: index.html:702 #plateauNote(通算12-16日/28-34日の
                // 停滞期はげまし文言)の1:1移植。app-record.js:58-62の閾値をそのまま使う。
                if !did {
                    if (12...16).contains(streak.total) {
                        // Text連結(+)はText型のみ許容するため、ここだけ.font(.kyono(...))を直接使う
                        // (bigtextの1.18倍はこの1箇所のみ非適用・影響は軽微)。
                        (Text("💡 いまは効果を感じにくい時期！体は変わり続けていますよ ")
                            + Text("とどくメーター").font(.kyono(.black900, size: 14)).foregroundColor(colors.tealInk)
                            + Text("で確かめてみて"))
                            .font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub)
                            .onTapGesture { onOpenMyRecord() }
                    } else if (28...34).contains(streak.total) {
                        Text("💡 1ヶ月ちかくまで来ました この時期を過ぎると変化を感じた報告がぐっと増えますよ のんびりどうぞ")
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                    }
                }
                // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-record.js:196-208 fdCardNudge
                // (「👇 つぎは ここを押してみて」)の1:1移植。
                if fdCardNudgeVisible {
                    Text("👇 つぎは ここを押してみて")
                        .kyonoFont(.black900, size: 14).foregroundColor(colors.pink)
                        .multilineTextAlignment(.center).frame(maxWidth: .infinity)
                }
                // TASK-C2-2026-07-29-ux-audit-G.md G3: index.html:703のボタン名「記録カードを画像でのこす」
                // の1:1移植。ツアーSlide3(OnboardingViews.swift)と使い方タブ(GuideView.swift)の案内は
                // どちらもこの文言で「◯◯を押す」と約束しており、ボタン名が「記録カードを見る」のままだと
                // ツアーを真面目に読む人ほど存在しないボタンを探すことになっていた。
                KyonoGhostButton("記録カードを画像でのこす") {
                    cardResult = renderTodayCard(store: store, streak: streak, ds: today)
                }
                .opacity(did ? 1 : 0.5)
                .disabled(!did)
                // app-record.js:196-208 fd-breathe(1.8s ease-in-out infinite・scale 1↔1.025)の1:1移植。
                .scaleEffect(fdCardNudgeVisible ? makeCardBtnBreatheScale : 1)
                .onAppear {
                    guard fdCardNudgeVisible && !reduceMotion else { return }
                    withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { makeCardBtnBreatheScale = 1.025 }
                }
                .onChange(of: fdCardNudgeVisible) { _, newValue in
                    if newValue && !reduceMotion {
                        withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { makeCardBtnBreatheScale = 1.025 }
                    } else {
                        makeCardBtnBreatheScale = 1
                    }
                }
                // 全画面完全性監査タスク #home: index.html:705 #cardHint(記録カードボタン下の常時ヒント)の1:1移植。
                // TASK-C2-2026-07-29-ux-audit-G.md G3(引き算): 未記録(!did)の間はボタン自体が無効化
                // されており、「保存かシェアでのこしてね」は存在しないカードの操作を約束してしまう
                // ため、その間だけ非表示にする(Web版はボタンの有効/無効と連動していないが、
                // ここはWebとの1:1パリティより実際の操作可能性に合わせる、という明示の指示)。
                if did {
                    Text("カード画像を保存かシェアでのこしてね📤")
                        .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                        .multilineTextAlignment(.center).frame(maxWidth: .infinity)
                }
            }

            // チェック済みのときはckCard(ミニ)+soudanCardをここ(streakCardの直後)に移動。
            if checked {
                CkCard(full: false, typeResult: typeResult, onStartQuiz: onOpenQuiz, onShowResult: onShowResult)
                SoudanCard(onOpenSoudan: onOpenSoudan)
            }
        }
        // index.html:82 body{padding:20px 18px 180px}の1:1移植。下だけ180ptと大きいのは
        // §C(scrollTo(doneBtn, anchor:.center)相当のdoneBtn中央寄せ)がページ末尾付近の
        // 要素でも実際に中央まで届くための余白(TASK-C2-2026-07-28: ページ末尾に近い状態だと
        // ScrollViewの実コンテンツ高さが足りずanchor:.centerが効かないまま見た目上
        // 「動いていない」ように見えるバグの根本原因だった。Android版MainActivity.ktの
        // HomeScreen Columnと同じ修正)。UI/UXパリティ監査GO-9・G6(2026-07-28)で共通定数化。
        .kyonoScreenPadding()
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
        // app-env.js:60 refreshDay相当。visibilitychangeの代わりにscenePhaseの.active復帰で
        // 日付またぎ・pendingNudgeを確認する(Android版のON_RESUMEと同じ役割)。
        .onChange(of: scenePhase) { _, newPhase in
            guard newPhase == .active else { return }
            checkRefreshDay() // checkDoneNudgeと同じ「一度出したら消す」もcheckRefreshDay内で行う
            // TASK-C2-2026-07-27-local-notifications.md: 前面復帰のたびに次回通知を予約し直す
            // (UNUserNotificationCenterのnon-repeating方式は自分で発火するたび次を積み直せない
            // ため、アプリを開くたびに再同期する設計。DailyNotifications.swiftの冒頭コメント参照)。
            DailyNotifications.resync(store: store)
        }
        .onReceive(dayTicker) { _ in checkRefreshDay() }
        // GO-G5(5視点ワンループ): ObuPreviewPopupの背景タップで閉じるパターンをこのカードモーダルにも
        // 適用(以前は.sheet()でスワイプでしか閉じられなかった)。
        .overlay {
            KyonoCardModalOverlay(isPresented: cardResult != nil, onClose: closeCardAndMaybeStartTour) {
                if let cardResult {
                    VStack {
                        Image(uiImage: cardResult.image).resizable().scaledToFit()
                        // TASK-C2-2026-07-27-milestone-card-export-nudge.md: index.html:1199,2783
                        // cardMsExportNudgeの1:1移植。節目カード(じまんカードは対象外=このシートは
                        // 元々きょうの記録カード専用)のときだけ、記録のひかえ(エクスポート)を促す。
                        if cardResult.isMilestone {
                            Text("せっかくの節目！記録のひかえを取っておくと あんしんです📦")
                                .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                                .multilineTextAlignment(.center)
                            KyonoGhostButton("記録のひかえを取る") {
                                self.cardResult = nil
                                onOpenSettings()
                            }
                        }
                        // TestFlight実機フィードバックD3(2026-07-29): index.html:1197-1199
                        // (btn-primary「保存・シェアする」→btn-line「とじる」の縦積み・各100%幅)の
                        // 1:1移植。以前はHStackで横並びにしていたため、幅を分け合った
                        // 「保存・シェアする」だけが2行に折り返し、1行の「とじる」と高さ・上端が
                        // 揃わなかった。絵文字(Web版📤)は本人の新ガイドライン(ボタン・タブ・見出しには
                        // OS絵文字を使わない・アイコンはデザイン生成のものを使う)により持ち込まない。
                        VStack(spacing: 12 * zoom) {
                            // index.html shareCard()相当(Step7bで新規実装)。
                            KyonoPrimaryButton("保存・シェアする") {
                                ShareImage.share(uiImage: cardResult.image, text: "#きょうのオガトレ \(streak.total)日目！")
                            }
                            KyonoLineButton("とじる", action: closeCardAndMaybeStartTour)
                        }
                    }
                }
            }
        }
        // TASK-C2-2026-07-27-behavior-parity-audit.md §B →
        // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §B修正: index.html:4393
        // scrollIntoView(todayVideo)(引数なし=ブラウザ既定behavior:"auto"=瞬時)の1:1移植。
        // オンボ完了直後だけ「きょうの1本」へ瞬時スクロールする(60msはindex.html:4393と同じ、
        // 直前のレイアウト確定を待つ猶予。withAnimationを付けるとWeb版より演出過剰になるため外す)。
        .onChange(of: scrollToTodayPending.wrappedValue) { _, pending in
            guard pending else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.06) {
                proxy.scrollTo("todayCard", anchor: .top)
                scrollToTodayPending.wrappedValue = false
            }
        }
        // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C補足: rDoneNudgeBtn(結果画面)
        // 経由でHomeへ来たときも、通常の動画復帰と同じくshowDoneNudgeを立てる(pulse+中央寄せの両方が
        // 自然に効く)。
        .onChange(of: pendingDoneNudge.wrappedValue) { _, pending in
            guard pending else { return }
            showDoneNudge = true
            pendingDoneNudge.wrappedValue = false
        }
        }
    }
}

// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #home): index.html:697-701
// #memoRow(ひとことメモ入力欄+「メモをのこす」ボタン)の1:1移植。RecordLogic.saveMemo/loadMemos
// (既存の純粋関数)を呼ぶだけに徹する(判定・データ構造は変更しない)。
private struct HomeMemoRow: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.kyonoBigText) private var bigText
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }
    let store: RecordStore
    let today: String

    @State private var text: String
    @State private var saved = false
    @State private var savedNote: String?

    init(store: RecordStore, today: String) {
        self.store = store
        self.today = today
        _text = State(initialValue: RecordLogic.loadMemos(store)[today] ?? "")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8 * zoom) {
            TextField("ひとことメモをどうぞ", text: Binding(
                get: { text },
                set: { text = String($0.prefix(30)); saved = false }
            )).textFieldStyle(.roundedBorder)
            KyonoLineButton(saved ? "のこしました ✓" : "メモをのこす", enabled: !saved) {
                // GO-G7(5視点ワンループ): 「きょうやった！」と同じ軽いハプティクスを完了系操作に広げる。
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                RecordLogic.saveMemo(store, today: today, text: text)
                savedNote = text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? "メモを消しました" : "メモをのこしました✍️ 記録カードにも入ります"
                saved = true
            }
            if let savedNote {
                Text(savedNote).kyonoFont(.bold700, size: 14).foregroundColor(colors.tealInk)
            }
        }
    }
}

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:627-640 #ckCard
// (かたさチェックカード)の1:1移植。full=falseはindex.html:198-202 #ckCard.mini(縮小・
// 「もう一回チェックする」ghostボタン+前回結果リンク)分岐(Android版CkCardと同一ロジック)。
private struct CkCard: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.kyonoBigText) private var bigText
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }
    let full: Bool
    let typeResult: QuizTypeResult?
    let onStartQuiz: () -> Void
    let onShowResult: (String) -> Void

    var body: some View {
        KyonoCard {
            KyonoSectionHeader(icon: .quizCheck, title: "かたさチェック", fill: colors.tealSoft, accent: colors.teal)
            if full {
                Spacer().frame(height: 10 * zoom)
                HStack(alignment: .center) {
                    Text("タップするだけ30秒でチェック✅\nあなたに合うストレッチがわかります")
                        .kyonoFont(.bold700, size: 15).foregroundColor(colors.sub2)
                    Spacer()
                    KyonoCharaImage(name: "chara-3").frame(width: 74 * zoom, height: 74 * zoom)
                }
                Spacer().frame(height: 12 * zoom)
                KyonoPrimaryButton("チェックをはじめる", action: onStartQuiz)
                Spacer().frame(height: 10 * zoom)
                Text("※目安をつかむセルフチェックです\n強い痛みや持病がある方は無理せず医療機関へ")
                    .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            } else {
                Spacer().frame(height: 6 * zoom)
                if let tr = typeResult, let name = quizTypes[tr.key]?.name {
                    // GO-G3(5視点ワンループ): 最小タップ領域44pt/48ptの確保(見た目は変えず当たり判定のみ拡張)。
                    Text("前回の結果: \(name)")
                        .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                        .padding(.vertical, 12 * zoom)
                        .onTapGesture { onShowResult(tr.key) }
                    Spacer().frame(height: 10 * zoom)
                }
                KyonoGhostButton("もう一回チェックする", action: onStartQuiz)
            }
        }
    }
}

// ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md §1): index.html:643-651 #soudanCard
// +index.html:3396 renderSoudanEntry()の1:1移植(Android版SoudanCardと同一ロジック)。soudan-kb
// 未読込(intents空)のときは非表示。おすすめチップはintents先頭3件+"jikan"(index.html:3403-3405)。
private struct SoudanCard: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.kyonoBigText) private var bigText
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }
    let onOpenSoudan: (String?) -> Void
    private let kb = SafetyKBLoader.shared

    private var picks: [SafetyKB.Intent] {
        var base = Array(kb.intents.prefix(3))
        if let extra = kb.intents.first(where: { $0.id == "jikan" }), !base.contains(where: { $0.id == extra.id }) {
            base.append(extra)
        }
        return base
    }

    var body: some View {
        if !kb.intents.isEmpty {
            KyonoCard {
                Button(action: { onOpenSoudan(nil) }) {
                    VStack(alignment: .leading, spacing: 0) {
                        KyonoSectionHeader(icon: .soudanBubble, title: "オガトレ相談室", fill: colors.tealSoft, accent: colors.teal)
                        Spacer().frame(height: 10 * zoom)
                        HStack(alignment: .center) {
                            Text("からだの悩み\nオガトレに聞いてみて💬")
                                .kyonoFont(.bold700, size: 15).foregroundColor(colors.sub2)
                            Spacer()
                            KyonoCharaImage(name: "chara-hitokoto").frame(width: 64 * zoom, height: 64 * zoom)
                        }
                        Spacer().frame(height: 10 * zoom)
                        // TASK-C2-2026-07-30-icon-system.md(I) 試作1件目: 💬→ふきだしアイコン
                        // (KyonoIcon.soudanBubble・タブバーと同じCanvas意匠を流用)。
                        KyonoPrimaryButton("相談する", icon: .soudanBubble) { onOpenSoudan(nil) }
                        Spacer().frame(height: 10 * zoom)
                        Text("👇 タップでそのまま聞けるよ").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                        Spacer().frame(height: 6 * zoom)
                        // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §5: index.html:438
                        // .chips{display:flex;flex-wrap:wrap}が既定(相談室フッターのチップ行だけが
                        // 例外の横スクロール)。index.html:650のこのチップは既定どおり折り返し対象。
                        FlowLayout(spacing: 8, lineSpacing: 8, alignment: .leading) {
                            ForEach(picks, id: \.id) { intent in
                                HomeSoudanChip(label: intent.chip) { onOpenSoudan(intent.id) }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// TASK-C2-2026-07-29-ux-audit-G.md G1: index.html:1711-1753 renderToday()の1:1移植。
// 優先順位はプラン実行中→タイプ判定済み→あさ/よる自動判定(Web版のstate.mode未設定時の既定と同じ)。
// セグメント切替(あなた用/あさ/よるの手動タブ)は「最低ライン」注記により第2段へ送るため、
// ここでは自動選出のみを行う(手動でmineを選んでいてもいなくても、使える条件ならmineが既定)。
private struct TodayVideoSection: View {
    @Environment(\.kyonoColors) private var colors
    let plan: SdPlanData?
    let typeResult: QuizTypeResult?
    let onVideoTap: (String) -> Void

    // OnboardingViews.swift ResultContentView.catalogById/lookupVideoと同じ形(結果画面のおすすめ
    // 動画3本と同じ変換表・カタログを再利用するため、そちらとロジックを分岐させない)。
    private var catalogById: [String: CatalogVideo] {
        Dictionary(uniqueKeysWithValues: CatalogLoader.shared.map { ($0.id, $0) })
    }
    private func lookupVideoById(_ id: String) -> CatalogVideo? { catalogById[id] }
    private func lookupVideoByKey(_ key: String) -> CatalogVideo? {
        quizVideoKeyToId[key].flatMap { catalogById[$0] }
    }

    // index.html:1771 planCurrent()の1:1移植(未完走のみ「実行中」とみなす)。完走判定の式自体は
    // PlanProgressCardView(既存)と同じにする(二重定義で式がずれるのを防ぐ)。
    private func planDayNum(_ p: SdPlanData, today: String) -> Int {
        max(1, RecordLogic.daysBetween(p.start, today) + 1)
    }

    var body: some View {
        let now = Date()
        let today = RecordLogic.todayStr(now: now)
        if let plan, !plan.videos.isEmpty, planDayNum(plan, today: today) <= plan.days {
            // index.html:1745-1748 m==="mine"&&plan分岐(planVideoHTML)の1:1移植。
            let dayNum = planDayNum(plan, today: today)
            let idx = ((dayIndex(now) % plan.videos.count) + plan.videos.count) % plan.videos.count
            if let v = lookupVideoById(plan.videos[idx]) {
                VideoRow(v: v, openUrl: onVideoTap, badge: "プラン\(dayNum)日目/\(plan.days)日: \(plan.label)")
                Text("相談室でつくった2週間プランの1本だよ🌱")
                    .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                    .frame(maxWidth: .infinity, alignment: .center).padding(.top, 6)
            }
        } else if let typeResult, quizTypes[typeResult.key] != nil {
            // index.html:1749-1755 m==="mine"&&typed分岐(fdGuide時の①だけ表示は、この画面自体が
            // fdFocusOnのときは丸ごと非表示になる既存の分岐(HomeView.body参照)と重複するため
            // ここでは扱わない)。
            let rx = currentRx(typeResult.key, now: now)
            Text("きょうのあなた用").kyonoFont(.black900, size: 12).foregroundColor(colors.sub)
            ForEach(rx, id: \.self) { key in
                if let v = lookupVideoByKey(key) {
                    VideoRow(v: v, openUrl: onVideoTap)
                }
            }
            if !rx.isEmpty {
                KyonoGhostButton("▶ あなたへの3本 連続再生はこちら") {
                    let ids = rx.compactMap { quizVideoKeyToId[$0] }.joined(separator: ",")
                    onVideoTap("https://www.youtube.com/watch_videos?video_ids=\(ids)")
                }
            }
        } else {
            // index.html:1756-1758 それ以外(asa/yoru自動判定)分岐の1:1移植。
            let mode = autoTodayMode(now)
            let list = mode == "asa" ? TODAY_ASA : TODAY_YORU
            let idx = ((dayIndex(now) % list.count) + list.count) % list.count
            if let v = lookupVideoByKey(list[idx]) {
                VideoRow(v: v, openUrl: onVideoTap, badge: mode == "asa" ? "きょうのあさ" : "きょうのよる")
            }
        }
        Text("動画がおわったら アプリにもどって\n下の「きょうやった！」を押してね✅")
            .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
            .frame(maxWidth: .infinity, alignment: .center).multilineTextAlignment(.center).padding(.top, 8)
    }
}

// index.html:440 .chip(丸ピル・line枠・card背景)の1:1移植。SoudanSheetView.swiftのKyonoChipは
// 同名衝突とfile-private境界を避けるためここに複製せず別名で用意する(見た目は同一)。
private struct HomeSoudanChip: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.kyonoBigText) private var bigText
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }
    let label: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label).kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                .padding(.horizontal, 16 * zoom).padding(.vertical, 10 * zoom)
                .overlay(Capsule().stroke(colors.line, lineWidth: 2 * zoom))
                .background(Capsule().fill(colors.card))
        }
        .buttonStyle(.plain)
    }
}

// TASK-C2-2026-07-27-milestone-card-export-nudge.md: 記録カードシートの節目促し表示可否を
// 呼び出し元が判定できるよう、描画結果と一緒にmilestone判定も返す。
struct TodayCardResult {
    let image: UIImage
    let isMilestone: Bool
}

// index.html:136-140 drawCardのテーマ選択(記念>季節>抽選の解決結果patから実際に描画するテーマへの
// 変換)をここで組み立てる。判定そのもの(cardPatternFor)はCardLotteryの純粋関数を呼ぶだけ。
// MyRecordView(dayInfoの記録カード表示)からも参照するため非privateにする
// (全画面完全性監査タスク #history)。
func renderTodayCard(store: RecordStore, streak: RecordLogic.StreakData, ds: String) -> TodayCardResult? {
    let data = CardDataLoader.shared
    let effTotal = streak.total
    let dateIdx = CardLottery.dateIdx(ds)
    let milestone = data.MILESTONES.contains(effTotal)

    // rotAssignは「空のときだけ旧方式でバックフィル」。cardPatternFor(→cardRotPick)が新しい日付ぶんを
    // 追記することがあるため、呼び出し後は毎回書き戻す。
    let existing: [String: Int] = store.get("rotAssign", default: [:])
    var rot = CardLottery.ensureRotAssign(dates: streak.dates, total: streak.total, existing: existing)
    let pat = CardLottery.cardPatternFor(ds: ds, effTotal: effTotal, dateIdx: dateIdx, rot: &rot)
    store.set("rotAssign", rot)

    let themeCount = dateIdx >= data.CARD_THEMES_V2_FROM ? data.CARD_THEMES.count : data.CARD_THEMES_V1_COUNT
    let fallback = data.CARD_THEMES[((dateIdx % themeCount) + themeCount) % themeCount]
    let theme: ResolvedTheme
    if let pat {
        theme = ResolvedTheme(name: pat.name, bg: pat.bg ?? fallback.bg, main: pat.main ?? fallback.main, deco: pat.deco ?? fallback.deco)
    } else if milestone {
        theme = ResolvedTheme(name: data.GOLD.name, bg: data.GOLD.bg, main: data.GOLD.main, deco: data.GOLD.deco)
    } else {
        theme = ResolvedTheme(name: fallback.name, bg: fallback.bg, main: fallback.main, deco: fallback.deco)
    }
    let milestoneTitle = data.MS.first { $0.d == effTotal }?.t

    // かたさタイプ/メモ(index.html:133,225の1:1移植。§7bパリティ突合タスクで追加)
    let typeResult: QuizTypeResult? = store.get("type", default: nil)
    let typeName = typeResult.flatMap { quizTypes[$0.key]?.name }
    let typeIconKey: String? = {
        guard let key = typeResult?.key, TYPE_IMG_NAMES[key] != nil else { return nil }
        return key
    }()
    let memos: [String: String] = store.get("memos", default: [:])

    let png = CardRenderer.render(
        ds: ds, effTotal: effTotal, theme: theme, milestone: milestone, milestoneTitle: milestoneTitle,
        dateIdx: dateIdx, cardThemesV2From: data.CARD_THEMES_V2_FROM,
        pat: pat, typeName: typeName, typeIconKey: typeIconKey, memoText: memos[ds], streakCount: streak.count
    )
    guard let image = UIImage(data: png) else { return nil }
    return TodayCardResult(image: image, isMilestone: milestone)
}

#Preview {
    HomeView(
        store: RecordStore(inMemory: [:]), onStartTour: { _ in },
        onOpenQuiz: {}, onShowResult: { _ in }, onOpenSoudan: { _ in }, onOpenMyRecord: {}, onOpenSettings: {}
    )
}
