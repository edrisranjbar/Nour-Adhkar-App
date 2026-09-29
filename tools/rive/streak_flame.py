#!/usr/bin/env python3
"""
Generates app/src/main/res/raw/streak_flame.riv, the streak dialog's flame, in Rive's runtime
binary format (major version 7). It is written by code rather than in the Rive editor so the
asset is reviewable and reproducible; it can later be replaced by an editor-made file with the
same contract:

  artboard        "StreakFlame"
  state machine   "StreakFlame"
  inputs          "celebrate" (trigger): replays the pop-in burst.
                  "level" (number 0..3): 0 = small first-days flame ... 3 = large, hot flame.

Layers: "idle" loops a layered flicker with rising embers; "celebrate" plays once on entry
(squash-and-stretch pop, glow flash, spark ring) and again whenever "celebrate" fires. "level"
blends between flame sizes and colours.

Format reference: rive-runtime (include/rive/generated/*_base.hpp, src/file.cpp,
runtime_header.hpp). Keys below are those from rive-runtime as of 2026-09.

Usage: python3 tools/rive/streak_flame.py [output.riv]
"""
import math
import os
import struct
import sys

# ---------------------------------------------------------------------------------------------
# Binary writer

UINT, STRING, DOUBLE, COLOR, BOOL = "uint", "string", "double", "color", "bool"

# property key -> (name, field type)
P = {
    "name": (4, STRING), "parentId": (5, UINT),
    "width": (7, DOUBLE), "height": (8, DOUBLE),
    "originX": (11, DOUBLE), "originY": (12, DOUBLE),
    "x": (13, DOUBLE), "y": (14, DOUBLE),
    "rotation": (15, DOUBLE), "scaleX": (16, DOUBLE), "scaleY": (17, DOUBLE), "opacity": (18, DOUBLE),
    "ellipseWidth": (20, DOUBLE), "ellipseHeight": (21, DOUBLE),
    "blendModeValue": (23, UINT),
    "vx": (24, DOUBLE), "vy": (25, DOUBLE),
    "isClosed": (32, BOOL),
    "colorValue": (37, COLOR),
    "stopColor": (38, COLOR), "stopPosition": (39, DOUBLE),
    "gStartX": (42, DOUBLE), "gStartY": (33, DOUBLE), "gEndX": (34, DOUBLE), "gEndY": (35, DOUBLE),
    "gOpacity": (46, DOUBLE),
    "objectId": (51, UINT), "propertyKey": (53, UINT),
    "animName": (55, STRING), "fps": (56, UINT), "duration": (57, UINT), "loopValue": (59, UINT),
    "x1": (63, DOUBLE), "y1": (64, DOUBLE), "x2": (65, DOUBLE), "y2": (66, DOUBLE),
    "frame": (67, UINT), "interpolationType": (68, UINT), "interpolatorId": (69, UINT),
    "kfValue": (70, DOUBLE), "kfColor": (88, COLOR),
    "inRotation": (84, DOUBLE), "inDistance": (85, DOUBLE),
    "outRotation": (86, DOUBLE), "outDistance": (87, DOUBLE),
    "smName": (138, STRING), "numberValue": (140, DOUBLE),
    "animationId": (149, UINT), "stateToId": (151, UINT), "transitionFlags": (152, UINT),
    "transitionDuration": (158, UINT), "exitTime": (160, UINT), "inputId": (155, UINT),
    "conditionOp": (156, UINT), "conditionValue": (157, DOUBLE),
    "blendInputId": (167, UINT), "blendValue": (166, DOUBLE), "blendAnimationId": (165, UINT),
}

