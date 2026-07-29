#!/usr/bin/env python3
"""硬さチェック6タイプのキャラを、スクイーズハンターのトーンで生成する。

本人決定(2026-07-29): 「6つ全部そろえ直す」「かたさモンスター図鑑とは別系統でよい」。
トーンの正本は squeeze-hunter/ART-STYLE-RULE.md(2026-07-28本人承認・パターンC系統)。
同ファイル・同ディレクトリは読むだけで、一切変更しない(他艦プロダクトへの直接編集禁止)。

既存3体(momo/kenko/yawara)は、いまのデザインを IMAGE 2 として渡して「同じ子」を保ったまま
素材だけスクイーズに置き換える。新規3体(koka/ashi/robot)はアンカーのみを渡して新しく描く。

出力は staging ディレクトリ。assets/ への反映は目視確認を通してから別途行う
(assets/type-*.png は Web版と共用のため、差し替えるとWeb版の絵も変わる)。

  python3 gen-type-art.py <id> [<id> ...]     # 指定したものだけ生成
  python3 gen-type-art.py all                 # 6体ぶん
"""
import base64
import json
import os
import subprocess
import sys

KEY_PATH = os.path.expanduser("~/.claude/secrets/openai-ogatore-manuals.key")
ANCHOR = os.path.expanduser("~/Claude/squeeze-hunter/art-raw/_anchor_melonpan.png")
ASSETS = os.path.expanduser("~/Claude/kyou-no-ogatore/assets")
OUT_DIR = os.path.expanduser("~/Claude/kyou-no-ogatore/.art-staging")

# ART-STYLE-RULE.md「共通スタイル節」をそのまま踏襲する(勝手に言い換えない)。
STYLE = (
    "Same toy line as IMAGE 1: a soft squishy squeeze toy with realistic-leaning colours, "
    "a kawaii face with small glossy dark eyes, tiny smiling mouth and rosy blush cheeks, "
    "smooth squishy polyurethane texture and slightly puffy silhouette. "
    "Fully transparent background, no cast shadow, no ground, no base, no text, no watermark, "
    "single object, centred, generous empty margin on all four sides."
)

# 名前・部位は app-quiz.js:44-80 の TYPES に一致させること。
TYPES = {
    "momo":   ("つっぱりモモンガ", "a flying squirrel with its patagium stretched wide open on both sides, "
                                   "sitting upright, greyish-brown and cream fur"),
    "koka":   ("開かずのトビラ",   "a small stubby wooden door with a round knob, shut tight, "
                                   "warm brown wood grain, standing upright"),
    "kenko":  ("飛べないダチョウ", "a very round chubby baby ostrich with a SHORT thick neck and a big round "
                                   "head, tiny useless wings, sandy beige feathers and a small orange beak, "
                                   "same chibi proportions as IMAGE 1"),
    "ashi":   ("棒立ちペンギン",   "a penguin standing perfectly stiff and straight with flippers pressed "
                                   "to its sides, black and white with orange feet"),
    "robot":  ("ガチガチロボット", "a small boxy retro robot standing stiffly, rounded square head and body, "
                                   "pale metallic grey with a few simple bolts"),
    "yawara": ("しなやかネコ",     "a supple relaxed cat sitting with its tail curled around, "
                                   "warm orange tabby fur"),
}


def load_key():
    if not os.path.exists(KEY_PATH):
        sys.exit(f"APIキーが見つかりません: {KEY_PATH}")
    return open(KEY_PATH, encoding="utf-8").read().strip()


def gen(type_id, fresh=False):
    """fresh=True のとき、既存デザインを参照せずに描き直す。

    既存デザインの維持が裏目に出る子がいる(飛べないダチョウは首が長く顔も小さいため、
    そのまま素材だけ替えると他の5体の丸いシルエットから1体だけ浮く)。そういう子は
    アンカーだけを渡して、このトイラインの体型で描き直す。
    """
    name, what = TYPES[type_id]
    existing = os.path.join(ASSETS, f"type-{type_id}.png")
    images = ["-F", f"image[]=@{ANCHOR}"]
    if os.path.exists(existing) and not fresh:
        # 既存の子は「同じ子のまま素材だけ替える」。デザイン維持が最優先(ART-STYLE-RULE)。
        images += ["-F", f"image[]=@{existing}"]
        prompt = (
            f"IMAGE 2 shows an existing character called {name} ({what}). "
            f"Reproduce the SAME character — same species, same pose, same colours, same personality — "
            f"but remade as a squeeze toy in the style of IMAGE 1. " + STYLE
        )
    else:
        prompt = (
            f"Draw a NEW squeeze toy in exactly the same style and material as IMAGE 1: {what}. "
            + STYLE
        )

    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, f"type-{type_id}.png")
    res = subprocess.run(
        ["curl", "-sS", "https://api.openai.com/v1/images/edits",
         "-H", f"Authorization: Bearer {load_key()}",
         "-F", "model=gpt-image-1", *images,
         "-F", f"prompt={prompt}",
         "-F", "size=1024x1024", "-F", "quality=medium", "-F", "background=transparent"],
        capture_output=True, text=True, timeout=300,
    )
    if res.returncode != 0:
        sys.exit(f"curl失敗({type_id}): {res.stderr[:300]}")
    data = json.loads(res.stdout)
    if "error" in data:
        sys.exit(f"APIエラー({type_id}): {data['error'].get('message')}")
    with open(out_path, "wb") as f:
        f.write(base64.b64decode(data["data"][0]["b64_json"]))
    print(f"-> {out_path} ({name})")


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        sys.exit(__doc__)
    ids = list(TYPES) if args[0] == "all" else args
    for t in ids:
        # "kenko:fresh" のように書くと既存デザインを参照せず描き直す
        t, _, mode = t.partition(":")
        if t not in TYPES:
            sys.exit(f"不明なタイプ: {t}")
        gen(t, fresh=(mode == "fresh"))
