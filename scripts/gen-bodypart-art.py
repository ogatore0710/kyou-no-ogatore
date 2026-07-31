#!/usr/bin/env python3
"""「からだの場所」タグ11部位イラスト本生産(TASK-C2-2026-07-31-bodypart-art-rollout.md)。

トライアル(gen-bodypart-trial.py)で本人選定した「記録カード風」を11部位に展開する。
トライアルの欠陥(background=opaqueで白背景が焼き付いていた)を必ず修正:
transparent生成+alpha bboxトリムで従来のチップ同様の透過PNGに整形する。

出力先はチップシステムと同じ.art-staging/直下(chip-<key>.png)。「腰」だけは
オンボの既存chip-youtsuu.pngと衝突するため、別キー"koshi"を新設する
(SearchView.swift/SearchScreen.ktのbodyTagChipKeyマッピングを"腰"→"koshi"に
差し替える前提。オンボ側のchip-youtsuu.pngは絶対に上書きしない)。

  python3 gen-bodypart-art.py zenshin
  python3 gen-bodypart-art.py all
"""
import base64
import json
import os
import subprocess
import sys

from PIL import Image

KEY_PATH = os.path.expanduser("~/.claude/secrets/openai-ogatore-manuals.key")
ASSETS = os.path.expanduser("~/Claude/kyou-no-ogatore/assets")
TRIAL_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/bodypart-trial")
OUT_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging")

# トライアルで本人承認済みの「記録カード風」出力そのものをスタイルアンカーにする
# (assets/cards/の生画像より、承認済み出力を直接見せた方がブレが少ない)。
REFS = [f"{TRIAL_DIR}/koshi-card.png", f"{TRIAL_DIR}/kata-card.png"]

STYLE = (
    "A cute chibi character illustration in EXACTLY the same art style as the reference "
    "images: soft flat 2D illustration, a thick warm brown rounded outline of uniform stroke "
    "weight throughout, warm flat color fill (cream/tan/orange, never white or pale-washed "
    "out), only very subtle soft shading, tiny simple dot eyes, a small simple nose, round "
    "blush-pink cheek marks, one single rounded pudgy body shape with short stubby limbs. "
    "Convey the idea through POSE AND FACIAL EXPRESSION only — never draw diagrams, arrows, "
    "or anatomical markings. Single character, centred, generous empty margin on all sides, "
    "no text, no watermark, no border pattern (one character only, not a repeating frame), "
    "fully transparent background. Must stay readable as one big recognizable shape and pose "
    "even at a tiny 22x22 pixel icon size — keep the pose big and the silhouette simple."
)

# 11部位。ヒントは発注書のポーズ例に準拠。「腰」はkoshi、他はチップ命名(kata/kubi等)を維持。
MOTIF = {
    "zenshin": (
        "a plain neutral character standing upright with a calm relaxed smile, arms loosely "
        "at its sides — representing the whole body in general, no specific tension anywhere"
    ),
    "kata": (
        "a character with both shoulders raised up in a shrug, hands touching its own "
        "shoulders, a slightly strained worried expression — shoulder tension"
    ),
    "kubi": (
        "a character tilting its head to one side and rubbing the side of its own neck with "
        "one paw/hand, eyes slightly closed as if it feels stiff — neck stiffness"
    ),
    "senaka": (
        "a character standing tall and stretching upward with both arms raised straight up "
        "overhead, chest out, a satisfied refreshed expression — good posture/stretching the back"
    ),
    "kokansetsu": (
        "a character sitting on the ground with the soles of its feet pressed together and "
        "knees dropped out to the sides (butterfly stretch pose), holding its own ankles, "
        "a focused expression — hip joint flexibility"
    ),
    "kaikyaku": (
        "a character sitting on the ground with both legs spread wide open flat in a full "
        "split, arms relaxed at its sides, a proud satisfied expression — doing the splits"
    ),
    "momoura": (
        "a character bending forward at the hips with legs straight, reaching down with both "
        "hands to touch its own toes, head tucked down — hamstring/forward-bend stretch"
    ),
    "futomomo": (
        "a character standing on one leg, reaching one hand back to hold its other foot "
        "pulled up behind it (a standing quad stretch pose), balancing with the other arm out "
        "— thigh stretch"
    ),
    "koshi": (
        "a character standing with both paws/hands on its hips, gently arching its back "
        "backward in a stretching pose, eyes closed contentedly — lower-back stretch"
    ),
    "hiza": (
        "a character standing with knees bent inward slightly and both hands resting on its "
        "own knees, leaning forward slightly, a gentle careful expression — knee care"
    ),
    "ashikubi": (
        "a character sitting on the ground with one leg extended, both hands wrapped around "
        "its own foot/ankle massaging it, eyes closed in relief — ankle/sole care"
    ),
}


def load_key():
    if not os.path.exists(KEY_PATH):
        sys.exit(f"APIキーが見つかりません: {KEY_PATH}")
    return open(KEY_PATH, encoding="utf-8").read().strip()


def trim_and_pad(path, margin_ratio=0.08, canvas=1024):
    """alpha bboxでトリムし、正方形キャンバスへ再配置する(既存チップ群と同じ寸法整形)。"""
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
        sys.exit(f"不明な部位: {part}")
    images = []
    for r in REFS:
        images += ["-F", f"image[]=@{r};type=image/png"]
    prompt = f"Draw {MOTIF[part]}. " + STYLE
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"chip-{part}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", *images,
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
