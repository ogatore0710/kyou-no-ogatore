#!/usr/bin/env python3
"""体の部位イラストへ珊瑚色ハイライトを追加(TASK-C2-2026-07-31-bodypart-art-legibility.md)。

老眼対応: 11体それぞれの該当部位に、かたさチェック図解の「かかと」ピンク(quizArtPink=
#E56A9A、QuizArt.swift:15)と同系の珊瑚色パッチを大きく重ねる。ポーズ・キャラ・トーンは
現行11体(.art-staging/chip-<key>.png)をそのままIMAGE 1参照にして維持し、色パッチの追加
だけを行う。background=transparent必須(前回教訓)。

  python3 gen-bodypart-highlight.py koshi
  python3 gen-bodypart-highlight.py all
"""
import base64
import json
import os
import subprocess
import sys

from PIL import Image

KEY_PATH = os.path.expanduser("~/.claude/secrets/openai-ogatore-manuals.key")
SRC_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging")
OUT_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/bodypart-highlight")

STYLE = (
    "Keep the character, pose, colors, outline, and composition EXACTLY as in the reference "
    "image — do not redraw or change the pose, face, or body shape in any way. The ONLY "
    "change is: add one bold, clearly visible coral-pink highlight patch (color similar to "
    "hex #E56A9A, a warm rose-pink, matching this app's existing 'body part called out in "
    "pink' visual language) as a soft glowing oval/blob shape overlaid on top of the specific "
    "body region named below. The patch must be LARGE — big enough to cover a substantial "
    "part of that body region, not a small dot — so it is unmistakably visible even shrunk to "
    "a tiny 22x22 pixel icon. The patch can have a soft slightly-transparent glow edge but its "
    "core must be a solid, clearly saturated coral-pink. Keep the thick brown character "
    "outline visible on top of/around the patch so the character silhouette doesn't get lost. "
    "Fully transparent background, no text, no watermark."
)

# 該当部位の当て色位置。既存11体の実際のポーズに合わせて指示する。
REGION = {
    "koshi": "the lower back/waist area where the character's hand is resting (the area it is stretching)",
    "kata": "both shoulders (the area the character is shrugging/tensing)",
    "kubi": "the neck/side of the neck that the character is rubbing with its hand",
    "senaka": "the whole back/spine area of the character's torso (the area it is stretching upward)",
    "kokansetsu": "the hip joint area on both sides where the legs meet the torso (the butterfly-stretch pose)",
    "kaikyaku": "the inner thigh/groin area between the two spread-open legs",
    "momoura": "the back of both thighs (the hamstring area, visible in the forward-bend pose)",
    "futomomo": "the front of the thigh being pulled up/back (the standing leg's thigh)",
    "hiza": "both knees (where the character's hands are resting)",
    "ashikubi": "the foot/ankle that the character is holding and rubbing with both hands",
}


def load_key():
    if not os.path.exists(KEY_PATH):
        sys.exit(f"APIキーが見つかりません: {KEY_PATH}")
    return open(KEY_PATH, encoding="utf-8").read().strip()


def trim_and_pad(path, margin_ratio=0.08, canvas=1024):
    im = Image.open(path).convert("RGBA")
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    w, h = im.size
    side = max(w, h)
    side_with_margin = int(side / (1 - 2 * margin_ratio))
    square = Image.new("RGBA", (side_with_margin, side_with_margin), (0, 0, 0, 0))
    square.paste(im, ((side_with_margin - w) // 2, (side_with_margin - h) // 2), im)
    square = square.resize((canvas, canvas), Image.LANCZOS)
    square.save(path)


def gen(part):
    if part not in REGION:
        sys.exit(f"不明な部位: {part}")
    src = os.path.join(SRC_DIR, f"chip-{part}.png")
    if not os.path.exists(src):
        sys.exit(f"元画像が無い: {src}")
    prompt = f"Add the coral-pink highlight patch on {REGION[part]}. " + STYLE
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"chip-{part}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", "-F", f"image[]=@{src};type=image/png",
         "-F", f"prompt={prompt}",
         "-F", "size=1024x1024", "-F", "quality=medium", "-F", "background=transparent"],
        capture_output=True, text=True, timeout=300,
    )
    if res.returncode != 0:
        sys.exit(f"curl失敗({part}): {res.stderr[:300]}")
    data = json.loads(res.stdout)
    if "error" in data:
        sys.exit(f"APIエラー({part}): {data['error'].get('message')}")
    with open(out_path, "wb") as f:
        f.write(base64.b64decode(data["data"][0]["b64_json"]))
    trim_and_pad(out_path)
    print(f"-> {out_path}")


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        sys.exit(__doc__)
    ids = list(REGION) if args[0] == "all" else args
    for i in ids:
        gen(i)
