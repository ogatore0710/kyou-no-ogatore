//
//  GuideView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b): 使い方タブ=よくあるしつもんUI(Android版
//  GuideScreen.ktと同一ロジック。index.html filterFaq()/toggleFaqGroup()の1:1移植)。検索の正規化は
//  Web版と同じくSafetyGate.norm(Step2で移植済み)を再利用する(判定ロジックではなく正規化ユーティリティ
//  としての再利用。マスタープラン§3-2の隔離対象=crisisHit/redFlagHit/redFlagKindの3関数のみ)。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:180-197,426-429 .faq-g/.faq details/.searchboxの1:1移植。
//
//  使い方タブ詳細ガイド6セクション欠落修正タスク(TASK-C2-2026-07-26-guide-sections-missing.md):
//  index.html:970-1070の1:1移植(目次チップ6個+gd-help/gd-start/gd-daily/gd-mamori/gd-tsuzuku/
//  gd-myrecの6セクション。Android版GuideScreen.ktと同一ロジック)。**gd-faq(よくあるしつもんQ&A・
//  下のFAQブロック)は1行も変更していない**——目次には含めず(index.html:982-987の範囲=よくある
//  しつもんチップ自体を除く6個)、gd-helpの3ボタンからのジャンプもFAQ本体のコードには触れず、
//  直前に置いた高さ1ptのアンカー(id: "gd-faq-anchor")へScrollViewReaderでスクロール+既存の
//  openGroups/openItems状態を外からセットするだけに留めている。

import SwiftUI
import SafetyCore
import RecordCore

struct GuideView: View {
    let store: RecordStore
    let onBack: () -> Void
    let onReenterOnboarding: () -> Void
    let onReenterTour: () -> Void
    let onOpenQuiz: () -> Void
    let onOpenSettings: () -> Void
    let onOpenMyRecord: () -> Void

    @State private var query = ""
    @State private var openGroups: Set<String> = [faqGroups[0].title]
    @State private var openItems: Set<String> = []
    // gd-startのみ既定で開いた状態(index.html:1002 <details ... open>)、他は閉じた状態。
    @State private var sectionOpen: [String: Bool] = ["gd-start": true]
    // Android版GuideScreen.ktのBackHandler(D1・5視点ワンループG6検収)と同じ`sectionEverToggled`。
    // 「今回のセッションでユーザー自身がどれかを開閉した」ことを追跡する。gd-startが既定で開いている
    // だけの状態(=ユーザーが1度も触っていない)ではこのフラグはfalseのままで、左端スワイプは
    // 通常どおりonBackへ直行する。ユーザーが1度でもどこかを開閉した後は、開いているセクションが
    // あれば左端スワイプでまずそれを閉じるだけに留め、onBackへは進まない(下のperformEdgeSwipeBack参照)。
    @State private var sectionEverToggled = false

    private var nq: String { SafetyGate.norm(query) }
    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            content
        }
        .edgeSwipeBack(onBack: performEdgeSwipeBack)
    }

    // GuideScreen.kt BackHandlerの1:1移植: `sectionEverToggled && sectionOpen.values.any { it }`。
    private func performEdgeSwipeBack() {
        if sectionEverToggled && sectionOpen.values.contains(true) {
            for key in sectionOpen.keys { sectionOpen[key] = false }
        } else {
            onBack()
        }
    }

    private var content: some View {
        GuideContentView(
            query: $query, openGroups: $openGroups, openItems: $openItems,
            sectionOpen: $sectionOpen, sectionEverToggled: $sectionEverToggled, nq: nq,
            onBack: onBack, onReenterOnboarding: onReenterOnboarding, onReenterTour: onReenterTour,
            onOpenQuiz: onOpenQuiz, onOpenSettings: onOpenSettings, onOpenMyRecord: onOpenMyRecord
        )
    }
}

// index.html:982-988 目次チップの1:1移植。
// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #guide): index.html:988
// 「❓ よくあるしつもん」チップが欠けていた(gd-help以外の6項目のみ)。id="gd-faq"はdetailsでは
// ない(常時カード)ためjumpToSectionのsectionOpen設定は無害な未使用エントリになるだけで、
// 実際のスクロール先はgd-faq-anchor(既存のFAQジャンプ用アンカー)へ差し替える。
private let gtocChips: [(label: String, id: String)] = [
    ("🆘 困ったときは", "gd-help"),
    ("🌅 はじめての日", "gd-start"),
    ("▶ まいにちの流れ", "gd-daily"),
    ("🛡 記録を守る", "gd-mamori"),
    ("🎫 つづくしくみ", "gd-tsuzuku"),
    ("📅 マイ記録", "gd-myrec"),
    ("❓ よくあるしつもん", "gd-faq"),
]

