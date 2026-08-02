//
//  ObuView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
//  (オガトレ部)の全件アーカイブUI(Android版ObuScreen.ktと同一ロジック。index.html
//  renderObuArchive()の1:1移植=新着順ソート+type別描画)。FABタップ時のプレビューポップアップ
//  (renderObuPopup/openObu)は当初簡略化していたが、TASK-C2-2026-07-27-obu-fab-preview-popup.mdで
//  ObuPreviewPopupView(下記)として追加移植した。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:266-278 .obu-post/.obu-post.obu-text(yellow-soft)/.obu-date/.obu-title/
//  .obu-capの1:1移植。
//  TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §1・§5・§6・§7・§8: ラジオ再生(AVAudioPlayer)・
//  obuFmtDate/obuIsStaleDateによる日付整形・ポップアップのスクロール・写真のアスペクト比保持・
//  安定ソート/末尾もどるボタンを追加(Android版ObuScreen.ktと同一ロジック)。

import SwiftUI
import RecordCore
import AVFoundation
import Combine

struct ObuView: View {
    let store: RecordStore
    let onBack: () -> Void

    private let posts: [ObuPost]

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8: index.html:1352
        // renderObuArchive()の安定ソート(日付のみ・同日内は元の記載順を保持)の1:1移植。
        // 以前はtimeでの二次ソートがあり、time付きの投稿が同日内で先頭に来てしまっていた。
        // SwiftのArray.sorted(by:)は安定ソートを保証する。
        self.posts = ObuLoader.shared.sorted { a, b in a.date > b.date }
    }

    private var themeSetting: String { store.get("theme", default: "light") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            ObuContentView(posts: posts, onBack: onBack)
        }
        // iOSスワイプもどり導線追加タスク(EdgeSwipeBack.swift参照): アコーディオン状態を持たない画面。
        .edgeSwipeBack(onBack: onBack)
    }
}

private struct ObuContentView: View {
    @Environment(\.kyonoColors) private var colors
    let posts: [ObuPost]
    let onBack: () -> Void
    private let today = RecordLogic.todayStr(now: Date())

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            KyonoLineButton("◀ もどる", action: onBack)
            KyonoSectionHeader(icon: .obuBubble, title: "オガトレ通信", fill: colors.pinkSoft)
            // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #obu):
            // index.html:932 説明文の1:1移植。
            Text("尾形さんからの ひとこと・写真・ラジオを ぜんぶまとめて見られます")
                .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
            if posts.isEmpty {
                KyonoCard {
                    Text("まだ投稿がありません また今度のぞいてみてね")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .multilineTextAlignment(.center)
                }
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(posts, id: \.id) { post in ObuPostCardView(post: post, today: today) }
                        // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8: 一覧末尾にも
                        // 「もどる」を追加(Web版・voicesアーカイブと同じく上下どちらからでも脱出できるように)。
                        KyonoLineButton("◀ もどる", action: onBack).padding(.top, 4)
                    }
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

struct ObuPostCardView: View {
    @Environment(\.kyonoColors) private var colors
    let post: ObuPost
    let today: String
    var photoWidthFraction: CGFloat = 1

    // index.html:271 .obu-post.obu-text(yellow-softの角丸ボックス)。photo/radioはボックスなしで並べる。
    private var isText: Bool { post.type != "photo" && post.type != "radio" }
    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §5: index.html:277-278
    // .obu-date-old(30日超は11px・sub-faintに落とす)の1:1移植。
    private var isOld: Bool { obuIsStaleDate(post.date, today) }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(obuFmtDate(post.date, post.time))
                .kyonoFont(isOld ? .bold700 : .black900, size: isOld ? 11 : 12)
                .foregroundColor(isOld ? colors.subFaint : (isText ? colors.sub2 : colors.sub))
            switch post.type {
            case "photo":
                if let imagePath = post.image, let base = ObuLoader.imageFileBaseName(imagePath),
                   let url = Bundle.main.url(forResource: base, withExtension: "jpg"),
                   let uiImage = UIImage(contentsOfFile: url.path) {
                    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §7: index.html:266
                    // .obu-post img{width:100%}(アーカイブは自然なアスペクト比・切り抜きなし)/
                    // #obuModal .obu-post img{width:75%}(ポップアップは75%幅中央寄せ)の1:1移植。
                    // 以前はheight固定180pt+fill+clippedで縦長写真の上下が欠けていた。
                    // containerRelativeFrame(iOS17+)で親幅に対する割合幅を指定しつつアスペクト比は保持する。
                    Image(uiImage: uiImage).resizable().aspectRatio(contentMode: .fit)
                        .containerRelativeFrame(.horizontal) { width, _ in width * photoWidthFraction }
                        .background(colors.card)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.line, lineWidth: 1.5))
                        .frame(maxWidth: .infinity, alignment: .center)
                }
                if let text = post.text { Text(text).kyonoFont(.bold700, size: 14).foregroundColor(colors.ink).lineSpacing(6) }
            case "radio":
                if let title = post.title { Text("\(title)").kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk) }
                Spacer().frame(height: 6)
                ObuRadioPlayerView(post: post)
            default:
                if let text = post.text { Text(text).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(9) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(isText ? 14 : 0)
        .background(isText ? colors.yellowSoft : Color.clear)
        .cornerRadius(isText ? 14 : 0)
        .padding(.bottom, 14)
    }
}

// TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §1: index.html:1334-1336
// <audio controls src=...>相当の1:1移植。AVAudioPlayerで再生/一時停止・再生位置と長さを表示する
// (最低限の要件。バックグラウンド再生・通知センター連携はスコープ外=作らない)。
private final class ObuAudioPlayerModel: NSObject, ObservableObject, AVAudioPlayerDelegate {
    @Published var isPlaying = false
    @Published var position: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    private var player: AVAudioPlayer?
    private var timer: Timer?

    func load(url: URL) {
        guard player == nil else { return }
        player = try? AVAudioPlayer(contentsOf: url)
        player?.delegate = self
        duration = player?.duration ?? 0
    }

    func toggle() {
        guard let player else { return }
        if isPlaying {
            player.pause()
            isPlaying = false
            stopTimer()
        } else {
            player.play()
            isPlaying = true
            startTimer()
        }
    }

    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.3, repeats: true) { [weak self] _ in
            guard let self, let player = self.player else { return }
            self.position = player.currentTime
        }
    }
    private func stopTimer() { timer?.invalidate(); timer = nil }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        isPlaying = false
        position = 0
        player.currentTime = 0
        stopTimer()
    }

    deinit { timer?.invalidate() }
}

struct ObuRadioPlayerView: View {
    @Environment(\.kyonoColors) private var colors
    @StateObject private var model = ObuAudioPlayerModel()
    let post: ObuPost

    var body: some View {
        if let audioPath = post.audio, let base = ObuLoader.audioFileBaseName(audioPath),
           let url = Bundle.main.url(forResource: base, withExtension: "mp3") {
            HStack(spacing: 10) {
                Button(action: { model.toggle() }) {
                    Text(model.isPlaying ? "⏸" : "▶")
                        .foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(colors.teal))
                }
                .buttonStyle(.plain)
                VStack(alignment: .leading, spacing: 4) {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 2).fill(colors.line).frame(height: 4)
                            RoundedRectangle(cornerRadius: 2).fill(colors.teal)
                                .frame(width: model.duration > 0 ? geo.size.width * CGFloat(model.position / model.duration) : 0, height: 4)
                        }
                    }.frame(height: 4)
                    Text("\(fmt(model.position)) / \(fmt(model.duration))")
                        .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                }
            }
            .padding(10)
            .background(RoundedRectangle(cornerRadius: 12).fill(colors.card))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 1.5))
            .onAppear { model.load(url: url) }
        } else {
            Text("音声を読み込めませんでした").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
        }
    }

    private func fmt(_ t: TimeInterval) -> String {
        let total = Int(t)
        return String(format: "%d:%02d", total / 60, total % 60)
    }
}

// TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:1344-1358 renderObuPopup/openObuの1:1移植。
// FABタップで直接全アーカイブへ遷移していたのをやめ、まずtext/photo/radio最新1件ずつ(最大3件)だけを
// 見せるプレビューにする。既読記録(obu_seen)・バッジ更新は呼び出し元(FABのaction)がポップアップを
// 開く時点で行う(index.html:1345-1348と同じ「開いた瞬間に既読」のタイミング)。
struct ObuPreviewPopupView: View {
    @Environment(\.kyonoColors) private var colors
    let onClose: () -> Void
    let onViewArchive: () -> Void

    private let items: [ObuPost]
    private let today = RecordLogic.todayStr(now: Date())

    init(onClose: @escaping () -> Void, onViewArchive: @escaping () -> Void) {
        self.onClose = onClose
        self.onViewArchive = onViewArchive
        let posts = ObuLoader.shared
        self.items = ["text", "photo", "radio"].compactMap { obuLatestByType(posts, $0) }
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
                .onTapGesture { onClose() }
            // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §6: index.html:265
            // #obuModal .obu-box{max-height:80vh;overflow-y:auto}の1:1移植。文字サイズ「大きめ」+
            // text/photo/radioの3件が揃うと「もっと見る」リンクと✕ボタンが画面外に出て操作不能に
            // なり得た欠落。
            GeometryReader { geo in
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("オガトレ通信").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                        Spacer()
                        // GO-G3(5視点ワンループ): 視覚サイズ(40px)は変えず、タップ領域だけ44pt相当に広げる。
                        Button(action: onClose) {
                            Color.clear
                                .frame(width: 44, height: 44)
                                .overlay(
                                    Text("✕").kyonoFont(.black900, size: 18).foregroundColor(colors.ink)
                                        .frame(width: 40, height: 40)
                                        .background(Circle().fill(colors.line))
                                )
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                    ScrollView {
                        if items.isEmpty {
                            Text("まだ投稿がありません また今度のぞいてみてね")
                                .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                                .frame(maxWidth: .infinity, alignment: .center)
                                .multilineTextAlignment(.center)
                                .padding(.vertical, 20)
                        } else {
                            VStack(alignment: .leading, spacing: 0) {
                                ForEach(items, id: \.id) { post in
                                    ObuPostCardView(post: post, today: today, photoWidthFraction: 0.75)
                                }
                            }
                        }
                    }
                    Text("もっと見る（過去の投稿もぜんぶ）")
                        .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .onTapGesture { onViewArchive() }
                }
                .padding(18)
                .frame(maxHeight: geo.size.height * 0.8)
                .background(RoundedRectangle(cornerRadius: 20).fill(colors.card))
                .frame(width: geo.size.width - 48, height: nil)
                .position(x: geo.size.width / 2, y: geo.size.height / 2)
            }
        }
    }
}
