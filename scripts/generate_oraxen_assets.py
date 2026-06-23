import hashlib
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ORAXEN_ROOT = ROOT / "src/main/resources/oraxen"
TEXTURE_ROOT = ORAXEN_ROOT / "pack/textures/ysbp"
ITEM_ROOT = ORAXEN_ROOT / "items"
MANIFEST = ORAXEN_ROOT / "asset-manifest.properties"
SIZE = 32
LOGICAL_SIZE = 16
SCALE = SIZE // LOGICAL_SIZE
TRANSPARENT = (0, 0, 0, 0)


def rgb(hex_color):
    hex_color = hex_color.lstrip("#")
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))


def rgba(hex_color, alpha=255):
    return (*rgb(hex_color), alpha)


def mix(a, b, t):
    ar, ag, ab = rgb(a) if isinstance(a, str) else a[:3]
    br, bg, bb = rgb(b) if isinstance(b, str) else b[:3]
    return (
        int(ar + (br - ar) * t),
        int(ag + (bg - ag) * t),
        int(ab + (bb - ab) * t),
        255,
    )


def scale_point(x, y):
    return (x * SCALE + SCALE // 2, y * SCALE + SCALE // 2)


def rect(draw, xy, color):
    x1, y1, x2, y2 = xy
    draw.rectangle((x1 * SCALE, y1 * SCALE,
                    (x2 + 1) * SCALE - 1, (y2 + 1) * SCALE - 1), fill=color)


def line(draw, pts, color, width=1):
    draw.line([scale_point(x, y) for x, y in pts], fill=color,
              width=max(1, width * SCALE))


def poly(draw, pts, color):
    draw.polygon([scale_point(x, y) for x, y in pts], fill=color)


def dot(draw, x, y, color):
    draw.rectangle((x * SCALE, y * SCALE,
                    (x + 1) * SCALE - 1, (y + 1) * SCALE - 1), fill=color)


def px_rect(draw, xy, color):
    draw.rectangle(xy, fill=color)


def px_line(draw, pts, color, width=1):
    draw.line(pts, fill=color, width=width)


def px_poly(draw, pts, color):
    draw.polygon(pts, fill=color)


def px_dot(draw, x, y, color):
    draw.point((x, y), fill=color)


def ensure_dirs():
    (TEXTURE_ROOT / "backpacks").mkdir(parents=True, exist_ok=True)
    (TEXTURE_ROOT / "backpacks/accents").mkdir(parents=True, exist_ok=True)
    (TEXTURE_ROOT / "upgrades").mkdir(parents=True, exist_ok=True)
    (TEXTURE_ROOT / "upgrades/functions").mkdir(parents=True, exist_ok=True)
    ITEM_ROOT.mkdir(parents=True, exist_ok=True)


DYE_VARIANTS = {
    "white": "#F9FFFE",
    "light_gray": "#9D9D97",
    "gray": "#474F52",
    "black": "#1D1D21",
    "brown": "#835432",
    "red": "#B02E26",
    "orange": "#F9801D",
    "yellow": "#FED83D",
    "lime": "#80C71F",
    "green": "#5E7C16",
    "cyan": "#169C9C",
    "light_blue": "#3AB3DA",
    "blue": "#3C44AA",
    "purple": "#8932B8",
    "magenta": "#C74EBD",
    "pink": "#F38BAA",
}


TIER_META = {
    "leather": ("<#A0703C>Leder-Rucksack", 1001, "#E08A2B"),
    "copper": ("<#C87B53>Kupfer-Rucksack", 1002, "#7A4A2B"),
    "iron": ("<#D8D8D8>Eisen-Rucksack", 1003, "#8E8E8E"),
    "gold": ("<#F2D14E>Gold-Rucksack", 1004, "#B8860B"),
    "diamond": ("<#5BE8D4>Diamant-Rucksack", 1005, "#3FA9F5"),
    "emerald": ("<#3BD16F>Smaragd-Rucksack", 1006, "#1E7A3C"),
    "netherite": ("<#6E5BC8>Netherite-Rucksack", 1007, "#1A1A1A"),
}


TIER_STYLES = {
    "leather": {
        "main": "#9f6b3a", "light": "#d49a5d", "dark": "#3a2114",
        "metal": "#e2a04a", "metal_dark": "#70401d", "gem": "#f0c76f",
    },
    "copper": {
        "main": "#c9784d", "light": "#f0a36f", "dark": "#4a2a1b",
        "metal": "#c9784d", "metal_dark": "#6c3a22", "gem": "#58c7a7",
    },
    "iron": {
        "main": "#cfd2d3", "light": "#ffffff", "dark": "#4d5158",
        "metal": "#92989d", "metal_dark": "#34383d", "gem": "#b7e0ff",
    },
    "gold": {
        "main": "#f1c842", "light": "#fff29b", "dark": "#745005",
        "metal": "#f1c842", "metal_dark": "#8a6008", "gem": "#ffe36c",
    },
    "diamond": {
        "main": "#50d9cd", "light": "#d7fff8", "dark": "#145f75",
        "metal": "#50d9cd", "metal_dark": "#15506c", "gem": "#85ffff",
    },
    "emerald": {
        "main": "#38c86a", "light": "#baffce", "dark": "#164825",
        "metal": "#38c86a", "metal_dark": "#0e2e17", "gem": "#70f59a",
    },
    "netherite": {
        "main": "#60526f", "light": "#a493b8", "dark": "#151116",
        "metal": "#60526f", "metal_dark": "#17121d", "gem": "#c670e8",
    },
}


def new_image():
    return Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)