private struct GuideContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.colorScheme) private var systemColorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Binding var query: String
    @Binding var openGroups: Set<String>
    @Binding var openItems: Set<String>
    @Binding var sectionOpen: [String: Bool]
    @Binding var sectionEverToggled: Bool
    let nq: String
    let onBack: () -> Void
    let onReenterOnboarding: () -> Void
    let onReenterTour: () -> Void
    let onOpenQuiz: () -> Void
    let onOpenSettings: () -> Void
    let onOpenMyRecord: () -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §A: index.html:1585 gJump()の
    // reduced-motion分岐(behavior:rm?"auto":"smooth")の1:1移植。reduced時はwithAnimationを外して
    // 瞬時スクロールにする。
    private func jump(_ proxy: ScrollViewProxy, _ id: String) {
        if reduceMotion {
            proxy.scrollTo(id, anchor: .top)
        } else {
            withAnimation { proxy.scrollTo(id, anchor: .top) }
        }
    }

    // index.html:1570 gJump()相当。FAQ本体のコードには触れず、既存のopenGroups/openItems状態
    // (下のFAQ描画ループが読む公開状態)を外からセットし、直前のアンカーへスクロールする。
    private func jumpToFaq(_ proxy: ScrollViewProxy, groupTitle: String, itemQ: String?) {
        query = ""
        openGroups.insert(groupTitle)
        if let itemQ { openItems.insert("\(groupTitle)|\(itemQ)") }
        jump(proxy, "gd-faq-anchor")
    }

    // Android版GuideScreen.kt jumpToSection()の1:1移植: 目次チップ経由で強制オープンする場合も
    // sectionEverToggled=trueにする(ユーザーがこのセクションの状態に関与した扱いにする)。
    private func jumpToSection(_ proxy: ScrollViewProxy, id: String) {
        sectionOpen[id] = true
        sectionEverToggled = true
        jump(proxy, id == "gd-faq" ? "gd-faq-anchor" : id)
    }

    // Android版GuideScreen.kt toggleSection()の1:1移植: 開閉と同時に「ユーザー自身が触った」ことを
    // 記録する(左端スワイプの「開いていれば閉じるだけ」判定=performEdgeSwipeBackが使う)。
    private func toggleSection(_ id: String) {
        sectionOpen[id] = !(sectionOpen[id] ?? false)
        sectionEverToggled = true
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
                    // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除。

                    // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94 .logoの1:1移植。使い方タブに
                    // 共通ヘッダーが無かった欠落の修正。
                    KyonoAppHeader()
                    Spacer().frame(height: 8)

                    // 使い方タブ再入場リンク欠落修正タスク(TASK-C2-2026-07-26-guide-reentry-links.md):
                    // index.html:970 .daychip×2(obReenterLink/obTourLink)の1:1移植。
                    // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §4-2: index.html:970
                    // display:flex;flex-wrap:wrapの1:1移植。幅が足りないときはラベルが割れるのではなく
                    // ボタンごと下の行に落ちるようHStackからFlowLayoutへ変更。
                    FlowLayout(spacing: 10, lineSpacing: 8, alignment: .center) {
                        Text("🌱 はじめてガイド")
                            .kyonoFont(.extraBold800, size: 14).foregroundColor(colors.tealInk)
                            .padding(.horizontal, 16).padding(.vertical, 9)
                            .background(Capsule().fill(colors.tealSoft))
                            .onTapGesture(perform: onReenterOnboarding)
                        Text("📖 使い方ツアー")
                            .kyonoFont(.extraBold800, size: 14).foregroundColor(dark ? Color(hex: 0xE8C74C) : Color(hex: 0x7E6400))
                            .padding(.horizontal, 16).padding(.vertical, 9)
                            .background(Capsule().fill(colors.yellowSoft))
                            .onTapGesture(perform: onReenterTour)
                    }
                    .frame(maxWidth: .infinity)
                    Text("「はじめてガイド」＝さいしょの質問からやりなおす／「使い方ツアー」＝つかいかたをスライドで見る")
                        .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 8)

                    // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
                    // §2 キャラクター画像: index.html:973-978 .card.grad-warm(chara.png 84x84+「おぼえるのはこれだけ！」)の1:1移植。
                    KyonoGradientCard(gradient: .warm) {
                        VStack(spacing: 8) {
                            KyonoCharaImage(name: "chara").frame(width: 84, height: 84)
                            Text("おぼえるのはこれだけ！").kyonoFont(.black900, size: 19).foregroundColor(colors.ink)
                            Text("1日1本うごいて\n「きょうやった！」を押す")
                                .kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                                .multilineTextAlignment(.center)
                            Text("あとはぜんぶ このアプリがおぼえてます").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub2)
                        }
                        .frame(maxWidth: .infinity)
                    }

                    // ---- 目次チップ(index.html:980-989) ----
                    // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §4: index.html:178
                    // display:flex;flex-wrap:wrap;justify-content:centerの1:1移植。横スクロール
                    // (旧ScrollView)だと7個中3個しか見えず「❓ よくあるしつもん」等が隠れていたため、
                    // FlowLayoutで折り返して全項目を常に見せる。
                    FlowLayout(spacing: 8, lineSpacing: 8, alignment: .center) {
                        ForEach(gtocChips, id: \.id) { chip in
                            Text(chip.label)
                                .kyonoFont(.black900, size: 13).foregroundColor(colors.sub)
                                .padding(.horizontal, 14).padding(.vertical, 10)
                                .background(Capsule().fill(colors.line))
                                .onTapGesture { jumpToSection(proxy, id: chip.id) }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .id("gtoc")
                    Text("下の見出しカードは タップするとひらきます")
                        .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 4).padding(.bottom, 14)

                    // ---- 1. 困ったときは(index.html:992-1000。折りたたみでなく常時カード) ----
                    KyonoCard {
                        KyonoSectionHeader(icon: .question, title: "困ったときは", fill: colors.pinkSoft)
                        Spacer().frame(height: 10)
                        VStack(spacing: 8) {
                            // 判断メモ: Web版の遷移先「通算が0日にもどってる！」FAQ(faqRecordMissing)は
                            // LINE内ブラウザ/Safari間のストレージ分離というPWA固有の前提の回答で、
                            // GuideData.swiftでも同じ理由(§2-2に準ずる判断)でhidden=trueにされ非表示。
                            // ジャンプ先をネイティブでも有効な同グループの「連続が切れちゃった…」に読み替えた。
                            KyonoGhostButton("📅 記録が消えた・0日にもどってる") { jumpToFaq(proxy, groupTitle: "📅 記録・続けるについて", itemQ: "連続が切れちゃった…") }
                            KyonoGhostButton("📱 機種変更したい", action: onOpenSettings)
                            KyonoGhostButton("🩹 ストレッチ中に痛かった") { jumpToFaq(proxy, groupTitle: "💬 きょうの1本・相談室", itemQ: "ストレッチ中に痛かったら？") }
                            KyonoGhostButton("🔔 通知・リマインダーについて") { jumpToFaq(proxy, groupTitle: "📱 きほんのき", itemQ: "通知はこないの？") }
                        }
                    }
                    .id("gd-help")

                    // ---- 2. はじめての日にやること(index.html:1002-1011。既定で開いた状態) ----
                    GdFoldSection(
                        id: "gd-start", icon: .quizCheck, title: "はじめての日にやること", fill: colors.yellowSoft, accent: nil,
                        open: sectionOpen["gd-start"] ?? false, onToggle: { toggleSection("gd-start") },
                        onBackToToc: { jump(proxy, "gtoc") },
                    ) {
                        GStep(marker: "1", title: "かたさチェック（30秒）", body: "5問タップするだけ 写真とイラストのお手本つき") {
                            KyonoCharaImage(name: "check-q1")
                                .frame(width: 110)
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 1.5))
                        }
                        GStep(marker: "2", title: "あなたの「かたさタイプ」が出ます", body: "タイプに合わせた おすすめ3本つき")
                        GStep(marker: "3", title: "まず1本 動画をやってみる", body: "おわったらホームの「きょうやった！」を押す")
                        GStep(marker: "💬", title: "オガトレ相談室", body: "からだの悩みを打つと オガトレの言葉で「どの動画をやればいいか」まで答えます\n右下の💬ボタンか ホームのカードからいつでもどうぞ")
                        GStep(marker: "🎯", title: "2週間プラン", body: "相談の答えを「2週間プラン」にすると ホームの「あなた用」がその悩み専用の動画にかわります")
                        Spacer().frame(height: 4)
                        KyonoPrimaryButton("チェックをはじめる", action: onOpenQuiz)
                    }

                    // ---- 3. 2日目からの毎日(index.html:1013-1027) ----
                    GdFoldSection(
                        id: "gd-daily", icon: .play, title: "2日目からの毎日", fill: colors.tealSoft, accent: colors.teal,
                        open: sectionOpen["gd-daily"] ?? false, onToggle: { toggleSection("gd-daily") },
                        onBackToToc: { jump(proxy, "gtoc") },
                    ) {
                        GFlow(steps: ["アプリを\nひらく", "きょうの1本を\n▶ 再生", "きょう\nやった！"])
                        Text("チェック済みの人は「あなた用」に あなたのおすすめが日替わりで出ます 気分をかえたい日は「あさ」「よる」へどうぞ")
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                        Spacer().frame(height: 10)
                        // index.html:1022 .seg.gmock(タップ不可の静止モック)。既存の見た目のみ流用し
                        // 選択を無効化(onSelectを何もしない)して「見た目は本物・操作は無効」を1:1で再現。
                        GdSegmentMock()
                        Spacer().frame(height: 10)
                        Text("おわったら✍️ ひとことメモも残せます（「はじめてつま先さわれた」など）あとで読み返すと たからものです")
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                        Spacer().frame(height: 14)
                        GStep(marker: "💬", title: "オガトレ通信", body: "右下のアイコンをタップすると 尾形さんからのひとこと・写真・ラジオが届きます📻「もっと見る」で過去ぶんも全部よめます")
                    }

                    // ---- 4. 記録が消えない3つの守り(index.html:1029-1045。ステップ1のA2HS手順のみ
                    // ネイティブ向けに置き換え。判断根拠はやることブロック参照。他は1:1) ----
                    GdFoldSection(
                        id: "gd-mamori", icon: .shieldCheck, title: "記録が消えない3つの守り", fill: colors.tealSoft, accent: colors.teal,
                        open: sectionOpen["gd-mamori"] ?? false, onToggle: { toggleSection("gd-mamori") },
                        onBackToToc: { jump(proxy, "gtoc") },
                    ) {
                        HStack {
                            KyonoCharaImage(name: "chara-good").frame(width: 60, height: 60)
                            Text("3つおさえれば安心です").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                        }
                        Spacer().frame(height: 10)
                        // A2HS(ホーム画面に追加)の概念自体がネイティブアプリには無いため、Web版の
                        // iPhone/Android別の追加手順・a2hsShowForce()ボタンは移植せず、ネイティブの
                        // 実態に合わせたシンプルな注意書きに置き換えた(タスク指示どおりの判断。
                        // 情報を削りすぎない=「アプリを消さない」という守りの本旨自体は残す)。
                        GStep(marker: "1", title: "このアプリを削除しないでね", body: "ネイティブアプリとして端末にインストール済みなので、Web版のような「ホーム画面に追加」の操作は不要です。このアプリを削除しない限り、記録はずっとこの端末に残ります。")
                        GStep(marker: "2", title: "記録カードを画像で保存", body: "写真フォルダが自動でバックアップになります\nつくりかた: ①「きょうやった！」のあと「記録カードを画像でのこす」を押す　②「保存・シェアする」→「画像を保存」で写真フォルダへ（画像の長押しでもOK）　③SNSにも投稿OK 動画のコメント欄にも仲間が待ってます👀") {
                            KyonoCharaImage(name: "card-sample").frame(width: 140, height: 140)
                        }
                        GStep(marker: "3", title: "機種変更のとき", body: "マイ記録→続ける設定→「記録のひっこし」で「記録をコピー」→新しいスマホで「よみこむ」")
                    }

                    // ---- 5. 続けるしくみ（ここがやさしい）(index.html:1047-1059) ----
                    GdFoldSection(
                        id: "gd-tsuzuku", icon: .star, title: "続けるしくみ（ここがやさしい）", fill: colors.yellowSoft, accent: nil,
                        open: sectionOpen["gd-tsuzuku"] ?? false, onToggle: { toggleSection("gd-tsuzuku") },
                        onBackToToc: { jump(proxy, "gtoc") },
                    ) {
                        GStep(marker: "🎫", title: "おやすみ券が毎月3枚", body: "休んでも 自動でつかわれて連続がつながる\n使い切っても通算日数はぜったい消えません")
                        GStep(marker: "👑", title: "節目はゴールドカード", body: "3日・7日・2週間…の節目の日は 記録カードがこんなゴールドのお祝いデザインになります↓")
                        VStack {
                            KyonoCharaImage(name: "card-sample-gold").frame(width: 180, height: 180)
                            Text("見本（ほんものは日付や あなたのメモ入り）").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                        }
                        .frame(maxWidth: .infinity)
                        Spacer().frame(height: 10)
                        Text("記念日・季節・レアなど カードのデザインは何種類もあります あつめた記録は「マイ記録」タブの「🎉お楽しみ機能」の中にあるカード図鑑📖でいつでも見返せます")
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                        Spacer().frame(height: 10)
                        GStep(marker: "🌱", title: "サボっても だいじょうぶ", body: "ひさしぶりに開くと「おかえりなさい」から始まります 責められません")
                        GStep(marker: "⏱", title: "時間がない日は30秒の1本でもOK", body: "「動画を探す」の「時間・シーン」→「ショート」を選べば すぐおわる動画だけ出ます それでも堂々と「きょうやった！」です")
                    }

                    // ---- 6. 「マイ記録」タブでできること(index.html:1061-1070) ----
                    GdFoldSection(
                        id: "gd-myrec", icon: .calendarCheck, title: "「マイ記録」タブでできること", fill: colors.tealSoft, accent: colors.teal,
                        open: sectionOpen["gd-myrec"] ?? false, onToggle: { toggleSection("gd-myrec") },
                        onBackToToc: { jump(proxy, "gtoc") },
                    ) {
                        GStep(marker: "📅", title: "カレンダー", body: "やった日に印がつく（×はつきません）")
                        GStep(marker: "📏", title: "とどくメーター", body: "前屈がどこまで届くか週1で記録 のびていく証拠が見えます")
                        GStep(marker: "🎉", title: "お楽しみ機能", body: "じまんカード・せんぱいの声・ひとことにっきがまとまっています")
                        GStep(marker: "⚙️", title: "続ける設定", body: "リマインダー（カレンダー通知）や画面のみため（夜は暗く）はここ")
                        GStep(marker: "🎬", title: "（こちらは下のタブ）「再生リスト」タブ", body: "連続再生できるまとめ 流しっぱなしでOK")
                        Spacer().frame(height: 4)
                        KyonoGhostButton("マイ記録タブをひらく", action: onOpenMyRecord)
                    }

                    // 高さ1ptのアンカー。gd-help内のFAQジャンプボタンがここまでスクロールする
                    // (FAQ本体のコードは一切変更しない。上のコメント参照)。
                    Color.clear.frame(height: 1).id("gd-faq-anchor")

                    // ==== ここから下(gd-faq)は既存のまま・1文字も変更していない ====
                    KyonoSectionHeader(icon: .question, title: "よくあるしつもん", fill: colors.coralSoft)
                    Text("しつもんをタップすると こたえがひらきます").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                    // index.html:426-429 .searchbox
                    TextField("🔍 キーワードでさがす（例: 記録 / 機種変更 / 痛い）", text: $query)
                        .padding(.horizontal, 14).padding(.vertical, 10)
                        .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 2))

                    LazyVStack(alignment: .leading, spacing: 6) {
                        ForEach(faqGroups, id: \.title) { group in
                            let visible = group.items.filter { item in
                                if item.hidden { return false }
                                if nq.isEmpty { return true }
                                return SafetyGate.norm(item.q).contains(nq) || SafetyGate.norm(item.a).contains(nq)
                            }
                            if !visible.isEmpty {
                                let isOpen = openGroups.contains(group.title) || !nq.isEmpty
                                // index.html:180-183 .faq-g(グループ見出し・開閉矢印)
                                HStack {
                                    Text(group.title).kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                                    Spacer()
                                    Text(isOpen ? "▴" : "▾").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                                }
                                .padding(.top, 12)
                                .contentShape(Rectangle())
                                .onTapGesture { toggleGroup(group.title) }
                                if isOpen {
                                    ForEach(visible, id: \.q) { item in
                                        let key = group.title + "|" + item.q
                                        let open = openItems.contains(key)
                                        // index.html:190-196 .faq details/summary(枠線ボックス・"Q"プレフィックス)
                                        VStack(alignment: .leading, spacing: 8) {
                                            HStack(alignment: .top) {
                                                Text("Q").kyonoFont(.black900, size: 15).foregroundColor(colors.pink)
                                                // UI/UXパリティ監査2巡目A1(2026-07-29): index.html:191 .faq summary
                                                // {font-size:14px;line-height:1.6}の1:1移植。前回G2は検索チップの
                                                // カスタムフォント行送り超過補正(Android KyonoTightLineTextStyle)を
                                                // 検索チップのみに適用していたが、iOSは1件も対応が無かったため
                                                // ここに展開する。
                                                Text(item.q).kyonoFont(.extraBold800, size: 14).foregroundColor(colors.ink).lineSpacing(8)
                                                Spacer()
                                                Text(open ? "▴" : "▾").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                                            }
                                            if open {
                                                // index.html:196 .faq .fa{font-size:14px;line-height:1.9}の1:1移植。
                                                Text(item.a).kyonoFont(.bold700, size: 14).foregroundColor(colors.sub).lineSpacing(13).padding(.leading, 18)
                                            }
                                        }
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .padding(13)
                                        .background(RoundedRectangle(cornerRadius: 14).fill(colors.bg))
                                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.line, lineWidth: 1.5))
                                        .contentShape(Rectangle())
                                        .onTapGesture { toggleItem(key) }
                                    }
                                }
                            }
                        }
                    }
                }
                // UI/UXパリティ監査GO-9・G6(2026-07-28): index.html:82 body{padding:20px 18px 180px}
                // の1:1移植。この画面だけ全辺16ptだった欠落を、共通のkyonoScreenPadding()へ統一する。
                .kyonoScreenPadding()
            }
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }

    private func toggleGroup(_ title: String) {
        if openGroups.contains(title) { openGroups.remove(title) } else { openGroups.insert(title) }
    }

    private func toggleItem(_ key: String) {
        if openItems.contains(key) { openItems.remove(key) } else { openItems.insert(key) }
    }
}

