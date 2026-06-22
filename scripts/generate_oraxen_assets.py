from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ORAXEN_ROOT = ROOT / "src/main/resources/oraxen"
TEXTURE_ROOT = ORAXEN_ROOT / "pack/textures/ysbp"
ITEM_ROOT = ORAXEN_ROOT / "items"
SIZE = 64
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


def rect(draw, xy, color):
    draw.rectangle(xy, fill=color)


def poly(draw, pts, color):
    draw.polygon(pts, fill=color)


def line(draw, pts, color, width=4):
    draw.line(pts, fill=color, width=width, joint="curve")


def paste_alpha(dst, src):
    dst.alpha_composite(src)


def ensure_dirs():
    (TEXTURE_ROOT / "backpacks").mkdir(parents=True, exist_ok=True)
    (TEXTURE_ROOT / "upgrades").mkdir(parents=True, exist_ok=True)
    (TEXTURE_ROOT / "upgrades/functions").mkdir(parents=True, exist_ok=True)
    ITEM_ROOT.mkdir(parents=True, exist_ok=True)


TIER_STYLES = {
    "leather": {
        "main": "#a0703c", "light": "#d8a064", "dark": "#4a2a18",
        "accent": "#e08a2b", "accent_dark": "#8d4d1d", "gem": "#f3c06a",
    },
    "copper": {
        "main": "#c87b53", "light": "#f1a77b", "dark": "#4b2d1c",
        "accent": "#7a4a2b", "accent_dark": "#3c2619", "gem": "#42bfa7",
    },
    "iron": {
        "main": "#d8d8d8", "light": "#ffffff", "dark": "#565a60",
        "accent": "#8e8e8e", "accent_dark": "#3b3e42", "gem": "#9ed8ff",
    },
    "gold": {
        "main": "#f2d14e", "light": "#fff1a3", "dark": "#7a5208",
        "accent": "#b8860b", "accent_dark": "#5a3905", "gem": "#ffe66b",
    },
    "diamond": {
        "main": "#5be8d4", "light": "#d8fff8", "dark": "#1f6d9e",
        "accent": "#3fa9f5", "accent_dark": "#15547a", "gem": "#7effff",
    },
    "emerald": {
        "main": "#3bd16f", "light": "#b9ffd0", "dark": "#124d28",
        "accent": "#1e7a3c", "accent_dark": "#0b3118", "gem": "#6dff9d",
    },
    "netherite": {
        "main": "#6e5bc8", "light": "#b7a8ff", "dark": "#1a1a1a",
        "accent": "#3e315f", "accent_dark": "#141018", "gem": "#d06cf5",
    },
}


def draw_backpack_base(path):
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    d = ImageDraw.Draw(img)

    # Tint layer: grayscale areas are intentionally bright so leather dye can drive the main color.
    body = [(17, 13), (47, 13), (55, 23), (55, 54), (49, 60), (15, 60), (9, 54), (9, 23)]
    poly(d, body, rgba("#dedede"))
    rect(d, (16, 24, 48, 54), rgba("#c7c7c7"))
    rect(d, (20, 29, 44, 39), rgba("#e7e7e7"))
    rect(d, (18, 42, 46, 56), rgba("#b6b6b6"))
    rect(d, (22, 44, 42, 50), rgba("#d6d6d6"))
    rect(d, (22, 51, 42, 56), rgba("#989898"))

    # Pixel highlights and shadow blocks keep the layer readable after tinting.
    rect(d, (20, 15, 34, 20), rgba("#f8f8f8"))
    rect(d, (14, 25, 19, 41), rgba("#eeeeee"))
    rect(d, (45, 23, 51, 54), rgba("#8e8e8e"))
    rect(d, (25, 58, 44, 60), rgba("#7c7c7c"))
    rect(d, (26, 31, 38, 34), rgba("#ffffff", 220))
    rect(d, (19, 45, 24, 49), rgba("#eeeeee", 210))

    img.save(path)


