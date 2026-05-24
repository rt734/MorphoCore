"""
Generate MorphoCore launcher icon PNGs at all required Android mipmap densities.
Uses only Python stdlib (struct + zlib) — no Pillow or other dependencies.

Run from repo root:
    python tools/gen_app_icons.py

Output:
    app/src/main/res/mipmap-mdpi/ic_launcher.png      (48×48)
    app/src/main/res/mipmap-hdpi/ic_launcher.png      (72×72)
    app/src/main/res/mipmap-xhdpi/ic_launcher.png     (96×96)
    app/src/main/res/mipmap-xxhdpi/ic_launcher.png    (144×144)
    app/src/main/res/mipmap-xxxhdpi/ic_launcher.png   (192×192)
    (and matching ic_launcher_round.png at each density — same image)
"""

import math
import os
import struct
import zlib


# Brand palette
BG = (13, 13, 13)           # #0D0D0D  near-black background
RING = (0, 191, 255)        # #00BFFF  cyan accent ring
FIGURE = (255, 255, 255)    # #FFFFFF  white figure
ACCENT = (0, 191, 255)      # #00BFFF  cyan M lettermark

DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}


# ── PNG encoding ──────────────────────────────────────────────────────────────

def _chunk(tag: bytes, data: bytes) -> bytes:
    c = struct.pack(">I", len(data)) + tag + data
    return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def encode_png(pixels: list[list[tuple[int, int, int]]], size: int) -> bytes:
    raw = b""
    for row in pixels:
        raw += b"\x00"  # filter byte: None
        for r, g, b in row:
            raw += bytes([r, g, b])
    signature = b"\x89PNG\r\n\x1a\n"
    ihdr = _chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0))
    idat = _chunk(b"IDAT", zlib.compress(raw, 9))
    iend = _chunk(b"IEND", b"")
    return signature + ihdr + idat + iend


# ── Drawing primitives ────────────────────────────────────────────────────────

def _blend(canvas: list[list[tuple]], x: int, y: int, color: tuple, alpha: float):
    if x < 0 or y < 0 or x >= len(canvas[0]) or y >= len(canvas):
        return
    r0, g0, b0 = canvas[y][x]
    r1, g1, b1 = color
    canvas[y][x] = (
        int(r0 + (r1 - r0) * alpha),
        int(g0 + (g1 - g0) * alpha),
        int(b0 + (b1 - b0) * alpha),
    )


def fill(canvas, color):
    for row in canvas:
        for x in range(len(row)):
            row[x] = color


def draw_circle(canvas, cx, cy, r, color, thickness=1.0, aa=True):
    """Anti-aliased filled or stroke circle."""
    size = len(canvas)
    for y in range(max(0, int(cy - r - 2)), min(size, int(cy + r + 2))):
        for x in range(max(0, int(cx - r - 2)), min(size, int(cx + r + 2))):
            dist = math.hypot(x - cx, y - cy)
            inner = r - thickness
            if aa:
                if inner <= dist <= r:
                    _blend(canvas, x, y, color, 1.0)
                elif dist < inner:
                    pass
                elif r < dist <= r + 1:
                    _blend(canvas, x, y, color, r + 1 - dist)
                elif inner - 1 < dist < inner:
                    _blend(canvas, x, y, color, dist - (inner - 1))
            else:
                if inner <= dist <= r:
                    canvas[y][x] = color


def draw_filled_circle(canvas, cx, cy, r, color):
    size = len(canvas)
    for y in range(max(0, int(cy - r - 1)), min(size, int(cy + r + 2))):
        for x in range(max(0, int(cx - r - 1)), min(size, int(cx + r + 2))):
            dist = math.hypot(x - cx, y - cy)
            if dist < r:
                _blend(canvas, x, y, color, 1.0)
            elif dist < r + 1:
                _blend(canvas, x, y, color, r + 1 - dist)


def draw_rect(canvas, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(canvas), y1)):
        for x in range(max(0, x0), min(len(canvas[0]), x1)):
            canvas[y][x] = color