def save(img, path):
    img.save(path, optimize=True)


def nearest_dye(hex_color):
    rr, gg, bb = rgb(hex_color)
    best_name = "white"
    best_score = 10**9
    for name, dye_hex in DYE_VARIANTS.items():
        dr, dg, db = rgb(dye_hex)
        score = (rr - dr) ** 2 + (gg - dg) ** 2 + (bb - db) ** 2
        if score < best_score:
            best_name = name
            best_score = score
    return best_name


def tint_color(hex_color, amount):
    return mix(hex_color, "#ffffff" if amount > 0 else "#000000", abs(amount))


def draw_backpack_base(path):
    img = new_image()
    d = ImageDraw.Draw(img)

    # Layer 0 is intentionally grayscale: vanilla leather tint multiplies it.
    # Native 32x32 keeps vanilla chunky pixels while giving room for backpack details.
    rows = {
        7: (11, 20, "#fafafa"),
        8: (9, 22, "#f4f4f4"),
        9: (8, 23, "#eeeeee"),
        10: (7, 24, "#e9e9e9"),
        11: (6, 25, "#e3e3e3"),
        12: (5, 26, "#dddddd"),
        13: (5, 26, "#d8d8d8"),
        14: (4, 27, "#d2d2d2"),
        15: (4, 27, "#cdcdcd"),
        16: (4, 27, "#c9c9c9"),
        17: (4, 27, "#c5c5c5"),
        18: (4, 27, "#c1c1c1"),
        19: (4, 27, "#bdbdbd"),
        20: (4, 27, "#b9b9b9"),
        21: (5, 26, "#b5b5b5"),
        22: (5, 26, "#b1b1b1"),
        23: (5, 26, "#adadad"),
        24: (6, 25, "#afafaf"),
        25: (7, 24, "#a9a9a9"),
        26: (8, 23, "#a1a1a1"),
        27: (9, 22, "#989898"),
        28: (11, 20, "#8e8e8e"),
    }
    for y, (x1, x2, color) in rows.items():
        px_rect(d, (x1, y, x2, y), rgba(color))

    px_rect(d, (2, 15, 6, 25), rgba("#b8b8b8"))
    px_rect(d, (3, 16, 5, 24), rgba("#c7c7c7"))
    px_rect(d, (25, 15, 29, 25), rgba("#aaaaaa"))
    px_rect(d, (26, 16, 28, 24), rgba("#bdbdbd"))
    px_rect(d, (7, 9, 24, 14), rgba("#ededed"))
    px_rect(d, (8, 10, 23, 12), rgba("#f8f8f8"))
    px_rect(d, (10, 19, 21, 27), rgba("#d6d6d6"))
    px_rect(d, (11, 20, 20, 26), rgba("#c6c6c6"))
    px_rect(d, (12, 21, 19, 24), rgba("#dddddd"))

    for xy in ((9, 10, 13, 10), (8, 13, 11, 13), (13, 20, 17, 20),
               (12, 23, 14, 23), (5, 16, 5, 18), (26, 17, 26, 19)):
        px_rect(d, xy, rgba("#ffffff", 210))
    for x, y in ((22, 14), (24, 19), (21, 26), (7, 25), (28, 23)):
        px_dot(d, x, y, rgba("#909090"))
    save(img, path)


def draw_backpack_accent(path, accent_hex):
    img = new_image()
    d = ImageDraw.Draw(img)
    main = tint_color(accent_hex, 0.08)
    light = tint_color(accent_hex, 0.34)
    dark = tint_color(accent_hex, -0.24)
    shade = tint_color(accent_hex, -0.34)

    # Accent dye controls the flap, straps and front pocket on purpose.
    px_line(d, [(12, 4), (19, 4)], main, 2)
    px_line(d, [(10, 6), (10, 9)], dark, 2)
    px_line(d, [(21, 6), (21, 9)], dark, 2)
    px_rect(d, (7, 9, 24, 13), dark)
    px_rect(d, (8, 9, 23, 11), main)
    px_rect(d, (9, 10, 17, 10), light)
    px_line(d, [(7, 14), (24, 14)], shade, 2)

    px_line(d, [(7, 14), (7, 27)], dark, 2)
    px_line(d, [(24, 14), (24, 27)], dark, 2)
    px_rect(d, (8, 15, 8, 23), main)
    px_rect(d, (23, 15, 23, 23), main)

    px_rect(d, (10, 19, 21, 27), shade)
    px_rect(d, (11, 19, 20, 25), dark)
    px_rect(d, (12, 20, 19, 24), main)
    px_rect(d, (13, 20, 18, 20), light)

    px_rect(d, (2, 17, 5, 25), dark)
    px_rect(d, (3, 17, 5, 23), main)
    px_rect(d, (26, 17, 29, 25), dark)
    px_rect(d, (26, 17, 28, 23), main)
    px_rect(d, (3, 18, 4, 19), light)
    px_rect(d, (27, 18, 28, 19), light)
    px_rect(d, (9, 28, 11, 29), dark)
    px_rect(d, (20, 28, 22, 29), dark)
    save(img, path)


