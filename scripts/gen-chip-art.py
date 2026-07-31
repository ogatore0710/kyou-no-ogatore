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
# B-2(本生産)で追加: かたさチップ4種(hard/normal/soft/unknown)・からだの場所タグ10種
# (11種のうちyoutsuu=腰は部位チップの絵を使い回すため新規生成しない・SearchView.swift/
# SearchScreen.ktのFadingChipRow「からだの場所」行に適用)。からだの場所タグは元コードに
# 短縮キーが無い(ラベル文字列そのものがid)ため、ファイル名用に短いローマ字キーを新設した。
SUBJECTS = {
    "katakori": "肩こり・首", "youtsuu": "腰", "zenkutsu": "前屈できない",
    "nemuri": "眠り", "none": "とくにない",
    "asa": "朝おきて", "furo": "おふろ上がり", "neru": "寝るまえ", "free": "きめてない",
    "hard": "ガチガチかも", "normal": "ふつう", "soft": "やわらかい", "unknown": "わからない",
    "zenshin": "全身", "kata": "肩・肩甲骨", "kubi": "首・肩こり", "senaka": "姿勢・背中",
    "kokansetsu": "股関節", "kaikyaku": "開脚", "momoura": "もも裏", "futomomo": "太もも・お尻",
    "hiza": "ひざ・O脚", "ashikubi": "足首・足うら",
}

