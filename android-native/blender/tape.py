"""
The police tape: always the OUTERMOST layer, wound round whatever protection sits on top (tight and dense on a
safe, loose with big gaps on a box). Rendered on its own, with the object underneath acting as a shadow catcher
(so the tape is hidden where it passes behind and casts shadows onto it). Also renders the loose cut end that the
app swings around its anchor, and writes the anchor (in sprite coordinates 0..1) to tape_anchors.json.

Taps: cut the tape (a band springs open), unwind half, pull the rest off.
"""
import importlib
import json
import math
import os
import random

import bpy
import bmesh
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view
from PIL import Image, ImageDraw, ImageFont

import kit
from kit import *

STEPS = 3
# bands per kind: (count, sag) — dense on solid boxes, sparse and saggy on soft or awkward shapes
WRAP = {"safe": (9, 0.0), "steel_plate": (8, 0.0), "glass_case": (8, 0.0), "bricks": (8, 0.0), "grate": (7, 0.02),
        "box": (5, 0.05), "sandbags": (6, 0.06), "beam": (6, 0.04), "chain_padlock": (6, 0.05), "handcuffs": (5, 0.06),
        "u_lock": (6, 0.04), "zip_ties": (6, 0.05), "barbed_wire": (5, 0.07), "rope": (6, 0.05), "duct_tape": (6, 0.04)}


def tape_texture():
    path = os.path.join(kit.HERE, "out", "_tape.png")
    if os.path.exists(path):
        return path
    os.makedirs(os.path.dirname(path), exist_ok=True)
    W, H = 2048, 128
    im = Image.new("RGBA", (W, H), (245, 245, 242, 255)); d = ImageDraw.Draw(im)
    x = -H
    while x < W + H:   # diagonal red bands
        d.polygon([(x, H), (x + H * 0.9, 0), (x + H * 1.9, 0), (x + H, H)], fill=(200, 12, 12, 255)); x += int(H * 2.2)
    d.rectangle((0, H * 0.24, W, H * 0.76), fill=(250, 250, 248, 255))
    try:
        f = ImageFont.truetype("DejaVuSans-Bold.ttf", int(H * 0.36))
    except Exception:
        f = ImageFont.load_default()
    txt = "POLIZIA  ·  NON OLTREPASSARE  ·  "
    tx = 10
    while tx < W:
        d.text((tx, H * 0.29), txt, fill=(15, 15, 15, 255), font=f); tx += d.textlength(txt, font=f)
    im.save(path)
    return path


def tape_mat():
    m = image_mat("tape", tape_texture(), rough=0.28)
    p = m.node_tree.nodes['Principled BSDF']
    p.inputs['Coat Weight'].default_value = 0.5
    return m


def hull_of(objs):
    """Convex hull of everything that makes up the protection (evaluated meshes), as one object."""
    dg = bpy.context.evaluated_depsgraph_get()
    bm = bmesh.new()
    for o in objs:
        if o.type not in {'MESH', 'CURVE', 'FONT'} or o.hide_render:
            continue
        me = o.evaluated_get(dg).to_mesh()
        tmp = bmesh.new(); tmp.from_mesh(me); tmp.transform(o.matrix_world)
        for v in tmp.verts:
            bm.verts.new(v.co)
        tmp.free(); o.evaluated_get(dg).to_mesh_clear()
    bmesh.ops.convex_hull(bm, input=bm.verts)
    # include the button disc so loops pass round it too
    for k in range(32):
        a = k / 32 * 2 * math.pi
        bm.verts.new((math.cos(a), math.sin(a), 0.0))
    bm2 = bmesh.new()
    for v in bm.verts: bm2.verts.new(v.co)
    bmesh.ops.convex_hull(bm2, input=bm2.verts)
    me = bpy.data.meshes.new("hull"); bm2.to_mesh(me); bm.free(); bm2.free()
    h = bpy.data.objects.new("hull", me); bpy.context.scene.collection.objects.link(h)
    h.hide_render = True
    return h


