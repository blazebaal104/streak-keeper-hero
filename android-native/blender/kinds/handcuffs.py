"""Police handcuffs locked round the button's rim. Taps: key opens cuff 1, cuff 2, remove."""
import math
from kit import *

STEPS = 3


def cuff(c, ang, opened, m, dark):
    # ring (C shape) + swing arm; open = arm rotated out
    ring = torus(0.42, 0.06, (c[0], c[1], c[2]), rot=(0, 0, ang), maj=48, mn=14, mat=m)
    ring.scale = (1.0, 1.0, 0.65)
    # lock box on the outer side
    bx = c[0] + math.cos(ang) * 0.5; by = c[1] + math.sin(ang) * 0.5
    cube((0.3, 0.42, 0.16), (bx, by, c[2] + 0.02), rot=(0, 0, ang), mat=dark, bevel=0.03)
    cyl(0.035, 0.02, (bx, by, c[2] + 0.11), mat=plastic("kh", (0.0, 0.0, 0.0), 0.8))
    if opened:
        arm = torus(0.42, 0.055, (c[0] - math.cos(ang) * 0.35, c[1] - math.sin(ang) * 0.35, c[2] + 0.05), rot=(0, math.radians(25), ang + math.radians(120)), maj=48, mn=14, mat=m)
        arm.scale = (1.0, 1.0, 0.6)


def build(step):
    m = steel("cuff", 0.12, (0.78, 0.79, 0.8), brushed=False)
    dark = steel("cuffbox", 0.3, (0.45, 0.46, 0.48), brushed=True)
    key_m = steel("key", 0.2, (0.7, 0.7, 0.72), brushed=False)
    l = (-0.95, 0.1, 0.12); r = (0.95, -0.05, 0.12)
    cuff(l, math.radians(180), step >= 1, m, dark)
    cuff(r, math.radians(0), step >= 2, m, dark)
    # chain between the cuffs (3 links + swivels)
    for i in range(4):
        t = (i + 0.5) / 4
        x = -0.45 + 0.9 * t; y = 0.06 - 0.12 * t
        o = torus(0.09, 0.025, (x, y, 0.28), rot=(math.radians(90) if i % 2 else 0, 0, 0), maj=24, mn=10, mat=m); o.scale = (1.5, 1, 1)
    # the little key sitting in the lock it just opened
    if step >= 1:
        kc = (l[0] - 0.5, l[1], 0.3) if step == 1 else (r[0] + 0.5, r[1], 0.3)
        cyl(0.02, 0.3, (kc[0], kc[1] - 0.15, kc[2]), rot=(math.radians(90), 0, 0), mat=key_m)
        torus(0.07, 0.02, (kc[0], kc[1] - 0.35, kc[2]), rot=(0, 0, 0), mat=key_m)
