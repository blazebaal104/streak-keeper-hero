"""Two strands of barbed wire stretched over the button. Taps: cut strand 1, strand 2 (sags), remove."""
import math
import random
from kit import *

STEPS = 3


def strand(y0, cut, m, seed):
    random.seed(seed)
    pts = []
    for k in range(17):
        x = -2.1 + k * 4.2 / 16
        sag = (1 - (x / 2.1) ** 2) * (0.35 if cut else 0.0)
        pts.append((x, y0 - sag + random.uniform(-0.03, 0.03), 0.18 + (0.12 * math.cos(x * 1.4) if not cut else 0.05)))
    a = curve_tube(pts, 0.022, m, "w1")
    pts2 = [(p[0], p[1] + 0.035 * math.sin(i * 1.9), p[2] + 0.03 * math.cos(i * 1.9)) for i, p in enumerate(pts)]
    curve_tube(pts2, 0.02, m, "w2")
    for k in range(1, 16, 2):
        x, y, z = pts[k]
        for j in range(4):
            a_ = j * math.pi / 2 + 0.6
            cyl(0.012, 0.22, (x + math.cos(a_) * 0.08, y + math.sin(a_) * 0.08, z), rot=(math.radians(70), 0, a_ + math.pi / 2), mat=m, verts=8)
        torus(0.035, 0.014, (x, y, z), rot=(0, math.radians(90), 0), maj=16, mn=8, mat=m)


def build(step):
    m = steel("barb", 0.35, (0.48, 0.47, 0.45), brushed=False, rust=0.4)
    strand(0.45, step >= 1, m, 1)
    strand(-0.45, step >= 2, m, 2)
