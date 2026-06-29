import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ORAXEN = ROOT / "src/main/resources/oraxen"
TEXTURES = ORAXEN / "pack/textures/ysbp"
ITEMS = ORAXEN / "items"
SOURCE_BUNDLE = ORAXEN / "source/bundle_reference.png"
SCALE = 4

ALPHA = (0, 0, 0, 0)
BLACK = (45, 29, 18, 255)
OUTLINE = (80, 49, 26, 255)
SHADOW = (114, 71, 40, 255)
MID = (143, 91, 53, 255)
LIGHT = (172, 110, 66, 255)
HIGHLIGHT = (209, 130, 73, 255)
IRON = (99, 107, 116, 255)
DARK_IRON = (83, 92, 102, 255)
PAPER = (207, 183, 117, 255)
PAPER_DARK = (116, 86, 48, 255)
PAPER_LIGHT = (238, 219, 154, 255)
GOLD = (245, 195, 66, 255)
GOLD_DARK = (139, 90, 23, 255)


def c(hex_color, alpha=255):
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def ensure_dirs():
    for path in (
        TEXTURES / "backpacks",
        TEXTURES / "backpacks/accents",
        TEXTURES / "backpacks/color_layers/main",
        TEXTURES / "backpacks/color_layers/accent",
        TEXTURES / "backpacks/dual_examples",
        TEXTURES / "placed/main",
        TEXTURES / "placed/accent",
        TEXTURES / "placed/detail",
        TEXTURES / "upgrades",
        TEXTURES / "upgrades/functions",
        ORAXEN / "pack/models/ysbp/placed",
        ITEMS,
    ):
        path.mkdir(parents=True, exist_ok=True)


def save64(img, path):
    img.resize((64, 64), Image.Resampling.NEAREST).save(path)


def px(img, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), color)


def rect(d, xy, color):
    d.rectangle(xy, fill=color)


def line(d, pts, color, width=1):
    d.line(pts, fill=color, width=width)


def poly(d, pts, color):
    d.polygon(pts, fill=color)


def bundle_source():
    return Image.open(SOURCE_BUNDLE).convert("RGBA")


def luminance(color):
    r, g, b, _a = color
    return r * 0.2126 + g * 0.7152 + b * 0.0722


TIER = {
    "leather": ("#a0703c", "#e08a2b", 1001, "Leder-Rucksack"),
    "copper": ("#c87b53", "#56bca8", 1002, "Kupfer-Rucksack"),
    "iron": ("#d8d8d8", "#9fb7c8", 1003, "Eisen-Rucksack"),
    "gold": ("#f2d14e", "#fff1a3", 1004, "Gold-Rucksack"),
    "diamond": ("#5be8d4", "#3fa9f5", 1005, "Diamant-Rucksack"),
    "emerald": ("#3bd16f", "#b9ffd0", 1006, "Smaragd-Rucksack"),
    "netherite": ("#6e5bc8", "#d06cf5", 1007, "Netherite-Rucksack"),
}

DYES = {
    "white": "#f9fffe",
    "orange": "#f9801d",
    "magenta": "#c74ebd",
    "light_blue": "#3ab3da",
    "yellow": "#fed83d",
    "lime": "#80c71f",
    "pink": "#f38baa",
    "gray": "#474f52",
    "light_gray": "#9d9d97",
    "cyan": "#169c9c",
    "purple": "#8932b8",
    "blue": "#3c44aa",
    "brown": "#835432",
    "green": "#5e7c16",
    "red": "#b02e26",
    "black": "#1d1d21",
}


def tint_color(base_hex, intensity):
    base = c(base_hex)
    white = (255, 255, 255, 255)
    black = (0, 0, 0, 255)
    if intensity >= 0.72:
        return mix(base, white, (intensity - 0.72) / 0.28 * 0.28)
    return mix(black, base, max(0.12, intensity / 0.72))


def bundle_main_layer(main_hex=None):
    src = bundle_source()
    out = Image.new("RGBA", (16, 16), ALPHA)
    visible = [src.getpixel((x, y)) for y in range(16) for x in range(16) if src.getpixel((x, y))[3]]
    min_l = min(luminance(p) for p in visible)
    max_l = max(luminance(p) for p in visible)
    for y in range(16):
        for x in range(16):
            color = src.getpixel((x, y))
            if color[3] == 0:
                continue
            # Keep metal/buckle pixels for the overlay layer.
            if abs(color[0] - color[1]) < 12 and abs(color[1] - color[2]) < 18:
                continue
            t = (luminance(color) - min_l) / max(1, max_l - min_l)
            if main_hex:
                out.putpixel((x, y), tint_color(main_hex, 0.34 + t * 0.58))
            else:
                g = int(58 + t * 178)
                out.putpixel((x, y), (g, g, g, 255))
    return out


def bundle_accent_layer(accent_hex):
    out = Image.new("RGBA", (16, 16), ALPHA)
    d = ImageDraw.Draw(out)
    accent = c(accent_hex)
    dark = mix(accent, (0, 0, 0, 255), 0.45)
    light = mix(accent, (255, 255, 255, 255), 0.32)
    # Minimal tie/strap details placed inside the supplied bundle silhouette.
    line(d, [(5, 5), (8, 8), (11, 5)], dark)
    line(d, [(6, 5), (8, 7), (10, 5)], light)
    rect(d, (4, 9, 10, 10), dark)
    rect(d, (5, 9, 9, 9), accent)
    px(out, 6, 11, light)
    px(out, 9, 11, dark)
    return out


