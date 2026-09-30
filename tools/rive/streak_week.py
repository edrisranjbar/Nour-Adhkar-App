#!/usr/bin/env python3
"""
Generates app/src/main/res/raw/streak_week.riv, the seven-day strip of the streak dialog, in Rive's
runtime binary format. It reuses the writer in streak_flame.py and, like the flame, is written by
code so it is reviewable and reproducible; an editor-made file with the same contract can replace it.

  artboard        "StreakWeek"   336 x 56, seven 48 px columns, circles centred at y = 28
  state machine   "StreakWeek"
  inputs          "day0".."day6" (number): state of each day, oldest (day0) to today (day6):
                      0 missed          1 done            2 covered by a freeze
                      3 freeze pending  4 today, not yet   5 today, done
                  "rtl" (number 0/1): 1 mirrors the column order (today on the left) for RTL layouts.
                  "fillToday" (trigger): plays today's fill (colour change and check pop).

Layers: "layout" (blend by rtl), "day0".."day6" (one blend each, by the matching number), "enter"
(one-shot staggered pop-in on load) and "fillToday" (one-shot, re-triggerable). The day labels and
the "امروز" caption stay in Compose as real text, aligned to the 48 px columns.

Usage: python3 tools/rive/streak_week.py [output.riv]
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import streak_flame as sf  # noqa: E402
from streak_flame import Artboard, RiveWriter, argb, KEY_OPACITY, KEY_SX, KEY_SY, KEY_X  # noqa: E402

# Extra keys from rive-runtime used here (Stroke, StraightVertex).
sf.P.update({"thickness": (47, sf.DOUBLE), "cap": (48, sf.UINT), "join": (49, sf.UINT),
             "radius": (26, sf.DOUBLE)})
sf.T.update({"Stroke": 24, "StraightVertex": 5})
KEY_COLOR = 37  # SolidColor.colorValue

COLUMN, WIDTH, HEIGHT, CY = 48, 7 * 48, 56, 28
DAYS = 7
STATES = ["missed", "done", "frozen", "pending", "today_empty", "today_done"]

WHITE = "#FFFFFF"
#            background,             bg alpha, ring, dot, check, flake, flake colour
POSES = {
    "missed":      ("#FFFFFF", 0.12, 0, 1, 0, 0, "#FFFFFF"),
    "done":        ("#2E7D32", 1.00, 0, 0, 1, 0, "#FFFFFF"),
    "frozen":      ("#1565C0", 1.00, 0, 0, 0, 1, "#FFFFFF"),
    "pending":     ("#FFFFFF", 0.12, 0, 0, 0, 1, "#42A5F5"),
    "today_empty": ("#37474F", 1.00, 1, 1, 0, 0, "#FFFFFF"),
    "today_done":  ("#FF9800", 1.00, 1, 0, 1, 0, "#FFFFFF"),
}
RING_COLOR = "#3A6931"  # SunGold, the dialog's "today" ring


def x_for(index, rtl):
    return COLUMN / 2 + COLUMN * ((DAYS - 1 - index) if rtl else index)


def stroke(ab, shape, color, thickness, alpha=1.0):
    s = ab.add("Stroke", parent=shape, thickness=thickness, cap=1, join=1)
    return ab.add("SolidColor", parent=s, colorValue=argb(color, alpha))


def build():
    ab = Artboard("StreakWeek", WIDTH, HEIGHT)
    ease = ab.add("CubicEaseInterpolator", x1=0.42, y1=0.0, x2=0.58, y2=1.0)
    back = ab.add("CubicEaseInterpolator", x1=0.34, y1=1.56, x2=0.64, y2=1.0)

    days = []
    for i in range(DAYS):
        node = ab.add("Node", f"day{i}", x=x_for(i, False), y=CY)
        # Rive draws earlier objects on top: check, snowflake, dot, ring, then the disc.
        check = ab.add("Shape", f"check{i}", parent=node, opacity=0)
        path = ab.add("PointsPath", parent=check)
        for vx, vy in ((-6.0, 0.5), (-1.8, 5.0), (6.5, -4.5)):
            ab.add("StraightVertex", parent=path, vx=vx, vy=vy, radius=0)
        stroke(ab, check, WHITE, 3.4)

        flake = ab.add("Shape", f"flake{i}", parent=node, opacity=0)
        for deg in (90, 30, 150):
            dx, dy = 6.5 * math.cos(math.radians(deg)), 6.5 * math.sin(math.radians(deg))
            p = ab.add("PointsPath", parent=flake)
            ab.add("StraightVertex", parent=p, vx=-dx, vy=-dy, radius=0)
            ab.add("StraightVertex", parent=p, vx=dx, vy=dy, radius=0)
        flake_color = stroke(ab, flake, WHITE, 2.4)

        dot = ab.add("Shape", f"dot{i}", parent=node, opacity=1)
        ab.add("Ellipse", parent=dot, ellipseWidth=6, ellipseHeight=6)
        df = ab.add("Fill", parent=dot)
        ab.add("SolidColor", parent=df, colorValue=argb(WHITE, 0.35))

        ring = ab.add("Shape", f"ring{i}", parent=node, opacity=0)
        ab.add("Ellipse", parent=ring, ellipseWidth=34, ellipseHeight=34)
        stroke(ab, ring, RING_COLOR, 2.4)

        disc = ab.add("Shape", f"disc{i}", parent=node)
        ab.add("Ellipse", parent=disc, ellipseWidth=33, ellipseHeight=33)
        bf = ab.add("Fill", parent=disc)
        disc_color = ab.add("SolidColor", parent=bf, colorValue=argb(WHITE, 0.12))
        days.append(dict(node=node, check=check, flake=flake, flake_color=flake_color, dot=dot,
                         ring=ring, disc_color=disc_color))

    animations = []

    # Static poses: one per (day, state), blended by the day's number input.
    for i, d in enumerate(days):
        for value, state in enumerate(STATES):
            bg, bg_alpha, ring, dot, check, flake, flake_col = POSES[state]
            track = {}
            track[d["disc_color"]] = {KEY_COLOR: [(0, argb(bg, bg_alpha), "color")]}
            track[d["flake_color"]] = {KEY_COLOR: [(0, argb(flake_col), "color")]}
            track[d["ring"]] = {KEY_OPACITY: [(0, ring, None)]}
            track[d["dot"]] = {KEY_OPACITY: [(0, dot, None)]}
            track[d["check"]] = {KEY_OPACITY: [(0, check, None)]}
            track[d["flake"]] = {KEY_OPACITY: [(0, flake, None)]}
            animations.append((f"day{i}_{state}", 60, 1, 0, track))

    # Column order: left-to-right (LTR) or mirrored (RTL).
    for name, rtl in (("layout_ltr", False), ("layout_rtl", True)):
        animations.append((name, 60, 1, 0, {d["node"]: {KEY_X: [(0, x_for(i, rtl), None)]}
                                            for i, d in enumerate(days)}))

    # enter: staggered pop-in of the seven columns (about 180 ms delay, 55 ms apart).
    enter = {}
    for i, d in enumerate(days):
        start = round((180 + 55 * i) * 60 / 1000)
        enter[d["node"]] = {
            KEY_SX: [(0, 0.4, back), (start, 0.4, back), (start + 24, 1.0, back)],
            KEY_SY: [(0, 0.4, back), (start, 0.4, back), (start + 24, 1.0, back)],
            KEY_OPACITY: [(0, 0.0, ease), (start, 0.0, ease), (start + 8, 1.0, ease)],
        }
    # Hold the first keys with interpolators that start flat.
    animations.append(("enter", 60, round((180 + 55 * 6) * 60 / 1000) + 26, 0, enter))

    # fillToday: today's disc turns orange and the check pops in with a spring-like overshoot.
    today = days[DAYS - 1]
    fill = {
        today["disc_color"]: {KEY_COLOR: [(0, argb("#37474F"), "color"), (6, argb("#37474F"), "color"),
                                          (18, argb("#FF9800"), "color")]},
        today["check"]: {
            KEY_OPACITY: [(0, 0.0, ease), (6, 0.0, ease), (8, 1.0, ease)],
            KEY_SX: [(0, 0.0, back), (6, 0.0, back), (26, 1.0, back)],
            KEY_SY: [(0, 0.0, back), (6, 0.0, back), (26, 1.0, back)],
        },
        today["dot"]: {KEY_OPACITY: [(0, 1.0, ease), (6, 0.0, ease)]},
    }
    animations.append(("fillToday", 60, 30, 0, fill))
    animations.append(("rest", 60, 1, 0, {}))
    return ab, animations


def write(path):
    ab, animations = build()
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
                        w.obj("KeyFrameDouble", frame=frame, interpolationType=2,
                              interpolatorId=interp, kfValue=value)

    index = {a[0]: i for i, a in enumerate(animations)}
    w.obj("StateMachine", animName="StreakWeek")
    for i in range(DAYS):
        w.obj("StateMachineNumber", smName=f"day{i}", numberValue=0)        # inputs 0..6
    w.obj("StateMachineNumber", smName="rtl", numberValue=0)                # input 7
    w.obj("StateMachineTrigger", smName="fillToday")                        # input 8
    RTL_INPUT, FILL_INPUT = DAYS, DAYS + 1

    def blend_layer(name, input_id, members):
        w.obj("StateMachineLayer", smName=name)
        w.obj("EntryState"); w.obj("StateTransition", stateToId=3)
        w.obj("AnyState")
        w.obj("ExitState")
        w.obj("BlendState1D", blendInputId=input_id)
        for anim_name, value in members:
            w.obj("BlendAnimation1D", blendAnimationId=index[anim_name], blendValue=value)

    blend_layer("layout", RTL_INPUT, [("layout_ltr", 0), ("layout_rtl", 1)])
    for i in range(DAYS):
        blend_layer(f"day{i}", i, [(f"day{i}_{state}", value) for value, state in enumerate(STATES)])

    # enter: Entry -> enter (one-shot; it holds its last frame, which is the resting layout).
    w.obj("StateMachineLayer", smName="enter")
    w.obj("EntryState"); w.obj("StateTransition", stateToId=3)
    w.obj("AnyState")
    w.obj("ExitState")
    w.obj("AnimationState", animationId=index["enter"])

    # fillToday: like the flame's "celebrate" layer, it rests in an empty state afterwards so the
    # trigger can fire again.
    exit_at_end = 4 | 8  # EnableExitTime | ExitTimeIsPercentage
    w.obj("StateMachineLayer", smName="fillToday")
    w.obj("EntryState")  # no transition: waits for the trigger
    w.obj("AnyState"); w.obj("StateTransition", stateToId=3); w.obj("TransitionTriggerCondition", inputId=FILL_INPUT)
    w.obj("ExitState")
    w.obj("AnimationState", animationId=index["fillToday"])
    w.obj("StateTransition", stateToId=4, transitionFlags=exit_at_end, exitTime=100)
    w.obj("AnimationState", animationId=index["rest"])

    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "wb") as f:
        f.write(w.out)
    return len(w.out)


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    default = os.path.join(here, "..", "..", "app", "src", "main", "res", "raw", "streak_week.riv")
    target = sys.argv[1] if len(sys.argv) > 1 else default
    print(f"wrote {write(target)} bytes to {os.path.normpath(target)}")