def band(hull, ang, off, width, sag, m, seed, open_cut=False):
    """One loop of tape: a vertical ring around the hull in the plane at angle `ang`, offset sideways, shrink-wrapped on."""
    random.seed(seed)
    bb = [hull.matrix_world @ Vector(c) for c in hull.bound_box]
    R = max(max(abs(v.x) for v in bb), max(abs(v.y) for v in bb)) + 0.6
    top = max(v.z for v in bb) + 0.6
    d = Vector((math.cos(ang), math.sin(ang), 0)); n = Vector((-math.sin(ang), math.cos(ang), 0))
    bm = bmesh.new()
    N = 160
    ring = []
    for i in range(N):
        t = i / N * 2 * math.pi
        # an ellipse around the hull in the (d, z) plane
        p = d * (math.cos(t) * R) + Vector((0, 0, top / 2 + math.sin(t) * top)) + n * off
        ring.append(p)
    verts_l = [bm.verts.new(p - n * width / 2) for p in ring]
    verts_r = [bm.verts.new(p + n * width / 2) for p in ring]
    last = N if not open_cut else N - 18
    for i in range(last):
        j = (i + 1) % N
        if open_cut and i < 18:
            continue
        bm.faces.new((verts_l[i], verts_l[j], verts_r[j], verts_r[i]))
    for v in list(bm.verts):
        if not v.link_faces: bm.verts.remove(v)
    me = bpy.data.meshes.new("band"); bm.to_mesh(me); bm.free()
    # UVs along the tape
    uv = me.uv_layers.new()
    for poly in me.polygons:
        for li in poly.loop_indices:
            vi = me.loops[li].vertex_index
            co = me.vertices[vi].co
            along = (math.atan2(co.z - top / 2, co.dot(d)) % (2 * math.pi)) / (2 * math.pi) * (2 * math.pi * (R + top) / 2) / (width * 15)
            across = (co.dot(n) - off) / width + 0.5
            uv.data[li].uv = (along + seed * 0.37, across)
    o = bpy.data.objects.new("band", me); bpy.context.scene.collection.objects.link(o)
    sw = o.modifiers.new("wrap", 'SHRINKWRAP'); sw.target = hull; sw.wrap_method = 'NEAREST_SURFACEPOINT'
    sw.wrap_mode = 'OUTSIDE_SURFACE'; sw.offset = 0.012 + sag * random.random()
    sub = o.modifiers.new("sub", 'SUBSURF'); sub.levels = sub.render_levels = 1
    wr = o.modifiers.new("wrinkle", 'DISPLACE'); t = bpy.data.textures.new("w", 'CLOUDS'); t.noise_scale = 0.12
    wr.texture = t; wr.strength = 0.012 + sag * 0.3; wr.texture_coords = 'GLOBAL'
    so = o.modifiers.new("th", 'SOLIDIFY'); so.thickness = 0.004
    o.data.materials.append(m)
    return o


def build_tape(kind, step):
    mod = importlib.import_module(kind)
    mod.build(0)
    objs = [o for o in bpy.data.objects if o.name not in ("cam", "key", "fill", "rim", "catcher")]
    hull = hull_of(objs)
    for o in objs:
        if o.type in {'MESH', 'CURVE', 'FONT'}:
            o.is_shadow_catcher = True
    m = tape_mat()
    n, sag = WRAP.get(kind, (6, 0.04))
    random.seed(hash(kind) & 0xffff)
    bands = []
    for i in range(n):
        ang = math.radians(random.uniform(-70, 70) + (i % 2) * 90)
        off = random.uniform(-0.9, 0.9)
        bands.append((ang, off))
    keep = n if step == 0 else (n if step == 1 else n // 2)
    for i, (ang, off) in enumerate(bands[:keep]):
        band(hull, ang, off, 0.3, sag, m, i, open_cut=(step >= 1 and i == keep - 1))
    return bands, hull


def anchor(kind):
    """Where the loose end leaves the wrapping, in sprite coordinates (0..1, y down)."""
    kit.reset(res=64, samples=1)
    bands, hull = build_tape(kind, 0)
    bb = [hull.matrix_world @ Vector(c) for c in hull.bound_box]
    x = max(v.x for v in bb) * 0.85; y = min(v.y for v in bb) * 0.6; z = max(v.z for v in bb) * 0.6
    cam = bpy.context.scene.camera
    co = world_to_camera_view(bpy.context.scene, cam, Vector((x, y, z)))
    return [round(co.x, 4), round(1 - co.y, 4)]


def tail():
    """The cut end hanging loose: a long strip with a gentle twist, anchored at the top centre of the sprite."""
    m = tape_mat()
    bm = bmesh.new()
    N = 60; L = 1.9; w = 0.3
    rows = []
    for i in range(N + 1):
        t = i / N
        y = 1.95 - t * L
        x = 0.06 * math.sin(t * 3.2) + 0.05 * t
        tw = 0.45 * math.sin(t * 2.4 + 0.4)
        a = Vector((x - math.cos(tw) * w / 2, y, 0.3 + math.sin(tw) * w / 2))
        b = Vector((x + math.cos(tw) * w / 2, y, 0.3 - math.sin(tw) * w / 2))
        rows.append((bm.verts.new(a), bm.verts.new(b)))
    for i in range(N):
        bm.faces.new((rows[i][0], rows[i + 1][0], rows[i + 1][1], rows[i][1]))
    me = bpy.data.meshes.new("tail"); bm.to_mesh(me); bm.free()
    uv = me.uv_layers.new()
    for poly in me.polygons:
        for li in poly.loop_indices:
            vi = me.loops[li].vertex_index
            uv.data[li].uv = ((vi // 2) / N * L / (w * 15), vi % 2)
    o = bpy.data.objects.new("tail", me); bpy.context.scene.collection.objects.link(o)
    so = o.modifiers.new("th", 'SOLIDIFY'); so.thickness = 0.004
    o.data.materials.append(m)
    bpy.data.objects["catcher"].hide_render = True
