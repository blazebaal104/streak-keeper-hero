"""Combination safe lying face-up on the button. Taps: 4 dial numbers, handle turn (door opens), remove."""
import math
import random
from kit import *

STEPS = 6
CODE = [35, 80, 15, 60]


def build(step):
    random.seed(7)
    paint = painted_metal("safe_paint", (0.045, 0.075, 0.065), wear=0.75)
    dark = plastic("safe_gap", (0.01, 0.01, 0.01), 0.6)
    ch = chrome()
    br = brass()
    blk = plastic("knob", (0.02, 0.02, 0.022), 0.35)
    white = plastic("ticks", (0.85, 0.85, 0.82), 0.4)
    led_off = plastic("led_off", (0.05, 0.08, 0.05), 0.2)
    led_on = emissive("led_on", (0.2, 1.0, 0.35), 18)
    opened = step >= 5

    body = cube((2.6, 2.6, 0.95), (0, 0, 0.475), mat=paint, bevel=0.07, segs=4)
    # feet
    for sx in (-1, 1):
        for sy in (-1, 1):
            cyl(0.12, 0.06, (sx * 1.1, sy * 1.1, 0.03), mat=blk)
    if opened:
        # cavity with the burning button glowing at the bottom
        cav = cube((2.2, 2.2, 0.8), (0, 0, 0.62), mat=dark, bevel=0.03)
        mod = body.modifiers.new("cut", 'BOOLEAN'); mod.object = cav; mod.operation = 'DIFFERENCE'
        bpy.context.view_layer.objects.active = body
        bpy.ops.object.modifier_apply(modifier="cut")
        cav.data.materials.clear(); cav.data.materials.append(plastic("inside", (0.05, 0.05, 0.05), 0.5))
        cav.hide_render = True
        # the safe has no bottom: through it you see whatever is under it
        hole(cube((2.2, 2.2, 0.02), (0, 0, 0.23)))
        hinge_x = -1.18
        door = cube((2.2, 2.2, 0.12), (0, 0, 0), mat=paint, bevel=0.04, segs=3)
        # open: swung ~105° around the left hinge, standing up off the side
        # hinge on the left top edge: the door swings up and back past vertical
        a = math.radians(118)
        door.location = (hinge_x + 1.1 * math.cos(a), 0, 0.95 + 1.1 * math.sin(a))
        door.rotation_euler = (0, -a, 0)
        return
    # door seam (dark groove) + door
    cube((2.24, 2.24, 0.03), (0, 0, 0.955), mat=dark)
    door = cube((2.18, 2.18, 0.09), (0, 0, 0.99), mat=paint, bevel=0.03, segs=3)
    # hinges on the left edge
    for y in (-0.7, 0.7):
        cyl(0.07, 0.42, (-1.13, y, 0.98), rot=(math.radians(90), 0, 0), mat=ch)
    # brand plate
    plate = cube((0.9, 0.22, 0.02), (0.15, 0.82, 1.045), mat=br, bevel=0.01)
    text_obj("SICURA", (0.15, 0.82, 1.06), 0.13, plastic("ink", (0.05, 0.03, 0.01), 0.5), extrude=0.003)
    # dial: ring with ticks and numbers, black knob with grip
    dc = (-0.38, 0.12, 1.035)
    cyl(0.5, 0.04, dc, mat=ch)
    cyl(0.43, 0.05, (dc[0], dc[1], 1.05), mat=blk)
    turned = 0
    for i in range(min(step, 4)):
        turned = CODE[i]
    ang = -math.radians(turned * 3.6)
    for k in range(50):
        a = k / 50 * 2 * math.pi + ang
        L = 0.07 if k % 5 == 0 else 0.035
        r = 0.39
        cube((0.012, L, 0.01), (dc[0] + math.sin(a) * (r - L / 2), dc[1] + math.cos(a) * (r - L / 2), 1.078), rot=(0, 0, -a), mat=white, smooth=False)
    for k in range(0, 100, 20):
        a = k / 100 * 2 * math.pi + ang
        text_obj(str(k), (dc[0] + math.sin(a) * 0.27, dc[1] + math.cos(a) * 0.27, 1.078), 0.075, white, extrude=0.002, rot=(0, 0, -a))
    knob = cyl(0.16, 0.14, (dc[0], dc[1], 1.12), mat=ch, verts=48)
    for k in range(24):
        a = k / 24 * 2 * math.pi
        cube((0.02, 0.03, 0.12), (dc[0] + math.sin(a) * 0.162, dc[1] + math.cos(a) * 0.162, 1.12), rot=(0, 0, -a), mat=ch, smooth=False)
    # index mark at 12 o'clock
    cube((0.02, 0.08, 0.01), (dc[0], dc[1] + 0.52, 1.06), mat=plastic("red", (0.8, 0.05, 0.03), 0.4))
    # four LEDs: one lights per correct number
    for i in range(4):
        cyl(0.035, 0.02, (0.35 + i * 0.13, 0.42, 1.045), mat=led_on if step > i else led_off)
    # handle: hub + three spokes with ball ends (turned at step 4)
    hc = (0.55, -0.35, 1.06)
    rot = math.radians(45 if step >= 4 else 0)
    cyl(0.14, 0.12, (hc[0], hc[1], 1.1), mat=ch)
    for k in range(3):
        a = rot + k * 2 * math.pi / 3
        cx = hc[0] + math.sin(a) * 0.3; cy = hc[1] + math.cos(a) * 0.3
        cyl(0.035, 0.6, ((hc[0] + cx) / 2, (hc[1] + cy) / 2, 1.14), rot=(math.radians(90), 0, -a), mat=ch)
        sphere(0.07, (cx, cy, 1.14), mat=ch)
    # keyhole cover
    cyl(0.07, 0.02, (0.55, 0.25, 1.045), mat=br)
