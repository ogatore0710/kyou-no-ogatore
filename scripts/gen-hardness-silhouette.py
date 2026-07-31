#!/usr/bin/env python3
"""かたさ選択肢4枚のうち3枚を「前屈の角度3段階」に(TASK-C2-2026-07-31-build11-renshu-journey.md B)。

横向きシルエットの前屈で、Q1の実写前屈と同じ視覚言語(指先の到達点で深さを示す)を
うちのキャラのトーンで再現する。「わからない」は現行のchip-unknown.pngを維持するため
このスクリプトの対象外。

  python3 gen-hardness-silhouette.py hard
  python3 gen-hardness-silhouette.py all
"""
import base64
import json
import os
import subprocess
import sys

from PIL import Image

KEY_PATH = os.path.expanduser("~/.claude/secrets/openai-ogatore-manuals.key")
SRC_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging")
OUT_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/hardness-silhouette")

STYLE = (
    "A cute chibi character illustration in EXACTLY the same art style as the reference "
    "image: soft flat 2D illustration, a thick warm brown rounded outline of uniform stroke "
    "weight throughout, warm flat color fill (cream/tan/orange, never white or pale-washed "
    "out), only very subtle soft shading, tiny simple dot eyes, one single rounded pudgy body "
    "shape with short stubby limbs. Side-view (profile) silhouette, standing on the ground, "
    "bending forward at the hips, legs kept straight, doing a standing forward-bend stretch "
    "— the exact depth of the bend is described below and MUST be visually unambiguous. "
    "Single character, centred, generous empty margin on all sides, no text, no watermark, "
    "no border pattern, fully transparent background. Must stay readable as one big "
    "recognizable pose even at a tiny 22x22 pixel icon size."
)

MOTIF = {
    "hard": (
        "bending forward only a LITTLE — the torso barely tips forward, both hands/paws "
        "reach down but stop around thigh/knee height, nowhere near the shins or floor, legs "
        "straight and clearly stiff, a strained uncomfortable expression — representing very "
        "poor flexibility (ガチガチかも)"
    ),
    "normal": (
        "bending forward HALFWAY — the torso tips forward about 45 degrees, both hands/paws "
        "reach down and the fingertips reach shin height (partway down the lower leg, not "
        "the floor), a neutral effortful expression — representing average flexibility (ふつう)"
    ),
    "soft": (
        "bending forward ALL THE WAY DOWN — the torso folds forward until it is nearly "
        "parallel to the legs, both palms are flat on the ground/floor line, a relaxed happy "
        "satisfied expression — representing excellent flexibility (やわらかい)"
    ),
}

REF = os.path.join(SRC_DIR, "chip-koshi.png")


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
    if part not in MOTIF:
        sys.exit(f"不明な選択肢: {part}")
    if not os.path.exists(REF):
        sys.exit(f"参照画像が無い: {REF}")
    prompt = f"Draw a character {MOTIF[part]}. " + STYLE
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"chip-{part}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", "-F", f"image[]=@{REF};type=image/png",
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
    ids = list(MOTIF) if args[0] == "all" else args
    for i in ids:
        gen(i)