T = {
    "Backboard": 23, "Artboard": 1, "Node": 2, "Shape": 3, "Ellipse": 4, "PointsPath": 16,
    "CubicDetachedVertex": 6, "Fill": 20, "SolidColor": 18, "LinearGradient": 22,
    "RadialGradient": 17, "GradientStop": 19, "LinearAnimation": 31, "KeyedObject": 25,
    "KeyedProperty": 26, "KeyFrameDouble": 30, "KeyFrameColor": 37, "CubicEaseInterpolator": 28,
    "StateMachine": 53, "StateMachineNumber": 56, "StateMachineTrigger": 58,
    "StateMachineLayer": 57, "EntryState": 63, "AnyState": 62, "ExitState": 64,
    "AnimationState": 61, "StateTransition": 65, "TransitionTriggerCondition": 68,
    "BlendState1D": 76, "BlendAnimation1D": 75,
}

# Property keys animated below (Vertex.x/y, Node transforms, opacity, gradient stop colour).
KEY_X, KEY_Y, KEY_ROT, KEY_SX, KEY_SY, KEY_OPACITY = 13, 14, 15, 16, 17, 18
KEY_VX, KEY_VY = 24, 25
KEY_STOP_COLOR = 38


def varuint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def encode(ftype, value):
    if ftype == UINT:
        return varuint(int(value))
    if ftype == DOUBLE:
        return struct.pack("<f", float(value))
    if ftype == STRING:
        data = value.encode("utf-8")
        return varuint(len(data)) + data
    if ftype == COLOR:
        return struct.pack("<I", value & 0xFFFFFFFF)
    if ftype == BOOL:
        return bytes([1 if value else 0])
    raise ValueError(ftype)


class RiveWriter:
    def __init__(self):
        self.out = bytearray(b"RIVE")
        self.out += varuint(7) + varuint(0) + varuint(0)  # major, minor, file id
        self.out += varuint(0)  # empty property table of contents: every key is known

    def obj(self, type_name, **props):
        self.out += varuint(T[type_name])
        for key, value in props.items():
            if value is None:
                continue
            pkey, ftype = P[key]
            self.out += varuint(pkey) + encode(ftype, value)
        self.out += varuint(0)


# ---------------------------------------------------------------------------------------------
# Scene description


def argb(hex_rgb, alpha=1.0):
    return (int(round(alpha * 255)) << 24) | int(hex_rgb.lstrip("#"), 16)


class Artboard:
    """Collects artboard components in file order; each component's id is its index."""

    def __init__(self, name, width, height):
        self.components = [("Artboard", dict(name=name, width=width, height=height, originX=0, originY=0))]
        self.names = {}

    def add(self, type_name, name=None, parent=0, **props):
        index = len(self.components)
        if type_name not in ("CubicEaseInterpolator",):
            props = dict(parentId=parent, **props)
        if name:
            props = dict(name=name, **props)
            self.names[name] = index
        self.components.append((type_name, props))
        return index


def flame_path_points(scale=1.0, lean=0.0):
    """Stylised flame outline, base-centre at (0, 0), tip up. Returns (point, in_ctrl, out_ctrl)."""
    # Smooth points have collinear in/out handles; the tip, tongue tip and dip are sharp.
    pts = [
        # tip: both handles below it so the point stays crisp
        ((6 + lean, -228), (-6 + lean, -196), (20 + lean, -196)),
        # right flank
        ((66, -94), (62, -152), (70, -40)),
        # rounded base
        ((0, 0), (44, 0), (-44, 0)),
        # left flank
        ((-68, -70), (-70, -26), (-66, -114)),
        # left tongue tip
        ((-44, -164), (-64, -134), (-36, -148)),
        # dip between tongue and main tip
        ((-18, -130), (-26, -136), (-10, -162)),
    ]
    return [tuple((p[0] * scale, p[1] * scale) for p in triple) for triple in pts]


def detached(point, cin, cout):
    px, py = point
    return dict(
        vx=px, vy=py,
        inRotation=math.atan2(cin[1] - py, cin[0] - px), inDistance=math.hypot(cin[0] - px, cin[1] - py),
        outRotation=math.atan2(cout[1] - py, cout[0] - px), outDistance=math.hypot(cout[0] - px, cout[1] - py),
    )


