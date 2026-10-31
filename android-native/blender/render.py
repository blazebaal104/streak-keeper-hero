"""
Renders every NNN protection at every tap state into sprites/<kind>_<step>.png (transparent, with contact shadow).

    python3.11 render.py                 # all kinds
    python3.11 render.py safe beam       # some kinds
    PREVIEW=1 python3.11 render.py safe  # quick low-res draft
"""
import importlib
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "kinds"))
import kit  # noqa: E402

KINDS = ["safe", "beam", "steel_plate", "chain_padlock", "handcuffs", "u_lock", "zip_ties", "barbed_wire", "glass_case",
         "rope", "grate", "box", "sandbags", "bricks", "duct_tape"]
OUT = os.environ.get("OUT", os.path.join(kit.HERE, "out"))


def main():
    kinds = sys.argv[1:] or KINDS
    preview = os.environ.get("PREVIEW") == "1"
    os.makedirs(OUT, exist_ok=True)
    if kinds and kinds[0] == "tape":
        return render_tape(kinds[1:] or KINDS, preview)
    for k in kinds:
        mod = importlib.import_module(k)
        steps = [int(s) for s in os.environ["ONLY"].split(",")] if os.environ.get("ONLY") else range(mod.STEPS)
        for s in steps:
            kit.reset(res=384 if preview else 640, samples=24 if preview else 48)
            mod.build(s)
            kit.render(os.path.join(OUT, f"{k}_{s}.png"))
            print("rendered", k, s, flush=True)


def render_tape(kinds, preview):
    import json
    import tape
    anchors_path = os.path.join(OUT, "tape_anchors.json")
    anchors = json.load(open(anchors_path)) if os.path.exists(anchors_path) else {}
    for k in kinds:
        anchors[k] = tape.anchor(k)
        steps = [int(s) for s in os.environ["ONLY"].split(",")] if os.environ.get("ONLY") else range(tape.STEPS - 1)
        for s in steps:   # the last tap removes it: no state to draw
            kit.reset(res=384 if preview else 640, samples=24 if preview else 48)
            tape.build_tape(k, s)
            kit.render(os.path.join(OUT, f"tape-{k}_{s}.png"))
            print("rendered tape", k, s, flush=True)
    kit.reset(res=384 if preview else 640, samples=24 if preview else 48)
    tape.tail()
    kit.render(os.path.join(OUT, "tape-tail_0.png"))
    json.dump(anchors, open(anchors_path, "w"), indent=1)


if __name__ == "__main__":
    main()
