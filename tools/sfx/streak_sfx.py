"""Synthesises the streak-celebration sound effects into app/src/main/res/raw/.

Run: python tools/sfx/streak_sfx.py   (needs numpy and ffmpeg on PATH)
Pure synthesis, so there are no licensing concerns. Output: mono 44.1 kHz Ogg Vorbis.
"""
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

SR = 44100
OUT = Path(__file__).resolve().parents[2] / "app/src/main/res/raw"
rng = np.random.default_rng(7)


def t(d):
    return np.arange(int(SR * d)) / SR


def env(n, attack=0.004, decay=6.0):
    x = np.arange(n) / SR
    return np.minimum(x / attack, 1.0) * np.exp(-decay * x)


def lowpass(x, k):
    return np.convolve(x, np.ones(k) / k, mode="same")


def bell(freq, d=1.2, decay=4.5):
    x = t(d)
    # Inharmonic partials give a glassy bell colour.
    parts = [(1, 1.0), (2.76, 0.45), (5.4, 0.22), (8.93, 0.1)]
    s = sum(a * np.sin(2 * np.pi * freq * r * x) * np.exp(-decay * r ** 0.5 * x) for r, a in parts)
    return s * np.minimum(x / 0.002, 1.0)


def place(buf, sig, at, gain=1.0):
    i = int(at * SR)
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[: end - i] * gain


def ignite():
    """A short, warm 'whoomp': a soft low swell and a gentle thump, with only a couple of embers."""
    d = 0.5
    x = t(d)
    noise = rng.standard_normal(len(x))
    body = lowpass(noise, 70) - lowpass(noise, 600)
    swell = body * (np.sin(np.pi * np.clip(x / d, 0, 1) ** 0.7) ** 2) * 5
    thump = np.sin(2 * np.pi * (95 * np.exp(-14 * x) + 52) * x) * env(len(x), 0.006, 9)
    ember = np.zeros(len(x))
    for at in (0.14, 0.27, 0.36):
        i = int(at * SR)
        ember[i : i + 70] += rng.standard_normal(70) * np.exp(-np.arange(70) / 14) * 0.22
    return swell * 0.45 + thump * 0.9 + lowpass(ember, 3) * 0.5


def tick():
    """Glassy blip. Played at rising pitches (a pentatonic ladder) as each circle checks."""
    x = t(0.26)
    a = np.sin(2 * np.pi * 784 * x) * env(len(x), 0.002, 16)
    b = np.sin(2 * np.pi * 784 * 2 * x) * env(len(x), 0.002, 26) * 0.35
    c = np.sin(2 * np.pi * 784 * 3.01 * x) * env(len(x), 0.001, 40) * 0.12
    click = lowpass(rng.standard_normal(len(x)), 4) * np.exp(-x * 900) * 0.15
    return a + b + c + click


def land():
    """Soft low pulse as the streak number lands."""
    x = t(0.3)
    return np.sin(2 * np.pi * (180 * np.exp(-10 * x) + 90) * x) * env(len(x), 0.004, 11)


def chime():
    """Rising major arpeggio with a shimmer tail: the day is filled."""
    buf = np.zeros(int(SR * 1.9))
    notes = [(659.25, 0.0), (830.61, 0.09), (987.77, 0.18), (1318.5, 0.28)]  # E5 G#5 B5 E6
    for f, at in notes:
        place(buf, bell(f), at, 0.55)
    place(buf, bell(1318.5 * 2, 1.4, 3.2), 0.28, 0.18)
    for _ in range(18):  # sparkle dust
        f = rng.choice([1975.5, 2349.3, 2637.0, 3135.9, 3951.1])
        place(buf, bell(f, 0.4, 14), rng.uniform(0.3, 0.9), rng.uniform(0.04, 0.1))
    return buf


def write(name, sig, gain=0.8):
    sig = sig / (np.max(np.abs(sig)) + 1e-9) * gain
    n = int(0.015 * SR)  # fade out tail to avoid a click
    sig[-n:] *= np.linspace(1, 0, n)
    pcm = (sig * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f"{name}.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1), w.setsampwidth(2), w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4",
                        str(OUT / f"{name}.ogg")], check=True)


if __name__ == "__main__":
    write("sfx_streak_ignite", ignite(), 0.5)
    write("sfx_streak_tick", tick(), 0.6)
    write("sfx_streak_land", land(), 0.55)
    write("sfx_streak_chime", chime(), 0.8)