def draw_backpack_overlay(path, style):
    img = new_image()
    d = ImageDraw.Draw(img)
    edge = rgba("#3a2c23")
    dark = mix(style["dark"], "#ffffff", 0.30)
    metal = rgba(style["metal"])
    metal_dark = mix(style["metal_dark"], "#ffffff", 0.22)
    light = rgba(style["light"])

    # Recognizable backpack silhouette: handle, boxy body, pouches and straps.
    px_line(d, [(10, 9), (10, 3), (12, 1), (19, 1), (21, 3), (21, 9)], edge, 2)
    px_line(d, [(12, 2), (19, 2)], light, 1)
    px_line(d, [(7, 8), (24, 8)], edge, 2)
    px_line(d, [(7, 8), (5, 10), (4, 14)], edge, 2)
    px_line(d, [(24, 8), (26, 10), (27, 14)], edge, 2)
    px_line(d, [(4, 14), (3, 15), (3, 26), (5, 28), (10, 29)], edge, 2)
    px_line(d, [(27, 14), (28, 15), (28, 26), (26, 28), (21, 29)], edge, 2)
    px_line(d, [(10, 29), (21, 29)], edge, 2)
    px_line(d, [(2, 15), (1, 17), (1, 25), (3, 27), (5, 27)], edge, 2)
    px_line(d, [(29, 15), (30, 17), (30, 25), (28, 27), (26, 27)], edge, 2)
    px_line(d, [(2, 15), (5, 15)], edge, 1)
    px_line(d, [(26, 15), (29, 15)], edge, 1)

    # Fixed tier material details; accent colors live on their own layer.
    px_line(d, [(7, 15), (24, 15)], dark, 2)
    px_line(d, [(10, 18), (21, 18)], dark, 1)
    px_line(d, [(10, 26), (21, 26)], dark, 1)
    px_line(d, [(10, 19), (10, 27)], dark, 1)
    px_line(d, [(21, 19), (21, 27)], dark, 1)
    px_rect(d, (14, 21, 17, 24), metal_dark)
    px_rect(d, (15, 22, 16, 23), edge)
    px_rect(d, (14, 21, 15, 22), metal)
    px_dot(d, 17, 21, light)
    px_rect(d, (14, 14, 17, 16), metal_dark)
    px_rect(d, (15, 14, 16, 15), metal)

    for xy in ((6, 13), (23, 13), (4, 25), (27, 25), (8, 28), (23, 28),
               (6, 17), (25, 17)):
        px_rect(d, (xy[0], xy[1], xy[0] + 1, xy[1] + 1), metal)
    px_line(d, [(12, 11), (18, 11)], light, 1)
    px_line(d, [(5, 21), (5, 25)], dark, 1)
    px_line(d, [(26, 21), (26, 25)], dark, 1)

    save(img, path)


def draw_tier_upgrade(path, key, style):
    img = new_image()
    d = ImageDraw.Draw(img)

    if key == "base":
        edge = rgba("#2a170e")
        rect(d, (4, 2, 11, 13), edge)
        rect(d, (3, 5, 12, 10), edge)
        rect(d, (4, 3, 11, 12), rgba("#9f6b3a"))
        rect(d, (5, 3, 10, 5), rgba("#d49a5d"))
        rect(d, (5, 10, 11, 12), rgba("#5a321c"))
        line(d, [(5, 4), (10, 12)], rgba("#ead6b5"))
        line(d, [(10, 4), (5, 12)], rgba("#ead6b5"))
        rect(d, (7, 7, 8, 8), rgba("#f0c76f"))
        save(img, path)
        return

    edge = rgba("#47321f")
    paper = rgba("#d5c690")
    paper_light = rgba("#fff0b0")
    paper_shadow = rgba("#9c7d50")
    main = rgba(style["main"])
    dark = rgba(style["dark"])
    light = rgba(style["light"])
    gem = rgba(style["gem"])

    rect(d, (4, 1, 11, 14), edge)
    rect(d, (3, 3, 12, 12), edge)
    rect(d, (4, 2, 11, 13), paper)
    rect(d, (5, 2, 10, 4), paper_light)
    rect(d, (5, 12, 11, 13), paper_shadow)
    poly(d, [(10, 2), (12, 2), (12, 4), (10, 4)], paper_light)

    poly(d, [(8, 4), (11, 7), (9, 11), (6, 11), (4, 7)], dark)
    poly(d, [(8, 5), (10, 7), (8, 10), (6, 7)], main)
    line(d, [(8, 5), (8, 10)], gem)
    dot(d, 7, 6, light)
    line(d, [(4, 12), (8, 12), (8, 11), (11, 13)], edge)
    save(img, path)


