"""Cardboard box taped shut over the button. Taps: slit the tape, open flap 1, open flap 2, remove."""
import math
from kit import *

STEPS = 4


def build(step):
    card = cardboard("card")
    tape = plastic("packtape", (0.55, 0.36, 0.14), 0.25)
    ink = plastic("ink", (0.55, 0.03, 0.03), 0.6)
    H = 0.9
    cube((2.8, 2.6, H), (0, 0, H / 2), mat=card, bevel=0.02)
    l_open = step >= 2; r_open = step >= 3
    if l_open or r_open:
        inside = cube((2.7, 2.5, 0.02), (0, 0, 0.1), mat=emissive("glow", (1.0, 0.35, 0.05), 1.4))
    # flaps (hinged at left/right edges)
    for side, opened in ((-1, l_open), (1, r_open)):
        f = cube((1.4, 2.6, 0.03), (0, 0, 0), mat=card, bevel=0.005)
        if opened:
            a = math.radians(135)
            f.location = (side * (1.4 + math.cos(a) * -0.7), 0, H + math.sin(a) * 0.7)
            f.rotation_euler = (0, side * a, 0)
        else:
            f.location = (side * 0.7, 0, H + 0.015)
    if not l_open and not r_open:
        cut = step >= 1
        # tape along the seam, down the sides
        if not cut:
            cube((0.36, 2.9, 0.008), (0, 0, H + 0.035), mat=tape)
        else:
            cube((0.17, 2.9, 0.008), (-0.1, 0, H + 0.035), mat=tape)
            cube((0.17, 2.6, 0.008), (0.11, -0.12, H + 0.036), rot=(0, 0, 0.02), mat=tape)
            # a peeled corner
            cube((0.17, 0.35, 0.008), (0.12, 1.32, H + 0.12), rot=(math.radians(-35), 0, 0), mat=tape)
        text_obj("FRAGILE", (-0.75, -0.75, H + 0.035), 0.24, ink, rot=(0, 0, math.radians(8)))
        text_obj("↑↑", (0.75, 0.8, H + 0.035), 0.3, plastic("blk", (0.05, 0.05, 0.05), 0.6))
