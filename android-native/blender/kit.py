"""
Shared Blender kit for the NNN protection sprites.

Every protection is modelled around the app's round button (radius 1, lying on the XY plane at z=0, seen from
above). Each tap state is rendered with a transparent background and a shadow catcher, so the app only has to
draw pre-rendered images (the "Clash Royale" way: no per-frame vector drawing, 60 fps on low-end phones).

Run with the bpy module:  python3.11 render.py [kinds...]
"""
import math
import os
import random

import bpy
from mathutils import Vector, Euler

HERE = os.path.dirname(os.path.abspath(__file__))
FRAME = 4.0           # the sprite covers -2..2 around the button (button diameter = half the sprite)


# --------------------------------------------------------------------------------------------- scene

def reset(res=768, samples=64):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    s = bpy.context.scene
    s.render.engine = 'CYCLES'
    s.cycles.device = 'CPU'
    s.cycles.samples = samples
    s.cycles.use_denoising = True
    s.cycles.max_bounces = 6
    s.render.resolution_x = s.render.resolution_y = res
    s.render.film_transparent = True
    s.render.image_settings.file_format = 'PNG'
    s.render.image_settings.color_mode = 'RGBA'
    s.view_settings.view_transform = 'AgX'
    s.view_settings.look = 'AgX - Medium High Contrast'
    # world: soft studio gradient so metals have something to reflect
    w = bpy.data.worlds.new("studio"); s.world = w
    nt = w.node_tree
    bg = nt.nodes['Background']
    grad = nt.nodes.new('ShaderNodeTexGradient'); grad.gradient_type = 'SPHERICAL'
    tc = nt.nodes.new('ShaderNodeTexCoord')
    mp = nt.nodes.new('ShaderNodeMapping'); mp.inputs['Scale'].default_value = (0.6, 0.6, 0.6)
    ramp = nt.nodes.new('ShaderNodeValToRGB')
    ramp.color_ramp.elements[0].color = (0.015, 0.012, 0.012, 1)
    ramp.color_ramp.elements[1].color = (0.55, 0.52, 0.5, 1)
    nt.links.new(tc.outputs['Generated'], mp.inputs['Vector'])
    nt.links.new(mp.outputs['Vector'], grad.inputs['Vector'])
    nt.links.new(grad.outputs['Fac'], ramp.inputs['Fac'])
    nt.links.new(ramp.outputs['Color'], bg.inputs['Color'])
    bg.inputs['Strength'].default_value = 0.55
    camera()
    lights()
    shadow_catcher()
    return s


def camera():
    cam = bpy.data.cameras.new("cam")
    # long lens, slightly tilted: the front faces show a little, like a product shot from above
    cam.type = 'PERSP'
    cam.sensor_width = 36
    dist = 12.0
    cam.lens = 36 * dist / FRAME
    o = bpy.data.objects.new("cam", cam)
    bpy.context.scene.collection.objects.link(o)
    tilt = math.radians(14)
    o.location = (0, -math.sin(tilt) * dist, math.cos(tilt) * dist)
    o.rotation_euler = (tilt, 0, 0)
    bpy.context.scene.camera = o
    return o


def area_light(name, loc, energy, size, color=(1, 1, 1)):
    l = bpy.data.lights.new(name, 'AREA')
    l.energy = energy; l.size = size; l.color = color
    o = bpy.data.objects.new(name, l)
    bpy.context.scene.collection.objects.link(o)
    o.location = loc
    d = Vector((0, 0, 0)) - Vector(loc)
    o.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()
    return o


def lights():
    # key from top-left (like the app's highlights), cool fill, warm rim from the burning button below
    area_light("key", (-4, 4, 9), 950, 4.5, (1.0, 0.97, 0.92))
    area_light("fill", (5, -2, 6), 140, 6, (0.8, 0.86, 1.0))
    area_light("rim", (0, -6, 2.5), 300, 3, (1.0, 0.55, 0.25))


def shadow_catcher():
    bpy.ops.mesh.primitive_plane_add(size=12, location=(0, 0, 0))
    p = bpy.context.object
    p.name = "catcher"
    p.is_shadow_catcher = True
    return p


def render(path):
    s = bpy.context.scene
    s.render.filepath = path
    bpy.ops.render.render(write_still=True)


def clear_objects(keep=("cam", "key", "fill", "rim", "catcher")):
    for o in list(bpy.data.objects):
        if o.name not in keep:
            bpy.data.objects.remove(o, do_unlink=True)


