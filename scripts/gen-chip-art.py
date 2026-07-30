#!/usr/bin/env python3
"""部位・時間帯選択チップの生成絵文字イラスト(TASK-C2-2026-07-30-icon-system-addendum-chips.md)。

本人決定: トンマナはタブバーの5つ(KyonoTabBar)に揃える(硬さチェック6タイプの
スクイーズハンター系統とは別・そちらには寄せない)。アンカーはタブバーのスクリーン
ショットから切り出した実物(icon-anchor-tabbar.png)。

出力は staging ディレクトリ。assets/ への反映は目視確認を通してから別途行う。

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

# タブバー5つの実物(太めの丸みストローク・フラットな幾何形・単色寄り+選択中だけ塗り)を
# そのままトーンの正本として説明する。gen-type-art.pyのSTYLE節と同じ考え方。
# alan5差し戻し(2026-07-30): 1回目は輪郭が細い波線・塗りが有機的なブロブでタブバーと質感が
# 揃わなかった。線の太さを均一にし、幾何学図形(角丸長方形・円)だけで構成するよう明示的に指定する。
STYLE = (
    "A minimalist icon using ONLY simple flat geometric shapes — rounded rectangles and circles, "
    "like a pictogram — in exactly the same visual style as the tab bar icons shown in IMAGE 1: "
    "a UNIFORM thick stroke width identical throughout (never thin, never tapering, never wavy or "
    "organic freehand contour lines), rounded corners, a single flat accent color fill on one part "
    "(matching IMAGE 1's single-color-fill convention, e.g. its solid yellow house icon), no "
    "gradients, no soft blob shapes, no 3D toy or plush character, not medical or anatomical, "
    "fully transparent background, no cast shadow, no ground, no text, no watermark, single icon, "
    "centred, generous empty margin on all four sides."
)

# 部位・時間帯の対象4箇所(worry/anchorチップ)。ラベルはOnboardingViews.swift/.ktの定義と一致。
SUBJECTS = {
    "kata":  "肩こり・首", "koshi": "腰", "zenkutsu": "前屈できない",
    "nemuri": "眠り", "toku": "とくにない",
    "asa": "朝おきて", "furo": "おふろ上がり", "neru": "寝るまえ", "kimetenai": "きめてない",
}

WHAT = {
    "kata": "a simple icon of shoulder and neck stiffness — a person's shoulders and neck with small tension lines",
    "koshi": (
        "a simple geometric pictogram of a person's lower back: build the figure out of a small "
        "circle for the head and one tall rounded rectangle for the torso (same construction as "
        "IMAGE 1's simple shapes), with a horizontal rounded-rectangle band across the lower third "
        "of the torso filled in the single accent color to mark the lower-back/waist area"
    ),
    "zenkutsu": "a simple icon of a person bending forward reaching toward their toes but stopping short",
    "nemuri": "a simple icon of a crescent moon with a small sleeping zzz",
    "toku": "a simple icon of a small calm checkmark or dot (nothing in particular)",
    "asa": "a simple icon of a sunrise — a sun peeking over a horizon line",
    "furo": "a simple icon of a bathtub with a small steam wisp above it",
    "neru": "a simple icon of a crescent moon and a small star",
    "kimetenai": "a simple icon of a small clock or question mark, undecided",
}


def load_key():
    if not os.path.exists(KEY_PATH):
        sys.exit(f"APIキーが見つかりません: {KEY_PATH}")
    return open(KEY_PATH, encoding="utf-8").read().strip()


def gen(chip_id):
    if chip_id not in WHAT:
        sys.exit(f"不明なID: {chip_id}")
    what = WHAT[chip_id]
    prompt = f"Draw {what}. " + STYLE
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"chip-{chip_id}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", "-F", f"image[]=@{ANCHOR}",
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
