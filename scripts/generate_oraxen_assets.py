from pathlib import Path
import hashlib

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
        TEXTURES / "upgrades",
        TEXTURES / "upgrades/functions",
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


def parchment():
    img = Image.new("RGBA", (16, 16), ALPHA)
    d = ImageDraw.Draw(img)
    rect(d, (4, 1, 11, 14), PAPER_DARK)
    rect(d, (3, 3, 12, 12), PAPER_DARK)
    rect(d, (4, 2, 11, 13), PAPER)
    rect(d, (5, 2, 10, 4), PAPER_LIGHT)
    rect(d, (5, 11, 11, 13), (160, 123, 68, 255))
    px(img, 11, 2, PAPER_LIGHT)
    px(img, 12, 3, PAPER_DARK)
    return img


def draw_gem(d, color, x=8, y=7):
    dark = mix(color, (0, 0, 0, 255), 0.48)
    light = mix(color, (255, 255, 255, 255), 0.36)
    poly(d, [(x, y - 4), (x + 4, y), (x + 2, y + 5), (x - 2, y + 5), (x - 4, y)], dark)
    poly(d, [(x, y - 3), (x + 3, y), (x + 1, y + 3), (x - 1, y + 3), (x - 3, y)], color)
    rect(d, (x - 1, y - 1, x - 1, y - 1), light)


def draw_upgrade_tiers():
    base = Image.new("RGBA", (16, 16), ALPHA)
    d = ImageDraw.Draw(base)
    rect(d, (4, 2, 11, 13), OUTLINE)
    rect(d, (3, 5, 12, 10), OUTLINE)
    rect(d, (4, 3, 11, 12), MID)
    rect(d, (5, 3, 10, 5), HIGHLIGHT)
    rect(d, (5, 10, 11, 12), SHADOW)
    line(d, [(4, 3), (11, 12)], PAPER_LIGHT)
    line(d, [(11, 3), (4, 12)], PAPER_LIGHT)
    save64(base, TEXTURES / "upgrades/base.png")

    for tier in ("copper", "iron", "gold", "diamond", "emerald", "netherite"):
        img = parchment()
        d = ImageDraw.Draw(img)
        draw_gem(d, c(TIER[tier][0]))
        line(d, [(5, 13), (8, 13), (8, 12), (11, 13), (8, 14)], OUTLINE)
        save64(img, TEXTURES / f"upgrades/{tier}.png")


def add_advanced(img):
    d = ImageDraw.Draw(img)
    px(img, 12, 2, GOLD_DARK)
    px(img, 13, 2, GOLD)
    px(img, 12, 3, GOLD)
    px(img, 13, 3, PAPER_LIGHT)


def add_auto(img):
    d = ImageDraw.Draw(img)
    line(d, [(12, 2), (10, 7), (13, 7), (11, 13)], GOLD)
    px(img, 11, 8, GOLD_DARK)


def scroll_icon(color="#58c96a"):
    img = parchment()
    return img, ImageDraw.Draw(img), c(color)


def glyph_arrow_bag(d, color):
    rect(d, (5, 8, 10, 12), OUTLINE)
    rect(d, (6, 8, 9, 11), MID)
    line(d, [(2, 5), (8, 5), (8, 4), (12, 7), (8, 10), (8, 8), (2, 8)], color)


def glyph_magnet(d, color):
    line(d, [(4, 4), (4, 10), (6, 12), (10, 12), (12, 10), (12, 4)], DARK_IRON)
    line(d, [(4, 4), (4, 7)], c("#c13b35"))
    line(d, [(12, 4), (12, 7)], c("#3f72c4"))
    rect(d, (8, 8, 8, 8), GOLD)


def glyph_filter(d, color):
    poly(d, [(3, 4), (13, 4), (9, 8), (9, 12), (7, 13), (7, 8)], OUTLINE)
    poly(d, [(4, 5), (12, 5), (8, 8), (8, 11), (7, 12), (7, 8)], color)