def draw_line(canvas, x0, y0, x1, y1, color, width=1):
    """Bresenham + width."""
    dx, dy = abs(x1 - x0), abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    hw = max(1, width // 2)
    while True:
        for oy in range(-hw, hw + 1):
            for ox in range(-hw, hw + 1):
                nx, ny = x0 + ox, y0 + oy
                if 0 <= nx < len(canvas[0]) and 0 <= ny < len(canvas):
                    canvas[ny][nx] = color
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy


# ── Icon design ───────────────────────────────────────────────────────────────

def render_icon(size: int) -> list[list[tuple]]:
    """
    MorphoCore icon: dark background, cyan ring, white stick figure in
    fighting stance, 'M' lettermark in lower third.

    All coordinates are expressed as fractions of `size` so the design
    scales cleanly across all densities.
    """
    s = size
    canvas = [[(0, 0, 0)] * s for _ in range(s)]
    fill(canvas, BG)

    cx, cy = s / 2, s / 2

    # Outer ring
    ring_r = s * 0.44
    ring_t = max(1.5, s * 0.04)
    draw_circle(canvas, cx, cy, ring_r, RING, thickness=ring_t)

    # ── Stick figure in fighting stance ──
    head_r = s * 0.075
    head_cx = cx - s * 0.03
    head_cy = s * 0.25
    draw_filled_circle(canvas, head_cx, head_cy, head_r, FIGURE)

    # Torso
    torso_x0 = int(cx - s * 0.04)
    torso_x1 = int(cx + s * 0.04)
    torso_y0 = int(head_cy + head_r + s * 0.01)
    torso_y1 = int(torso_y0 + s * 0.20)
    draw_rect(canvas, torso_x0, torso_y0, torso_x1, torso_y1, FIGURE)

    # Lead arm extended (left arm punching forward-up)
    lw = max(2, int(s * 0.04))
    draw_line(canvas,
              int(cx - s * 0.02), int(torso_y0 + s * 0.04),
              int(cx - s * 0.22), int(torso_y0 - s * 0.06),
              FIGURE, width=lw)

    # Rear arm guard (right arm at cheek)
    draw_line(canvas,
              int(cx + s * 0.02), int(torso_y0 + s * 0.03),
              int(cx + s * 0.14), int(head_cy + s * 0.02),
              RING, width=lw)

    # Lead leg forward
    draw_line(canvas,
              int(cx), int(torso_y1),
              int(cx - s * 0.12), int(torso_y1 + s * 0.18),
              FIGURE, width=lw)

    # Rear leg back
    draw_line(canvas,
              int(cx), int(torso_y1),
              int(cx + s * 0.12), int(torso_y1 + s * 0.18),
              FIGURE, width=lw)

    # ── 'M' lettermark ──
    m_t = max(1, int(s * 0.035))   # stroke width
    m_y0 = int(s * 0.76)
    m_y1 = int(s * 0.88)
    m_x0 = int(cx - s * 0.14)
    m_x1 = int(cx + s * 0.14)
    m_mid = int(cy + s * 0.34)
    # Left vertical
    draw_line(canvas, m_x0, m_y0, m_x0, m_y1, RING, width=m_t)
    # Right vertical
    draw_line(canvas, m_x1, m_y0, m_x1, m_y1, RING, width=m_t)
    # Left diagonal down to midpoint
    draw_line(canvas, m_x0, m_y0, int(cx), m_mid, RING, width=m_t)
    # Right diagonal down to midpoint
    draw_line(canvas, m_x1, m_y0, int(cx), m_mid, RING, width=m_t)

    return canvas


# ── Entry point ───────────────────────────────────────────────────────────────

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    res_root = os.path.join(repo_root, "app", "src", "main", "res")

    for density, size in DENSITIES.items():
        canvas = render_icon(size)
        png_data = encode_png(canvas, size)

        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            out_dir = os.path.join(res_root, density)
            os.makedirs(out_dir, exist_ok=True)
            out_path = os.path.join(out_dir, name)
            with open(out_path, "wb") as f:
                f.write(png_data)
            print(f"  created: {density}/{name}  ({size}×{size})")

    print("\nDone.")


if __name__ == "__main__":
    main()
