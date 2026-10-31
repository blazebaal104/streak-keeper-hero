"""Thick steel plate bolted over the button. Taps: unscrew 4 bolts one by one, remove."""
import math
from kit import *

STEPS = 5


def build(step):
    plate_m = steel("plate", 0.5, (0.26, 0.265, 0.27), brushed=True, rust=0.45)
    bolt_m = steel("bolt", 0.3, (0.55, 0.55, 0.56), brushed=False, rust=0.2)
    paint = plastic("stencil", (0.95, 0.72, 0.05), 0.55)
    ink = plastic("stencil_ink", (0.03, 0.03, 0.03), 0.6)
    cube((2.7, 2.7, 0.14), (0, 0, 0.07), mat=plate_m, bevel=0.03, segs=3)
    # yellow warning band with stencilled text, paint a bit worn
    cube((1.9, 0.38, 0.004), (0, -0.05, 0.142), mat=paint)
    text_obj("NON TOCCARE", (0, -0.05, 0.146), 0.24, ink, extrude=0.001)
    corners = [(-1, 1), (1, 1), (1, -1), (-1, -1)]
    for i, (sx, sy) in enumerate(corners):
        x, y = sx * 1.08, sy * 1.08
        if step > i:
            # bolt gone: threaded hole, the bolt lying beside it
            cyl(0.085, 0.02, (x, y, 0.135), mat=plastic("hole", (0.01, 0.01, 0.01), 0.8))
            ang = math.radians(40 + i * 70)
            bx, by = x - sx * 0.05 + math.cos(ang) * 0.32, y + math.sin(ang) * 0.32
            cyl(0.06, 0.34, (bx, by, 0.2), rot=(0, math.radians(90), ang), mat=bolt_m)
            cyl(0.13, 0.08, (bx + math.cos(ang) * 0.2, by + math.sin(ang) * 0.2, 0.2), rot=(0, math.radians(90), ang), verts=6, mat=bolt_m)
        else:
            cyl(0.15, 0.03, (x, y, 0.155), mat=bolt_m)          # washer
            cyl(0.12, 0.09, (x, y, 0.2), verts=6, rot=(0, 0, i * 0.4), mat=bolt_m, bevel=0.01)  # hex head