def draw_module_frame(color, advanced=False):
    img = new_image()
    d = ImageDraw.Draw(img)
    main = rgba(color)
    light = mix(color, "#ffffff", 0.45)
    dark = mix(color, "#000000", 0.50)
    edge = rgba("#202124")

    poly(d, [(6, 1), (10, 1), (13, 4), (13, 10), (10, 13), (5, 14), (2, 11), (2, 5)], edge)
    poly(d, [(6, 2), (10, 2), (12, 5), (12, 10), (9, 12), (5, 13), (3, 10), (3, 5)], dark)
    poly(d, [(6, 3), (10, 3), (11, 5), (11, 10), (9, 11), (6, 12), (4, 10), (4, 5)], main)
    line(d, [(6, 3), (10, 3), (11, 5)], light)
    line(d, [(6, 12), (9, 11), (11, 10)], dark)
    dot(d, 4, 6, light)
    dot(d, 12, 9, edge)

    if advanced:
        rect(d, (11, 1, 14, 4), rgba("#8a630c"))
        rect(d, (12, 2, 13, 3), rgba("#fff09b"))
    return img


def plus(d, x, y, color):
    dot(d, x, y - 1, color)
    dot(d, x - 1, y, color)
    dot(d, x, y, color)
    dot(d, x + 1, y, color)
    dot(d, x, y + 1, color)


def bolt(d, color):
    poly(d, [(11, 4), (9, 8), (11, 8), (9, 12), (13, 7), (11, 7)], color)


def icon_bag(d):
    rect(d, (5, 8, 10, 12), rgba("#70401d"))
    rect(d, (6, 7, 9, 8), rgba("#d49a5d"))
    line(d, [(6, 7), (6, 6), (9, 6), (9, 7)], rgba("#2a170e"))
    dot(d, 8, 10, rgba("#f0c76f"))


def icon_arrow_to_bag(d, color):
    icon_bag(d)
    line(d, [(4, 5), (10, 5)], rgba(color))
    line(d, [(10, 5), (8, 3)], rgba(color))
    line(d, [(10, 5), (8, 7)], rgba(color))


def icon_magnet(d):
    line(d, [(5, 4), (5, 10), (7, 12), (10, 10), (10, 4)], rgba("#2e333a"), 2)
    line(d, [(5, 4), (5, 7)], rgba("#d84538"), 2)
    line(d, [(10, 4), (10, 7)], rgba("#347bdc"), 2)
    dot(d, 7, 5, rgba("#ffe36c"))
    dot(d, 3, 10, rgba("#ffe36c"))
    dot(d, 12, 10, rgba("#ffe36c"))


def icon_filter(d):
    poly(d, [(4, 4), (12, 4), (9, 8), (9, 11), (7, 12), (7, 8)], rgba("#f0d9a7"))
    line(d, [(4, 4), (12, 4), (9, 8), (9, 11), (7, 12), (7, 8), (4, 4)], rgba("#4a321d"))
    line(d, [(6, 5), (10, 5)], rgba("#ffffff"))


def icon_void(d):
    rect(d, (5, 4, 10, 11), rgba("#0b0910"))
    rect(d, (6, 5, 9, 10), rgba("#3a1d5c"))
    rect(d, (7, 6, 8, 9), rgba("#050407"))
    line(d, [(5, 8), (7, 6), (10, 7)], rgba("#a06cff"))
    line(d, [(6, 11), (9, 10)], rgba("#c670e8"))


def icon_transfer(d, mode):
    rect(d, (3, 6, 6, 10), rgba("#7c4a2a"))
    rect(d, (10, 5, 13, 9), rgba("#9f6b3a"))
    dot(d, 4, 7, rgba("#d49a5d"))
    dot(d, 11, 6, rgba("#d49a5d"))
    colors = {
        "refill": "#85ffff",
        "restock": "#70f59a",
        "deposit": "#ffe36c",
    }
    if mode == "refill":
        line(d, [(11, 12), (6, 12), (6, 9), (4, 9)], rgba(colors[mode]))
        line(d, [(4, 9), (6, 7)], rgba(colors[mode]))
    else:
        line(d, [(4, 3), (11, 3), (11, 6)], rgba(colors[mode]))
        line(d, [(11, 6), (13, 4)], rgba(colors[mode]))
        if mode == "deposit":
            line(d, [(4, 12), (11, 12), (11, 9)], rgba(colors[mode]))


def icon_crafting(d):
    for y in range(3):
        for x in range(3):
            rect(d, (4 + x * 3, 4 + y * 3, 5 + x * 3, 5 + y * 3), rgba("#d5c690"))
    line(d, [(11, 7), (13, 8), (11, 9)], rgba("#ffffff"))