// index.html:186-189 details.gd-fold(折りたたみカード・見出しタップで開閉+末尾に「↑目次へ戻る」)の1:1移植。
private struct GdFoldSection<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    let id: String
    let icon: KyonoIcon
    let title: String
    let fill: Color
    let accent: Color?
    let open: Bool
    let onToggle: () -> Void
    let onBackToToc: () -> Void
    @ViewBuilder let sectionContent: () -> Content

    init(
        id: String, icon: KyonoIcon, title: String, fill: Color, accent: Color?,
        open: Bool, onToggle: @escaping () -> Void, onBackToToc: @escaping () -> Void,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.id = id; self.icon = icon; self.title = title; self.fill = fill; self.accent = accent
        self.open = open; self.onToggle = onToggle; self.onBackToToc = onBackToToc; self.sectionContent = content
    }

    var body: some View {
        KyonoCard {
            HStack {
                KyonoSectionHeader(icon: icon, title: title, fill: fill, accent: accent ?? Color(hex: 0xE56A9A))
                Spacer()
                Text(open ? "▴" : "▾").kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
            }
            .contentShape(Rectangle())
            .onTapGesture(perform: onToggle)
            if open {
                Spacer().frame(height: 10)
                VStack(alignment: .leading, spacing: 0, content: sectionContent)
                Text("↑ 目次へ戻る")
                    .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 8)
                    .contentShape(Rectangle())
                    .onTapGesture(perform: onBackToToc)
            }
        }
        .id(id)
    }
}