# --------------------------------------------------------------------------------------------- helpers

def obj(o, name=None, mat=None, bevel=0.0, segs=3, smooth=True):
    if name: o.name = name
    if bevel > 0:
        m = o.modifiers.new("bevel", 'BEVEL'); m.width = bevel; m.segments = segs; m.limit_method = 'ANGLE'
    if smooth:
        try:
            bpy.ops.object.select_all(action='DESELECT'); o.select_set(True); bpy.context.view_layer.objects.active = o
            bpy.ops.object.shade_auto_smooth(angle=math.radians(35))
        except Exception:
            pass
    if mat is not None:
        o.data.materials.clear(); o.data.materials.append(mat)
    return o


def cube(size, loc, rot=(0, 0, 0), **kw):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc, rotation=rot)
    o = bpy.context.object; o.scale = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    return obj(o, **kw)


def cyl(r, depth, loc, rot=(0, 0, 0), verts=64, **kw):
    bpy.ops.mesh.primitive_cylinder_add(radius=r, depth=depth, location=loc, rotation=rot, vertices=verts)
    return obj(bpy.context.object, **kw)


def torus(R, r, loc, rot=(0, 0, 0), maj=64, mn=20, **kw):
    bpy.ops.mesh.primitive_torus_add(major_radius=R, minor_radius=r, location=loc, rotation=rot, major_segments=maj, minor_segments=mn)
    return obj(bpy.context.object, **kw)


def sphere(r, loc, **kw):
    bpy.ops.mesh.primitive_uv_sphere_add(radius=r, location=loc, segments=32, ring_count=16)
    return obj(bpy.context.object, **kw)


def curve_tube(points, radius, mat, name="tube", res=12, twist=0.0):
    cu = bpy.data.curves.new(name, 'CURVE'); cu.dimensions = '3D'
    cu.bevel_depth = radius; cu.bevel_resolution = 4; cu.resolution_u = res
    sp = cu.splines.new('NURBS'); sp.points.add(len(points) - 1)
    for p, c in zip(sp.points, points): p.co = (c[0], c[1], c[2], 1)
    sp.use_endpoint_u = True; sp.order_u = min(4, len(points))
    o = bpy.data.objects.new(name, cu); bpy.context.scene.collection.objects.link(o)
    o.data.materials.append(mat)
    return o


def text_obj(body, loc, size, mat, extrude=0.004, rot=(0, 0, 0), align='CENTER', font=None):
    cu = bpy.data.curves.new("txt", 'FONT'); cu.body = body; cu.size = size; cu.extrude = extrude
    cu.align_x = align; cu.align_y = 'CENTER'
    if font: cu.font = font
    o = bpy.data.objects.new("txt", cu); bpy.context.scene.collection.objects.link(o)
    o.location = loc; o.rotation_euler = rot
    o.data.materials.append(mat)
    return o


# --------------------------------------------------------------------------------------------- materials

def _mat(name):
    m = bpy.data.materials.new(name)
    nt = m.node_tree
    p = nt.nodes['Principled BSDF']
    return m, nt, p


def _noise(nt, scale, detail=8, rough=0.6, coord='Object', distortion=0.0):
    tc = nt.nodes.new('ShaderNodeTexCoord')
    n = nt.nodes.new('ShaderNodeTexNoise'); n.inputs['Scale'].default_value = scale
    n.inputs['Detail'].default_value = detail; n.inputs['Roughness'].default_value = rough
    n.inputs['Distortion'].default_value = distortion
    nt.links.new(tc.outputs[coord], n.inputs['Vector'])
    return n


def _ramp(nt, src, stops):
    r = nt.nodes.new('ShaderNodeValToRGB')
    els = r.color_ramp.elements
    while len(els) < len(stops): els.new(0.5)
    for e, (pos, col) in zip(els, stops):
        e.position = pos; e.color = col if len(col) == 4 else (*col, 1)
    nt.links.new(src, r.inputs['Fac'])
    return r


def _bump(nt, p, height_src, strength=0.3, dist=0.02):
    b = nt.nodes.new('ShaderNodeBump'); b.inputs['Strength'].default_value = strength; b.inputs['Distance'].default_value = dist
    nt.links.new(height_src, b.inputs['Height'])
    nt.links.new(b.outputs['Normal'], p.inputs['Normal'])
    return b


