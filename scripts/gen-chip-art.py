#!/usr/bin/env python3
"""部位・時間帯選択チップの生成絵文字イラスト(TASK-C2-2026-07-30-icon-system-addendum-chips.md)。

本人決定: トンマナはタブバーの5つ(KyonoTabBar)に揃える(硬さチェック6タイプの
スクイーズハンター系統とは別・そちらには寄せない)。アンカーはタブバーのスクリーン
ショットから切り出した実物(icon-anchor-tabbar.png)。

出力は staging ディレクトリ。assets/ への反映は目視確認を通してから別途行う。
IDはOnboardingViews.swift/OnboardingScreens.ktのObChip.v(実際の値)と一致させる
(iOS側ファイル名"chip-<id>.png"・Android側"chip_<id>.png"の元ネタになるため)。

  python3 gen-chip-art.py <id> [<id> ...]
  python3 gen-chip-art.py all
"""
import base64
import json
import os
import subprocess
import sys

KEY_PATH = os.path.expanduser("~/.claude/secrets/openai-ogatore-manuals.key")
ANCHOR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/icon-anchor-tabbar.png")
OUT_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging")

# タブバー5つの実物(太めの丸みストローク・フラットな幾何形)をトーンの正本として説明する。
# gen-type-art.pyのSTYLE節と同じ考え方。
# alan5差し戻し(2026-07-30・v1→v2): 1回目は輪郭が細い波線・塗りが有機的なブロブでタブバーと
# 質感が揃わなかった。線の太さを均一にし、幾何学図形(角丸長方形・円)だけで構成するよう明示的に
# 指定して解消(v2の腰=youtsuuでOK)。
# B-0改定(TASK-C2-2026-07-31-feedback-round2.md・2026-07-31): 既存9種の実機ダークモード確認で
# v2にも3つの敗因が見つかった。①塗りが白・薄色の部分が残っていて、ダークモードのチップ背景に
# 対して「白だけ浮く」見え方になっていた ②32pt表示だと細部(二重線・小さい飾り等)が潰れて
# 何を表しているか読めなくなっていた(特に「前屈できない」) ③意味の伝わらない抽象形
# (「とくにない」の同心円=単なる的にしか見えない)。
# 対策: 白/薄色塗りを全面禁止し、シルエット全体を暖色系のはっきりした単色で塗りつぶした上に
# 墨色の太い輪郭を乗せる(2色構成に固定=どのテーマ背景でも同じ2色が乗るだけなので、背景色に
# 溶けたり浮いたりしない)。モチーフも「大きな単純な形1つ」だけに絞り、32pxまで縮めても
# シルエットで意味が分かることを唯一の判定基準にする。
STYLE = (
    "A minimalist flat sticker-style icon: render the ENTIRE subject as ONE big, bold, simple "
    "silhouette filled completely with a single saturated WARM accent color such as terracotta, "
    "coral-orange or amber (never white, never a pale/light tint, never left empty or unfilled — "
    "every part of the shape must be solidly colored), outlined all around with a thick, perfectly "
    "uniform-width dark ink stroke (a warm near-black charcoal, like #3A3A35 — never thin, never "
    "tapering, never a wavy or organic freehand line), rounded corners throughout, constructed only "
    "from simple geometric shapes (rounded rectangles and circles) in the same pictogram spirit as "
    "the tab bar icons shown in IMAGE 1. The subject must read as ONE big recognizable shape even "
    "shrunk down to a tiny 32x32 pixel icon — no fine internal details, no thin secondary lines, no "
    "small decorative marks that would disappear at that size. No gradients, no glow, no drop "
    "shadow, no 3D toy or plush character, not medical or anatomical, no text, no watermark, single "
    "icon, centred, generous empty margin on all four sides, fully transparent background."
)

# ラベルはOnboardingViews.swift/.ktの定義と一致。キーはObChip.vの実際の値。
SUBJECTS = {
    "katakori": "肩こり・首", "youtsuu": "腰", "zenkutsu": "前屈できない",
    "nemuri": "眠り", "none": "とくにない",
    "asa": "朝おきて", "furo": "おふろ上がり", "neru": "寝るまえ", "free": "きめてない",
}