# 「腰」(youtsuu)と同じ人型構成(円の頭+角丸長方形の胴)を体パーツ系は踏襲し、アクセントの
# 位置・ポーズだけ変えてシリーズとして揃える。時間帯系(asa/furo/neru/free)とかたさ系
# (hard/normal/soft)はタブバーの家/虫眼鏡と同じ「単純な幾何学図形1つ+ワンポイント」の構成。
# B-0改定(2026-07-31): 「アクセント色の帯だけ塗り、残りは輪郭のみ」の旧構成をやめ、シルエット
# 全体を単色で塗りつぶした1枚絵にする。部位の目印(腰の帯・肩のはね等)は塗り色を増やさず、
# 輪郭と同じ墨色の太いストロークでシルエットの上に描き足す(2色構成を維持)。
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
        "a simple pictogram of shoulder/neck stiffness, symmetric front-facing figure: a small "
        "circle head centred on top of one tall rounded-rectangle torso with two arms (two short "
        "vertical rounded-rectangle strokes, one along the left edge of the torso and one along the "
        "right edge, both the SAME length), the WHOLE figure filled solid in the single warm accent "
        "color with the thick dark outline all around it, exactly like IMAGE 2's construction. The "
        "torso silhouette itself must be completely PLAIN below the neck — no belt line, no "
        "horizontal stroke, no marking anywhere on the torso body at all. Add exactly two small "
        "lightning-bolt zigzag strokes in dark ink FLOATING in the empty space beside the head, "
        "outside the silhouette: one just to the upper-left of the head, one just to the upper-right "
        "of the head, mirror images of each other, same size, same distance from the head on both "
        "sides — like two small spark marks framing the head to suggest neck/shoulder tension. "
        "Nothing else is marked on the figure."
    ),
    "zenkutsu": (
        "a simple pictogram of a person bending forward: the same small-circle-head + "
        "rounded-rectangle-torso construction as IMAGE 2, but the torso is bent forward at roughly "
        "90 degrees (an L-shaped rounded rectangle) over short rounded-rectangle legs, the WHOLE "
        "bent figure filled solid in the single warm accent color with the thick dark outline all "
        "around it — keep it to this one bent L-shape silhouette only, no extra small marks, hands, "
        "or ground lines, so it stays readable as a big simple shape at tiny icon size"
    ),
    "nemuri": (
        "a simple pictogram of a pillow: one wide rounded rectangle with a smaller rounded "
        "indent/fold across the top third, the WHOLE pillow filled solid in the single warm accent "
        "color with the thick dark outline all around it and the fold line drawn as a dark ink "
        "stroke"
    ),
    "none": (
        "a simple pictogram of a smiling face meaning 'nothing in particular, all good': a circle "
        "filled solid in the single warm accent color with the thick dark outline, with two small "
        "dark ink dot eyes and one thick dark ink curved smile stroke inside it — like a simple "
        "smiley face icon, nothing else"
    ),
    "asa": (
        "a simple pictogram of a sunrise: one circle (the sun) filled solid in the single warm "
        "accent color with the thick dark outline, sitting on top of one thick dark ink horizontal "
        "stroke (the horizon line), with three short thick dark ink rays fanned out above the circle"
    ),
    "furo": (
        "a simple pictogram of a bathtub with steam: one wide rounded-rectangle tub shape filled "
        "solid in the single warm accent color with the thick dark outline, with two short thick "
        "dark ink wavy steam strokes rising just above it"
    ),
    "neru": (
        "a simple pictogram of night-time: one crescent-moon shape (a circle with a smaller circle "
        "cut out of one side) filled solid in the single warm accent color with the thick dark "
        "outline, with one small solid four-point star of the same accent color (with dark outline) "
        "beside it"
    ),
    "free": (
        "a simple pictogram of 'undecided': a circle filled solid in the single warm accent color "
        "with the thick dark outline, with one bold thick dark ink question-mark stroke centred "
        "inside it"
    ),
    "hard": (
        "a simple pictogram of a chunky, angular rock/boulder: one bold rounded-but-angular "
        "boulder silhouette (a few flat facets, not a smooth circle) filled solid in the single "
        "warm accent color with the thick dark outline, with two short straight dark ink facet "
        "lines on the surface to suggest a hard rock texture — keep it to one big chunky shape, no "
        "fine cracks or many small facets"
    ),
    "normal": (
        "a simple pictogram meaning 'calm/balanced/normal': one gently rounded wave shape (a flat "
        "rounded-rectangle band with one soft S-curve) filled solid in the single warm accent color "
        "with the thick dark outline — a calm, symmetrical, unremarkable big shape"
    ),
    "soft": (
        "a simple pictogram of a soft, droplet/slime blob: one big rounded droplet silhouette "
        "(wide rounded bottom tapering to a soft rounded point on top, like a water drop) filled "
        "solid in the single warm accent color with the thick dark outline, with one small "
        "highlight-shaped dark ink curve near the top to suggest a soft squishy surface"
    ),
    # B-2指示どおり「わからない」は時間帯「きめてない」(free)と同モチーフの描き直しで統一。
    "unknown": (
        "a simple pictogram of 'undecided', IDENTICAL in construction to IMAGE 2: a circle filled "
        "solid in the single warm accent color with the thick dark outline, with one bold thick "
        "dark ink question-mark stroke centred inside it"
    ),
    "zenshin": (
        "a simple pictogram of a whole person, head to toe: a small circle head, one tall "
        "rounded-rectangle torso, and two short rounded-rectangle legs slightly apart, the WHOLE "
        "figure filled solid in the single warm accent color with the thick dark outline all "
        "around it — no extra accent marks anywhere on the body, since this represents the whole "
        "body rather than one part"
    ),
    "kata": (
        "a simple pictogram of shoulder-blade tension, same solid-filled person construction as "
        "IMAGE 2 (small circle head, tall rounded-rectangle torso), but with two short thick dark "
        "ink diagonal tick marks placed side by side across the UPPER-BACK of the torso (shoulder "
        "blade area) to mark that region — same dark ink color as the outline"
    ),
    "kubi": (
        "a simple pictogram of neck stiffness, same solid-filled person construction as IMAGE 2 "
        "(small circle head, tall rounded-rectangle torso), but with one thick dark ink horizontal "
        "stroke drawn right at the narrow neck junction between the head circle and the torso "
        "rectangle, plus one small dark ink zigzag mark beside the neck to suggest tension"
    ),
    "senaka": (
        "a simple pictogram of posture/back, same solid-filled person construction as IMAGE 2 "
        "(small circle head, tall rounded-rectangle torso) shown from the side, but with one thick "
        "dark ink curved stroke running down the full length of the back edge of the torso to show "
        "the spine/posture line"
    ),
    "kokansetsu": (
        "a simple pictogram of the hip joints, same solid-filled person construction as IMAGE 2 "
        "(small circle head, tall rounded-rectangle torso with two short rounded-rectangle legs), "
        "but with two small solid dark ink dots marked at the LEFT and RIGHT points where the legs "
        "join the torso (the hip joints)"
    ),
    "kaikyaku": (
        "a simple pictogram of doing the splits: the same solid-filled person construction as "
        "IMAGE 2 (small circle head, rounded-rectangle torso), but with two long rounded-rectangle "
        "legs spread WIDE apart into a horizontal V shape below the torso, the whole figure filled "
        "solid in the single warm accent color with the thick dark outline all around it"
    ),
    "momoura": (
        "a simple pictogram of the back of the thigh (hamstring), symmetric front-facing figure: a "
        "small circle head centred on top of one tall rounded-rectangle torso with two short arms "
        "(vertical rounded-rectangle strokes along each side of the torso) and two long "
        "rounded-rectangle legs below the torso, separated by a thin gap down the middle, the WHOLE "
        "figure filled solid in the single warm accent color with the thick dark outline all around "
        "it, exactly like IMAGE 2's construction. The head, torso, and hip area must be completely "
        "PLAIN — no belt line, no horizontal stroke, no marking anywhere above the legs at all. Add "
        "exactly one marking to the figure: a bold thick dark ink horizontal stroke drawn straight "
        "across BOTH legs together near the BOTTOM of the legs, just above the ankles (NOT near the "
        "top of the legs, NOT near the hips), spanning the full width from the outer edge of the "
        "left leg to the outer edge of the right leg as one single continuous large stroke crossing "
        "the small gap between the legs too. This is the only mark anywhere on the figure."
    ),
    "futomomo": (
        "a simple pictogram of the thigh/glutes area, same solid-filled person construction as "
        "IMAGE 2 (small circle head, tall rounded-rectangle torso with two short rounded-rectangle "
        "legs), but with one wide thick dark ink horizontal band marked across the TOP of both legs "
        "where they meet the torso (hip/glute/thigh area)"
    ),
    "hiza": (
        "a simple pictogram of bow-legged knees (O脚), same solid-filled person construction as "
        "IMAGE 2 (small circle head, tall rounded-rectangle torso), but with two short "
        "rounded-rectangle legs that bow OUTWARD at the knee (like a wide letter O between the "
        "knees) and one small solid dark ink dot marked at each knee"
    ),
    "ashikubi": (
        "a simple pictogram of the ankle/sole of the foot, same solid-filled person construction "
        "as IMAGE 2 (small circle head, tall rounded-rectangle torso with two short "
        "rounded-rectangle legs), but with one thick dark ink horizontal stroke marked across both "
        "ankles near the bottom of the legs"
    ),
}