def _scratches(nt, scale=18.0):
    """Fine directional scratches: stretched noise thresholded to thin bright lines."""
    tc = nt.nodes.new('ShaderNodeTexCoord')
    mp = nt.nodes.new('ShaderNodeMapping'); mp.inputs['Scale'].default_value = (1.0, 28.0, 1.0)
    mp.inputs['Rotation'].default_value = (0, 0, random.uniform(0.2, 1.2))
    nt.links.new(tc.outputs['Object'], mp.inputs['Vector'])
    n = nt.nodes.new('ShaderNodeTexNoise'); n.inputs['Scale'].default_value = scale; n.inputs['Detail'].default_value = 2
    nt.links.new(mp.outputs['Vector'], n.inputs['Vector'])
    r = _ramp(nt, n.outputs['Fac'], [(0.0, (0, 0, 0)), (0.74, (0, 0, 0)), (0.76, (1, 1, 1))])
    return r


def painted_metal(name, color, wear=0.6, rough=0.42):
    """Paint over steel: chipped edges (pointiness), scuffs, scratches, grime in creases."""
    m, nt, p = _mat(name)
    # edge mask: where the bevel-rounded normal leaves the true normal (works on any mesh, unlike pointiness)
    geo = nt.nodes.new('ShaderNodeNewGeometry')
    bev = nt.nodes.new('ShaderNodeBevel'); bev.inputs['Radius'].default_value = 0.035 * (0.6 + wear)
    dot = nt.nodes.new('ShaderNodeVectorMath'); dot.operation = 'DOT_PRODUCT'
    nt.links.new(bev.outputs['Normal'], dot.inputs[0]); nt.links.new(geo.outputs['Normal'], dot.inputs[1])
    edge = _ramp(nt, dot.outputs['Value'], [(0.93, (1, 1, 1)), (0.995, (0, 0, 0))])
    chip = _noise(nt, 22, 12, 0.7)
    chipr = _ramp(nt, chip.outputs['Fac'], [(0.42, (0, 0, 0)), (0.55, (1, 1, 1))])
    mul = nt.nodes.new('ShaderNodeMix'); mul.data_type = 'RGBA'; mul.blend_type = 'MULTIPLY'; mul.inputs['Factor'].default_value = 1
    nt.links.new(edge.outputs['Color'], mul.inputs[6]); nt.links.new(chipr.outputs['Color'], mul.inputs[7])
    scr = _scratches(nt)
    worn = nt.nodes.new('ShaderNodeMix'); worn.data_type = 'RGBA'; worn.blend_type = 'LIGHTEN'; worn.inputs['Factor'].default_value = 0.22
    nt.links.new(mul.outputs[2], worn.inputs[6]); nt.links.new(scr.outputs['Color'], worn.inputs[7])
    # paint colour with mottling and grime
    mott = _noise(nt, 4, 6, 0.55)
    paint = nt.nodes.new('ShaderNodeMix'); paint.data_type = 'RGBA'; paint.inputs['Factor'].default_value = 0.18
    paint.inputs[6].default_value = (*color, 1); paint.inputs[7].default_value = (color[0] * 0.55, color[1] * 0.55, color[2] * 0.55, 1)
    nt.links.new(mott.outputs['Fac'], paint.inputs['Factor'])
    col = nt.nodes.new('ShaderNodeMix'); col.data_type = 'RGBA'
    col.inputs[7].default_value = (0.55, 0.55, 0.56, 1)  # bare steel
    nt.links.new(worn.outputs[2], col.inputs['Factor']); nt.links.new(paint.outputs[2], col.inputs[6])
    nt.links.new(col.outputs[2], p.inputs['Base Color'])
    met = nt.nodes.new('ShaderNodeMath'); met.operation = 'MULTIPLY'; met.inputs[1].default_value = 1.0
    nt.links.new(worn.outputs[2], met.inputs[0]); nt.links.new(met.outputs[0], p.inputs['Metallic'])
    rmix = nt.nodes.new('ShaderNodeMapRange'); rmix.inputs['To Min'].default_value = rough; rmix.inputs['To Max'].default_value = 0.28
    nt.links.new(worn.outputs[2], rmix.inputs['Value']); nt.links.new(rmix.outputs['Result'], p.inputs['Roughness'])
    _bump(nt, p, worn.outputs[2], 0.25, 0.01)
    return m