def bundle_overlay_layer(tier_key):
    src = bundle_source()
    out = Image.new("RGBA", (16, 16), ALPHA)
    main_hex, accent_hex, _cmd, _name = TIER[tier_key]
    metal = c(accent_hex)
    metal_dark = mix(metal, (0, 0, 0, 255), 0.50)
    for y in range(16):
        for x in range(16):
            color = src.getpixel((x, y))
            if color[3] == 0:
                continue
            lum = luminance(color)
            is_gray = abs(color[0] - color[1]) < 12 and abs(color[1] - color[2]) < 18
            if is_gray:
                out.putpixel((x, y), IRON if lum > 96 else DARK_IRON)
            elif lum < 74:
                out.putpixel((x, y), mix(c(main_hex), (0, 0, 0, 255), 0.65))
    # Tiny tier clasp.
    px(out, 7, 9, metal_dark)
    px(out, 8, 9, metal)
    return out


def composite_bundle(main_hex, accent_hex, tier_key="leather"):
    out = Image.new("RGBA", (16, 16), ALPHA)
    out.alpha_composite(bundle_main_layer(main_hex))
    out.alpha_composite(bundle_accent_layer(accent_hex))
    out.alpha_composite(bundle_overlay_layer(tier_key))
    return out


def generate_backpacks():
    for tier_key, (main, accent, _cmd, _name) in TIER.items():
        save64(bundle_main_layer(), TEXTURES / f"backpacks/{tier_key}_base.png")
        save64(bundle_accent_layer(accent), TEXTURES / f"backpacks/{tier_key}_accent.png")
        save64(bundle_overlay_layer(tier_key), TEXTURES / f"backpacks/{tier_key}_overlay.png")
        for dye_name, dye_color in DYES.items():
            save64(bundle_accent_layer(dye_color), TEXTURES / f"backpacks/accents/{tier_key}_{dye_name}.png")

    for name, color in DYES.items():
        save64(bundle_main_layer(color), TEXTURES / f"backpacks/color_layers/main/{name}.png")
        save64(bundle_accent_layer(color), TEXTURES / f"backpacks/color_layers/accent/{name}.png")

    # Compact Oraxen-ready examples for common dual-color testing without generating thousands of tier variants.
    examples = [
        ("brown_orange", "#835432", "#f9801d"),
        ("black_purple", "#1d1d21", "#8932b8"),
        ("blue_light_blue", "#3c44aa", "#3ab3da"),
        ("green_lime", "#5e7c16", "#80c71f"),
        ("red_gold", "#b02e26", "#fed83d"),
        ("white_cyan", "#f9fffe", "#169c9c"),
    ]
    for slug, main, accent in examples:
        save64(composite_bundle(main, accent), TEXTURES / f"backpacks/dual_examples/{slug}.png")


def ensure_backpack_assets():
    expected = []
    for tier_key in TIER:
        expected.extend([
            TEXTURES / f"backpacks/{tier_key}_base.png",
            TEXTURES / f"backpacks/{tier_key}_accent.png",
            TEXTURES / f"backpacks/{tier_key}_overlay.png",
        ])
        expected.extend(TEXTURES / f"backpacks/accents/{tier_key}_{dye}.png" for dye in DYES)
    expected.extend(TEXTURES / f"backpacks/color_layers/main/{dye}.png" for dye in DYES)
    expected.extend(TEXTURES / f"backpacks/color_layers/accent/{dye}.png" for dye in DYES)
    expected.extend(TEXTURES / f"backpacks/dual_examples/{slug}.png" for slug in (
        "brown_orange", "black_purple", "blue_light_blue", "green_lime", "red_gold", "white_cyan",
    ))
    if any(not path.exists() for path in expected):
        generate_backpacks()




def clamp(v):
    return max(0, min(255, int(v)))


def lighten(color, amount):
    return mix(color if not isinstance(color, str) else c(color), (255, 255, 255, 255), amount)


def darken(color, amount):
    return mix(color if not isinstance(color, str) else c(color), (0, 0, 0, 255), amount)