def draw_backpack_overlay(path, style):
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    d = ImageDraw.Draw(img)
    outline = rgba(style["dark"])
    black = rgba("#120d0a")
    main = rgba(style["main"])
    light = rgba(style["light"])
    accent = rgba(style["accent"])
    accent_dark = rgba(style["accent_dark"])
    gem = rgba(style["gem"])

    body = [(17, 13), (47, 13), (55, 23), (55, 54), (49, 60), (15, 60), (9, 54), (9, 23)]
    line(d, body + [body[0]], black, 4)
    line(d, [(23, 13), (23, 8), (41, 8), (41, 13)], black, 4)
    line(d, [(24, 13), (24, 10), (40, 10), (40, 13)], light, 3)

    # Lid and trim.
    line(d, [(14, 24), (24, 17), (40, 17), (50, 24)], black, 4)
    line(d, [(17, 24), (25, 20), (39, 20), (47, 24)], light, 3)
    rect(d, (13, 25, 51, 29), accent_dark)
    rect(d, (17, 25, 47, 27), accent)

    # Side straps and belt.
    rect(d, (15, 30, 21, 57), outline)
    rect(d, (43, 30, 49, 57), outline)
    rect(d, (18, 31, 21, 54), main)
    rect(d, (43, 31, 46, 54), main)
    rect(d, (13, 37, 51, 44), outline)
    rect(d, (17, 38, 47, 41), main)
    rect(d, (28, 36, 36, 45), accent_dark)
    rect(d, (30, 38, 34, 42), gem)
    rect(d, (31, 39, 33, 41), light)

    # Lower pocket and tier studs.
    rect(d, (21, 47, 43, 56), accent_dark)
    rect(d, (24, 48, 40, 51), accent)
    rect(d, (26, 52, 38, 54), outline)
    for x in (17, 47):
        rect(d, (x, 21, x + 3, 24), gem)
        rect(d, (x, 55, x + 3, 58), gem)
    rect(d, (12, 31, 15, 39), accent)
    rect(d, (49, 31, 52, 39), accent_dark)

    img.save(path)


def draw_tier_upgrade(path, key, style):
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    d = ImageDraw.Draw(img)

    if key == "base":
        outline = rgba("#2a170e")
        rect(d, (16, 10, 48, 54), outline)
        rect(d, (12, 18, 52, 46), outline)
        rect(d, (16, 14, 48, 50), rgba("#a0703c"))
        rect(d, (20, 15, 44, 23), rgba("#d29a63"))
        rect(d, (20, 43, 48, 50), rgba("#5a321c"))
        line(d, [(19, 15), (45, 50)], rgba("#eadcc1"), 3)
        line(d, [(45, 15), (19, 50)], rgba("#eadcc1"), 3)
        rect(d, (27, 28, 37, 37), rgba("#6d3b20"))
        rect(d, (30, 31, 34, 35), rgba("#f1d3a5"))
        img.save(path)
        return

    outline = rgba("#4b3524")
    paper = rgba("#d9c99a")
    light = rgba("#fff0b8")
    shadow = rgba("#9f7f55")
    main = rgba(style["main"])
    dark = rgba(style["dark"])
    gem = rgba(style["gem"])

    rect(d, (17, 8, 47, 55), outline)
    rect(d, (13, 13, 51, 50), outline)
    rect(d, (16, 12, 48, 51), paper)
    rect(d, (20, 12, 43, 18), light)
    rect(d, (20, 46, 48, 51), shadow)
    poly(d, [(44, 12), (52, 12), (52, 20), (44, 20)], outline)
    poly(d, [(44, 13), (50, 13), (50, 19), (44, 19)], light)

    # Upgrade crystal.
    poly(d, [(32, 18), (44, 27), (38, 43), (26, 43), (20, 27)], dark)
    poly(d, [(32, 20), (41, 28), (36, 39), (28, 39), (23, 28)], main)
    poly(d, [(32, 20), (36, 29), (32, 39), (28, 29)], gem)
    rect(d, (30, 25, 34, 29), light)
    line(d, [(20, 48), (34, 48), (34, 44), (44, 48), (34, 52), (34, 48)], outline, 4)
    img.save(path)