def steel(name, rough=0.22, color=(0.62, 0.63, 0.65), brushed=True, rust=0.0):
    m, nt, p = _mat(name)
    p.inputs['Metallic'].default_value = 1.0
    n = _noise(nt, 30, 10, 0.6)
    r = nt.nodes.new('ShaderNodeMapRange'); r.inputs['To Min'].default_value = rough * 0.6; r.inputs['To Max'].default_value = rough * 1.6
    nt.links.new(n.outputs['Fac'], r.inputs['Value']); nt.links.new(r.outputs['Result'], p.inputs['Roughness'])
    if brushed:
        p.inputs['Anisotropic'].default_value = 0.6
    if rust > 0:
        rn = _noise(nt, 6, 14, 0.75, distortion=0.4)
        rr = _ramp(nt, rn.outputs['Fac'], [(0.62 - rust * 0.25, (0, 0, 0)), (0.7 - rust * 0.2, (1, 1, 1))])
        col = nt.nodes.new('ShaderNodeMix'); col.data_type = 'RGBA'
        col.inputs[6].default_value = (*color, 1); col.inputs[7].default_value = (0.32, 0.12, 0.04, 1)
        nt.links.new(rr.outputs['Color'], col.inputs['Factor']); nt.links.new(col.outputs[2], p.inputs['Base Color'])
        inv = nt.nodes.new('ShaderNodeMath'); inv.operation = 'SUBTRACT'; inv.inputs[0].default_value = 1
        nt.links.new(rr.outputs['Color'], inv.inputs[1]); nt.links.new(inv.outputs[0], p.inputs['Metallic'])
        _bump(nt, p, rn.outputs['Fac'], 0.35, 0.01)
    else:
        p.inputs['Base Color'].default_value = (*color, 1)
        scr = _scratches(nt, 26)
        _bump(nt, p, scr.outputs['Color'], 0.08, 0.004)
    return m


def chrome(name="chrome"):
    return steel(name, 0.08, (0.85, 0.86, 0.88), brushed=False)


def brass(name="brass"):
    m = steel(name, 0.25, (0.85, 0.62, 0.25), brushed=False)
    return m


def plastic(name, color, rough=0.45):
    m, nt, p = _mat(name)
    p.inputs['Base Color'].default_value = (*color, 1)
    n = _noise(nt, 60, 6, 0.5)
    r = nt.nodes.new('ShaderNodeMapRange'); r.inputs['To Min'].default_value = rough * 0.8; r.inputs['To Max'].default_value = rough * 1.25
    nt.links.new(n.outputs['Fac'], r.inputs['Value']); nt.links.new(r.outputs['Result'], p.inputs['Roughness'])
    _bump(nt, p, n.outputs['Fac'], 0.05, 0.002)
    return m


