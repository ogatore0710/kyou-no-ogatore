#!/usr/bin/env python3
"""ビルド12 W2: オンボチップ刷新(7枚)+タグ3カテゴリ新規15種(TASK-C2-2026-07-31-
build12-journey2-splash-emoji.md W2)。

トーンは部位クローズアップ(.art-staging/bodypart-closeup/)と同族(カード風・太い茶輪郭・
透過)。参照はchip-koshi.pngを使う(既存チップ群と質感を揃えるため)。カテゴリごとに
出力先を分ける:
  onboarding-chips/  オンボの悩み5種のうち新規3種+時間帯4種(chip-<v>.png形式)
  tag-icons/         時間・シーン5・目的6・その他4の新規15種(chip-<key>.png形式)

  python3 gen-w2-icons.py zenkutsu
  python3 gen-w2-icons.py all
"""
import base64
import json
import os
import subprocess
import sys

from PIL import Image

KEY_PATH = os.path.expanduser("~/.claude/secrets/openai-ogatore-manuals.key")
REF = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/chip-koshi.png")
BASE = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging")

STYLE = (
    "A cute chibi character illustration in EXACTLY the same art style as the reference image: "
    "soft flat 2D illustration, a thick warm brown rounded outline of uniform stroke weight "
    "throughout, warm flat color fill (cream/tan/orange, never white or pale-washed out), only "
    "very subtle soft shading, tiny simple dot eyes, one single rounded pudgy body shape with "
    "short stubby limbs. Convey the idea through POSE, PROPS, AND FACIAL EXPRESSION only — never "
    "draw diagrams, arrows, or text. Single character (plus small simple props if noted), centred, "
    "generous empty margin on all sides, no text, no watermark, no border pattern, fully "
    "transparent background. Must stay readable as one big recognizable shape and pose even at a "
    "tiny 22x22 pixel icon size — keep the pose/prop big and the silhouette simple and bold."
)