def glyph_void(d, color):
    d.ellipse((3, 3, 13, 13), fill=c("#241338"))
    d.ellipse((5, 5, 11, 11), fill=c("#0a0610"))
    rect(d, (6, 5, 6, 5), color)
    rect(d, (10, 10, 10, 10), c("#b978ff"))


def glyph_transfer(d, color, mode):
    if mode == "refill":
        line(d, [(12, 5), (5, 5), (5, 4), (2, 7), (5, 10), (5, 8), (12, 8)], color)
    elif mode == "restock":
        line(d, [(3, 4), (10, 4), (10, 3), (14, 7), (10, 11), (10, 9), (3, 9)], color)
    else:
        line(d, [(3, 10), (10, 10), (10, 11), (14, 7), (10, 3), (10, 5), (3, 5)], color)
    rect(d, (4, 11, 11, 13), OUTLINE)


def glyph_grid(d, color):
    for y in (4, 7, 10):
        for x in (4, 7, 10):
            rect(d, (x, y, x + 1, y + 1), OUTLINE)
    line(d, [(12, 7), (14, 8), (12, 9)], color)


def glyph_flame(d, color, auto=False):
    rect(d, (4, 3, 12, 13), DARK_IRON)
    rect(d, (5, 4, 11, 6), IRON)
    rect(d, (5, 9, 11, 12), (28, 22, 17, 255))
    poly(d, [(8, 7), (5, 12), (11, 12)], c("#d9552b"))
    poly(d, [(8, 8), (7, 12), (10, 12)], GOLD)


def glyph_stack(d, color, slug):
    levels = {
        "stack_starter": 2, "stack_tier_1": 3, "stack_tier_2": 4,
        "stack_tier_3": 5, "stack_tier_4": 6, "stack_omega": 7,
        "stack_downgrade_1": 4, "stack_downgrade_2": 3, "stack_downgrade_3": 2,
    }[slug]
    for i in range(levels):
        x, y = 3 + i, 11 - i
        rect(d, (x, y, x + 7, y + 2), PAPER_DARK)
        rect(d, (x + 1, y, x + 7, y + 1), PAPER_LIGHT)
    if "downgrade" in slug:
        line(d, [(11, 3), (5, 9), (3, 7)], c("#b02e26"))
    elif slug == "stack_omega":
        rect(d, (12, 4, 12, 4), c("#d06cf5"))
        rect(d, (13, 5, 13, 5), c("#d06cf5"))
    else:
        line(d, [(4, 4), (8, 2), (12, 4)], c("#3bd16f"))


def glyph_tool(d, color):
    line(d, [(4, 12), (12, 4)], SHADOW)
    rect(d, (10, 2, 13, 5), IRON)
    line(d, [(3, 4), (12, 13)], c("#6b4a2b"))
    rect(d, (2, 3, 5, 5), c("#5be8d4"))


def glyph_food(d, color):
    poly(d, [(4, 5), (8, 3), (12, 5), (12, 9), (9, 12), (5, 11), (3, 8)], c("#d99238"))
    rect(d, (5, 6, 11, 9), c("#fed83d"))
    rect(d, (5, 9, 10, 11), OUTLINE)


def glyph_disc(d, color):
    d.ellipse((3, 3, 13, 13), fill=(22, 20, 19, 255))
    d.ellipse((6, 6, 10, 10), fill=color)
    rect(d, (4, 12, 12, 14), OUTLINE)


def glyph_shield(d, color, infinity=False):
    poly(d, [(8, 2), (13, 5), (12, 10), (8, 14), (4, 10), (3, 5)], IRON)
    poly(d, [(8, 4), (11, 6), (10, 10), (8, 12), (6, 10), (5, 6)], color)
    if infinity:
        line(d, [(5, 9), (7, 7), (8, 9), (9, 11), (11, 9), (9, 7), (8, 9), (7, 11), (5, 9)], GOLD)
    else:
        rect(d, (7, 6, 9, 11), GOLD)
        rect(d, (5, 8, 11, 9), GOLD)