def icon_compacting(d, advanced=False):
    coords = [(5, 4), (8, 4), (5, 7), (8, 7)]
    if advanced:
        coords += [(11, 4), (11, 7), (8, 10)]
    for x, y in coords:
        rect(d, (x, y, x + 1, y + 1), rgba("#d0d3d4"))
        dot(d, x + 1, y + 1, rgba("#6d7378"))
    line(d, [(4, 12), (8, 12), (8, 11), (11, 13)], rgba("#ffe36c"))


def icon_furnace(d, kind, auto=False):
    rect(d, (4, 3, 11, 12), rgba("#33363b"))
    rect(d, (5, 4, 10, 6), rgba("#777d82"))
    rect(d, (5, 8, 10, 11), rgba("#141414"))
    if kind == "smoking":
        line(d, [(6, 4), (5, 2), (7, 1)], rgba("#d8d8d8"))
        line(d, [(9, 4), (8, 2), (10, 1)], rgba("#bdbdbd"))
    elif kind == "blasting":
        dot(d, 3, 5, rgba("#d8d8d8"))
        dot(d, 12, 5, rgba("#d8d8d8"))
    poly(d, [(8, 8), (6, 11), (10, 11)], rgba("#e35b2b"))
    poly(d, [(8, 7), (7, 11), (9, 11)], rgba("#ffd36f"))
    if auto:
        bolt(d, rgba("#fff09b"))


def icon_stonecutter(d):
    rect(d, (5, 4, 11, 10), rgba("#cfd2d3"))
    rect(d, (7, 6, 9, 8), rgba("#777d82"))
    line(d, [(8, 3), (8, 11)], rgba("#ffffff"))
    line(d, [(4, 7), (12, 7)], rgba("#ffffff"))
    rect(d, (5, 11, 11, 12), rgba("#6d5944"))


def icon_anvil(d):
    rect(d, (4, 8, 12, 10), rgba("#303238"))
    rect(d, (6, 6, 10, 8), rgba("#565b60"))
    rect(d, (3, 5, 8, 6), rgba("#777d82"))
    rect(d, (7, 10, 8, 12), rgba("#25272a"))
    rect(d, (5, 13, 11, 13), rgba("#25272a"))


def icon_smithing(d):
    icon_anvil(d)
    line(d, [(10, 3), (13, 6)], rgba("#5a321c"))
    rect(d, (8, 2, 11, 3), rgba("#d0d3d4"))


def icon_alchemy(d):
    rect(d, (7, 3, 8, 6), rgba("#d0d3d4"))
    poly(d, [(6, 7), (9, 7), (11, 12), (4, 12)], rgba("#50d9cd"))
    line(d, [(6, 7), (9, 7), (11, 12), (4, 12), (6, 7)], rgba("#123136"))
    line(d, [(5, 10), (10, 10)], rgba("#85ffff"))
    dot(d, 11, 5, rgba("#c670e8"))
    dot(d, 5, 4, rgba("#c670e8"))


def icon_stack(d, variant):
    levels = {
        "stack_starter": 2, "stack_tier_1": 3, "stack_tier_2": 4,
        "stack_tier_3": 5, "stack_tier_4": 6, "stack_omega": 6,
        "stack_downgrade_1": 4, "stack_downgrade_2": 3, "stack_downgrade_3": 2,
    }.get(variant, 3)
    for i in range(levels):
        x = 4 + min(i, 4)
        y = 11 - i
        rect(d, (x, y, x + 5, y + 1), rgba("#5b4430"))
        rect(d, (x, y, x + 4, y), rgba("#d5c690"))
    if "downgrade" in variant:
        line(d, [(12, 4), (8, 8), (4, 4)], rgba("#d84538"))
    elif variant == "stack_omega":
        line(d, [(4, 5), (6, 3), (8, 5), (10, 3), (12, 5)], rgba("#c670e8"))
    else:
        line(d, [(4, 5), (8, 3), (12, 5)], rgba("#70f59a"))


def icon_food(d):
    rect(d, (5, 6, 11, 10), rgba("#f1c842"))
    rect(d, (5, 10, 10, 12), rgba("#70401d"))
    dot(d, 6, 5, rgba("#fff29b"))
    dot(d, 10, 6, rgba("#fff29b"))


def icon_tools(d):
    line(d, [(4, 12), (11, 5)], rgba("#5a321c"))
    rect(d, (10, 3, 13, 4), rgba("#cfd2d3"))
    line(d, [(4, 4), (12, 12)], rgba("#6b4a2b"))
    rect(d, (3, 3, 6, 4), rgba("#50d9cd"))


def icon_jukebox(d):
    rect(d, (4, 7, 12, 12), rgba("#7c4a2a"))
    rect(d, (5, 8, 11, 11), rgba("#b56c32"))
    rect(d, (6, 3, 10, 7), rgba("#151515"))
    dot(d, 8, 5, rgba("#60526f"))
    rect(d, (7, 10, 9, 10), rgba("#ffe36c"))


