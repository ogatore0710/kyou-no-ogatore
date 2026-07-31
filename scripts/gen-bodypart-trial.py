#!/usr/bin/env python3
"""体の部位イラストのトーン刷新トライアル(TASK-C2-2026-07-31-bodypart-art-trial.md)。

B-2の平置きステッカー調(gen-chip-art.py)が本人評価で不合格だったため、まったく別の
2トーンを試作する。アンカーはタブバーではなく実物の記録カード(assets/cards/)と
硬さチェック6体(assets/type-*.png)。STYLE文もB-2のものは一切使わない。

トライアルのみ・アプリへの組み込みは無し。出力は.art-staging/bodypart-trial/。

  python3 gen-bodypart-trial.py card koshi
  python3 gen-bodypart-trial.py clay kata
  python3 gen-bodypart-trial.py all
"""
import base64
import json
import os
import subprocess
import sys

KEY_PATH = os.path.expanduser("~/.claude/secrets/openai-ogatore-manuals.key")
ASSETS = os.path.expanduser("~/Claude/kyou-no-ogatore/assets")
OUT_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/bodypart-trial")

# 記録カード風: assets/cards/の実物2枚(猫のボーダー・くまのボーダー)をトーン見本にする。
# 硬さチェック6体風: assets/type-momo.pngの実物1枚をトーン見本にする。
CARD_REFS = [f"{ASSETS}/cards/rare_neko.webp", f"{ASSETS}/cards/rare_kuma.webp"]
CLAY_REFS = [f"{ASSETS}/type-momo.png"]

CARD_STYLE = (
    "A cute chibi character illustration in EXACTLY the same art style as the reference "
    "images: soft flat 2D illustration, a thick warm brown rounded outline of uniform stroke "
    "weight throughout, warm flat color fill with only very subtle soft shading, tiny simple "
    "dot eyes, a small simple nose, round blush-pink cheek marks, one single rounded pudgy "
    "body shape with short stubby limbs, a closed cheerful mouth, warm cream/tan/orange color "
    "palette. Single character, centred, plain solid white background, no text, no watermark, "
    "no border pattern (draw ONE character only, not a repeating frame). Must stay readable as "
    "one big recognizable shape even at a small icon size — simple pose, minimal fine details."
)

CLAY_STYLE = (
    "A cute soft 3D clay/vinyl-toy figure rendered in EXACTLY the same style as the reference "
    "image: glossy soft plastic material with gentle rounded highlights, a rounded puffy chibi "
    "body, tiny simple dot eyes, a small triangular nose, round blush-pink cheek marks, a "
    "closed simple smile, short stubby rounded limbs, warm tan/beige coloring, soft ambient "
    "shading with no hard cast shadow. Single character, centred, plain solid white "
    "background, no text, no watermark. Must stay readable as one big recognizable shape even "
    "at a small icon size — simple pose, minimal fine details."
)

MOTIF = {
    "koshi": (
        "a character standing with both paws/hands on its hips, gently arching its back "
        "backward in a stretching pose, to represent lower-back stretching"
    ),
    "kata": (
        "a character with both shoulders raised up in a shrug, hands touching its own "
        "shoulders, to represent shoulder/shoulder-blade tension"
    ),
}


def load_key():
    if not os.path.exists(KEY_PATH):
        sys.exit(f"APIキーが見つかりません: {KEY_PATH}")
    return open(KEY_PATH, encoding="utf-8").read().strip()


def gen(tone, part):
    if part not in MOTIF:
        sys.exit(f"不明な部位: {part}")
    if tone == "card":
        refs, style = CARD_REFS, CARD_STYLE
    elif tone == "clay":
        refs, style = CLAY_REFS, CLAY_STYLE
    else:
        sys.exit(f"不明なトーン: {tone}")
    images = []
    for r in refs:
        mime = "image/webp" if r.endswith(".webp") else "image/png"
        images += ["-F", f"image[]=@{r};type={mime}"]
    prompt = f"Draw {MOTIF[part]}. " + style
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"{part}-{tone}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", *images,
         "-F", f"prompt={prompt}",
         "-F", "size=1024x1024", "-F", "quality=medium", "-F", "background=opaque"],
        capture_output=True, text=True, timeout=300,
    )
    if res.returncode != 0:
        sys.exit(f"curl失敗({tone}/{part}): {res.stderr[:300]}")
    data = json.loads(res.stdout)
    if "error" in data:
        sys.exit(f"APIエラー({tone}/{part}): {data['error'].get('message')}")
    with open(out_path, "wb") as f:
        f.write(base64.b64decode(data["data"][0]["b64_json"]))
    print(f"-> {out_path}")


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        sys.exit(__doc__)
    if args[0] == "all":
        for tone in ("card", "clay"):
            for part in MOTIF:
                gen(tone, part)
    else:
        tone, part = args[0], args[1]
        gen(tone, part)