def glyph_tank(d, color, pump=False):
    rect(d, (5, 2, 11, 13), DARK_IRON)
    rect(d, (6, 3, 10, 12), color)
    rect(d, (6, 3, 10, 6), c("#8ee8ff"))
    if pump:
        line(d, [(3, 10), (8, 10), (8, 14), (13, 14)], IRON)
        rect(d, (14, 14, 14, 14), color)


def glyph_xp(d, color):
    d.ellipse((4, 4, 8, 8), fill=c("#80ff5a"))
    d.ellipse((9, 3, 13, 7), fill=c("#b9ffd0"))
    d.ellipse((6, 9, 12, 15), fill=c("#5e7c16"))


def glyph_battery(d, color):
    rect(d, (3, 5, 12, 11), DARK_IRON)
    rect(d, (12, 7, 14, 9), DARK_IRON)
    rect(d, (4, 6, 10, 10), c("#80c71f"))
    line(d, [(9, 3), (7, 8), (10, 8), (7, 14)], GOLD)


FUNCTIONS = [
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
    ("auto_smelting", "Auto-Smelting Upgrade", "#e46b37", "auto_smelting", False),
    ("smoking", "Smoking Upgrade", "#9a7b5c", "smoking", False),
    ("auto_smoking", "Auto-Smoking Upgrade", "#9a7b5c", "auto_smoking", False),
    ("blasting", "Blasting Upgrade", "#d8d8d8", "blasting", False),
    ("auto_blasting", "Auto-Blasting Upgrade", "#d8d8d8", "auto_blasting", False),
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
    ("tool_swapper", "Tool Swapper Upgrade", "#6fa0d6", "tool_swapper", False),
    ("advanced_tool_swapper", "Advanced Tool Swapper Upgrade", "#6fa0d6", "tool_swapper", True),
    ("jukebox", "Jukebox Upgrade", "#b66f3a", "jukebox", False),
    ("advanced_jukebox", "Advanced Jukebox Upgrade", "#b66f3a", "jukebox", True),
    ("inception", "Inception Upgrade", "#a0703c", "inception", False),
    ("everlasting", "Everlasting Upgrade", "#cfcfcf", "everlasting", False),
    ("infinity_admin", "Infinity Upgrade Admin", "#6e5bc8", "infinity", False),
    ("survival_infinity", "Survival Infinity Upgrade", "#3bd16f", "infinity", False),
    ("tank", "Tank Upgrade", "#2b82c9", "tank", False),
    ("pump", "Pump Upgrade", "#2b82c9", "pump", False),
    ("advanced_pump", "Advanced Pump Upgrade", "#2b82c9", "pump", True),
    ("xp_pump", "XP Pump Upgrade", "#5bd75b", "xp_pump", False),
    ("battery", "Battery Upgrade", "#f2d14e", "battery", False),
]