def icon_inception(d):
    rect(d, (4, 6, 12, 12), rgba("#70401d"))
    rect(d, (5, 5, 11, 6), rgba("#d49a5d"))
    rect(d, (7, 8, 9, 11), rgba("#9f6b3a"))
    dot(d, 8, 8, rgba("#f0c76f"))


def icon_shield(d, infinity=False):
    poly(d, [(8, 3), (12, 5), (11, 10), (8, 13), (5, 10), (4, 5)], rgba("#d0d3d4"))
    poly(d, [(8, 5), (10, 6), (10, 9), (8, 11), (6, 9), (6, 6)], rgba("#60526f"))
    if infinity:
        line(d, [(5, 8), (7, 7), (9, 9), (11, 8), (9, 7), (7, 9), (5, 8)], rgba("#fff09b"))
    else:
        plus(d, 8, 8, rgba("#fff09b"))


def icon_tank(d):
    rect(d, (6, 3, 10, 12), rgba("#565b60"))
    rect(d, (7, 4, 9, 11), rgba("#2d84c8"))
    rect(d, (7, 4, 9, 6), rgba("#85ffff"))
    line(d, [(5, 6), (3, 6), (3, 10), (5, 10)], rgba("#d0d3d4"))
    line(d, [(11, 6), (13, 6), (13, 10), (11, 10)], rgba("#d0d3d4"))


def icon_pump(d):
    icon_tank(d)
    line(d, [(4, 12), (8, 12), (8, 14), (12, 14)], rgba("#92989d"))
    dot(d, 13, 14, rgba("#85ffff"))


def icon_xp(d):
    rect(d, (4, 5, 6, 7), rgba("#70f59a"))
    rect(d, (9, 4, 11, 6), rgba("#70f59a"))
    rect(d, (7, 9, 10, 12), rgba("#70f59a"))
    dot(d, 5, 6, rgba("#baffce"))
    dot(d, 10, 5, rgba("#baffce"))
    line(d, [(4, 13), (12, 13)], rgba("#38c86a"))


def icon_battery(d):
    rect(d, (4, 6, 11, 10), rgba("#25272a"))
    rect(d, (12, 7, 13, 9), rgba("#25272a"))
    rect(d, (5, 7, 10, 9), rgba("#38c86a"))
    rect(d, (5, 7, 7, 9), rgba("#baffce"))
    bolt(d, rgba("#fff09b"))


def draw_function_upgrade(path, slug, color, icon, advanced=False):
    img = draw_module_frame(color, advanced)
    d = ImageDraw.Draw(img)

    if icon == "pickup":
        icon_arrow_to_bag(d, "#70f59a")
    elif icon == "magnet":
        icon_magnet(d)
    elif icon == "filter":
        icon_filter(d)
    elif icon == "void":
        icon_void(d)
    elif icon in ("refill", "restock", "deposit"):
        icon_transfer(d, icon)
    elif icon == "crafting":
        icon_crafting(d)
    elif icon == "compacting":
        icon_compacting(d, advanced)
    elif icon == "smelting":
        icon_furnace(d, "smelting", slug.startswith("auto_"))
    elif icon == "smoking":
        icon_furnace(d, "smoking", slug.startswith("auto_"))
    elif icon == "blasting":
        icon_furnace(d, "blasting", slug.startswith("auto_"))
    elif icon == "stonecutter":
        icon_stonecutter(d)
    elif icon == "anvil":
        icon_anvil(d)
    elif icon == "smithing":
        icon_smithing(d)
    elif icon == "alchemy":
        icon_alchemy(d)
    elif icon == "stack":
        icon_stack(d, slug)
    elif icon == "feeding":
        icon_food(d)
    elif icon == "tools":
        icon_tools(d)
    elif icon == "jukebox":
        icon_jukebox(d)
    elif icon == "inception":
        icon_inception(d)
    elif icon == "everlasting":
        icon_shield(d)
    elif icon == "infinity":
        icon_shield(d, True)
    elif icon == "tank":
        icon_tank(d)
    elif icon == "pump":
        icon_pump(d)
    elif icon == "xp":
        icon_xp(d)
    elif icon == "battery":
        icon_battery(d)

    if advanced and not slug.startswith("advanced_compacting"):
        dot(d, 4, 12, rgba("#fff09b"))
        dot(d, 12, 12, rgba("#fff09b"))
    if slug.startswith("auto_"):
        plus(d, 4, 4, rgba("#fff09b"))

    save(img, path)