def draw_module_frame(color, advanced=False):
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    d = ImageDraw.Draw(img)
    main = rgba(color)
    light = mix(color, "#ffffff", 0.42)
    dark = mix(color, "#000000", 0.45)
    edge = rgba("#1c1d22")

    # A compact module plate: square enough for inventory icons, with Minecraft-like stepped corners.
    poly(d, [(14, 6), (50, 6), (58, 14), (58, 50), (50, 58), (14, 58), (6, 50), (6, 14)], edge)
    poly(d, [(16, 10), (48, 10), (54, 16), (54, 48), (48, 54), (16, 54), (10, 48), (10, 16)], dark)
    rect(d, (15, 15, 49, 49), main)
    rect(d, (18, 18, 46, 24), light)
    rect(d, (18, 43, 49, 49), dark)
    rect(d, (10, 28, 14, 36), light)
    rect(d, (50, 28, 54, 36), edge)

    if advanced:
        rect(d, (44, 6, 56, 18), rgba("#b8860b"))
        rect(d, (47, 9, 53, 15), rgba("#fff1a3"))
        rect(d, (49, 8, 51, 16), rgba("#6b4708"))
        rect(d, (46, 11, 54, 13), rgba("#6b4708"))

    return img


def draw_plus(draw, x, y, color, size=5):
    rect(draw, (x - 1, y - size, x + 1, y + size), color)
    rect(draw, (x - size, y - 1, x + size, y + 1), color)


def draw_lightning(draw, color):
    poly(draw, [(44, 16), (36, 34), (44, 34), (38, 50), (53, 28), (45, 28)], color)
    line(draw, [(44, 16), (36, 34), (44, 34), (38, 50)], rgba("#593900"), 2)


def icon_arrow_to_bag(d, color):
    rect(d, (18, 34, 40, 49), rgba("#7a4a2b"))
    rect(d, (22, 29, 36, 34), rgba("#d29a63"))
    line(d, [(26, 29), (26, 24), (34, 24), (34, 29)], rgba("#3c2619"), 3)
    line(d, [(19, 17), (34, 17), (34, 13), (46, 24), (34, 35), (34, 30), (19, 30)], rgba(color), 5)
    rect(d, (28, 39, 36, 44), rgba("#f3c06a"))


def icon_magnet(d):
    line(d, [(20, 18), (20, 42), (30, 50), (42, 42), (42, 18)], rgba("#343840"), 8)
    line(d, [(20, 18), (20, 29)], rgba("#d94343"), 8)
    line(d, [(42, 18), (42, 29)], rgba("#3f82ff"), 8)
    rect(d, (16, 14, 24, 20), rgba("#ffb0a8"))
    rect(d, (38, 14, 46, 20), rgba("#a8c8ff"))
    for x, y in ((31, 28), (15, 39), (49, 39), (33, 48)):
        rect(d, (x - 2, y - 2, x + 2, y + 2), rgba("#ffe66b"))


def icon_filter(d):
    poly(d, [(15, 16), (49, 16), (37, 32), (37, 47), (27, 52), (27, 32)], rgba("#f1d3a5"))
    line(d, [(15, 16), (49, 16), (37, 32), (37, 47), (27, 52), (27, 32), (15, 16)], rgba("#4b3524"), 4)
    rect(d, (21, 22, 43, 25), rgba("#ffffff", 180))
    rect(d, (29, 35, 35, 46), rgba("#b8860b"))


