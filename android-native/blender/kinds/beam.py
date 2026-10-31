"""Two rough timber planks nailed across the button. Taps: pull nail 1, pull nail 2, remove."""
import math
import random
from kit import *

STEPS = 3


def nail(x, y, z, pulled, mat_head, mat_shaft, tilt=0.0):
    if pulled:
        # yanked out and bent over: the shaft lies almost flat beside its dark hole
        cyl(0.045, 0.02, (x, y, z + 0.002), mat=plastic("hole", (0.02, 0.012, 0.008), 0.9))
        ang = math.radians(35 + tilt * 4)
        L = 0.5
        cx = x + math.cos(ang) * L / 2; cy = y + math.sin(ang) * L / 2
        cyl(0.028, L, (cx, cy, z + 0.06), rot=(0, math.radians(78), ang), mat=mat_shaft)
        cyl(0.075, 0.025, (x + math.cos(ang) * L, y + math.sin(ang) * L, z + 0.1), rot=(0, math.radians(78), ang), mat=mat_head)
    else:
        cyl(0.075, 0.025, (x, y, z + 0.012), mat=mat_head)
        # dent in the wood around the head
        cyl(0.1, 0.006, (x, y, z + 0.001), mat=plastic("dent", (0.08, 0.05, 0.03), 0.9))


def build(step):
    random.seed(3)
    w1 = wood("plank_a", (0.23, 0.14, 0.07))
    w2 = wood("plank_b", (0.19, 0.12, 0.065))
    head = steel("nail_head", 0.45, (0.32, 0.31, 0.3), brushed=False, rust=0.55)
    shaft = steel("nail_shaft", 0.25, (0.6, 0.6, 0.62), brushed=False, rust=0.15)
    # two planks crossing in an X, the top one slightly raised where it rests on the other
    a = cube((3.7, 0.62, 0.16), (0, 0, 0.25), rot=(0, 0, math.radians(-24)), mat=w1, bevel=0.025, segs=2)
    b = cube((3.7, 0.56, 0.16), (0, 0, 0.42), rot=(0, 0, math.radians(21)), mat=w2, bevel=0.025, segs=2)
    # splintered ends: a few small chunks missing
    for o in (a, b):
        d = o.modifiers.new("dis", 'DISPLACE'); d.strength = 0.015
        t = bpy.data.textures.new("tx", 'CLOUDS'); t.noise_scale = 0.15; d.texture = t
        o.modifiers.move(len(o.modifiers) - 1, 0)
        sub = o.modifiers.new("sub", 'SUBSURF'); sub.levels = 2; sub.render_levels = 2; sub.subdivision_type = 'SIMPLE'
        o.modifiers.move(len(o.modifiers) - 1, 0)
    # supports under the plank ends so they read as resting, not floating
    for ang, z in ((-24, 0.25), (21, 0.42)):
        for s in (-1, 1):
            r = math.radians(ang)
            nail(s * 1.45 * math.cos(r) * (0.85 if s > 0 else 1), s * 1.45 * math.sin(r), z + 0.08, False, head, shaft) if False else None
    r = math.radians(21)
    nails = [(-1.35 * math.cos(r), -1.35 * math.sin(r)), (1.35 * math.cos(r), 1.35 * math.sin(r))]
    for i, (x, y) in enumerate(nails):
        nail(x, y, 0.5, step > i, head, shaft, tilt=i * 6)
    r = math.radians(-24)
    for x, y in [(-1.4 * math.cos(r), -1.4 * math.sin(r)), (1.4 * math.cos(r), 1.4 * math.sin(r))]:
        nail(x, y, 0.33, False, head, shaft)