FUNCTION_UPGRADES = [
    ("pickup", "Pickup Upgrade", "#58c96a", "pickup", False),
    ("advanced_pickup", "Advanced Pickup Upgrade", "#58c96a", "pickup", True),
    ("magnet", "Magnet Upgrade", "#5aa7ff", "magnet", False),
    ("advanced_magnet", "Advanced Magnet Upgrade", "#5aa7ff", "magnet", True),
    ("filter", "Filter Upgrade", "#d6aa4c", "filter", False),
    ("advanced_filter", "Advanced Filter Upgrade", "#d6aa4c", "filter", True),
    ("void", "Void Upgrade", "#6e5bc8", "void", False),
    ("advanced_void", "Advanced Void Upgrade", "#6e5bc8", "void", True),
    ("refill", "Refill Upgrade", "#4bbfd0", "refill", False),
    ("advanced_refill", "Advanced Refill Upgrade", "#4bbfd0", "refill", True),
    ("restock", "Restock Upgrade", "#53bd74", "restock", False),
    ("advanced_restock", "Advanced Restock Upgrade", "#53bd74", "restock", True),
    ("deposit", "Deposit Upgrade", "#f0b848", "deposit", False),
    ("advanced_deposit", "Advanced Deposit Upgrade", "#f0b848", "deposit", True),
    ("crafting", "Crafting Upgrade", "#9b7447", "crafting", False),
    ("compacting", "Compacting Upgrade", "#8f9ca8", "compacting", False),
    ("advanced_compacting", "Advanced Compacting Upgrade", "#8f9ca8", "compacting", True),
    ("smelting", "Smelting Upgrade", "#e46b37", "smelting", False),
    ("auto_smelting", "Auto-Smelting Upgrade", "#e46b37", "smelting", False),
    ("smoking", "Smoking Upgrade", "#9a7b5c", "smoking", False),
    ("auto_smoking", "Auto-Smoking Upgrade", "#9a7b5c", "smoking", False),
    ("blasting", "Blasting Upgrade", "#d8d8d8", "blasting", False),
    ("auto_blasting", "Auto-Blasting Upgrade", "#d8d8d8", "blasting", False),
    ("stonecutter", "Stonecutter Upgrade", "#b7b7b7", "stonecutter", False),
    ("anvil", "Anvil Upgrade", "#777b83", "anvil", False),
    ("smithing", "Smithing Upgrade", "#7f8ea3", "smithing", False),
    ("alchemy", "Alchemy Upgrade", "#50c8c8", "alchemy", False),
    ("advanced_alchemy", "Advanced Alchemy Upgrade", "#50c8c8", "alchemy", True),
    ("stack_starter", "Stack Upgrade Starter Tier", "#d9c99a", "stack", False),
    ("stack_tier_1", "Stack Upgrade Tier 1", "#d9c99a", "stack", False),
    ("stack_tier_2", "Stack Upgrade Tier 2", "#d9c99a", "stack", False),
    ("stack_tier_3", "Stack Upgrade Tier 3", "#d9c99a", "stack", False),
    ("stack_tier_4", "Stack Upgrade Tier 4", "#d9c99a", "stack", False),
    ("stack_omega", "Stack Upgrade Omega Tier", "#d06cf5", "stack", False),
    ("stack_downgrade_1", "Stack Downgrade Tier 1", "#d94343", "stack", False),
    ("stack_downgrade_2", "Stack Downgrade Tier 2", "#d94343", "stack", False),
    ("stack_downgrade_3", "Stack Downgrade Tier 3", "#d94343", "stack", False),
    ("feeding", "Feeding Upgrade", "#e0a843", "feeding", False),
    ("advanced_feeding", "Advanced Feeding Upgrade", "#e0a843", "feeding", True),
    ("tool_swapper", "Tool Swapper Upgrade", "#6fa0d6", "tools", False),
    ("advanced_tool_swapper", "Advanced Tool Swapper Upgrade", "#6fa0d6", "tools", True),
    ("jukebox", "Jukebox Upgrade", "#b66f3a", "jukebox", False),
    ("advanced_jukebox", "Advanced Jukebox Upgrade", "#b66f3a", "jukebox", True),
    ("inception", "Inception Upgrade", "#a0703c", "inception", False),
    ("everlasting", "Everlasting Upgrade", "#cfcfcf", "everlasting", False),
    ("infinity_admin", "Infinity Upgrade Admin", "#6e5bc8", "infinity", False),
    ("survival_infinity", "Survival Infinity Upgrade", "#3bd16f", "infinity", False),
    ("tank", "Tank Upgrade", "#2b82c9", "tank", False),
    ("pump", "Pump Upgrade", "#2b82c9", "pump", False),
    ("advanced_pump", "Advanced Pump Upgrade", "#2b82c9", "pump", True),
    ("xp_pump", "XP Pump Upgrade", "#5bd75b", "xp", False),
    ("battery", "Battery Upgrade", "#f2d14e", "battery", False),
]


def generate_existing_textures():
    for tier, style in TIER_STYLES.items():
        draw_backpack_base(TEXTURE_ROOT / "backpacks" / f"{tier}_base.png")
        for dye, dye_hex in DYE_VARIANTS.items():
            draw_backpack_accent(TEXTURE_ROOT / "backpacks/accents" / f"{tier}_{dye}.png", dye_hex)
        draw_backpack_overlay(TEXTURE_ROOT / "backpacks" / f"{tier}_overlay.png", style)

    draw_tier_upgrade(TEXTURE_ROOT / "upgrades/base.png", "base", TIER_STYLES["leather"])
    for tier in ("copper", "iron", "gold", "diamond", "emerald", "netherite"):
        draw_tier_upgrade(TEXTURE_ROOT / f"upgrades/{tier}.png", tier, TIER_STYLES[tier])