def icon_void(d):
    d.ellipse((14, 14, 50, 50), fill=rgba("#111018"))
    d.ellipse((20, 20, 44, 44), fill=rgba("#34185e"))
    d.ellipse((26, 25, 39, 39), fill=rgba("#07060b"))
    line(d, [(17, 33), (26, 25), (38, 27), (46, 37)], rgba("#9b68ff"), 4)
    line(d, [(19, 43), (29, 47), (40, 42)], rgba("#d06cf5"), 3)


def icon_transfer(d, mode):
    rect(d, (10, 22, 27, 45), rgba("#8b5a2b"))
    rect(d, (37, 18, 54, 42), rgba("#a0703c"))
    rect(d, (12, 24, 25, 29), rgba("#d29a63"))
    rect(d, (39, 20, 52, 26), rgba("#d29a63"))
    if mode == "refill":
        line(d, [(38, 49), (24, 49), (24, 40), (12, 40), (24, 29), (24, 36), (38, 36)], rgba("#7effff"), 5)
    elif mode == "restock":
        line(d, [(15, 13), (40, 13), (40, 9), (53, 21), (40, 33), (40, 28), (15, 28)], rgba("#6dff9d"), 5)
    else:
        line(d, [(15, 48), (40, 48), (40, 52), (53, 40), (40, 28), (40, 33), (15, 33)], rgba("#ffe66b"), 5)


def icon_crafting(d):
    for y in range(3):
        for x in range(3):
            x1, y1 = 15 + x * 11, 15 + y * 11
            rect(d, (x1, y1, x1 + 8, y1 + 8), rgba("#d9c99a"))
            rect(d, (x1 + 2, y1 + 2, x1 + 6, y1 + 6), rgba("#8b6b44"))
    line(d, [(45, 27), (54, 32), (45, 37)], rgba("#ffffff"), 4)


def icon_compacting(d, advanced=False):
    blocks = [(18, 18), (31, 18), (18, 31), (31, 31)]
    if advanced:
        blocks += [(44, 18), (44, 31), (18, 44), (31, 44), (44, 44)]
    for x, y in blocks:
        rect(d, (x, y, x + 9, y + 9), rgba("#b6b6b6"))
        rect(d, (x + 2, y + 2, x + 7, y + 4), rgba("#eeeeee"))
        rect(d, (x + 2, y + 7, x + 9, y + 9), rgba("#777777"))
    line(d, [(12, 50), (31, 50), (31, 45), (44, 52), (31, 59), (31, 54), (12, 54)], rgba("#f2d14e"), 4)


def icon_furnace(d, kind, auto=False):
    rect(d, (16, 12, 48, 52), rgba("#3b3e42"))
    rect(d, (20, 16, 44, 26), rgba("#777777"))
    rect(d, (20, 32, 44, 48), rgba("#161616"))
    if kind == "smoking":
        line(d, [(24, 16), (20, 8), (28, 6)], rgba("#d9d9d9"), 4)
        line(d, [(34, 16), (30, 7), (39, 6)], rgba("#bdbdbd"), 4)
    elif kind == "blasting":
        rect(d, (14, 17, 18, 26), rgba("#d8d8d8"))
        rect(d, (46, 17, 50, 26), rgba("#d8d8d8"))
    poly(d, [(32, 34), (24, 47), (40, 47)], rgba("#e85c2a"))
    poly(d, [(32, 31), (29, 45), (37, 45)], rgba("#ffd166"))
    if auto:
        draw_lightning(d, rgba("#fff1a3"))


def icon_stonecutter(d):
    d.ellipse((16, 14, 48, 46), fill=rgba("#d8d8d8"))
    d.ellipse((23, 21, 41, 39), fill=rgba("#8e8e8e"))
    line(d, [(32, 13), (32, 47)], rgba("#ffffff"), 3)
    line(d, [(17, 30), (47, 30)], rgba("#ffffff"), 3)
    rect(d, (17, 47, 47, 54), rgba("#6b5a48"))


