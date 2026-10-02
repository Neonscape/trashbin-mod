"""Rebuild original pixel textures, block models and data using only Python's stdlib.

This is the editable asset source. It never downloads or copies another mod's art.
Run `python scripts/generate_assets.py`; generated resources are checked into Git.
"""
from pathlib import Path
import json
import random
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "common/src/main/resources"
ASSETS = RES / "assets/trashbin"


def save_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def png(path, pixels):
    height, width = len(pixels), len(pixels[0])
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    data = b"".join(b"\0" + bytes(c for pixel in row for c in pixel) for row in pixels)
    output = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    output += chunk(b"IDAT", zlib.compress(data, 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(output)


def rgba(color):
    return (*color, 255)


def metal(base, seed):
    rng = random.Random(seed)
    return [[rgba(tuple(max(0, min(255, channel + rng.choice([-3, -1, 0, 0, 1, 3]))) for channel in base))
             for _ in range(16)] for _ in range(16)]


def rect(pixels, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            pixels[y][x] = rgba(color)


def textures():
    body = metal((119, 124, 119), 13)
    rect(body, 0, 0, 15, 0, (169, 174, 164))
    rect(body, 0, 1, 15, 1, (139, 145, 135))
    rect(body, 0, 2, 0, 15, (147, 152, 141))
    rect(body, 15, 2, 15, 15, (78, 87, 83))
    rect(body, 0, 14, 15, 15, (71, 80, 75))
    for x in (2, 13):
        rect(body, x, 3, x, 12, (108, 116, 108))
        rect(body, x, 4, x, 4, (185, 167, 119))
        rect(body, x, 12, x, 12, (145, 129, 94))
    front = [row.copy() for row in body]
    # Small embossed disposal mark; subdued and readable in both workshops and homes.
    rect(front, 5, 6, 10, 6, (75, 83, 77))
    rect(front, 6, 7, 9, 11, (86, 94, 86))
    rect(front, 7, 8, 7, 10, (159, 165, 143))
    rect(front, 9, 8, 9, 10, (159, 165, 143))
    rect(front, 7, 5, 8, 5, (89, 97, 89))
    lid = metal((91, 103, 96), 27)
    rect(lid, 0, 0, 15, 0, (159, 166, 149))
    rect(lid, 0, 0, 0, 15, (135, 145, 130))
    rect(lid, 0, 15, 15, 15, (57, 71, 64))
    rect(lid, 15, 0, 15, 15, (60, 74, 67))
    rect(lid, 2, 2, 13, 2, (115, 127, 113))
    rect(lid, 2, 3, 2, 13, (105, 117, 104))
    rect(lid, 3, 13, 13, 13, (73, 88, 77))
    rim = metal((61, 72, 65), 29)
    rect(rim, 0, 0, 15, 1, (111, 124, 109))
    rect(rim, 0, 14, 15, 15, (44, 54, 49))
    handle = metal((156, 140, 102), 42)
    rect(handle, 0, 0, 15, 1, (199, 182, 131))
    rect(handle, 0, 14, 15, 15, (92, 85, 66))
    active = metal((96, 132, 79), 43)
    inactive = metal((127, 76, 60), 44)
    rect(active, 2, 2, 13, 4, (177, 192, 112))
    rect(inactive, 2, 2, 13, 4, (183, 126, 83))
    for name, image in dict(body=body, front=front, lid=lid, rim=rim, handle=handle,
                            active=active, inactive=inactive).items():
        png(ASSETS / f"textures/block/{name}.png", image)
    bar = [[rgba((47, 53, 45)) for _ in range(160)] for _ in range(32)]
    rect(bar, 0, 0, 159, 0, (67, 74, 60))
    rect(bar, 1, 1, 158, 6, (37, 43, 36))
    rect(bar, 0, 7, 159, 7, (236, 236, 219))
    for y, color in enumerate([(164, 188, 94), (126, 157, 63), (112, 147, 53),
                                (112, 147, 53), (97, 131, 45), (80, 111, 38)], 8):
        rect(bar, 0, y, 159, y, color)
    png(ASSETS / "textures/gui/experience_bar.png", bar)


def element(start, end, texture, overrides=None, rotation=None):
    faces = {face: {"texture": f"#{texture}", "uv": [0, 0, 16, 16]} for face in
             ("up", "down", "north", "south", "east", "west")}
    for face, tex in (overrides or {}).items():
        faces[face]["texture"] = f"#{tex}"
    result = {"from": start, "to": end, "faces": faces}
    if rotation:
        result["rotation"] = rotation
    return result


def models():
    save_json(ASSETS / "models/block/trash_bin_base.json", {
        "ambientocclusion": True,
        "textures": {key: f"trashbin:block/{key}" for key in ("body", "front", "lid", "rim", "handle")},
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375]*3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4]*3},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4]*3}
        }
    })
    variants = {}
    for active in (True, False):
        for opened in (True, False):
            name = f"trash_bin_{'active' if active else 'inactive'}_{'open' if opened else 'closed'}"
            tilt = {"origin": [8, 14, 15], "axis": "x", "angle": 22.5, "rescale": False} if opened else None
            parts = [
                element([2, 0, 2], [14, 1, 14], "rim"),
                element([1.5, 1, 1.5], [14.5, 13, 14.5], "body", {"north": "front"}),
                element([1, 13, 1], [15, 14, 15], "rim"),
                element([1, 14, 1], [15, 15, 15], "lid", rotation=tilt),
                element([6, 15, 7], [10, 16, 9], "handle", rotation=tilt),
                element([4, 13.5, 14.5], [12, 15.5, 15], "handle"),
                element([10, 11.25, 1.25], [12, 12.25, 1.5], "indicator"),
            ]
            save_json(ASSETS / f"models/block/{name}.json", {
                "parent": "trashbin:block/trash_bin_base",
                "textures": {"particle": "trashbin:block/body", "indicator": f"trashbin:block/{'active' if active else 'inactive'}"},
                "elements": parts
            })
            for facing, angle in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
                variants[f"active={str(active).lower()},facing={facing},open={str(opened).lower()}"] = {
                    "model": f"trashbin:block/{name}", "y": angle, "uvlock": False
                }
    save_json(ASSETS / "blockstates/trash_bin.json", {"variants": variants})
    save_json(ASSETS / "models/item/trash_bin.json", {"parent": "trashbin:block/trash_bin_active_closed"})