// index.html:163-165 .stepn/.gstep(丸番号バッジ+太字見出し+本文)の1:1移植。
private struct GStep<Extra: View>: View {
    @Environment(\.kyonoColors) private var colors
    let marker: String
    let title: String
    let body_: String
    let extra: (() -> Extra)?

    init(marker: String, title: String, body: String, @ViewBuilder extra: @escaping () -> Extra) {
        self.marker = marker; self.title = title; self.body_ = body; self.extra = extra
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle().fill(colors.yellow)
                // B1(2026-07-29): 黄色背景マーカーの文字はcolors.yellowInk(ライト値固定)を使う。
                Text(marker).kyonoFont(.black900, size: 13).foregroundColor(colors.yellowInk)
            }
            .frame(width: 30, height: 30)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                if !body_.isEmpty {
                    Text(body_).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                }
                if let extra {
                    Spacer().frame(height: 8)
                    extra()
                }
            }
        }
        .padding(.bottom, 14)
    }
}

extension GStep where Extra == EmptyView {
    init(marker: String, title: String, body: String) {
        self.init(marker: marker, title: title, body: body, extra: { EmptyView() })
    }
}

// index.html:166-168 .gflow/.gf/.ga(3ステップの矢印フロー図解)の1:1移植。
private struct GFlow: View {
    @Environment(\.kyonoColors) private var colors
    let steps: [String]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(Array(steps.enumerated()), id: \.offset) { i, s in
                Text(s)
                    .kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 10).padding(.vertical, 10)
                    .frame(minWidth: 76)
                    .background(RoundedRectangle(cornerRadius: 14).fill(colors.bg))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.line, lineWidth: 1.5))
                if i < steps.count - 1 {
                    Text("→").kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
    }
}