def wood(name, color=(0.42, 0.26, 0.13)):
    """Rough sawn timber: stretched rings + grain, darker grime, splintery bump."""
    m, nt, p = _mat(name)
    tc = nt.nodes.new('ShaderNodeTexCoord')
    mp = nt.nodes.new('ShaderNodeMapping'); mp.inputs['Scale'].default_value = (0.35, 6.0, 6.0)
    nt.links.new(tc.outputs['Object'], mp.inputs['Vector'])
    wave = nt.nodes.new('ShaderNodeTexWave'); wave.wave_type = 'RINGS'; wave.rings_direction = 'X'
    wave.inputs['Scale'].default_value = 1.6; wave.inputs['Distortion'].default_value = 9; wave.inputs['Detail'].default_value = 6
    nt.links.new(mp.outputs['Vector'], wave.inputs['Vector'])
    grain = nt.nodes.new('ShaderNodeTexNoise'); grain.inputs['Scale'].default_value = 40; grain.inputs['Detail'].default_value = 12
    mp2 = nt.nodes.new('ShaderNodeMapping'); mp2.inputs['Scale'].default_value = (0.08, 3.0, 3.0)
    nt.links.new(tc.outputs['Object'], mp2.inputs['Vector']); nt.links.new(mp2.outputs['Vector'], grain.inputs['Vector'])
    mix = nt.nodes.new('ShaderNodeMath'); mix.operation = 'MULTIPLY'
    nt.links.new(wave.outputs['Fac'], mix.inputs[0]); nt.links.new(grain.outputs['Fac'], mix.inputs[1])
    c0 = tuple(x * 0.35 for x in color); c1 = color; c2 = tuple(min(1, x * 1.25) for x in color)
    r = _ramp(nt, mix.outputs[0], [(0.0, c0), (0.3, c1), (0.7, c2)])
    # weathering: big blotchy grime + darker, dirtier edges and corners
    grime = _noise(nt, 2.5, 8, 0.65, distortion=0.3)
    gr = _ramp(nt, grime.outputs['Fac'], [(0.35, (1, 1, 1)), (0.7, (0.45, 0.4, 0.36))])
    geo = nt.nodes.new('ShaderNodeNewGeometry')
    bev = nt.nodes.new('ShaderNodeBevel'); bev.inputs['Radius'].default_value = 0.05
    dot = nt.nodes.new('ShaderNodeVectorMath'); dot.operation = 'DOT_PRODUCT'
    nt.links.new(bev.outputs['Normal'], dot.inputs[0]); nt.links.new(geo.outputs['Normal'], dot.inputs[1])
    ed = _ramp(nt, dot.outputs['Value'], [(0.9, (0.5, 0.45, 0.4)), (0.995, (1, 1, 1))])
    m1 = nt.nodes.new('ShaderNodeMix'); m1.data_type = 'RGBA'; m1.blend_type = 'MULTIPLY'; m1.inputs['Factor'].default_value = 1
    nt.links.new(r.outputs['Color'], m1.inputs[6]); nt.links.new(gr.outputs['Color'], m1.inputs[7])
    m2 = nt.nodes.new('ShaderNodeMix'); m2.data_type = 'RGBA'; m2.blend_type = 'MULTIPLY'; m2.inputs['Factor'].default_value = 1
    nt.links.new(m1.outputs[2], m2.inputs[6]); nt.links.new(ed.outputs['Color'], m2.inputs[7])
    nt.links.new(m2.outputs[2], p.inputs['Base Color'])
    p.inputs['Roughness'].default_value = 0.86
    # deep grain grooves + fibres
    fib = _noise(nt, 160, 4, 0.7)
    add = nt.nodes.new('ShaderNodeMath'); add.operation = 'MULTIPLY_ADD'; add.inputs[1].default_value = 0.25
    nt.links.new(fib.outputs['Fac'], add.inputs[0]); nt.links.new(mix.outputs[0], add.inputs[2])
    _bump(nt, p, add.outputs[0], 0.7, 0.03)
    return m


def cardboard(name):
    m, nt, p = _mat(name)
    n = _noise(nt, 9, 8, 0.55)
    r = _ramp(nt, n.outputs['Fac'], [(0.3, (0.48, 0.32, 0.17)), (0.7, (0.62, 0.44, 0.25))])
    nt.links.new(r.outputs['Color'], p.inputs['Base Color'])
    p.inputs['Roughness'].default_value = 0.88
    tc = nt.nodes.new('ShaderNodeTexCoord')
    wv = nt.nodes.new('ShaderNodeTexWave'); wv.inputs['Scale'].default_value = 60; wv.wave_profile = 'SIN'
    nt.links.new(tc.outputs['Object'], wv.inputs['Vector'])
    add = nt.nodes.new('ShaderNodeMath'); add.operation = 'ADD'
    sc = nt.nodes.new('ShaderNodeMath'); sc.operation = 'MULTIPLY'; sc.inputs[1].default_value = 0.25
    nt.links.new(wv.outputs['Fac'], sc.inputs[0])
    nt.links.new(sc.outputs[0], add.inputs[0]); nt.links.new(n.outputs['Fac'], add.inputs[1])
    _bump(nt, p, add.outputs[0], 0.18, 0.01)
    return m