def data():
    save_json(RES / "data/minecraft/tags/blocks/mineable/pickaxe.json", {"replace": False, "values": ["trashbin:trash_bin"]})
    save_json(RES / "data/trashbin/loot_tables/blocks/trash_bin.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "trashbin:trash_bin"}]}]
    })
    save_json(RES / "data/trashbin/recipes/trash_bin.json", {
        "type": "minecraft:crafting_shaped", "pattern": [" I ", "IBI", " I "],
        "key": {"I": {"item": "minecraft:iron_ingot"}, "B": {"item": "minecraft:barrel"}},
        "result": {"item": "trashbin:trash_bin", "count": 1}
    })
    save_json(RES / "data/trashbin/advancements/recipes/trash_bin.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_barrel": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["minecraft:barrel"]}]}},
            "has_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": "trashbin:trash_bin"}}
        }, "requirements": [["has_barrel", "has_recipe"]], "rewards": {"recipes": ["trashbin:trash_bin"]}
    })
    en = {
        "block.trashbin.trash_bin": "Trash Bin", "itemGroup.trashbin": "Trash Bin",
        "gui.trashbin.clear": "Recycle", "gui.trashbin.extract": "Claim XP", "gui.trashbin.redstone": "Redstone",
        "gui.trashbin.experience": "XP %s / %s", "gui.trashbin.enabled": "Enabled", "gui.trashbin.disabled": "Disabled",
        "gui.trashbin.fluid.0": "None (install CEI or Sophisticated Core)",
        "gui.trashbin.fluid.1": "CEI liquid experience (1 mB / XP)",
        "gui.trashbin.fluid.2": "Sophisticated liquid experience (20 mB / XP)",
        "mode.trashbin.none": "None", "mode.trashbin.powered": "Enabled when powered", "mode.trashbin.unpowered": "Enabled when unpowered",
        "tooltip.trashbin.clear": "Permanently recycle all 27 slots into XP. Any excess XP appears at your feet. Partial conversion progress is kept.",
        "tooltip.trashbin.extract": "Release the cached whole XP points as orbs at your feet. Fractional XP remains stored.",
        "tooltip.trashbin.redstone": "Redstone behavior: %s. Click to cycle. This controls all automatic item and fluid input/output; player access remains available.",
        "tooltip.trashbin.status": "Automation: %s\nLiquid XP output: %s\nOccupied slots: %s / 27\nFractional cached XP: 0.%s\nAutomatic XP overflow is discarded."
    }
    zh = {
        "block.trashbin.trash_bin": "垃圾桶", "itemGroup.trashbin": "垃圾桶",
        "gui.trashbin.clear": "全部清空", "gui.trashbin.extract": "提取经验", "gui.trashbin.redstone": "红石信号行为",
        "gui.trashbin.experience": "经验 %s / %s", "gui.trashbin.enabled": "启用", "gui.trashbin.disabled": "停用",
        "gui.trashbin.fluid.0": "无（需安装 CEI 或精妙核心）",
        "gui.trashbin.fluid.1": "CEI 液态经验（1 mB / 经验点）",
        "gui.trashbin.fluid.2": "精妙核心液态经验（20 mB / 经验点）",
        "mode.trashbin.none": "无", "mode.trashbin.powered": "充能时启用", "mode.trashbin.unpowered": "未充能时启用",
        "tooltip.trashbin.clear": "永久销毁全部 27 格物品并转化为经验。溢出经验在脚下生成经验球，转换余数保留并跨次累计。",
        "tooltip.trashbin.extract": "在脚下释放缓存中的整点经验，小数经验和转换余数继续保留。",
        "tooltip.trashbin.redstone": "红石信号行为：%s。点击切换，控制全部自动化物品及液体输入输出，手动操作始终可用。",
        "tooltip.trashbin.status": "自动化：%s\n液态经验输出：%s\n占用槽位：%s / 27\n缓存中的小数经验：0.%s\n自动化产生的溢出经验直接丢弃。"
    }
    save_json(ASSETS / "lang/en_us.json", en)
    save_json(ASSETS / "lang/zh_cn.json", zh)


if __name__ == "__main__":
    textures(); models(); data()
    print("Generated original Trash Bin textures, models, translations, recipe and loot table.")