FUNCTIONS = [
    ("pickup", "Pickup Upgrade", "#58c96a", "pickup", False, 2100),
    ("advanced_pickup", "Advanced Pickup Upgrade", "#58c96a", "pickup", True, 2101),
    ("magnet", "Magnet Upgrade", "#ff6b6b", "magnet", False, 2102),
    ("advanced_magnet", "Advanced Magnet Upgrade", "#ff6b6b", "magnet", True, 2103),
    ("filter", "Filter Upgrade", "#d6aa4c", "filter", False, 2104),
    ("advanced_filter", "Advanced Filter Upgrade", "#d6aa4c", "filter", True, 2105),
    ("void", "Void Upgrade", "#6e5bc8", "void", False, 2106),
    ("advanced_void", "Advanced Void Upgrade", "#6e5bc8", "void", True, 2107),
    ("refill", "Refill Upgrade", "#4bbfd0", "refill", False, 2108),
    ("advanced_refill", "Advanced Refill Upgrade", "#4bbfd0", "refill", True, 2109),
    ("restock", "Restock Upgrade", "#53bd74", "restock", False, 2110),
    ("advanced_restock", "Advanced Restock Upgrade", "#53bd74", "restock", True, 2111),
    ("deposit", "Deposit Upgrade", "#f0b848", "deposit", False, 2112),
    ("advanced_deposit", "Advanced Deposit Upgrade", "#f0b848", "deposit", True, 2113),
    ("crafting", "Crafting Upgrade", "#7fd7ff", "crafting", False, 2114),
    ("compacting", "Compacting Upgrade", "#d2b48c", "compacting", False, 2115),
    ("advanced_compacting", "Advanced Compacting Upgrade", "#d2b48c", "compacting", True, 2116),
    ("smelting", "Smelting Upgrade", "#ff8c42", "smelting", False, 2117),
    ("auto_smelting", "Auto-Smelting Upgrade", "#ff8c42", "auto_smelting", False, 2118),
    ("smoking", "Smoking Upgrade", "#ffd27f", "smoking", False, 2119),
    ("auto_smoking", "Auto-Smoking Upgrade", "#ffd27f", "auto_smoking", False, 2120),
    ("blasting", "Blasting Upgrade", "#ffb36b", "blasting", False, 2121),
    ("auto_blasting", "Auto-Blasting Upgrade", "#ffb36b", "auto_blasting", False, 2122),
    ("stonecutter", "Stonecutter Upgrade", "#c0c0c0", "stonecutter", False, 2123),
    ("anvil", "Anvil Upgrade", "#777b83", "anvil", False, 2124),
    ("smithing", "Smithing Upgrade", "#8ab4f8", "smithing", False, 2125),
    ("alchemy", "Alchemy Upgrade", "#50c8c8", "alchemy", False, 2126),
    ("advanced_alchemy", "Advanced Alchemy Upgrade", "#50c8c8", "alchemy", True, 2127),
    ("stack_starter", "Stack Upgrade Starter Tier", "#d9c99a", "stack", False, 2128),
    ("stack_tier_1", "Stack Upgrade Tier 1", "#d9c99a", "stack", False, 2129),
    ("stack_tier_2", "Stack Upgrade Tier 2", "#d9c99a", "stack", False, 2130),
    ("stack_tier_3", "Stack Upgrade Tier 3", "#d9c99a", "stack", False, 2131),
    ("stack_tier_4", "Stack Upgrade Tier 4", "#d9c99a", "stack", False, 2132),
    ("stack_omega", "Stack Upgrade Omega Tier", "#d06cf5", "stack", False, 2133),
    ("stack_downgrade_1", "Stack Downgrade Tier 1", "#d94343", "stack", False, 2134),
    ("stack_downgrade_2", "Stack Downgrade Tier 2", "#d94343", "stack", False, 2135),
    ("stack_downgrade_3", "Stack Downgrade Tier 3", "#d94343", "stack", False, 2136),
    ("feeding", "Feeding Upgrade", "#e0a843", "feeding", False, 2137),
    ("advanced_feeding", "Advanced Feeding Upgrade", "#e0a843", "feeding", True, 2138),
    ("tool_swapper", "Tool Swapper Upgrade", "#6fa0d6", "tool_swapper", False, 2139),
    ("advanced_tool_swapper", "Advanced Tool Swapper Upgrade", "#6fa0d6", "tool_swapper", True, 2140),
    ("jukebox", "Jukebox Upgrade", "#b66f3a", "jukebox", False, 2141),
    ("advanced_jukebox", "Advanced Jukebox Upgrade", "#b66f3a", "jukebox", True, 2142),
    ("inception", "Inception Upgrade", "#a0703c", "inception", False, 2143),
    ("everlasting", "Everlasting Upgrade", "#b388ff", "everlasting", False, 2144),
    ("infinity_admin", "Infinity Upgrade Admin", "#6e5bc8", "infinity", False, 2145),
    ("survival_infinity", "Survival Infinity Upgrade", "#3bd16f", "infinity", False, 2146),
    ("tank", "Tank Upgrade", "#2b82c9", "tank", False, 2147),
    ("pump", "Pump Upgrade", "#2b82c9", "pump", False, 2148),
    ("advanced_pump", "Advanced Pump Upgrade", "#2b82c9", "pump", True, 2149),
    ("ender_link", "Ender-Link Upgrade", "#a66bff", "ender_link", False, 2150),
    ("recall", "Recall Upgrade", "#5bc8ff", "recall", False, 2151),
    ("trash", "Trash Upgrade", "#8b8b8b", "trash", False, 2152),
    ("xp_pump", "XP Pump Upgrade", "#5bd75b", "xp_pump", False, 2153),
    ("battery", "Battery Upgrade", "#f2d14e", "battery", False, 2154),
    ("xp", "XP Storage Upgrade", "#7cff6b", "xp_store", False, 2155),
]


def generate_functions():
    for slug, _display, color, kind, advanced, _cmd in FUNCTIONS:
        draw_function(slug, color, kind, advanced)


def icon_canvas():
    return Image.new("RGBA", (16, 16), ALPHA)


def shift_pixel_color(color, target, amount):
    r, g, b, a = color
    if a == 0:
        return color
    return (
        int(r + (target[0] - r) * amount),
        int(g + (target[1] - g) * amount),
        int(b + (target[2] - b) * amount),
        a,
    )