// index.html:1022 .seg.gmock(タップ不可の静止モック)。既存KyonoSegmentedControl相当の見た目を
// 簡潔に複製(あなた用/あさ/よるの3ボタン・「あなた用」だけ選択状態)。タップは無効(操作不可)。
private struct GdSegmentMock: View {
    @Environment(\.kyonoColors) private var colors
    var body: some View {
        HStack(spacing: 4) {
            ForEach(["あなた用", "あさ", "よる"], id: \.self) { label in
                Text(label)
                    .kyonoFont(.black900, size: 14)
                    .foregroundColor(label == "あなた用" ? colors.ink : colors.sub)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(RoundedRectangle(cornerRadius: 12).fill(label == "あなた用" ? colors.card : Color.clear))
            }
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 16).fill(colors.line))
    }
}

// TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §4/§4-2: index.html:178,970の
// display:flex;flex-wrap:wrapの1:1移植。SwiftUIに標準の折り返しレイアウトが無いため、Layout
// プロトコル(iOS16+)で最小限のflex-wrap相当を実装する。子要素は幅が足りなければ丸ごと次の行へ
// 落ちる(Web版と同じ「ラベルは割れず、ボタン単位で折り返す」挙動)。
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8
    var alignment: HorizontalAlignment = .center

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        let rows = computeRows(maxWidth: maxWidth, subviews: subviews)
        let height = rows.reduce(0) { $0 + $1.height } + CGFloat(max(0, rows.count - 1)) * lineSpacing
        let width = rows.map { $0.width }.max() ?? 0
        return CGSize(width: maxWidth.isFinite ? maxWidth : width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let rows = computeRows(maxWidth: bounds.width, subviews: subviews)
        var y = bounds.minY
        var idx = 0
        for row in rows {
            var x: CGFloat
            switch alignment {
            case .leading: x = bounds.minX
            case .trailing: x = bounds.maxX - row.width
            default: x = bounds.minX + (bounds.width - row.width) / 2
            }
            for size in row.sizes {
                subviews[idx].place(at: CGPoint(x: x, y: y + (row.height - size.height) / 2), proposal: ProposedViewSize(size))
                x += size.width + spacing
                idx += 1
            }
            y += row.height + lineSpacing
        }
    }

    private struct Row { let sizes: [CGSize]; let width: CGFloat; let height: CGFloat }

    private func computeRows(maxWidth: CGFloat, subviews: Subviews) -> [Row] {
        var rows: [Row] = []
        var sizes: [CGSize] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if !sizes.isEmpty && width + spacing + size.width > maxWidth {
                rows.append(Row(sizes: sizes, width: width, height: height))
                sizes = []; width = 0; height = 0
            }
            if !sizes.isEmpty { width += spacing }
            sizes.append(size)
            width += size.width
            height = max(height, size.height)
        }
        if !sizes.isEmpty { rows.append(Row(sizes: sizes, width: width, height: height)) }
        return rows
    }
}