def generate_function_textures():
    for slug, _display, color, icon, advanced in FUNCTION_UPGRADES:
        draw_function_upgrade(
            TEXTURE_ROOT / "upgrades/functions" / f"{slug}.png",
            slug,
            color,
            icon,
            advanced,
        )


def write_function_yaml():
    lines = [
        "# Prepared functional backpack upgrade items.",
        "# These are texture/model providers only; gameplay logic can bind to the IDs later.",
        "",
    ]
    base_cmd = 2100
    for index, (slug, display, color, _icon, _advanced) in enumerate(FUNCTION_UPGRADES):
        item_id = f"ysbp_upgrade_{slug}"
        cmd = base_cmd + index
        lines.extend([
            f"{item_id}:",
            f"  displayname: \"<{color}><bold>{display}</bold>\"",
            "  material: PAPER",
            "  Pack:",
            "    generate_model: true",
            "    parent_model: \"item/generated\"",
            "    textures:",
            f"      - ysbp/upgrades/functions/{slug}.png",
            f"    custom_model_data: {cmd}",
            "",
        ])
    (ITEM_ROOT / "yourshika_function_upgrades.yml").write_text("\n".join(lines), encoding="utf-8")


def backpack_textures(tier, accent_name):
    return [
        f"ysbp/backpacks/{tier}_base.png",
        f"ysbp/backpacks/accents/{tier}_{accent_name}.png",
        f"ysbp/backpacks/{tier}_overlay.png",
    ]


def append_backpack_item(lines, item_id, display, cmd, tier, accent_name):
    lines.extend([
        f"{item_id}:",
        f"  displayname: \"{display}\"",
        "  material: LEATHER_HORSE_ARMOR",
        "  color: 255, 255, 255",
        "  Pack:",
        "    generate_model: true",
        "    parent_model: \"item/generated\"",
        "    textures:",
    ])
    for texture in backpack_textures(tier, accent_name):
        lines.append(f"      - {texture}")
    lines.extend([
        f"    custom_model_data: {cmd}",
        "",
    ])


def append_upgrade_item(lines, item_id, display, material, texture, cmd):
    lines.extend([
        f"{item_id}:",
        f"  displayname: \"{display}\"",
        f"  material: {material}",
        "  Pack:",
        "    generate_model: true",
        "    parent_model: \"item/generated\"",
        "    textures:",
        f"      - {texture}",
        f"    custom_model_data: {cmd}",
        "",
    ])


def write_backpack_yaml():
    lines = [
        "# Prepared backpack and tier-upgrade item model providers.",
        "# Backpack base layer is dye-tinted by the leather color; accent layers are generated variants.",
        "",
    ]
    for tier_index, (tier, (display, cmd, default_accent)) in enumerate(TIER_META.items()):
        base_id = f"ysbp_{tier}_backpack"
        default_variant = nearest_dye(default_accent)
        append_backpack_item(lines, base_id, display, cmd, tier, default_variant)
        for dye_index, dye in enumerate(DYE_VARIANTS):
            variant_id = f"{base_id}_accent_{dye}"
            variant_cmd = 11000 + tier_index * 100 + dye_index
            append_backpack_item(lines, variant_id, display, variant_cmd, tier, dye)

    append_upgrade_item(lines, "ysbp_upgrade_base",
                        "<#A0703C><bold>Upgrade-Leder</bold>", "LEATHER",
                        "ysbp/upgrades/base.png", 2000)
    for index, tier in enumerate(("copper", "iron", "gold", "diamond", "emerald", "netherite"), start=1):
        display, _cmd, _accent = TIER_META[tier]
        name = display.split(">", 1)[0] + f"><bold>{tier.capitalize()}-Upgrade</bold>"
        append_upgrade_item(lines, f"ysbp_upgrade_{tier}", name, "PAPER",
                            f"ysbp/upgrades/{tier}.png", 2000 + index)

    (ITEM_ROOT / "yourshika_backpacks.yml").write_text("\n".join(lines), encoding="utf-8")


def write_manifest():
    entries = []
    for root in (ITEM_ROOT, TEXTURE_ROOT):
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            rel = "oraxen/" + path.relative_to(ORAXEN_ROOT).as_posix()
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            entries.append((rel, digest))

    lines = [
        "# Generated by scripts/generate_oraxen_assets.py",
        "asset-version=3",
    ]
    lines.extend(f"sha256.{rel}={digest}" for rel, digest in entries)
    MANIFEST.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    ensure_dirs()
    generate_existing_textures()
    generate_function_textures()
    write_backpack_yaml()
    write_function_yaml()
    write_manifest()


if __name__ == "__main__":
    main()