def polish_icon16(img):
    if img.size != (16, 16):
        return img

    original = img.copy()
    src = original.load()
    dst = img.load()
    visible = [
        (x, y)
        for y in range(16)
        for x in range(16)
        if src[x, y][3] > 0
    ]
    if not visible:
        return img

    shadow = (31, 22, 15, 150)
    soft_shadow = (31, 22, 15, 85)
    for x, y in visible:
        for dx, dy, color in ((1, 1, shadow), (0, 1, soft_shadow), (1, 0, soft_shadow)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and src[nx, ny][3] == 0 and dst[nx, ny][3] == 0:
                dst[nx, ny] = color

    min_x = min(x for x, _y in visible)
    max_x = max(x for x, _y in visible)
    max_y = max(y for _x, y in visible)
    contact_y = max_y + 1
    if contact_y < 16:
        for x in range(min_x + 1, max_x):
            if src[x, contact_y][3] == 0 and dst[x, contact_y][3] == 0:
                dst[x, contact_y] = (22, 16, 11, 105)

    for x, y in visible:
        color = src[x, y]
        new = color
        top_edge = (y == 0 or src[x, y - 1][3] == 0) or (x == 0 or src[x - 1, y][3] == 0)
        low_edge = (y == 15 or src[x, y + 1][3] == 0) or (x == 15 or src[x + 1, y][3] == 0)
        if top_edge and luminance(color) > 60:
            new = shift_pixel_color(new, (255, 242, 190, 255), 0.13)
        if low_edge:
            new = shift_pixel_color(new, (16, 10, 6, 255), 0.14)
        dst[x, y] = new
    return img


def save_icon(img, path):
    polish_icon16(img)
    img.resize((64, 64), Image.Resampling.NEAREST).save(path, optimize=True)


def tiny_frame(color="#58c96a"):
    img = icon_canvas()
    d = ImageDraw.Draw(img)
    accent = c(color)
    accent_dark = darken(accent, 0.50)
    accent_light = lighten(accent, 0.38)

    # A few loose pixels keep the color family visible without making every
    # upgrade look like the same rectangular card.
    px(img, 3, 13, accent_dark)
    px(img, 4, 14, accent_dark)
    px(img, 12, 2, accent)
    px(img, 13, 3, accent_light)
    return img, d, accent


def mark_advanced_16(img):
    d = ImageDraw.Draw(img)
    px(img, 12, 1, GOLD_DARK)
    px(img, 13, 1, GOLD)
    px(img, 12, 2, GOLD)
    px(img, 14, 2, PAPER_LIGHT)


def mark_auto_16(img):
    d = ImageDraw.Draw(img)
    poly(d, [(2, 1), (1, 5), (3, 5), (2, 9), (6, 4), (4, 4)], GOLD)
    px(img, 3, 5, GOLD_DARK)


def glyph_bag16(d, x, y, fill=MID):
    rect(d, (x + 1, y + 3, x + 6, y + 7), OUTLINE)
    rect(d, (x + 2, y + 3, x + 5, y + 6), fill)
    line(d, [(x + 2, y + 3), (x + 2, y + 2), (x + 5, y + 2), (x + 5, y + 3)], OUTLINE)


def draw_tier_gem_16(d, color):
    dark = darken(color, 0.45)
    light = lighten(color, 0.40)
    poly(d, [(8, 4), (12, 8), (10, 12), (6, 12), (4, 8)], OUTLINE)
    poly(d, [(8, 5), (11, 8), (9, 11), (7, 11), (5, 8)], dark)
    poly(d, [(8, 6), (10, 8), (8, 10), (6, 8)], color)
    line(d, [(8, 6), (8, 10)], light)
    d.point((7, 7), fill=light)


def outlined_line16(d, pts, color, width=1):
    line(d, pts, OUTLINE, width + 2)
    line(d, pts, color, width)


def tiny_gem16(d, cx, cy, color):
    dark = darken(color, 0.36)
    light = lighten(color, 0.42)
    poly(d, [(cx, cy - 3), (cx + 3, cy), (cx, cy + 3), (cx - 3, cy)], OUTLINE)
    poly(d, [(cx, cy - 2), (cx + 2, cy), (cx, cy + 2), (cx - 2, cy)], dark)
    poly(d, [(cx, cy - 1), (cx + 1, cy), (cx, cy + 1), (cx - 1, cy)], color)
    d.point((cx - 1, cy - 1), fill=light)


def draw_upgrade_tiers():
    img = icon_canvas()
    d = ImageDraw.Draw(img)
    leather = c("#a0703c")
    poly(d, [(6, 2), (10, 2), (12, 4), (12, 7), (13, 9), (11, 12), (8, 13), (5, 12), (3, 10), (4, 7), (3, 5)], OUTLINE)
    poly(d, [(6, 3), (10, 3), (11, 4), (11, 7), (12, 9), (10, 11), (8, 12), (5, 11), (4, 10), (5, 7), (4, 5)], leather)
    line(d, [(5, 4), (10, 11)], lighten(leather, 0.36))
    line(d, [(10, 4), (5, 11)], darken(leather, 0.32))
    rect(d, (7, 7, 8, 8), GOLD_DARK)
    px(img, 8, 7, GOLD)
    save_icon(img, TEXTURES / "upgrades/base.png")

    for tier in ("copper", "iron", "gold", "diamond", "emerald", "netherite"):
        img = icon_canvas()
        d = ImageDraw.Draw(img)
        color = c(TIER[tier][0])
        draw_tier_gem_16(d, color)
        line(d, [(4, 13), (8, 13), (8, 12), (12, 14)], OUTLINE)
        line(d, [(5, 12), (8, 12), (8, 11)], lighten(color, 0.34))
        save_icon(img, TEXTURES / f"upgrades/{tier}.png")


def glyph_arrow_bag16(d, color):
    glyph_bag16(d, 7, 6, MID)
    outlined_line16(d, [(2, 5), (7, 5)], color)
    poly(d, [(8, 5), (5, 2), (5, 8)], OUTLINE)
    poly(d, [(7, 5), (6, 4), (6, 6)], color)
    d.point((3, 4), fill=lighten(color, 0.35))


def glyph_magnet16(d, color):
    line(d, [(5, 4), (5, 10), (7, 12), (9, 12), (11, 10), (11, 4)], OUTLINE, 2)
    line(d, [(5, 4), (5, 7)], c("#d94b42"), 2)
    line(d, [(11, 4), (11, 7)], c("#417bd8"), 2)
    d.point((8, 6), fill=GOLD)
    d.point((4, 11), fill=GOLD)
    d.point((12, 11), fill=GOLD)


def glyph_filter16(d, color):
    poly(d, [(3, 4), (13, 4), (10, 8), (10, 11), (7, 13), (7, 8)], OUTLINE)
    poly(d, [(4, 5), (12, 5), (9, 8), (9, 10), (8, 11), (8, 8)], c("#f1d38a"))
    line(d, [(5, 5), (11, 5)], PAPER_LIGHT)


def glyph_void16(d, color):
    rect(d, (4, 4, 11, 11), c("#1b1027"))
    rect(d, (5, 5, 10, 10), c("#0b0612"))
    line(d, [(5, 8), (7, 5), (10, 6)], color)
    line(d, [(6, 11), (10, 9)], c("#b978ff"))


def glyph_transfer16(d, color, mode):
    glyph_bag16(d, 1, 7, MID)
    rect(d, (10, 7, 14, 11), OUTLINE)
    rect(d, (11, 8, 13, 10), c("#8a552e"))
    rect(d, (11, 7, 13, 8), HIGHLIGHT)
    if mode == "refill":
        flow = c("#42d7d7")
        outlined_line16(d, [(13, 5), (8, 5), (8, 8)], flow)
        poly(d, [(6, 8), (9, 6), (9, 10)], OUTLINE)
        poly(d, [(7, 8), (9, 7), (9, 9)], flow)
    elif mode == "restock":
        flow = c("#54d36b")
        outlined_line16(d, [(3, 5), (9, 5), (9, 8)], flow)
        poly(d, [(12, 8), (9, 6), (9, 10)], OUTLINE)
        poly(d, [(11, 8), (9, 7), (9, 9)], flow)
    else:
        outlined_line16(d, [(4, 13), (11, 13), (11, 11)], GOLD)
        poly(d, [(13, 13), (10, 10), (10, 14)], OUTLINE)
        poly(d, [(12, 13), (11, 12), (11, 14)], GOLD)


def glyph_grid16(d, color):
    wood = c("#8a552e")
    rect(d, (3, 4, 12, 12), OUTLINE)
    rect(d, (4, 5, 11, 11), wood)
    rect(d, (4, 5, 11, 6), HIGHLIGHT)
    line(d, [(6, 5), (6, 11)], darken(wood, 0.28))
    line(d, [(9, 5), (9, 11)], darken(wood, 0.28))
    line(d, [(4, 8), (11, 8)], darken(wood, 0.28))
    d.point((5, 6), fill=PAPER_LIGHT)
    outlined_line16(d, [(12, 7), (14, 8)], color)


def glyph_compacting16(d, color):
    rect(d, (4, 3, 11, 5), OUTLINE)
    rect(d, (5, 4, 10, 5), IRON)
    rect(d, (6, 6, 9, 8), DARK_IRON)
    rect(d, (4, 10, 12, 12), OUTLINE)
    rect(d, (5, 10, 11, 11), IRON)
    for x, y in ((3, 6), (11, 6), (6, 9)):
        rect(d, (x, y, x + 1, y + 1), PAPER_LIGHT)
    outlined_line16(d, [(5, 14), (9, 14), (9, 12)], color)


def glyph_furnace16(d, color, kind):
    rect(d, (4, 3, 11, 12), OUTLINE)
    rect(d, (5, 4, 10, 11), DARK_IRON)
    rect(d, (5, 4, 10, 6), IRON)
    rect(d, (5, 8, 10, 11), c("#1c1611"))
    if kind == "smoking":
        line(d, [(6, 4), (5, 2), (7, 1)], c("#c8c8c8"))
        line(d, [(9, 4), (8, 2), (10, 1)], c("#b0b0b0"))
    elif kind == "blasting":
        d.point((3, 5), fill=IRON)
        d.point((12, 5), fill=IRON)
    poly(d, [(8, 7), (5, 12), (11, 12)], c("#d9552b"))
    poly(d, [(8, 8), (7, 12), (10, 12)], GOLD)


def glyph_stack16(d, color, slug):
    levels = {
        "stack_starter": 2, "stack_tier_1": 3, "stack_tier_2": 4,
        "stack_tier_3": 5, "stack_tier_4": 6, "stack_omega": 6,
        "stack_downgrade_1": 4, "stack_downgrade_2": 3, "stack_downgrade_3": 2,
    }.get(slug, 3)
    for i in range(levels):
        x = 4 + min(i, 4)
        y = 11 - i
        rect(d, (x, y, x + 5, y + 1), PAPER_DARK)
        rect(d, (x, y, x + 4, y), PAPER_LIGHT)
    if "downgrade" in slug:
        line(d, [(12, 4), (8, 8), (4, 4)], c("#b02e26"))
    elif slug == "stack_omega":
        line(d, [(4, 5), (6, 3), (8, 5), (10, 3), (12, 5)], c("#d06cf5"))
    else:
        line(d, [(4, 5), (8, 3), (12, 5)], c("#3bd16f"))


def glyph_tool16(d, color):
    line(d, [(4, 12), (11, 5)], OUTLINE, 2)
    line(d, [(4, 12), (11, 5)], SHADOW)
    rect(d, (10, 3, 13, 4), IRON)
    line(d, [(4, 4), (12, 12)], c("#6b4a2b"), 2)
    rect(d, (3, 3, 6, 4), color)


def glyph_food16(d, color):
    rect(d, (4, 8, 7, 10), c("#f0e6c7"))
    rect(d, (10, 5, 12, 7), OUTLINE)
    rect(d, (8, 6, 12, 10), c("#d88734"))
    rect(d, (8, 9, 11, 11), c("#8a4f2a"))
    d.point((10, 6), fill=c("#ffe0a3"))


def glyph_disc16(d, color):
    rect(d, (4, 8, 12, 12), OUTLINE)
    rect(d, (5, 9, 11, 11), c("#8a4f2a"))
    rect(d, (6, 3, 10, 7), c("#151515"))
    d.point((8, 5), fill=color)
    rect(d, (7, 10, 9, 10), GOLD)


def glyph_shield16(d, color, infinity=False):
    poly(d, [(8, 3), (12, 5), (11, 10), (8, 13), (5, 10), (4, 5)], IRON)
    poly(d, [(8, 5), (10, 6), (10, 9), (8, 11), (6, 9), (6, 6)], color)
    if infinity:
        line(d, [(5, 8), (7, 7), (9, 9), (11, 8), (9, 7), (7, 9), (5, 8)], GOLD)
    else:
        rect(d, (7, 6, 9, 10), GOLD)
        rect(d, (6, 8, 10, 8), GOLD)


def glyph_tank16(d, color, pump=False):
    rect(d, (6, 3, 10, 12), OUTLINE)
    rect(d, (7, 4, 9, 11), color)
    rect(d, (7, 4, 9, 6), c("#8ee8ff"))
    if pump:
        line(d, [(4, 12), (8, 12), (8, 14), (12, 14)], IRON)
        d.point((13, 14), fill=color)


def glyph_xp16(d, color):
    tiny_gem16(d, 5, 6, c("#80ff5a"))
    tiny_gem16(d, 11, 5, c("#b9ffd0"))
    tiny_gem16(d, 8, 11, c("#5e7c16"))
    d.point((12, 4), fill=PAPER_LIGHT)


def glyph_xp_store16(d, color):
    # Erfahrungsfläschchen (Stil wie das Alchemy-Glyph) -> "XP gespeichert".
    rect(d, (7, 3, 8, 4), OUTLINE)                              # Hals/Korken
    poly(d, [(6, 5), (9, 5), (11, 12), (4, 12)], OUTLINE)       # Glas-Outline
    poly(d, [(6, 6), (9, 6), (10, 11), (5, 11)], c("#3f6b14"))  # dunkles Glas
    poly(d, [(6, 8), (9, 8), (10, 11), (5, 11)], c("#80ff5a"))  # XP-Flüssigkeit
    line(d, [(6, 7), (7, 7)], c("#d6ffb0"))                     # Glanz
    d.point((11, 4), fill=c("#b9ffd0"))                         # XP-Funke
    d.point((12, 6), fill=PAPER_LIGHT)


def glyph_battery16(d, color):
    rect(d, (4, 6, 11, 10), OUTLINE)
    rect(d, (12, 7, 13, 9), OUTLINE)
    rect(d, (5, 7, 10, 9), c("#80c71f"))
    rect(d, (5, 7, 7, 9), c("#b9ffd0"))
    poly(d, [(9, 2), (6, 8), (9, 8), (7, 14), (12, 7), (9, 7)], OUTLINE)
    poly(d, [(9, 3), (7, 8), (10, 8), (8, 12), (11, 7), (9, 7)], GOLD)


def glyph_stonecutter16(d, color):
    rect(d, (5, 4, 11, 10), IRON)
    rect(d, (7, 6, 9, 8), DARK_IRON)
    line(d, [(8, 3), (8, 11)], PAPER_LIGHT)
    line(d, [(4, 7), (12, 7)], PAPER_LIGHT)
    rect(d, (5, 11, 11, 12), SHADOW)


def glyph_anvil16(d, color):
    rect(d, (4, 8, 12, 10), OUTLINE)
    rect(d, (6, 6, 10, 8), IRON)
    rect(d, (3, 5, 8, 6), IRON)
    rect(d, (7, 10, 8, 12), OUTLINE)
    rect(d, (5, 13, 11, 13), OUTLINE)


def glyph_alchemy16(d, color):
    rect(d, (7, 3, 8, 6), IRON)
    poly(d, [(6, 7), (9, 7), (11, 12), (4, 12)], OUTLINE)
    poly(d, [(6, 8), (9, 8), (10, 11), (5, 11)], color)
    line(d, [(5, 10), (10, 10)], lighten(color, 0.42))
    d.point((11, 5), fill=c("#d06cf5"))


def glyph_ender_link16(d, color):
    rect(d, (4, 7, 12, 12), OUTLINE)
    rect(d, (5, 8, 11, 11), c("#251831"))
    rect(d, (7, 6, 9, 12), DARK_IRON)
    poly(d, [(8, 3), (11, 5), (8, 7), (5, 5)], c("#56e0c7"))
    d.point((8, 5), fill=c("#0b4f56"))


def glyph_recall16(d, color):
    glyph_bag16(d, 6, 7, MID)
    line(d, [(4, 6), (6, 4), (10, 4), (12, 6)], color)
    line(d, [(4, 6), (4, 3)], color)
    line(d, [(4, 6), (7, 6)], color)


def glyph_trash16(d, color):
    rect(d, (5, 6, 11, 12), OUTLINE)
    rect(d, (6, 7, 10, 11), DARK_IRON)
    rect(d, (4, 5, 12, 5), OUTLINE)
    rect(d, (6, 3, 10, 4), OUTLINE)
    line(d, [(7, 8), (7, 11)], IRON)
    line(d, [(9, 8), (9, 11)], IRON)
    poly(d, [(5, 13), (8, 10), (11, 13)], c("#d9552b"))
    poly(d, [(7, 13), (8, 11), (10, 13)], GOLD)


def draw_function(slug, color, kind, advanced):
    img, d, col = tiny_frame(color)
    if kind == "pickup":
        glyph_arrow_bag16(d, col)
    elif kind == "magnet":
        glyph_magnet16(d, col)
    elif kind == "filter":
        glyph_filter16(d, col)
    elif kind == "void":
        glyph_void16(d, col)
    elif kind in ("refill", "restock", "deposit"):
        glyph_transfer16(d, col, kind)
    elif kind == "crafting":
        glyph_grid16(d, col)
    elif kind == "compacting":
        glyph_compacting16(d, col)
    elif kind in ("smelting", "auto_smelting"):
        glyph_furnace16(d, col, "smelting")
    elif kind in ("smoking", "auto_smoking"):
        glyph_furnace16(d, col, "smoking")
    elif kind in ("blasting", "auto_blasting"):
        glyph_furnace16(d, col, "blasting")
    elif kind == "stonecutter":
        glyph_stonecutter16(d, col)
    elif kind == "anvil":
        glyph_anvil16(d, col)
    elif kind == "smithing":
        glyph_tool16(d, col)
    elif kind == "alchemy":
        glyph_alchemy16(d, col)
    elif kind == "stack":
        glyph_stack16(d, col, slug)
    elif kind == "feeding":
        glyph_food16(d, col)
    elif kind == "tool_swapper":
        glyph_tool16(d, col)
    elif kind == "jukebox":
        glyph_disc16(d, col)
    elif kind == "inception":
        glyph_bag16(d, 3, 4, MID)
        glyph_bag16(d, 7, 7, HIGHLIGHT)
    elif kind == "everlasting":
        glyph_shield16(d, col)
    elif kind == "infinity":
        glyph_shield16(d, col, True)
    elif kind == "tank":
        glyph_tank16(d, col)
    elif kind == "pump":
        glyph_tank16(d, col, True)
    elif kind == "xp_pump":
        glyph_xp16(d, col)
    elif kind == "xp_store":
        glyph_xp_store16(d, col)
    elif kind == "battery":
        glyph_battery16(d, col)
    elif kind == "ender_link":
        glyph_ender_link16(d, col)
    elif kind == "recall":
        glyph_recall16(d, col)
    elif kind == "trash":
        glyph_trash16(d, col)

    if advanced:
        mark_advanced_16(img)
    if kind.startswith("auto_"):
        mark_auto_16(img)
    save_icon(img, TEXTURES / f"upgrades/functions/{slug}.png")


def nearest_dye_name(hex_color):
    rr, gg, bb, _ = c(hex_color)
    best = "white"
    best_score = 10**9
    for name, dye in DYES.items():
        dr, dg, db, _ = c(dye)
        score = (rr - dr) ** 2 + (gg - dg) ** 2 + (bb - db) ** 2
        if score < best_score:
            best = name
            best_score = score
    return best


def model_palette(path, base_hex, accent=False):
    base = c(base_hex)
    light = lighten(base, 0.32 if accent else 0.24)
    mid = lighten(base, 0.10 if accent else 0.06)
    dark = darken(base, 0.34 if accent else 0.28)
    img = Image.new("RGBA", (16, 16), mid)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=mid)
    d.rectangle((0, 13, 15, 15), fill=dark)
    d.rectangle((0, 0, 15, 2), fill=light)
    for x in range(0, 16, 4):
        d.point((x, 3), fill=light)
        d.point((x + 2, 12), fill=dark)
    img.save(path, optimize=True)


