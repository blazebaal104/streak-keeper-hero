"""Thick jute rope wound twice round the button and knotted. Taps: loosen knot, untie, remove."""
import math
import random
from kit import *

STEPS = 3


def rope_curve(pts, m, r=0.085):
    o = curve_tube(pts, r, m, "rope", res=16)
    # twisted strands: screw-like bump via a displace on a converted mesh would be heavy; a twist texture in the material instead
    return o


def build(step):
    random.seed(2)
    jute = fabric("jute", (0.55, 0.40, 0.22), scale=40, rough=0.95)
    loose = 0.12 if step >= 1 else 0.0
    for k, ry in enumerate((0.32, -0.28)):
        pts = []
        for i in range(29):
            a = i / 28 * 2 * math.pi
            R = 1.12 + loose * (1 + math.sin(a * 3 + k))
            z = 0.12 + 0.07 * k + (0.04 * math.sin(a * 2))
            pts.append((math.cos(a) * R, math.sin(a) * R * 0.92 + ry * 0.2, z if math.sin(a) < 0.3 else 0.03))
        rope_curve(pts, jute)
    # the knot on top + two hanging ends with frayed tips
    kc = (0.35, -1.05, 0.25)
    if step < 2:
        for i in range(3):
            torus(0.15, 0.08, (kc[0] + 0.05 * i, kc[1], kc[2] + 0.05 * i), rot=(math.radians(60 * i), math.radians(30), 0), maj=32, mn=12, mat=jute)
    rope_curve([kc, (kc[0] + 0.3, kc[1] - 0.4, 0.1), (kc[0] + 0.5, kc[1] - 0.8, 0.05)], jute, 0.075)
    rope_curve([kc, (kc[0] - 0.25, kc[1] - 0.45, 0.1), (kc[0] - 0.55, kc[1] - 0.75, 0.05)], jute, 0.075)