# (出力先サブフォルダ, ファイル名キー, モチーフ) の辞書
JOBS = {
    # --- オンボ: 悩み(worry)の残り3種。katakori/youtsuuは部位クローズアップの流用のため対象外。
    "zenkutsu": (
        "onboarding-chips",
        "a character bending forward trying to touch its toes but only reaching its knees, legs "
        "stiff and straight, a strained frustrated expression — representing 'can't bend forward' worry",
    ),
    "nemuri": (
        "onboarding-chips",
        "a character standing but visibly drowsy, eyes half-closed with heavy droopy eyelids, one "
        "paw rubbing its own eye, a small floating zzz-style sleepy squiggle beside its head — "
        "representing sleep/tiredness worry",
    ),
    "none": (
        "onboarding-chips",
        "a plain neutral character standing upright with a calm relaxed close-mouthed smile, arms "
        "loosely at its sides, no tension or worry anywhere — representing 'nothing in particular' worry",
    ),
    # --- オンボ: 時間帯(anchor)4種。
    "asa": (
        "onboarding-chips",
        "a character stretching both arms up and out with a bright cheerful morning expression, a "
        "small simple sun shape with a few short rays drawn beside it — representing waking up in "
        "the morning",
    ),
    "furo": (
        "onboarding-chips",
        "a character sitting relaxed with a warm content expression, a small simple steam-wisp "
        "shape (two or three soft curved lines) floating above its head — representing right after "
        "a bath",
    ),
    "neru": (
        "onboarding-chips",
        "a character standing with eyes gently closed and a peaceful sleepy smile, a small simple "
        "crescent-moon shape with a couple of tiny star dots beside it — representing right before "
        "bed",
    ),
    "free": (
        "onboarding-chips",
        "a character with both arms raised in a relaxed shrug, an easygoing carefree smile, a small "
        "simple clock-face shape with no specific hands emphasized floating beside it — representing "
        "no fixed time",
    ),
    # --- 動画を探す: 時間・シーン(5)
    "toki_asa": (
        "tag-icons",
        "a character stretching both arms up and out with a bright cheerful morning expression, a "
        "small simple sun shape with a few short rays drawn beside it — representing morning time",
    ),
    "toki_yoru": (
        "tag-icons",
        "a character standing with eyes gently closed and a peaceful sleepy smile, a small simple "
        "crescent-moon shape with a couple of tiny star dots beside it — representing night/before bed",
    ),
    "toki_suwaru": (
        "tag-icons",
        "a character sitting cross-legged on the ground with a calm relaxed expression, both paws "
        "resting on its knees — representing doing it seated",
    ),
    "toki_10pun": (
        "tag-icons",
        "a character standing with one paw pointing at a small simple round clock-face shape held "
        "up beside it (the clock has no specific numbers, just a plain round face with two hands), "
        "a confident quick expression — representing a short 10-minutes-or-less session",
    ),
    "toki_short": (
        "tag-icons",
        "a character mid-motion with one leg stepping forward and a small simple lightning-bolt "
        "zigzag shape floating beside it, a snappy energetic expression — representing a very short "
        "quick video",
    ),
    # --- 動画を探す: 目的(6)
    "mokuteki_mukumi": (
        "tag-icons",
        "a character gently pressing both paws against its own lower leg/calf with a soft relieved "
        "expression, a couple of small simple upward wavy arrow-less lines above the leg suggesting "
        "fluid moving up — representing reducing swelling/puffiness",
    ),
    "mokuteki_hikishime": (
        "tag-icons",
        "a character flexing one arm with a determined confident expression, a small simple star "
        "sparkle shape beside the flexed arm — representing toning up/firming the body",
    ),
    "mokuteki_massage": (
        "tag-icons",
        "a character using both paws to gently knead/press its own shoulder with a relieved content "
        "expression — representing fascia release/massage",
    ),
    "mokuteki_jiritsu": (
        "tag-icons",
        "a character sitting cross-legged with both paws resting palms-up on its knees, eyes gently "
        "closed, a calm serene meditative expression — representing balancing the autonomic nervous "
        "system",
    ),
    "mokuteki_sports": (
        "tag-icons",
        "a character mid-lunge in an athletic stretching pose with a focused energetic expression, "
        "a small simple sweat-drop shape beside its head — representing sports/exercise warm-up or "
        "cool-down",
    ),
    "mokuteki_selfcare": (
        "tag-icons",
        "a character hugging itself gently with both arms wrapped around its own torso, a soft warm "
        "content expression, one small simple heart shape floating beside it — representing daily "
        "life self-care",
    ),
    # --- 動画を探す: その他(4)
    "sonota_kaisetsu": (
        "tag-icons",
        "a character standing beside a small simple rounded speech-bubble shape (empty, no text "
        "inside), one paw raised as if explaining something, an attentive friendly expression — "
        "representing an explanation/commentary video",
    ),
    "sonota_suizokukan": (
        "tag-icons",
        "a character standing with a delighted curious expression, a small simple rounded fish "
        "silhouette shape floating beside it — representing an aquarium location shoot",
    ),
    "sonota_kominka": (
        "tag-icons",
        "a character sitting relaxed in seiza-like pose, a small simple house shape with a "
        "triangular roof floating beside it — representing an old Japanese folk-house location "
        "shoot",
    ),
    "sonota_sonota": (
        "tag-icons",
        "a plain neutral character standing upright with a gentle open curious expression, a small "
        "simple rounded question-mark-free ellipsis (three small dots in a row) floating beside it "
        "— representing miscellaneous/other",
    ),
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


def gen(key):
    if key not in JOBS:
        sys.exit(f"不明なキー: {key}")
    subdir, motif = JOBS[key]
    prompt = f"Draw a character {motif}. " + STYLE
    out_dir = os.path.join(BASE, subdir)
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, f"chip-{key}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", "-F", f"image[]=@{REF};type=image/png",
         "-F", f"prompt={prompt}",
         "-F", "size=1024x1024", "-F", "quality=medium", "-F", "background=transparent"],
        capture_output=True, text=True, timeout=300,
    )
    if res.returncode != 0:
        sys.exit(f"curl失敗({key}): {res.stderr[:300]}")
    data = json.loads(res.stdout)
    if "error" in data:
        sys.exit(f"APIエラー({key}): {data['error'].get('message')}")
    with open(out_path, "wb") as f:
        f.write(base64.b64decode(data["data"][0]["b64_json"]))
    trim_and_pad(out_path)
    print(f"-> {out_path}")


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        sys.exit(__doc__)
    ids = list(JOBS) if args[0] == "all" else args
    for i in ids:
        gen(i)
