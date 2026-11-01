"""Red 'break in case of emergency' box with a glass front. Taps: crack, smash, remove."""
import math
import random
from kit import *

STEPS = 3


def build(step):
    random.seed(5)
    red = painted_metal("case", (0.55, 0.02, 0.02), wear=0.5, rough=0.32)
    cube((2.7, 2.7, 0.5), (0, 0, 0.25), mat=red, bevel=0.06, segs=4)
    # the case is open at the back: behind the glass you see what's under it
    hole(cube((2.2, 2.2, 0.02), (0, -0.05, 0.505)))
    # label strip
    cube((2.2, 0.3, 0.01), (0, 1.15, 0.51), mat=plastic("w", (0.92, 0.92, 0.9), 0.5))
    text_obj("IN CASO DI EMERGENZA", (0, 1.15, 0.52), 0.13, plastic("ink", (0.6, 0.02, 0.02), 0.4))
    # the button glows through (cut a window)
    g = glass()
    if step == 0:
        cube((2.2, 2.2, 0.04), (0, -0.05, 0.56), mat=g)
    elif step == 1:
        cube((2.2, 2.2, 0.04), (0, -0.05, 0.56), mat=g)
        # crack star from the impact point
        crack = plastic("crack", (0.95, 0.97, 1.0), 0.1)
        for k in range(11):
            a = random.uniform(0, 2 * math.pi); L = random.uniform(0.4, 1.0)
            cube((L, 0.008, 0.002), (0.2 + math.cos(a) * L / 2, -0.2 + math.sin(a) * L / 2, 0.582), rot=(0, 0, a), mat=crack, smooth=False)
        torus(0.12, 0.006, (0.2, -0.2, 0.582), mat=crack)
    else:
        # only jagged shards left in the frame + a few on the case
        for k in range(14):
            side = k % 4
            x = random.uniform(-1.0, 1.0); y = random.uniform(-1.0, 1.0)
            if side == 0: y = 1.0
            elif side == 1: y = -1.1
            elif side == 2: x = -1.05
            else: x = 1.05
            s = cube((random.uniform(0.15, 0.45), random.uniform(0.1, 0.3), 0.03), (x, y, 0.56), rot=(0, 0, random.uniform(0, 3)), mat=g, smooth=False)
    # small hammer on a chain, classic
    hm = steel("hammer", 0.3, (0.6, 0.6, 0.6), brushed=False)
    cube((0.12, 0.6, 0.08), (1.55, -0.5, 0.15), mat=plastic("handle", (0.7, 0.05, 0.03), 0.4), bevel=0.02)
    cube((0.3, 0.1, 0.1), (1.55, -0.82, 0.15), mat=hm, bevel=0.01)