def build():
    # Tall enough for the stretch peak and the level-3 pose (scale 1.1) without clipping the tip.
    W, H = 300, 340
    ab = Artboard("StreakFlame", W, H)
    ease = ab.add("CubicEaseInterpolator", x1=0.42, y1=0.0, x2=0.58, y2=1.0)   # ease-in-out
    ease_out = ab.add("CubicEaseInterpolator", x1=0.16, y1=1.0, x2=0.3, y2=1.0)  # soft ease-out
    back_out = ab.add("CubicEaseInterpolator", x1=0.34, y1=1.56, x2=0.64, y2=1.0)  # overshoot

    base_x, base_y = W / 2, 300
    root = ab.add("Node", "flameRoot", x=base_x, y=base_y)

    # Rising embers (idle) and a ring of sparks (celebrate).
    embers = []
    for i in range(6):
        e = ab.add("Shape", f"ember{i}", parent=root, x=0, y=-60, opacity=0)
        ab.add("Ellipse", parent=e, ellipseWidth=7 - (i % 3), ellipseHeight=7 - (i % 3))
        f = ab.add("Fill", parent=e)
        ab.add("SolidColor", parent=f, colorValue=argb("#FFD36B" if i % 2 else "#FF9A3C"))
        embers.append(e)
    sparks = []
    for i in range(10):
        s = ab.add("Shape", f"spark{i}", parent=root, x=0, y=-110, opacity=0)
        ab.add("Ellipse", parent=s, ellipseWidth=8, ellipseHeight=8)
        f = ab.add("Fill", parent=s)
        ab.add("SolidColor", parent=f, colorValue=argb("#FFE08A" if i % 2 else "#FFFFFF"))
        sparks.append(s)

    layers = {}

    def flame_layer(name, scale, lean, top, bottom, y_offset=0.0):
        shape = ab.add("Shape", name, parent=root, x=0, y=y_offset)
        path = ab.add("PointsPath", parent=shape, isClosed=True)
        vertex_ids = [ab.add("CubicDetachedVertex", parent=path, **detached(*v)) for v in flame_path_points(scale, lean)]
        fill = ab.add("Fill", parent=shape)
        grad = ab.add("LinearGradient", parent=fill, gStartX=0, gStartY=-222 * scale, gEndX=0, gEndY=0, gOpacity=1)
        top_stop = ab.add("GradientStop", parent=grad, stopColor=argb(top), stopPosition=0)
        bottom_stop = ab.add("GradientStop", parent=grad, stopColor=argb(bottom), stopPosition=1)
        layers[name] = dict(shape=shape, vertices=vertex_ids, scale=scale, lean=lean, top=top_stop, bottom=bottom_stop)

    # Rive draws objects that come earlier in the file on top, so add front-most first:
    # particles, bright core, warm middle, deep orange body, then the glow behind everything.
    flame_layer("core", 0.42, 6.0, "#FFE27A", "#FFF7D6", y_offset=-8)
    flame_layer("middle", 0.7, 4.0, "#FF8C1A", "#FFC23D", y_offset=-4)
    flame_layer("outer", 1.0, 0.0, "#FF4E1A", "#FF9A1F")

    # Soft glow behind the flame.
    glow = ab.add("Shape", "glow", parent=root, x=0, y=-100)
    ab.add("Ellipse", parent=glow, ellipseWidth=250, ellipseHeight=250)
    glow_fill = ab.add("Fill", parent=glow)
    glow_grad = ab.add("RadialGradient", parent=glow_fill, gStartX=0, gStartY=0, gEndX=125, gEndY=0, gOpacity=1)
    ab.add("GradientStop", parent=glow_grad, stopColor=argb("#FFB547", 0.55), stopPosition=0)
    ab.add("GradientStop", parent=glow_grad, stopColor=argb("#FF8A1F", 0.18), stopPosition=0.55)
    ab.add("GradientStop", parent=glow_grad, stopColor=argb("#FF8A1F", 0.0), stopPosition=1)

    # -----------------------------------------------------------------------------------------
    # Animations: list of (name, fps, duration, loop, {object: {property: [(frame, value, interp)]}})

    animations = []

    def keys(track, obj, prop, frames, interp=ease):
        track.setdefault(obj, {})[prop] = [(f, v, interp) for f, v in frames]

    # idle: 4 s seamless loop at 60 fps; every track starts and ends on the same value.
    idle = {}
    L = 240
    o, m, c = layers["outer"], layers["middle"], layers["core"]
    keys(idle, o["shape"], KEY_SY, [(0, 1.0), (50, 1.045), (110, 0.975), (170, 1.03), (240, 1.0)])
    keys(idle, o["shape"], KEY_SX, [(0, 1.0), (50, 0.975), (110, 1.02), (170, 0.985), (240, 1.0)])
    keys(idle, o["shape"], KEY_ROT, [(0, 0.0), (80, 0.025), (160, -0.02), (240, 0.0)])
    tip, _, _ = flame_path_points(1.0)[0]
    keys(idle, o["vertices"][0], KEY_VX, [(0, tip[0]), (60, tip[0] - 14), (130, tip[0] + 12), (190, tip[0] - 6), (240, tip[0])])
    keys(idle, o["vertices"][0], KEY_VY, [(0, tip[1]), (60, tip[1] - 10), (130, tip[1] + 6), (190, tip[1] - 4), (240, tip[1])])
    tongue = flame_path_points(1.0)[4][0]
    keys(idle, o["vertices"][4], KEY_VX, [(0, tongue[0]), (45, tongue[0] - 8), (120, tongue[0] + 5), (200, tongue[0] - 4), (240, tongue[0])])
    keys(idle, o["vertices"][4], KEY_VY, [(0, tongue[1]), (45, tongue[1] - 12), (120, tongue[1] + 8), (200, tongue[1] - 6), (240, tongue[1])])

    keys(idle, m["shape"], KEY_SY, [(0, 1.0), (40, 1.06), (100, 0.96), (160, 1.05), (200, 0.98), (240, 1.0)])
    keys(idle, m["shape"], KEY_SX, [(0, 1.0), (40, 0.965), (100, 1.03), (160, 0.97), (200, 1.01), (240, 1.0)])
    keys(idle, m["shape"], KEY_ROT, [(0, 0.0), (60, -0.035), (140, 0.03), (240, 0.0)])
    mtip = flame_path_points(0.7, 4.0)[0][0]
    keys(idle, m["vertices"][0], KEY_VX, [(0, mtip[0]), (50, mtip[0] + 10), (120, mtip[0] - 12), (180, mtip[0] + 6), (240, mtip[0])])

    keys(idle, c["shape"], KEY_SY, [(0, 1.0), (30, 1.08), (60, 0.95), (100, 1.06), (140, 0.97), (190, 1.07), (240, 1.0)])
    keys(idle, c["shape"], KEY_SX, [(0, 1.0), (30, 0.95), (60, 1.04), (100, 0.96), (140, 1.03), (190, 0.95), (240, 1.0)])
    keys(idle, c["shape"], KEY_ROT, [(0, 0.0), (70, 0.04), (150, -0.04), (240, 0.0)])

    keys(idle, glow, KEY_OPACITY, [(0, 0.85), (60, 1.0), (130, 0.8), (190, 0.97), (240, 0.85)])
    keys(idle, glow, KEY_SX, [(0, 1.0), (60, 1.06), (130, 0.97), (190, 1.04), (240, 1.0)])
    keys(idle, glow, KEY_SY, [(0, 1.0), (60, 1.06), (130, 0.97), (190, 1.04), (240, 1.0)])

    # Embers rise ~200 px over 1.6 s with a sideways drift, staggered through the loop. Frames wrap
    # around the loop end so the motion is continuous.
    ember_x = [-26, 18, -8, 30, -34, 6]
    for i, e in enumerate(embers):
        start = (i * 40) % L
        life = 96
        drift = 22 if i % 2 else -20
        track_y, track_x, track_o = [], [], []
        for step, t in enumerate((0, 0.25, 0.5, 0.75, 1.0)):
            f = start + int(life * t)
            track_y.append((f, -70 - 190 * t))
            track_x.append((f, ember_x[i] + drift * math.sin(t * math.pi * 1.5)))
            track_o.append((f, [0.0, 1.0, 0.85, 0.4, 0.0][step]))

        def wrap(track, rest):
            # Split keys that pass the loop end; hold the rest value outside the ember's life.
            inside = [(f, v) for f, v in track if f <= L]
            outside = [(f - L, v) for f, v in track if f > L]
            result = outside + ([(outside[-1][0] + 1, rest)] if outside else [(0, rest)])
            result += [(max(inside[0][0] - 1, result[-1][0] + 1), rest)] if inside[0][0] > result[-1][0] + 1 else []
            result += inside
            if result[-1][0] < L:
                result += [(min(result[-1][0] + 1, L), rest), (L, rest)]
            # de-duplicate frames, keep order
            seen, clean = set(), []
            for f, v in result:
                if f not in seen and 0 <= f <= L:
                    seen.add(f)
                    clean.append((f, v))
            return sorted(clean)

        keys(idle, e, KEY_Y, wrap(track_y, -70), interp=None)
        keys(idle, e, KEY_X, wrap(track_x, ember_x[i]), interp=ease)
        keys(idle, e, KEY_OPACITY, wrap(track_o, 0.0), interp=None)
    animations.append(("idle", 60, L, 1, idle))

    # celebrate: 1.3 s one-shot pop-in with squash and stretch, glow flash and a spark ring.
    cel = {}
    keys(cel, root, KEY_SX, [(0, 0.1), (14, 1.12), (24, 1.2), (34, 0.92), (46, 1.04), (60, 0.99), (72, 1.0)], ease_out)
    keys(cel, root, KEY_SY, [(0, 0.1), (14, 1.16), (24, 0.84), (34, 1.07), (46, 0.97), (60, 1.01), (72, 1.0)], ease_out)
    keys(cel, root, KEY_Y, [(0, base_y + 16), (14, base_y - 10), (26, base_y + 4), (40, base_y - 2), (72, base_y)], ease_out)
    keys(cel, glow, KEY_OPACITY, [(0, 0.0), (16, 1.0), (40, 1.0), (78, 0.85)], ease)
    for i, s in enumerate(sparks):
        angle = -math.pi / 2 + (i - 4.5) * (math.pi * 1.4 / 9)
        radius = 125 + (18 if i % 2 else 0)
        sx, sy = radius * math.cos(angle), -110 + radius * math.sin(angle)
        keys(cel, s, KEY_X, [(0, 0), (12, 0), (44, sx), (78, sx * 1.08)], ease_out)
        keys(cel, s, KEY_Y, [(0, -110), (12, -110), (44, sy), (78, sy + 24)], ease_out)
        keys(cel, s, KEY_OPACITY, [(0, 0), (16, 0), (22, 1), (46, 0.9), (78, 0)], ease)
        keys(cel, s, KEY_SX, [(0, 0.4), (16, 1.2), (78, 0.3)], ease)
        keys(cel, s, KEY_SY, [(0, 0.4), (16, 1.2), (78, 0.3)], ease)
    animations.append(("celebrate", 60, 78, 0, cel))
    # Empty pose the celebrate layer rests in, so the any-state trigger can re-enter "celebrate"
    # (Rive ignores an any-state transition into the state that is already active).
    animations.append(("rest", 60, 1, 0, {}))

    # level_small / level_large: static poses blended by the "level" input (0..3).
    small, large = {}, {}
    keys(small, o["shape"], KEY_SX, [(0, 0.82)]) ; keys(small, o["shape"], KEY_SY, [(0, 0.8)])
    keys(large, o["shape"], KEY_SX, [(0, 1.06)]) ; keys(large, o["shape"], KEY_SY, [(0, 1.1)])
    small_colors = {"outer": ("#FF7A2E", "#FFB547"), "middle": ("#FFA33D", "#FFD166"), "core": ("#FFE9A3", "#FFF9E6")}
    large_colors = {"outer": ("#F2361B", "#FF8A1F"), "middle": ("#FF7A1A", "#FFC23D"), "core": ("#FFF1A8", "#FFFFFF")}
    for name, layer in layers.items():
        for anim, colors in ((small, small_colors), (large, large_colors)):
            anim.setdefault(layer["top"], {})[KEY_STOP_COLOR] = [(0, argb(colors[name][0]), "color")]
            anim.setdefault(layer["bottom"], {})[KEY_STOP_COLOR] = [(0, argb(colors[name][1]), "color")]
    animations.append(("level_small", 60, 1, 0, small))
    animations.append(("level_large", 60, 1, 0, large))
    return ab, animations, dict(ease=ease)