def icon_anvil(d):
    rect(d, (18, 35, 48, 43), rgba("#31343a"))
    rect(d, (25, 26, 42, 35), rgba("#565a60"))
    rect(d, (12, 23, 33, 30), rgba("#777b83"))
    rect(d, (30, 43, 36, 53), rgba("#25272a"))
    rect(d, (20, 53, 47, 57), rgba("#25272a"))


def icon_smithing(d):
    icon_anvil(d)
    line(d, [(43, 14), (53, 24)], rgba("#5a321c"), 4)
    rect(d, (34, 10, 47, 18), rgba("#d8d8d8"))
    rect(d, (36, 12, 45, 16), rgba("#ffffff"))


def icon_alchemy(d):
    rect(d, (27, 13, 37, 29), rgba("#d8d8d8"))
    poly(d, [(24, 29), (40, 29), (48, 50), (16, 50)], rgba("#5be8d4"))
    line(d, [(24, 29), (40, 29), (48, 50), (16, 50), (24, 29)], rgba("#0e3438"), 4)
    rect(d, (23, 39, 41, 45), rgba("#7effff"))
    for x, y in ((21, 19), (45, 22), (38, 12)):
        d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=rgba("#d06cf5"))


def icon_stack(d, variant):
    color = rgba("#d9c99a")
    dark = rgba("#5b4430")
    levels = {
        "stack_starter": 3, "stack_tier_1": 4, "stack_tier_2": 5,
        "stack_tier_3": 6, "stack_tier_4": 7, "stack_omega": 8,
        "stack_downgrade_1": 5, "stack_downgrade_2": 4, "stack_downgrade_3": 3,
    }.get(variant, 4)
    for i in range(levels):
        x = 17 + i * 3
        y = 43 - i * 4
        rect(d, (x, y, x + 24, y + 8), dark)
        rect(d, (x + 2, y + 1, x + 22, y + 6), color)
    if "downgrade" in variant:
        line(d, [(48, 16), (32, 32), (16, 16)], rgba("#d94343"), 5)
    elif variant == "stack_omega":
        line(d, [(18, 21), (26, 13), (34, 21), (42, 13), (50, 21)], rgba("#d06cf5"), 4)
    else:
        line(d, [(16, 17), (32, 10), (48, 17)], rgba("#6dff9d"), 5)


def icon_food(d):
    poly(d, [(19, 23), (29, 16), (43, 20), (46, 34), (36, 47), (23, 42)], rgba("#d9a24f"))
    rect(d, (21, 28, 44, 39), rgba("#f2d14e"))
    rect(d, (22, 37, 38, 43), rgba("#8d4d1d"))
    rect(d, (34, 21, 42, 27), rgba("#fff1a3"))


def icon_tools(d):
    line(d, [(18, 46), (46, 18)], rgba("#5a321c"), 5)
    rect(d, (39, 10, 51, 20), rgba("#d8d8d8"))
    rect(d, (42, 20, 47, 27), rgba("#8e8e8e"))
    line(d, [(17, 18), (47, 48)], rgba("#6b4a2b"), 5)
    rect(d, (11, 12, 25, 19), rgba("#5be8d4"))
    rect(d, (16, 19, 21, 27), rgba("#3fa9f5"))


def icon_jukebox(d):
    rect(d, (15, 22, 49, 51), rgba("#8b5a2b"))
    rect(d, (19, 26, 45, 47), rgba("#b86f35"))
    d.ellipse((22, 11, 44, 33), fill=rgba("#161616"))
    d.ellipse((28, 17, 38, 27), fill=rgba("#6e5bc8"))
    rect(d, (25, 36, 39, 42), rgba("#f2d14e"))


def icon_inception(d):
    rect(d, (14, 18, 50, 51), rgba("#7a4a2b"))
    rect(d, (20, 14, 44, 22), rgba("#d29a63"))
    rect(d, (25, 30, 39, 46), rgba("#a0703c"))
    rect(d, (28, 27, 36, 32), rgba("#f3c06a"))
    rect(d, (27, 36, 37, 43), rgba("#d29a63"))