def model_face(texture, uv, tint=False):
    f = {"uv": uv, "texture": texture}
    if tint:
        f["tintindex"] = 0
    return f


def model_cuboid(start, end, texture, uv, side_uv=None, top_uv=None, bottom_texture=None, bottom_uv=None, tint=False):
    side_uv = side_uv or uv
    top_uv = top_uv or side_uv
    bottom_uv = bottom_uv or top_uv
    bottom_texture = bottom_texture or texture
    # tint: alle Flächen, die die Haupt-Textur nutzen, werden über die Leder-Farbe
    # eingefärbt (tintindex 0) – die Bodenfläche (bottom_texture) bleibt ungetönt.
    return {
        "from": start,
        "to": end,
        "faces": {
            "north": model_face(texture, uv, tint),
            "south": model_face(texture, uv, tint),
            "east": model_face(texture, side_uv, tint),
            "west": model_face(texture, side_uv, tint),
            "up": model_face(texture, top_uv, tint),
            "down": model_face(bottom_texture, bottom_uv, tint and bottom_texture == texture),
        },
    }


def write_placed_model(path, tier, accent):
    model = {
        "credit": "Generated by yourShika Backpacks",
        "textures": {
            "particle": f"ysbp/placed/main/{tier}",
            "main": f"ysbp/placed/main/{tier}",
            "accent": f"ysbp/placed/accent/{accent}",
            "detail": f"ysbp/placed/detail/{tier}",
        },
        "elements": [
            model_cuboid(
                [3, 0, 5], [13, 12, 13],
                "#main", [3, 4, 13, 16],
                side_uv=[5, 4, 13, 16],
                top_uv=[3, 5, 13, 13],
                bottom_texture="#detail",
                bottom_uv=[3, 5, 13, 13],
                tint=True,
            ),
            model_cuboid([4, 9, 4.25], [12, 13, 5.25], "#accent", [4, 2, 12, 6]),
            model_cuboid([5, 3, 3.8], [11, 8, 5.0], "#accent", [5, 8, 11, 13]),
            model_cuboid([1.5, 2, 6], [3.25, 8.5, 11.5], "#accent", [1, 8, 4, 14]),
            model_cuboid([12.75, 2, 6], [14.5, 8.5, 11.5], "#accent", [12, 8, 15, 14]),
            model_cuboid([5, 12, 7], [6.25, 16, 9], "#detail", [5, 0, 7, 4]),
            model_cuboid([9.75, 12, 7], [11, 16, 9], "#detail", [9, 0, 11, 4]),
            model_cuboid([5, 15, 7], [11, 16.5, 9], "#detail", [5, 0, 11, 2]),
            model_cuboid([7, 5, 3.4], [9, 7, 4.2], "#detail", [7, 9, 9, 11]),
        ],
        "display": {
            "ground": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 0],
                "scale": [0.9, 0.9, 0.9],
            },
            "fixed": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [1.0, 1.0, 1.0],
            },
            "gui": {
                "rotation": [30, 225, 0],
                "translation": [0, 0, 0],
                "scale": [0.75, 0.75, 0.75],
            },
        },
    }
    path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def generate_placed_models():
    for tier, (main, accent, _cmd, _name) in TIER.items():
        # Hauptkörper neutral/hell, damit ihn die Leder-Hauptfarbe (tintindex 0)
        # auf die gewählte Farbe einfärbt – statt die Tier-Farbe fest einzubacken.
        model_palette(TEXTURES / f"placed/main/{tier}.png", "#ffffff")
        model_palette(TEXTURES / f"placed/detail/{tier}.png", accent, True)
    for dye, dye_hex in DYES.items():
        model_palette(TEXTURES / f"placed/accent/{dye}.png", dye_hex, True)

    model_root = ORAXEN / "pack/models/ysbp/placed"
    for tier, (_main, accent_hex, _cmd, _name) in TIER.items():
        default = nearest_dye_name(accent_hex)
        write_placed_model(model_root / f"{tier}_{default}.json", tier, default)
        for dye in DYES:
            write_placed_model(model_root / f"{tier}_{dye}.json", tier, dye)