def fabric(name, color, scale=90, rough=0.95):
    """Woven fibre (burlap / jute / webbing): two crossing sine waves + fuzz."""
    m, nt, p = _mat(name)
    tc = nt.nodes.new('ShaderNodeTexCoord')
    w1 = nt.nodes.new('ShaderNodeTexWave'); w1.inputs['Scale'].default_value = scale; w1.bands_direction = 'X'
    w2 = nt.nodes.new('ShaderNodeTexWave'); w2.inputs['Scale'].default_value = scale; w2.bands_direction = 'Y'
    for w in (w1, w2): nt.links.new(tc.outputs['Object'], w.inputs['Vector']); w.inputs['Distortion'].default_value = 1.5
    mx = nt.nodes.new('ShaderNodeMath'); mx.operation = 'MAXIMUM'
    nt.links.new(w1.outputs['Fac'], mx.inputs[0]); nt.links.new(w2.outputs['Fac'], mx.inputs[1])
    fz = _noise(nt, 180, 4, 0.8)
    c0 = tuple(x * 0.4 for x in color)
    r = _ramp(nt, mx.outputs[0], [(0.15, c0), (0.85, color)])
    dirt = _noise(nt, 3, 6, 0.6)
    dr = _ramp(nt, dirt.outputs['Fac'], [(0.35, (1, 1, 1)), (0.7, (0.55, 0.5, 0.45))])
    mul = nt.nodes.new('ShaderNodeMix'); mul.data_type = 'RGBA'; mul.blend_type = 'MULTIPLY'; mul.inputs['Factor'].default_value = 0.35
    nt.links.new(r.outputs['Color'], mul.inputs[6]); nt.links.new(fz.outputs['Color'], mul.inputs[7])
    m3 = nt.nodes.new('ShaderNodeMix'); m3.data_type = 'RGBA'; m3.blend_type = 'MULTIPLY'; m3.inputs['Factor'].default_value = 1
    nt.links.new(mul.outputs[2], m3.inputs[6]); nt.links.new(dr.outputs['Color'], m3.inputs[7])
    nt.links.new(m3.outputs[2], p.inputs['Base Color'])
    p.inputs['Roughness'].default_value = rough
    p.inputs['Sheen Weight'].default_value = 0.4
    _bump(nt, p, mx.outputs[0], 0.9, 0.02)
    return m


def glass(name="glass"):
    m, nt, p = _mat(name)
    p.inputs['Base Color'].default_value = (0.92, 0.97, 0.98, 1)
    p.inputs['Transmission Weight'].default_value = 1.0
    p.inputs['Roughness'].default_value = 0.03
    p.inputs['IOR'].default_value = 1.5
    smudge = _noise(nt, 7, 6, 0.6)
    r = nt.nodes.new('ShaderNodeMapRange'); r.inputs['To Min'].default_value = 0.0; r.inputs['To Max'].default_value = 0.12
    nt.links.new(smudge.outputs['Fac'], r.inputs['Value']); nt.links.new(r.outputs['Result'], p.inputs['Roughness'])
    return m


def emissive(name, color, strength):
    m, nt, p = _mat(name)
    p.inputs['Base Color'].default_value = (*color, 1)
    p.inputs['Emission Color'].default_value = (*color, 1)
    p.inputs['Emission Strength'].default_value = strength
    return m


def image_mat(name, path, rough=0.35, metallic=0.0, plastic_sheen=True):
    m, nt, p = _mat(name)
    img = bpy.data.images.load(path)
    t = nt.nodes.new('ShaderNodeTexImage'); t.image = img
    nt.links.new(t.outputs['Color'], p.inputs['Base Color'])
    nt.links.new(t.outputs['Alpha'], p.inputs['Alpha'])
    p.inputs['Roughness'].default_value = rough; p.inputs['Metallic'].default_value = metallic
    if plastic_sheen: p.inputs['Coat Weight'].default_value = 0.3
    return m


def masonry(name, color):
    """Fired clay / concrete: mottled colour, pits, rough relief, chalky dirt."""
    m, nt, p = _mat(name)
    n = _noise(nt, 5, 10, 0.65)
    c0 = tuple(x * 0.6 for x in color); c2 = tuple(min(1, x * 1.3) for x in color)
    r = _ramp(nt, n.outputs['Fac'], [(0.3, c0), (0.5, color), (0.75, c2)])
    pits = _noise(nt, 70, 4, 0.7)
    pr = _ramp(nt, pits.outputs['Fac'], [(0.0, (0.6, 0.6, 0.6)), (0.3, (1, 1, 1))])
    mx = nt.nodes.new('ShaderNodeMix'); mx.data_type = 'RGBA'; mx.blend_type = 'MULTIPLY'; mx.inputs['Factor'].default_value = 1
    nt.links.new(r.outputs['Color'], mx.inputs[6]); nt.links.new(pr.outputs['Color'], mx.inputs[7])
    nt.links.new(mx.outputs[2], p.inputs['Base Color'])
    p.inputs['Roughness'].default_value = 0.93
    add = nt.nodes.new('ShaderNodeMath'); add.operation = 'ADD'
    nt.links.new(n.outputs['Fac'], add.inputs[0]); nt.links.new(pits.outputs['Fac'], add.inputs[1])
    _bump(nt, p, add.outputs[0], 0.6, 0.02)
    return m


def hole(o):
    """Makes an object a see-through cut-out: wherever it is visible the sprite is transparent, so the app shows
    whatever lies underneath (the next protection, or the real button)."""
    o.is_holdout = True
    return o
