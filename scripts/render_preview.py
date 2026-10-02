"""Offline asset/layout proof using Pillow. This is not a Minecraft screenshot.

Usage: python scripts/render_preview.py <path-to-Minecraft-client.jar>
The original JSON models and PNG textures are rendered with nearest-neighbor sampling.
Vanilla GUI pixels are read from a locally downloaded Minecraft jar, not redistributed.
"""
from pathlib import Path
from io import BytesIO
import json
import math
import sys
import zipfile
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "common/src/main/resources/assets/trashbin"


def render_model(model_name, size=280):
    model = json.loads((ASSETS / f"models/block/{model_name}.json").read_text())
    parent = json.loads((ASSETS / "models/block/trash_bin_base.json").read_text())
    textures = parent["textures"] | model["textures"]
    images = {key: Image.open(ASSETS / f"textures/{path.split(':')[1]}.png").convert("RGBA")
              for key, path in textures.items()}
    result = Image.new("RGBA", (size, size))
    pixels = result.load()
    depth = [[-1e9] * size for _ in range(size)]
    scale = size / 34

    def transform(point, rotation):
        x, y, z = point
        if rotation:
            ox, oy, oz = rotation["origin"]
            angle = math.radians(rotation["angle"])
            y, z = oy + (y - oy) * math.cos(angle) - (z - oz) * math.sin(angle), oz + (y - oy) * math.sin(angle) + (z - oz) * math.cos(angle)
        x, y, z = x - 8, y - 8, z - 8
        return (size / 2 + (x + z) * .7071 * scale,
                size * .61 + ((x - z) * .3536 - y * .866) * scale,
                (x - z) * .6124 + y * .5)

    def triangle(vertices, uv, texture, shade):
        (ax, ay, az), (bx, by, bz), (cx, cy, cz) = vertices
        denom = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(denom) < 1e-9:
            return
        for y in range(max(0, int(min(ay, by, cy))), min(size, int(max(ay, by, cy)) + 1)):
            for x in range(max(0, int(min(ax, bx, cx))), min(size, int(max(ax, bx, cx)) + 1)):
                a = ((by - cy) * (x + .5 - cx) + (cx - bx) * (y + .5 - cy)) / denom
                b = ((cy - ay) * (x + .5 - cx) + (ax - cx) * (y + .5 - cy)) / denom
                c = 1 - a - b
                if min(a, b, c) < -1e-6:
                    continue
                d = a * az + b * bz + c * cz
                if d <= depth[y][x]:
                    continue
                u = max(0, min(15, int(a * uv[0][0] + b * uv[1][0] + c * uv[2][0])))
                v = max(0, min(15, int(a * uv[0][1] + b * uv[1][1] + c * uv[2][1])))
                color = texture.getpixel((u, v))
                pixels[x, y] = tuple(int(channel * shade) for channel in color[:3]) + (color[3],)
                depth[y][x] = d

    for part in model["elements"]:
        x0, y0, z0 = part["from"]; x1, y1, z1 = part["to"]
        faces = {
            "north": [(x0,y1,z0),(x1,y1,z0),(x1,y0,z0),(x0,y0,z0)],
            "south": [(x1,y1,z1),(x0,y1,z1),(x0,y0,z1),(x1,y0,z1)],
            "east": [(x1,y1,z0),(x1,y1,z1),(x1,y0,z1),(x1,y0,z0)],
            "west": [(x0,y1,z1),(x0,y1,z0),(x0,y0,z0),(x0,y0,z1)],
            "up": [(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],
            "down": [(x0,y0,z1),(x1,y0,z1),(x1,y0,z0),(x0,y0,z0)]}
        for face, points in faces.items():
            tex = images[part["faces"][face]["texture"][1:]]
            points = [transform(p, part.get("rotation")) for p in points]
            shade = {"up": 1, "north": .84, "east": .67, "south": .7, "west": .7, "down": .5}[face]
            uv = [(0,0),(16,0),(16,16),(0,16)]
            for ids in [(0,1,2),(0,2,3)]:
                triangle([points[i] for i in ids], [uv[i] for i in ids], tex, shade)
    return result


def font(size):
    for path in [Path("C:/Windows/Fonts/simhei.ttf"), Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")]:
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def gui(client_jar):
    with zipfile.ZipFile(client_jar) as jar:
        chest = Image.open(BytesIO(jar.read("assets/minecraft/textures/gui/container/generic_54.png"))).convert("RGBA")
    panel = Image.new("RGBA", (176, 222))
    panel.paste(chest.crop((0,0,176,71)), (0,0))
    for i in range(5):
        panel.paste(chest.crop((0,4,176,15)), (0,71 + i*11))
    panel.paste(chest.crop((0,126,176,222)), (0,126))
    bar = Image.open(ASSETS / "textures/gui/experience_bar.png")
    panel.paste(bar.crop((0,0,160,8)), (8,92))
    panel.paste(bar.crop((0,8,81,14)), (9,93))
    draw = ImageDraw.Draw(panel)
    draw.rectangle((162,80,166,84), fill="#6d936c")
    for x, width in [(8,48),(58,48),(108,62)]:
        draw.rectangle((x,106,x+width-1,125), fill="#737373", outline="#202020")
        draw.line((x+1,107,x+width-2,107), fill="#c6c6c6")
        draw.line((x+1,108,x+1,124), fill="#b0b0b0")
        draw.line((x+2,124,x+width-2,124), fill="#383838")
    result = panel.resize((528, 666), Image.Resampling.NEAREST)
    draw = ImageDraw.Draw(result)
    draw.text((24,15), "垃圾桶", font=font(24), fill="#404040")
    draw.text((24,236), "经验 512000 / 999999", font=font(24), fill="#404040")
    draw.text((24,383), "物品栏", font=font(24), fill="#404040")
    for x, width, label in [(8,48,"全部清空"),(58,48,"提取经验"),(108,62,"红石信号行为")]:
        f = font(22); length = draw.textlength(label, font=f)
        draw.text((x*3+(width*3-length)/2, 329), label, font=f, fill="#ffffff")
    return result


def main():
    page = Image.new("RGB", (1280, 850), "#edece5")
    draw = ImageDraw.Draw(page)
    draw.text((44,28), "TRASH BIN  /  垃圾桶", font=font(34), fill="#303c32")
    draw.text((44,76), "模型与界面离线预览 · 原生像素材质 · 非游戏截图", font=font(20), fill="#73796b")
    for model, label, x, y in [("trash_bin_active_closed","启用 / 盖板关闭",42,170),
                                ("trash_bin_inactive_closed","停用 / 暖红指示灯",338,170),
                                ("trash_bin_active_open","启用 / 盖板打开",190,476)]:
        image = render_model(model)
        page.paste(image, (x,y), image)
        draw.text((x+32,y+276), label, font=font(21), fill="#455340")
    interface = gui(Path(sys.argv[1]))
    page.paste(interface, (708,138), interface)
    draw.text((716,814), "27 个槽位 · 原版资源包槽位和按钮 · 经验缓存条", font=font(18), fill="#68745e")
    target = ROOT / "docs/preview.png"; target.parent.mkdir(exist_ok=True)
    page.save(target)
    print(target)


if __name__ == "__main__":
    main()
