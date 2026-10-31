"""Heavy chain wrapped round the button, closed by a brass padlock. Taps: key in + turn, shackle pops open, remove."""
import math
from kit import *

STEPS = 3


def link_chain(path_pts, link_len, mat, thick=0.045):
    """Interlocking oval links laid along a polyline, alternating 90° so they actually interlock."""
    total = 0
    segs = []
    for a, b in zip(path_pts, path_pts[1:]):
        d = math.dist(a[:2], b[:2]); segs.append((a, b, d)); total += d
    n = int(total / (link_len * 0.78))
    k = 0
    for a, b, d in segs:
        m = max(1, int(d / (link_len * 0.78)))
        ang = math.atan2(b[1] - a[1], b[0] - a[0])
        for j in range(m):
            t = (j + 0.5) / m
            x = a[0] + (b[0] - a[0]) * t; y = a[1] + (b[1] - a[1]) * t; z = a[2] + (b[2] - a[2]) * t
            o = torus(link_len * 0.32, thick, (x, y, z), rot=(math.radians(90) if k % 2 else 0, 0, ang), maj=32, mn=12, mat=mat)
            o.scale = (1.55, 1.0, 1.0)
            k += 1


def build(step):
    chain_m = steel("chain", 0.3, (0.5, 0.5, 0.52), brushed=False, rust=0.25)
    brass_m = brass()
    ch = chrome()
    key_m = steel("key", 0.2, (0.75, 0.62, 0.3), brushed=False)
    # two chain runs crossing over the button, ends hanging off the edges
    link_chain([(-2.0, 1.2, 0.12), (-0.6, 0.35, 0.3), (0.0, 0.0, 0.34), (0.6, -0.35, 0.3), (2.0, -1.25, 0.12)], 0.36, chain_m)
    link_chain([(-2.0, -1.0, 0.1), (-0.6, -0.3, 0.26), (0.6, 0.3, 0.26), (2.0, 1.05, 0.1)], 0.36, chain_m)
    # padlock at the crossing
    pc = (0.15, -0.35, 0.55)
    body = cube((0.62, 0.22, 0.52), pc, mat=brass_m, bevel=0.06, segs=4)
    up = 0.18 if step >= 2 else 0.0
    sh = torus(0.2, 0.05, (pc[0] - (0.05 if step >= 2 else 0), pc[1], pc[2] + 0.34 + up), rot=(math.radians(90), 0, 0), mat=ch)
    # keyway, and the key once inserted
    cyl(0.04, 0.02, (pc[0], pc[1] - 0.115, pc[2] - 0.08), rot=(math.radians(90), 0, 0), mat=plastic("kh", (0.01, 0.01, 0.01), 0.7))
    if step >= 1:
        rot = math.radians(90) if step >= 1 else 0
        cube((0.05, 0.3, 0.12), (pc[0], pc[1] - 0.26, pc[2] - 0.08), rot=(0, rot, 0), mat=key_m, bevel=0.01)
        torus(0.12, 0.035, (pc[0], pc[1] - 0.5, pc[2] - 0.08), rot=(math.radians(90), rot, 0), mat=key_m)
    text_obj("SECUR", (pc[0], pc[1] - 0.112, pc[2] + 0.12), 0.09, plastic("ink2", (0.15, 0.09, 0.02), 0.5), rot=(math.radians(90), 0, 0))