def write_core_yaml():
    lines = []
    dye_keys = list(DYES.keys())
    for tier_index, (key, (_main, _accent, cmd, display)) in enumerate(TIER.items()):
        item_id = f"ysbp_{key}_backpack"
        default_dye = nearest_dye_name(_accent)
        lines.extend([
            f"{item_id}:",
            f"  displayname: \"<#A0703C>{display}\"" if key == "leather" else f"  displayname: \"<{TIER[key][0]}>{display}\"",
            "  material: LEATHER_HORSE_ARMOR",
            "  color: 255, 255, 255",
            "  Pack:",
            "    generate_model: true",
            "    parent_model: \"item/generated\"",
            "    textures:",
            f"      - ysbp/backpacks/{key}_base.png",
            f"      - ysbp/backpacks/{key}_accent.png",
            f"      - ysbp/backpacks/{key}_overlay.png",
            f"    custom_model_data: {cmd}",
            "",
        ])
        for dye_index, dye in enumerate(dye_keys):
            variant_cmd = 11000 + tier_index * 100 + dye_index
            lines.extend([
                f"{item_id}_accent_{dye}:",
                f"  displayname: \"<{TIER[key][0]}>{display} ({dye.replace('_', ' ')})\"",
                "  material: LEATHER_HORSE_ARMOR",
                "  color: 255, 255, 255",
                "  Pack:",
                "    generate_model: true",
                "    parent_model: \"item/generated\"",
                "    textures:",
                f"      - ysbp/backpacks/{key}_base.png",
                f"      - ysbp/backpacks/accents/{key}_{dye}.png",
                f"      - ysbp/backpacks/{key}_overlay.png",
                f"    custom_model_data: {variant_cmd}",
                "",
            ])
        lines.extend([
            f"{item_id}_placed:",
            f"  displayname: \"<{TIER[key][0]}>{display} (placed visual)\"",
            "  material: LEATHER_HORSE_ARMOR",
            "  Pack:",
            "    generate_model: false",
            f"    model: ysbp/placed/{key}_{default_dye}.json",
            f"    custom_model_data: {12000 + tier_index * 100}",
            "",
        ])
        for dye_index, dye in enumerate(dye_keys):
            lines.extend([
                f"{item_id}_placed_accent_{dye}:",
                f"  displayname: \"<{TIER[key][0]}>{display} placed ({dye.replace('_', ' ')})\"",
                "  material: LEATHER_HORSE_ARMOR",
                "  Pack:",
                "    generate_model: false",
                f"    model: ysbp/placed/{key}_{dye}.json",
                f"    custom_model_data: {12001 + tier_index * 100 + dye_index}",
                "",
            ])
    upgrades = [
        ("base", "Upgrade-Leder", "#A0703C", "LEATHER", 2000),
        ("copper", "Copper-Upgrade", "#C87B53", "PAPER", 2001),
        ("iron", "Iron-Upgrade", "#D8D8D8", "PAPER", 2002),
        ("gold", "Gold-Upgrade", "#F2D14E", "PAPER", 2003),
        ("diamond", "Diamond-Upgrade", "#5BE8D4", "PAPER", 2004),
        ("emerald", "Emerald-Upgrade", "#3BD16F", "PAPER", 2005),
        ("netherite", "Netherite-Upgrade", "#6E5BC8", "PAPER", 2006),
    ]
    for slug, display, color, material, cmd in upgrades:
        lines.extend([
            f"ysbp_upgrade_{slug}:",
            f"  displayname: \"<{color}><bold>{display}</bold>\"",
            f"  material: {material}",
            "  Pack:",
            "    generate_model: true",
            "    parent_model: \"item/generated\"",
            "    textures:",
            f"      - ysbp/upgrades/{slug}.png",
            f"    custom_model_data: {cmd}",
            "",
        ])
    (ITEMS / "yourshika_backpacks.yml").write_text("\n".join(lines), encoding="utf-8")