# 「腰」(youtsuu)と同じ人型構成(円の頭+角丸長方形の胴)を体パーツ系(katakori/zenkutsu)は
# 踏襲し、アクセントの位置だけ変えてシリーズとして揃える。時間帯系(asa/furo/neru/free)は
# タブバーの家/虫眼鏡と同じ「単純な幾何学図形1つ+ワンポイント」の構成にする。
# B-0改定(2026-07-31): 「アクセント色の帯だけ塗り、残りは輪郭のみ」の旧構成をやめ、人型
# シルエット全体を単色で塗りつぶした1枚絵にする。部位の目印(腰の帯・肩のはね等)は塗り色を
# 増やさず、輪郭と同じ墨色の太いストロークで人物シルエットの上に描き足す(2色構成を維持)。
WHAT = {
    "youtsuu": (
        "a simple pictogram of a person's lower back: a small circle head and one tall "
        "rounded-rectangle torso (same construction as IMAGE 1's simple shapes), the WHOLE figure "
        "filled solid in the single warm accent color with the thick dark outline all around it, "
        "plus one extra thick dark ink horizontal stroke drawn across the LOWER THIRD of the torso "
        "(like a belt line) to mark the lower-back/waist area — this marker line uses the SAME dark "
        "ink color as the outline, not a second fill color"
    ),
    "katakori": (
        "a simple geometric pictogram of shoulder/neck stiffness: the exact same figure "
        "construction as IMAGE 2 (small circle head, one tall rounded-rectangle torso), but with "
        "the horizontal accent-color rounded-rectangle band placed across the TOP of the torso at "
        "shoulder height instead of the lower back, plus two short thick rounded-rectangle tick "
        "marks pointing outward from each shoulder to suggest tension"
    ),
    "zenkutsu": (
        "a simple geometric pictogram of a person bending forward: the same small-circle-head + "
        "rounded-rectangle-torso construction as IMAGE 2, but the torso rounded rectangle is bent "
        "forward at roughly 90 degrees (an L-shaped rounded rectangle) over a short rounded-"
        "rectangle 'legs' base, with a small accent-color rounded-rectangle mark at the reaching "
        "hand end to show effort, and a small gap between the hand end and a thin horizontal "
        "ground line below to show it does not quite reach"
    ),
    "nemuri": (
        "a simple geometric pictogram of a pillow: one wide rounded rectangle with a smaller "
        "rounded-rectangle indent/fold on top, filled in the single accent color"
    ),
    "none": (
        "a simple geometric pictogram meaning 'nothing in particular': a plain circle outline "
        "with one smaller solid accent-color circle centred inside it, like a simple target dot"
    ),
    "asa": (
        "a simple geometric pictogram of a sunrise: one accent-color filled circle (the sun) "
        "with a single straight horizontal rounded-rectangle bar beneath it (the horizon), and "
        "three short thick rounded-rectangle rays fanned out above the circle"
    ),
    "furo": (
        "a simple geometric pictogram of a bathtub: one wide rounded rectangle (the tub) with a "
        "small accent-color filled rounded-rectangle wisp shape floating just above it (steam)"
    ),
    "neru": (
        "a simple geometric pictogram of night-time: one accent-color filled crescent-moon shape "
        "(a large circle with a smaller circle cut out of one side) with one small accent-color "
        "filled rounded four-point star beside it"
    ),
    "free": (
        "a simple geometric pictogram of 'undecided': a circle outline with a bold rounded "
        "question-mark shape centred inside it, the question mark filled in the single accent color"
    ),
}


def load_key():
    if not os.path.exists(KEY_PATH):
        sys.exit(f"APIキーが見つかりません: {KEY_PATH}")
    return open(KEY_PATH, encoding="utf-8").read().strip()


def gen(chip_id):
    if chip_id not in WHAT:
        sys.exit(f"不明なID: {chip_id}")
    what = WHAT[chip_id]
    images = ["-F", f"image[]=@{ANCHOR}"]
    existing_youtsuu = os.path.join(OUT_DIR, "chip-youtsuu.png")
    # katakori/zenkutsuは「腰」と同じ人型構成を踏襲するため、既存のyoutsuu画像もIMAGE 2として渡す
    # (プロンプト内の「IMAGE 2」参照とインデックスを一致させる)。
    if chip_id in ("katakori", "zenkutsu") and os.path.exists(existing_youtsuu):
        images += ["-F", f"image[]=@{existing_youtsuu}"]
    prompt = f"Draw {what}. " + STYLE
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"chip-{chip_id}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", *images,
         "-F", f"prompt={prompt}",
         "-F", "size=1024x1024", "-F", "quality=medium", "-F", "background=transparent"],
        capture_output=True, text=True, timeout=300,
    )
    if res.returncode != 0:
        sys.exit(f"curl失敗({chip_id}): {res.stderr[:300]}")
    data = json.loads(res.stdout)
    if "error" in data:
        sys.exit(f"APIエラー({chip_id}): {data['error'].get('message')}")
    with open(out_path, "wb") as f:
        f.write(base64.b64decode(data["data"][0]["b64_json"]))
    print(f"-> {out_path} ({SUBJECTS[chip_id]})")


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        sys.exit(__doc__)
    ids = list(WHAT) if args[0] == "all" else args
    for i in ids:
        gen(i)