def draw_function(slug, color, kind, advanced):
    img, d, col = scroll_icon(color)
    if kind == "pickup":
        glyph_arrow_bag(d, col)
    elif kind == "magnet":
        glyph_magnet(d, col)
    elif kind == "filter":
        glyph_filter(d, col)
    elif kind == "void":
        glyph_void(d, col)
    elif kind in ("refill", "restock", "deposit"):
        glyph_transfer(d, col, kind)
    elif kind == "crafting":
        glyph_grid(d, col)
    elif kind == "compacting":
        glyph_grid(d, col)
        px(img, 13, 12, IRON)
    elif kind in ("smelting", "auto_smelting"):
        glyph_flame(d, col, kind.startswith("auto"))
    elif kind in ("smoking", "auto_smoking"):
        glyph_flame(d, col, kind.startswith("auto"))
        px(img, 4, 2, (190, 190, 190, 255))
    elif kind in ("blasting", "auto_blasting"):
        glyph_flame(d, col, kind.startswith("auto"))
        px(img, 12, 4, IRON)
    elif kind == "stonecutter":
        d.ellipse((3, 3, 13, 13), fill=IRON)
        line(d, [(8, 2), (8, 14)], (245, 245, 245, 255))
        line(d, [(2, 8), (14, 8)], (245, 245, 245, 255))
    elif kind == "anvil":
        rect(d, (3, 8, 13, 10), DARK_IRON)
        rect(d, (6, 5, 11, 8), IRON)
        rect(d, (6, 10, 9, 13), DARK_IRON)
    elif kind == "smithing":
        glyph_tool(d, col)
    elif kind == "alchemy":
        poly(d, [(6, 4), (10, 4), (12, 13), (4, 13)], col)
        rect(d, (7, 2, 9, 5), IRON)
        px(img, 11, 3, c("#d06cf5"))
    elif kind == "stack":
        glyph_stack(d, col, slug)
    elif kind == "feeding":
        glyph_food(d, col)
    elif kind == "tool_swapper":
        glyph_tool(d, col)
    elif kind == "jukebox":
        glyph_disc(d, col)
    elif kind == "inception":
        glyph_arrow_bag(d, col)
        rect(d, (6, 9, 9, 11), HIGHLIGHT)
    elif kind == "everlasting":
        glyph_shield(d, col)
    elif kind == "infinity":
        glyph_shield(d, col, True)
    elif kind == "tank":
        glyph_tank(d, col)
    elif kind == "pump":
        glyph_tank(d, col, True)
    elif kind == "xp_pump":
        glyph_xp(d, col)
    elif kind == "battery":
        glyph_battery(d, col)

    if advanced:
        add_advanced(img)
    if kind.startswith("auto_"):
        add_auto(img)
    save64(img, TEXTURES / f"upgrades/functions/{slug}.png")


def generate_functions():
    for slug, _display, color, kind, advanced in FUNCTIONS:
        draw_function(slug, color, kind, advanced)


def write_core_yaml():
    lines = []
    dye_keys = list(DYES.keys())
    for tier_index, (key, (_main, _accent, cmd, display)) in enumerate(TIER.items()):
        item_id = f"ysbp_{key}_backpack"
        lines.extend([
            f"{item_id}:",
            f"  displayname: \"<#A0703C>{display}\"" if key == "leather" else f"  displayname: \"<{TIER[key][0]}>{display}\"",
            "  material: LEATHER_HORSE_ARMOR",
            "  color: 255, 255, 255",
            "  Pack:",
            "    generate_model: true",
            "    parent_model: \"ysbp:backpack\"",
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
                "    parent_model: \"ysbp:backpack\"",
                "    textures:",
                f"      - ysbp/backpacks/{key}_base.png",
                f"      - ysbp/backpacks/accents/{key}_{dye}.png",
                f"      - ysbp/backpacks/{key}_overlay.png",
                f"    custom_model_data: {variant_cmd}",
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
    for i, (slug, display, color, _kind, _adv) in enumerate(FUNCTIONS):
        lines.extend([
            f"ysbp_upgrade_{slug}:",
            f"  displayname: \"<{color}><bold>{display}</bold>\"",
            "  material: PAPER",
            "  Pack:",
            "    generate_model: true",
            "    parent_model: \"item/generated\"",
            "    textures:",
            f"      - ysbp/upgrades/functions/{slug}.png",
            f"    custom_model_data: {2100 + i}",
            "",
        ])
    (ITEMS / "yourshika_function_upgrades.yml").write_text("\n".join(lines), encoding="utf-8")


def write_manifest():
    lines = [
        "# Generated by scripts/generate_oraxen_assets.py",
        "asset-version=5",
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
    generate_backpacks()
    draw_upgrade_tiers()
    generate_functions()
    write_core_yaml()
    write_function_yaml()
    write_manifest()


if __name__ == "__main__":
    main()