def write_function_yaml():
    lines = [
        "# Prepared functional backpack upgrade items.",
        "# Texture/model providers only; gameplay logic can bind to these IDs later.",
        "",
    ]
    for slug, display, color, _kind, _adv, cmd in FUNCTIONS:
        lines.extend([
            f"ysbp_upgrade_{slug}:",
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
    (ITEMS / "yourshika_function_upgrades.yml").write_text("\n".join(lines), encoding="utf-8")


def write_manifest():
    lines = [
        "# Generated by scripts/generate_oraxen_assets.py",
        "asset-version=6",
    ]
    paths = sorted((ORAXEN / "items").glob("*.yml"))
    paths += sorted((ORAXEN / "pack/textures").rglob("*.png"))
    paths += sorted((ORAXEN / "pack/models").rglob("*.json"))
    resource_root = ROOT / "src/main/resources"
    for path in paths:
        rel = path.relative_to(resource_root).as_posix()
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        lines.append(f"sha256.{rel}={digest}")
    (ORAXEN / "asset-manifest.properties").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    ensure_dirs()
    ensure_backpack_assets()
    draw_upgrade_tiers()
    generate_functions()
    generate_placed_models()
    write_core_yaml()
    write_function_yaml()
    write_manifest()


if __name__ == "__main__":
    main()
