"""Bike U-lock clamped over the button. Taps: key in + turn, shackle pulled out, remove."""
import math
from kit import *

STEPS = 3


def build(step):
    rubber = plastic("rubber", (0.025, 0.025, 0.027), 0.55)
    bar_m = plastic("bar", (0.03, 0.03, 0.035), 0.3)
    yellow = plastic("label", (0.95, 0.75, 0.05), 0.4)
    ch = chrome()
    out = 0.55 if step >= 2 else 0.0
    # shackle: U of thick rubber-coated steel
    pts = [(-1.0, 1.3 + out, 0.25), (-1.0, 0.2 + out, 0.25), (-0.95, -0.55 + out, 0.25), (-0.5, -1.15 + out, 0.25), (0.0, -1.3 + out, 0.25),
           (0.5, -1.15 + out, 0.25), (0.95, -0.55 + out, 0.25), (1.0, 0.2 + out, 0.25), (1.0, 1.3 + out, 0.25)]
    curve_tube(pts, 0.13, rubber, "shackle")
    # crossbar with end caps and the key cylinder
    cyl(0.2, 2.7, (0, 1.3, 0.3), rot=(0, math.radians(90), 0), mat=bar_m)
    cube((1.0, 0.25, 0.42), (0, 1.3, 0.3), mat=yellow, bevel=0.03)
    text_obj("KRYPTO", (0, 1.18, 0.51), 0.14, plastic("ink", (0.02, 0.02, 0.02), 0.5))
    for sx in (-1, 1):
        cyl(0.24, 0.12, (sx * 1.38, 1.3, 0.3), rot=(0, math.radians(90), 0), mat=ch)
    cyl(0.07, 0.03, (1.45, 1.3, 0.3), rot=(0, math.radians(90), 0), mat=plastic("kh", (0, 0, 0), 0.8))
    if step >= 1:
        cube((0.3, 0.05, 0.1), (1.6, 1.3, 0.3), rot=(math.radians(90), 0, 0), mat=ch, bevel=0.01)
        cube((0.1, 0.32, 0.32), (1.8, 1.3, 0.3), rot=(math.radians(90), 0, 0), mat=plastic("keyhead", (0.05, 0.05, 0.05), 0.4), bevel=0.05)