# 人型構成の踏襲元(IMAGE 2として渡す既存参照画像)。腰(youtsuu)を基準にする体パーツ系と、
# 「わからない」(unknown)は「きめてない」(free)を基準にする(B-2指示: 同モチーフの描き直し)。
REFERENCE_IMAGE = {
    "katakori": "youtsuu", "zenkutsu": "youtsuu", "kata": "youtsuu", "kubi": "youtsuu",
    "senaka": "youtsuu", "kokansetsu": "youtsuu", "kaikyaku": "youtsuu", "momoura": "youtsuu",
    "futomomo": "youtsuu", "hiza": "youtsuu", "ashikubi": "youtsuu", "zenshin": "youtsuu",
    "unknown": "free",
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
    ref_id = REFERENCE_IMAGE.get(chip_id)
    if ref_id:
        ref_path = os.path.join(OUT_DIR, f"chip-{ref_id}.png")
        # 参照元はこのスクリプトで既に新STYLEで再生成済みである前提(古いv2画像を参照に
        # 使うと新旧の質感が混ざるため)。呼び出し順序に注意(youtsuu/freeを先に)。
        if os.path.exists(ref_path):
            images += ["-F", f"image[]=@{ref_path}"]
        else:
            sys.exit(f"参照元 {ref_id} が未生成です。先に生成してください: python3 {sys.argv[0]} {ref_id}")
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
