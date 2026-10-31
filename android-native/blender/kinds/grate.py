"""Rusty iron grate over the button with a sliding bolt. Taps: slide bolt, swing the door open, remove."""
import math
from kit import *

STEPS = 3


def door(m, step):
    parts = []
    parts.append(cube((2.5, 0.12, 0.12), (0, 1.19, 0.3), mat=m, bevel=0.02))
    parts.append(cube((2.5, 0.12, 0.12), (0, -1.19, 0.3), mat=m, bevel=0.02))
    parts.append(cube((0.12, 2.5, 0.12), (-1.19, 0, 0.3), mat=m, bevel=0.02))
    parts.append(cube((0.12, 2.5, 0.12), (1.19, 0, 0.3), mat=m, bevel=0.02))
    for i in range(-4, 5):
        parts.append(cyl(0.045, 2.4, (i * 0.26, 0, 0.3), rot=(math.radians(90), 0, 0), mat=m, verts=16))
    for j in (-0.6, 0.6):
        parts.append(cube((2.4, 0.08, 0.05), (0, j, 0.36), mat=m, bevel=0.01))
    return parts


def build(step):
    m = steel("iron", 0.6, (0.22, 0.21, 0.2), brushed=False, rust=0.75)
    ch = steel("bolt", 0.3, (0.55, 0.55, 0.56), brushed=True, rust=0.2)
    # fixed frame on the floor
    frame = cube((2.8, 2.8, 0.12), (0, 0, 0.06), mat=m, bevel=0.02)
    hole = cube((2.5, 2.5, 0.3), (0, 0, 0.06), mat=m)
    b = frame.modifiers.new("cut", 'BOOLEAN'); b.object = hole; b.operation = 'DIFFERENCE'
    hole.hide_render = True
    parts = door(m, step)
    if step >= 2:
        # swung up on the left hinges to ~100°
        bpy.ops.object.select_all(action='DESELECT')
        piv = bpy.data.objects.new("piv", None); bpy.context.scene.collection.objects.link(piv); piv.location = (-1.25, 0, 0.3)
        for p in parts:
            p.parent = piv; p.location = (p.location[0] + 1.25, p.location[1], p.location[2] - 0.3)
        piv.rotation_euler = (0, math.radians(-100), 0)
    # sliding bolt on the right edge
    slide = -0.35 if step >= 1 else 0.0
    cube((0.3, 0.3, 0.1), (1.35, 0, 0.32), mat=ch, bevel=0.02)
    cyl(0.05, 0.7, (1.2 + slide, 0, 0.42), rot=(0, math.radians(90), 0), mat=ch)
    cyl(0.035, 0.22, (1.2 + slide, 0, 0.53), mat=ch)
    sphere(0.06, (1.2 + slide, 0, 0.65), mat=ch)
