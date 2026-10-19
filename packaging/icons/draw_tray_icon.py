"""
Рисует значок для системного трея: python3 packaging/icons/draw_tray_icon.py
Упрощённая иконка приложения — блокнот с кольцами и звездой на бирюзовом квадрате, без мелких деталей,
чтобы читалась в 16–24 px. Пишет в ресурсы desktop-приложения:
  tray-alpha-<N>.png — с прозрачностью, для StatusNotifierItem (панель рисует его сама);
  tray-<N>.png — без прозрачности, для запасного AWT-трея (там прозрачные пиксели становятся чёрными).
"""
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE.parent.parent / "composeApp/src/desktopMain/resources/tray"
S = 1024
TEAL = '#0097A7'


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def star(cx, cy, big, small):
    return [(cx + (big if i % 2 == 0 else small) * math.cos(-math.pi / 2 + i * math.pi / 5),
             cy + (big if i % 2 == 0 else small) * math.sin(-math.pi / 2 + i * math.pi / 5)) for i in range(10)]


def draw(opaque):
    img = Image.new('RGBA', (S, S), hexc(TEAL) + (255,) if opaque else (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = S * .03
    d.rounded_rectangle([m, m, S - m, S - m], int(S * .22), fill=hexc(TEAL) + (255,))
    px0, py0, px1, py1 = S * .24, S * .22, S * .76, S * .82
    d.rounded_rectangle([px0, py0, px1, py1], int(S * .08), fill=(255, 255, 255, 255))
    for cx in (S * .40, S * .60):
        d.rounded_rectangle([cx - S * .045, py0 - S * .09, cx + S * .045, py0 + S * .10], int(S * .045),
                            fill=hexc('#00363D') + (255,))
    d.polygon(star(S * .5, S * .56, S * .17, S * .075), fill=hexc('#FFB300') + (255,))
    return img


OUT.mkdir(parents=True, exist_ok=True)
alpha, opaque = draw(False), draw(True).convert('RGB')
for n in (16, 22, 24, 32, 48, 64, 128):
    alpha.resize((n, n), Image.LANCZOS).save(OUT / f"tray-alpha-{n}.png")
for n in (16, 22, 24, 32, 48, 64):
    opaque.resize((n, n), Image.LANCZOS).save(OUT / f"tray-{n}.png")