def icon_shield(d, infinity=False):
    poly(d, [(32, 10), (49, 18), (46, 43), (32, 54), (18, 43), (15, 18)], rgba("#d8d8d8"))
    poly(d, [(32, 15), (44, 21), (41, 40), (32, 48), (23, 40), (20, 21)], rgba("#6e5bc8"))
    if infinity:
        line(d, [(20, 33), (27, 25), (32, 33), (37, 41), (44, 33), (37, 25), (32, 33), (27, 41), (20, 33)], rgba("#fff1a3"), 4)
    else:
        rect(d, (29, 22, 35, 42), rgba("#fff1a3"))
        rect(d, (24, 30, 40, 34), rgba("#fff1a3"))


def icon_tank(d):
    rect(d, (20, 11, 44, 53), rgba("#565a60"))
    rect(d, (24, 15, 40, 49), rgba("#2b82c9"))
    rect(d, (24, 15, 40, 25), rgba("#7effff"))
    line(d, [(18, 22), (10, 22), (10, 42), (18, 42)], rgba("#d8d8d8"), 5)
    line(d, [(46, 22), (54, 22), (54, 42), (46, 42)], rgba("#d8d8d8"), 5)


def icon_pump(d):
    icon_tank(d)
    line(d, [(12, 43), (28, 43), (28, 55), (48, 55)], rgba("#8e8e8e"), 5)
    poly(d, [(47, 49), (58, 55), (47, 61)], rgba("#7effff"))


def icon_xp(d):
    for x, y, s in ((22, 25, 8), (39, 22, 7), (32, 42, 10)):
        d.ellipse((x - s, y - s, x + s, y + s), fill=rgba("#6dff9d"))
        d.ellipse((x - s + 3, y - s + 3, x + s - 3, y + s - 3), fill=rgba("#b9ffd0"))
    line(d, [(14, 50), (50, 50)], rgba("#3bd16f"), 4)


def icon_battery(d):
    rect(d, (15, 20, 47, 46), rgba("#25272a"))
    rect(d, (47, 27, 53, 39), rgba("#25272a"))
    rect(d, (20, 25, 42, 41), rgba("#3bd16f"))
    rect(d, (20, 25, 30, 41), rgba("#b9ffd0"))
    draw_lightning(d, rgba("#fff1a3"))


def draw_function_upgrade(path, slug, color, icon, advanced=False):
    img = draw_module_frame(color, advanced)
    d = ImageDraw.Draw(img)

    if icon == "pickup":
        icon_arrow_to_bag(d, "#6dff9d")
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
        # Gold side gems communicate a broader/advanced version without text.
        for x, y in ((15, 15), (49, 49), (15, 49)):
            rect(d, (x - 2, y - 2, x + 2, y + 2), rgba("#fff1a3"))
    if slug.startswith("auto_"):
        rect(d, (9, 9, 20, 20), rgba("#b8860b"))
        draw_plus(d, 14, 14, rgba("#fff1a3"), 4)

    img.save(path)


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
        draw_backpack_overlay(TEXTURE_ROOT / "backpacks" / f"{tier}_overlay.png", style)

    draw_tier_upgrade(TEXTURE_ROOT / "upgrades/base.png", "base", TIER_STYLES["leather"])
    for tier in ("copper", "iron", "gold", "diamond", "emerald", "netherite"):
        draw_tier_upgrade(TEXTURE_ROOT / f"upgrades/{tier}.png", tier, TIER_STYLES[tier])


def generate_function_textures():
    for slug, _display, color, icon, advanced in FUNCTION_UPGRADES:
        draw_function_upgrade(TEXTURE_ROOT / "upgrades/functions" / f"{slug}.png",
                              slug, color, icon, advanced)


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


def main():
    ensure_dirs()
    generate_existing_textures()
    generate_function_textures()
    write_function_yaml()


if __name__ == "__main__":
    main()
