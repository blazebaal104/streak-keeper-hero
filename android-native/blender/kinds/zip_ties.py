"""Three black nylon zip ties cinched over the button (straps across the top, tucked under the rim). Taps: snip one at a time."""
import math
from kit import *

STEPS = 3


def tie(y, ang, m):
    c, s = math.cos(ang), math.sin(ang)
    def P(x, yy, z): return (x * c - yy * s, x * s + yy * c, z)
    # strap arching over the button, tucking under at both sides
    pts = [P(-1.25, y, 0.0), P(-1.05, y, 0.1), P(-0.6, y, 0.17), P(0.0, y, 0.19), P(0.6, y, 0.17), P(1.05, y, 0.1), P(1.25, y, 0.0)]
    o = curve_tube(pts, 0.06, m, "tie"); o.scale = (1, 1, 1)
    o.data.bevel_depth = 0.0; o.data.extrude = 0.012
    # flat strap: use a rectangular profile via bevel object-less extrude width
    o.data.offset = 0; o.data.bevel_depth = 0.05; o.data.bevel_resolution = 0
    # ratchet head + the long cut tail
    hx, hy, hz = P(0.55, y, 0.24)
    cube((0.2, 0.17, 0.11), (hx, hy, hz), rot=(0, 0, ang), mat=m, bevel=0.02)
    tx, ty, tz = P(0.85, y - 0.12, 0.26)
    cube((0.5, 0.1, 0.022), (tx, ty, tz), rot=(0, math.radians(-10), ang - 0.25), mat=m, bevel=0.005)


def build(step):
    m = plastic("nylon", (0.02, 0.02, 0.022), 0.32)
    for i, (y, a) in enumerate(((-0.55, -8), (0.0, 4), (0.55, -3))):
        if step > i:
            continue
        tie(y, math.radians(a), m)
