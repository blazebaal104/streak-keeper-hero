"""A bricked-up block over the button. Taps: knock out three clusters of bricks, then it's gone."""
import math
import random
from kit import *

STEPS = 4
GONE = {1: [(2, 3), (2, 4), (3, 3)], 2: [(4, 2), (4, 3), (3, 2)], 3: [(1, 2), (2, 2), (1, 3)]}


def build(step):
    random.seed(9)
    mortar = masonry("mortar", (0.3, 0.29, 0.27))
    cube((2.7, 2.7, 0.3), (0, 0, 0.15), mat=mortar)
    mats = [masonry("brick%d" % i, (0.24 + 0.05 * i, 0.075 + 0.015 * i, 0.045)) for i in range(3)]
    removed = set()
    for s in range(1, step + 1):
        removed |= set(GONE.get(s, []))
    bw, bh = 0.52, 0.25
    rows = 10
    for r in range(rows):
        off = (bw / 2) if r % 2 else 0
        y = -1.3 + bh / 2 + r * (bh + 0.03)
        for c in range(-1, 6):
            x = -1.35 + off + c * (bw + 0.03) + bw / 2
            l = max(x - bw / 2, -1.35); rr = min(x + bw / 2, 1.35)
            if rr - l < 0.08:
                continue
            if (r // 2, c) in removed:
                continue
            cube((rr - l, bh, 0.18), ((l + rr) / 2, y, 0.38 + random.uniform(-0.01, 0.01)), rot=(0, 0, random.uniform(-0.01, 0.01)), mat=random.choice(mats), bevel=0.02, segs=2)
    # knocked-out bricks leave real holes right through the wall
    for r in range(rows):
        off = (bw / 2) if r % 2 else 0
        y = -1.3 + bh / 2 + r * (bh + 0.03)
        for c in range(-1, 6):
            if (r // 2, c) not in removed:
                continue
            x = -1.35 + off + c * (bw + 0.03) + bw / 2
            l = max(x - bw / 2, -1.35); rr = min(x + bw / 2, 1.35)
            if rr - l < 0.08:
                continue
            hole(cube((rr - l + 0.03, bh + 0.03, 0.02), ((l + rr) / 2, y, 0.31)))
