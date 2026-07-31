#!/usr/bin/env python3
"""部位イラスト11枚を「クローズアップ」に総取り替え(TASK-C2-2026-07-31-build11-renshu-journey.md A)。

**この方式に至った経緯**: 当初はgpt-image-1へ「全身ポーズをやめて部位だけの
extreme close-upに描き直せ」という編集プロンプトを投げたが、11枚中10枚が
「輪郭線だけで塗りが無い」壊れた結果になり、kaikyakuは安全システムに2回
連続で弾かれた(groin/spread-legsという語がsexual判定)。images/editsは
参照画像の構図を大きく作り変える指示に弱いと判断し、方針転換: 既存の
本番ハイライト版(`.art-staging/bodypart-highlight/chip-<key>.png`、
alan5最終ゲート通過済み)をPILで直接クロップする方式に切り替えた。
塗り・線・珊瑚色ハイライトは既存の承認済みピクセルそのものなので、
AIの再現ブレやセーフティ誤爆が原理的に起きない。

CROPSの座標は本番ハイライト版(1024x1024)に対する(left, top, right, bottom)。
alpha bboxトリム+正方形パディングは既存チップと同じ手法。

  python3 gen-bodypart-closeup.py            # 全部再生成(全部crop)
  python3 gen-bodypart-closeup.py kata       # 1件だけ
"""
import os
import sys

from PIL import Image

SRC_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/bodypart-highlight")
OUT_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging/bodypart-closeup")

# 見た目を確認しながら1枚ずつ手作業で追い込んだ座標(momoura/kubiは1回ずつ
# 見づらさ・顔の写り込みを理由に再調整済み)。
CROPS = {
    "zenshin":    (170, 420, 720, 820),
    "kata":       (140, 430, 720, 730),
    "kubi":       (200, 470, 750, 780),
    "senaka":     (160, 420, 800, 850),
    "kokansetsu": (140, 540, 800, 890),
    "kaikyaku":   (60, 520, 900, 900),
    "momoura":    (380, 500, 850, 900),
    "futomomo":   (330, 470, 790, 830),
    "koshi":      (300, 560, 800, 910),
    "hiza":       (210, 570, 660, 960),
    "ashikubi":   (110, 590, 710, 960),
}


def trim_and_pad(im, margin_ratio=0.08, canvas=1024):
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    w, h = im.size
    side = max(w, h)
    side_with_margin = int(side / (1 - 2 * margin_ratio))
    square = Image.new("RGBA", (side_with_margin, side_with_margin), (0, 0, 0, 0))
    square.paste(im, ((side_with_margin - w) // 2, (side_with_margin - h) // 2), im)
    return square.resize((canvas, canvas), Image.LANCZOS)


def gen(part):
    if part not in CROPS:
        sys.exit(f"不明な部位: {part}")
    src = os.path.join(SRC_DIR, f"chip-{part}.png")
    if not os.path.exists(src):
        sys.exit(f"元画像が無い: {src}")
    os.makedirs(OUT_DIR, exist_ok=True)
    im = Image.open(src).convert("RGBA")
    crop = im.crop(CROPS[part])
    out = trim_and_pad(crop)
    out_path = os.path.join(OUT_DIR, f"chip-{part}.png")
    out.save(out_path)
    print(f"-> {out_path}")


if __name__ == "__main__":
    args = sys.argv[1:]
    ids = list(CROPS) if not args or args[0] == "all" else args
    for i in ids:
        gen(i)
