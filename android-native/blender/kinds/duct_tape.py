"""Silver duct tape strips slapped across the button. Taps: rip one strip at a time."""
import math
import random
from kit import *

STEPS = 3


def strip(ang, z, m):
    bpy.ops.mesh.primitive_plane_add(size=1, location=(0, 0, z), rotation=(0, 0, ang))
    o = bpy.context.object
    o.scale = (3.4, 0.42, 1)
    bpy.ops.object.transform_apply(scale=True)
    sub = o.modifiers.new("s", 'SUBSURF'); sub.subdivision_type = 'SIMPLE'; sub.levels = sub.render_levels = 4
    d = o.modifiers.new("wrinkle", 'DISPLACE'); t = bpy.data.textures.new("w", 'CLOUDS'); t.noise_scale = 0.25; d.texture = t; d.strength = 0.05; d.texture_coords = 'GLOBAL'
    sol = o.modifiers.new("th", 'SOLIDIFY'); sol.thickness = 0.008
    obj(o, mat=m)


def build(step):
    random.seed(4)
    m = fabric("ducttape", (0.36, 0.37, 0.39), scale=55, rough=0.4)
    m.node_tree.nodes['Principled BSDF'].inputs['Metallic'].default_value = 0.55
    for i, (a, z) in enumerate(((-38, 0.05), (34, 0.07), (4, 0.09))):
        if step > i:
            continue
        strip(math.radians(a), z, m)