def write(path):
    ab, animations, _ = build()
    w = RiveWriter()
    w.obj("Backboard")
    for type_name, props in ab.components:
        w.obj(type_name, **props)

    for name, fps, duration, loop, tracks in animations:
        w.obj("LinearAnimation", animName=name, fps=fps, duration=duration, loopValue=loop)
        for obj, props in tracks.items():
            w.obj("KeyedObject", objectId=obj)
            for prop, frames in props.items():
                w.obj("KeyedProperty", propertyKey=prop)
                for frame, value, interp in frames:
                    if interp == "color":
                        w.obj("KeyFrameColor", frame=frame, interpolationType=1, kfColor=value)
                    elif interp is None:
                        w.obj("KeyFrameDouble", frame=frame, interpolationType=1, kfValue=value)
                    else:
                        w.obj("KeyFrameDouble", frame=frame, interpolationType=2, interpolatorId=interp, kfValue=value)

    anim_index = {a[0]: i for i, a in enumerate(animations)}
    w.obj("StateMachine", animName="StreakFlame")
    w.obj("StateMachineTrigger", smName="celebrate")   # input 0
    w.obj("StateMachineNumber", smName="level", numberValue=1)  # input 1

    # Layer "idle": Entry(0) -> Idle(3)
    w.obj("StateMachineLayer", smName="idle")
    w.obj("EntryState"); w.obj("StateTransition", stateToId=3)
    w.obj("AnyState")
    w.obj("ExitState")
    w.obj("AnimationState", animationId=anim_index["idle"])

    # Layer "celebrate": Entry(0) -> Celebrate(3) -> (when finished) Rest(4);
    # Any(1) --celebrate--> Celebrate(3).
    exit_at_end = 4 | 8  # EnableExitTime | ExitTimeIsPercentage
    w.obj("StateMachineLayer", smName="celebrate")
    w.obj("EntryState"); w.obj("StateTransition", stateToId=3)
    w.obj("AnyState"); w.obj("StateTransition", stateToId=3); w.obj("TransitionTriggerCondition", inputId=0)
    w.obj("ExitState")
    w.obj("AnimationState", animationId=anim_index["celebrate"])
    w.obj("StateTransition", stateToId=4, transitionFlags=exit_at_end, exitTime=100)
    w.obj("AnimationState", animationId=anim_index["rest"])

    # Layer "level": Entry(0) -> Blend(3), blending small (0) .. large (3) by input 1.
    w.obj("StateMachineLayer", smName="level")
    w.obj("EntryState"); w.obj("StateTransition", stateToId=3)
    w.obj("AnyState")
    w.obj("ExitState")
    w.obj("BlendState1D", blendInputId=1)
    w.obj("BlendAnimation1D", blendAnimationId=anim_index["level_small"], blendValue=0)
    w.obj("BlendAnimation1D", blendAnimationId=anim_index["level_large"], blendValue=3)

    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "wb") as f:
        f.write(w.out)
    return len(w.out)


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    default = os.path.join(here, "..", "..", "app", "src", "main", "res", "raw", "streak_flame.riv")
    target = sys.argv[1] if len(sys.argv) > 1 else default
    print(f"wrote {write(target)} bytes to {os.path.normpath(target)}")
