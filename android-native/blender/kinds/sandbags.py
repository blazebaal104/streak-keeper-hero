"""Three burlap sandbags piled on the button. Taps: drag off one bag at a time."""
import math
import random
from kit import *

STEPS = 3


def bag(loc, rot, m, tie_m, seed):
    random.seed(seed)
    bpy.ops.mesh.primitive_uv_sphere_add(radius=1, location=loc, segments=48, ring_count=24)
    o = bpy.context.object
    o.scale = (1.15, 0.62, 0.36); o.rotation_euler = (0, 0, rot)
    d = o.modifiers.new("lump", 'DISPLACE'); t = bpy.data.textures.new("t", 'CLOUDS'); t.noise_scale = 0.35; d.texture = t; d.strength = 0.14
    obj(o, mat=m)
    # tied end
    c, s = math.cos(rot), math.sin(rot)
    ex = loc[0] - 1.1 * c; ey = loc[1] - 1.1 * s
    cyl(0.12, 0.3, (ex, ey, loc[2]), rot=(0, math.radians(90), rot), mat=m)
    torus(0.12, 0.03, (ex + 0.1 * c, ey + 0.1 * s, loc[2]), rot=(0, math.radians(90), rot), mat=tie_m)


def build(step):
    m = fabric("burlap", (0.36, 0.28, 0.16), scale=22)
    tie_m = fabric("twine", (0.35, 0.25, 0.12), scale=120)
    bags = [((-0.45, 0.55, 0.22), 0.25), ((0.5, -0.5, 0.22), -0.3), ((0.05, 0.05, 0.55), 0.05)]
    for i, (loc, rot) in enumerate(bags):
        if step > 2 - i:
            continue  # the top bag goes first
        bag(loc, rot, m, tie_m, i)
