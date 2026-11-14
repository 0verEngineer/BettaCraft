"""M23: the sounds of the tank equipment (all procedural, own work, same license as the mod).

Writes mono 44.1 kHz Ogg Vorbis files to ``assets/bettacraft/sounds/block/``. Every random
value comes from a fixed seed, so the PCM is the same on every run (the Ogg container gets a
random stream serial number from libsndfile, so the bytes of the .ogg files can differ).
Needs ``numpy`` and ``soundfile`` (its wheels ship libsndfile with Vorbis). Not part of build.py.

    python tools/assetgen/sounds.py                 # write all sounds
    python tools/assetgen/sounds.py --report DIR    # also print the analysis and write spectrogram PNGs to DIR

Building blocks
---------------
* Bubble: one gas bubble rings at its Minnaert frequency f0 = 3.26 m*Hz / r and decays with the
  damping d = 0.043 f + 0.0014 f^1.5 (1/s, van den Doel 2005). While it rises the frequency
  climbs, f(t) = f0 (1 + xi d t), xi ~ 0.1 .. 0.3. Each bubble is a sine with that chirp, a
  0.3 ms raised-cosine onset (no click) and the exponential decay. Bubbles are placed at
  Poisson-distributed times with log-uniform radii; a slowly wandering rate makes them come in
  clusters like in real water.
* Noise beds: white noise band-passed in the frequency domain (zero-phase Butterworth-shaped
  magnitude) and modulated by a smooth random envelope (low-passed noise through exp()).
* One-shots get a 0.15 s raised-cosine fade-in and a 0.45 s fade-out, a 40 Hz high-pass and DC
  removal, then they are scaled to the target RMS. A soft limiter (tanh above -5 dBFS) keeps
  the peak below -1 dBFS; after encoding the file is decoded again and scaled down a little if
  the Vorbis overshoot pushed the decoded peak above -1.3 dBFS.
* Loops are built to be exactly periodic over their length (88200 samples = 2 s): every tone is
  a multiple of 0.5 Hz, noise is shaped by circular FFT filtering of one loop length, and every
  modulation has a period that divides 2 s. The seam is therefore as smooth as any other sample
  pair; ``--report`` checks it after the Vorbis round trip.

Per sound
---------
* ``filter/running1..3`` (2.4-2.8 s): water of the filter outlet falling into the tank. A
  splash bed (noise 250-3500 Hz, fast random envelope), a stream of mid bubbles (r 1-4 mm,
  ~0.8-3.3 kHz, 45/s in clusters), a few large plunging bubbles (r 5-11 mm, 300-650 Hz) and
  a faint low rumble.
* ``air_pump/hum`` (loop 2 s): membrane air pump. The magnet swings at the 50 Hz mains, the
  diaphragm moves at 100 Hz: harmonic series of 50 Hz with the 100 Hz line strongest and the
  even harmonics above it, a short noise rattle burst (2-7 kHz) on every 100 Hz stroke with a
  slight random strength per stroke, faint air hiss, slow 0.5/1.5 Hz amplitude wobble.
* ``air_stone/bubbles1..3`` (2.2-2.8 s): air stone. Dense tiny bubbles (r 0.35-1.4 mm,
  2.3-9 kHz, ~260/s), some mid bubbles, short surface-burst ticks and a fizz bed
  (noise 2.5-10 kHz).
* ``sponge_filter/gurgle1..2`` (2.6-2.8 s): sponge filter lift tube. Groups ("glugs") of
  2-5 large bubbles (r 3.5-9 mm, 360-930 Hz, slower rise) every ~0.35 s, a low water slosh
  (noise < 900 Hz) with a burst per glug and a soft surface splash (noise 600-4000 Hz).
* ``flow_pump/whir`` (loop 2 s): small circulation pump with a synchronous motor. Weak 50/100 Hz
  hum, impeller blade pass at 200 Hz with harmonics, a faint motor whine near 1.5 kHz with slow
  FM, and the water swoosh (noise 150-1800 Hz) with slow periodic swells, plus a little
  high flow hiss.
* ``feeder/click1..2`` (0.3 s): automatic feeder drum. Plastic ticks are impulses through a
  bank of damped modes (1.1-7.4 kHz, 4-25 ms) plus a short low body thump. click1: ratchet
  tick, second tick 70 ms later and the soft drum thump. click2: one tick followed by a few
  pellets rattling (tiny high ticks).
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44100
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src" / "main" / "resources" / "assets" / "bettacraft" / "sounds" / "block"

LOOP_N = 2 * SR  # 2 s loops, 88200 samples (a whole number of 50 Hz cycles)


# ---------------------------------------------------------------------------- helpers
def db(x: float) -> float:
    return 20.0 * np.log10(max(x, 1e-12))


def fft_filter(x: np.ndarray, lo: float | None = None, hi: float | None = None,
               order: int = 2, circular: bool = False) -> np.ndarray:
    """Zero-phase band-pass with a Butterworth-shaped magnitude.

    circular=True filters one period of a loop (wraps around, keeps it seamless); otherwise
    the signal is zero-padded so nothing wraps.
    """
    n = len(x)
    pad = 0 if circular else min(SR, n)
    y = np.concatenate([x, np.zeros(pad)]) if pad else x
    spec = np.fft.rfft(y)
    f = np.fft.rfftfreq(len(y), 1.0 / SR)
    h = np.ones_like(f)
    if lo:
        with np.errstate(divide="ignore"):
            h *= 1.0 / np.sqrt(1.0 + (lo / np.maximum(f, 1e-9)) ** (2 * order))
        h[0] = 0.0
    if hi:
        h *= 1.0 / np.sqrt(1.0 + (f / hi) ** (2 * order))
    return np.fft.irfft(spec * h, len(y))[:n]


def smooth_env(rng: np.random.Generator, n: int, rate_hz: float, depth: float,
               circular: bool = False) -> np.ndarray:
    """Positive random envelope around 1 that wanders at about rate_hz."""
    e = fft_filter(rng.standard_normal(n), hi=rate_hz, order=2, circular=circular)
    e /= np.std(e) + 1e-12
    return np.exp(depth * e)


def fades(x: np.ndarray, fin: float = 0.15, fout: float = 0.45) -> np.ndarray:
    n_in, n_out = int(fin * SR), int(fout * SR)
    w = np.ones(len(x))
    w[:n_in] = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, n_in))
    w[-n_out:] = 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, n_out))
    return x * w


def bubble_damping(f: float) -> float:
    return 0.043 * f + 0.0014 * f ** 1.5


def add_bubble(buf: np.ndarray, t0: float, radius_mm: float, amp: float, xi: float = 0.15) -> None:
    """Add one Minnaert bubble (chirped, exponentially decaying sine) at time t0."""
    f0 = 3260.0 / radius_mm
    if f0 > 16000:
        return
    d = bubble_damping(f0)
    dur = min(6.0 / d, 0.25)
    i0 = int(t0 * SR)
    m = min(int(dur * SR), len(buf) - i0)
    if m <= 8 or i0 < 0:
        return
    t = np.arange(m) / SR
    sigma = xi * d
    phase = 2 * np.pi * f0 * (t + 0.5 * sigma * t * t)
    env = np.exp(-d * t)
    na = min(int(0.0003 * SR) + 1, m)
    env[:na] *= 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, na))
    buf[i0:i0 + m] += amp * env * np.sin(phase)


def poisson_times(rng: np.random.Generator, dur: float, rate: float,
                  cluster: float = 0.0, cluster_hz: float = 3.0) -> np.ndarray:
    """Event times with a mean rate; cluster > 0 lets the rate wander (bursty)."""
    n = int(dur * SR)
    step = 64
    grid = n // step
    lam = np.full(grid, rate)
    if cluster > 0:
        e = fft_filter(rng.standard_normal(grid), hi=cluster_hz * step, order=2)
        e /= np.std(e) + 1e-12
        lam = rate * np.exp(cluster * e - 0.5 * cluster ** 2)
    p = lam * step / SR
    hits = rng.random(grid) < p
    idx = np.nonzero(hits)[0]
    return (idx * step + rng.integers(0, step, len(idx))) / SR


def log_uniform(rng: np.random.Generator, lo: float, hi: float, size: int) -> np.ndarray:
    return np.exp(rng.uniform(np.log(lo), np.log(hi), size))


def band_noise(rng, n, lo, hi, order=2, circular=False):
    x = fft_filter(rng.standard_normal(n), lo=lo, hi=hi, order=order, circular=circular)
    return x / (np.std(x) + 1e-12)


def finish(x: np.ndarray, rms_db: float | None = None, peak_db: float | None = None,
           highpass: bool = True, circular: bool = False, one_shot: bool = True) -> np.ndarray:
    x = x - np.mean(x)
    if highpass:
        x = fft_filter(x, lo=40.0, order=2, circular=circular)
    if one_shot:
        x = fades(x)
    if rms_db is not None:
        x = x * (10 ** (rms_db / 20) / np.sqrt(np.mean(x ** 2)))
    if peak_db is not None:
        x = x * (10 ** (peak_db / 20) / np.max(np.abs(x)))
    # soft limiter: linear up to -5 dBFS, tanh knee towards -2 dBFS (Vorbis adds ~1 dB of overshoot)
    th, ceil = 10 ** (-5 / 20), 10 ** (-2 / 20)
    a = np.abs(x)
    over = a > th
    a2 = a.copy()
    a2[over] = th + (ceil - th) * np.tanh((a[over] - th) / (ceil - th))
    return np.sign(x) * a2


# ---------------------------------------------------------------------------- sounds
def filter_running(seed: int, dur: float) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    splash = band_noise(rng, n, 250, 3500) * smooth_env(rng, n, 9.0, 0.7) * 0.10
    splash += band_noise(rng, n, 1200, 6000) * smooth_env(rng, n, 14.0, 0.9) * 0.03
    rumble = band_noise(rng, n, 50, 220) * smooth_env(rng, n, 2.0, 0.4) * 0.05
    bub = np.zeros(n)
    times = poisson_times(rng, dur, 45.0, cluster=0.9, cluster_hz=4.0)
    for t, r in zip(times, log_uniform(rng, 1.0, 4.0, len(times))):
        add_bubble(bub, t, r, 0.08 * (r / 2.5) ** 0.5 * rng.uniform(0.4, 1.0), xi=rng.uniform(0.1, 0.3))
    times = poisson_times(rng, dur, 4.0, cluster=0.6)
    for t, r in zip(times, rng.uniform(5.0, 11.0, len(times))):
        add_bubble(bub, t, r, 0.10 * rng.uniform(0.5, 1.0), xi=rng.uniform(0.05, 0.15))
    return finish(splash + rumble + bub, rms_db=-20)


def air_pump_hum(seed: int) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = LOOP_N
    t = np.arange(n) / SR
    x = np.zeros(n)
    # harmonic series of 50 Hz; the diaphragm stroke at 100 Hz dominates
    amps = {1: 0.35, 2: 1.0, 3: 0.22, 4: 0.55, 5: 0.10, 6: 0.32, 7: 0.06, 8: 0.20, 10: 0.12,
            12: 0.08, 14: 0.05, 16: 0.035, 20: 0.02, 24: 0.012}
    for k, a in amps.items():
        x += a * np.sin(2 * np.pi * 50 * k * t + rng.uniform(0, 2 * np.pi))
    x /= np.max(np.abs(x))
    # rattle: noise burst on every 100 Hz stroke (period 441 samples, 200 strokes per loop)
    rattle = np.zeros(n)
    burst = np.exp(-np.arange(160) / (0.0018 * SR))
    period = SR // 100
    noise = rng.standard_normal(n)
    for k in range(n // period):
        i0 = k * period + 60
        s = rng.uniform(0.6, 1.0)
        rattle[i0:i0 + 160] += s * burst * noise[i0:i0 + 160]
    rattle = fft_filter(rattle, lo=2000, hi=7000, circular=True)
    rattle /= np.std(rattle)
    hiss = band_noise(rng, n, 1500, 8000, circular=True)
    wobble = 1.0 + 0.06 * np.sin(2 * np.pi * 0.5 * t + 1.0) + 0.04 * np.sin(2 * np.pi * 1.5 * t + 2.0)
    y = (x * 1.0 + 0.10 * rattle + 0.02 * hiss) * wobble
    return finish(y, rms_db=-22, highpass=False, circular=True, one_shot=False)


def air_stone(seed: int, dur: float) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    bub = np.zeros(n)
    times = poisson_times(rng, dur, 260.0, cluster=0.5, cluster_hz=6.0)
    for t, r in zip(times, log_uniform(rng, 0.35, 1.4, len(times))):
        add_bubble(bub, t, r, 0.05 * rng.uniform(0.3, 1.0), xi=rng.uniform(0.1, 0.35))
    times = poisson_times(rng, dur, 18.0, cluster=0.5)
    for t, r in zip(times, log_uniform(rng, 1.4, 3.0, len(times))):
        add_bubble(bub, t, r, 0.06 * rng.uniform(0.4, 1.0), xi=rng.uniform(0.1, 0.25))
    # surface bursts: very short broadband ticks
    ticks = np.zeros(n)
    for t in poisson_times(rng, dur, 120.0, cluster=0.5):
        i = int(t * SR)
        m = min(40, n - i)
        ticks[i:i + m] += rng.uniform(-1, 1) * np.exp(-np.arange(m) / 6.0)
    ticks = fft_filter(ticks, lo=3000, hi=12000) * 0.25
    fizz = band_noise(rng, n, 2500, 10000) * smooth_env(rng, n, 12.0, 0.6) * 0.012
    return finish(bub + ticks + fizz, rms_db=-20)


def sponge_gurgle(seed: int, dur: float) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    bub = np.zeros(n)
    burst_env = np.zeros(n)
    t = rng.uniform(0.05, 0.2)
    while t < dur - 0.1:
        for _ in range(rng.integers(2, 6)):
            add_bubble(bub, t, rng.uniform(3.5, 9.0), 0.12 * rng.uniform(0.5, 1.0), xi=rng.uniform(0.04, 0.12))
            t += rng.uniform(0.015, 0.06)
        i = int(t * SR)
        m = min(int(0.12 * SR), n - i)
        if m > 0:
            burst_env[i:i + m] += np.sin(np.linspace(0, np.pi, m)) ** 2
        t += rng.uniform(0.2, 0.5)
    slosh = band_noise(rng, n, 70, 900) * (0.25 + burst_env) * 0.05
    splash = band_noise(rng, n, 600, 4000) * (0.1 + burst_env) * smooth_env(rng, n, 25.0, 0.6) * 0.015
    return finish(bub + slosh + splash, rms_db=-20)


def flow_pump_whir(seed: int) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = LOOP_N
    t = np.arange(n) / SR
    tones = np.zeros(n)
    for f, a in ((50, 0.15), (100, 0.35), (200, 0.6), (400, 0.25), (600, 0.12), (800, 0.06)):
        tones += a * np.sin(2 * np.pi * f * t + rng.uniform(0, 2 * np.pi))
    whine = 0.05 * np.sin(2 * np.pi * 1500 * t + 3.0 * np.sin(2 * np.pi * 0.5 * t))
    tones = (tones + whine) / np.max(np.abs(tones + whine))
    swell = (1.0 + 0.25 * np.sin(2 * np.pi * 0.5 * t + rng.uniform(0, 6))
             + 0.15 * np.sin(2 * np.pi * 1.0 * t + rng.uniform(0, 6))
             + 0.08 * np.sin(2 * np.pi * 2.5 * t + rng.uniform(0, 6)))
    swoosh = band_noise(rng, n, 150, 1800, circular=True) * swell
    hiss = band_noise(rng, n, 2500, 7000, circular=True) * swell
    y = 0.35 * tones + 0.6 * swoosh + 0.08 * hiss
    return finish(y, rms_db=-21, circular=True, one_shot=False)


def tick(n: int, rng: np.random.Generator, modes, amp: float = 1.0) -> np.ndarray:
    t = np.arange(n) / SR
    y = np.zeros(n)
    for f, tau, a in modes:
        f = f * rng.uniform(0.97, 1.03)
        y += a * np.exp(-t / tau) * np.sin(2 * np.pi * f * t + rng.uniform(0, 2 * np.pi))
    y[:12] *= np.linspace(0, 1, 12)
    return amp * y


PLASTIC = ((1150, 0.025, 0.5), (2300, 0.012, 0.8), (3400, 0.008, 1.0), (5100, 0.006, 0.6), (7400, 0.004, 0.4))


def feeder_click(seed: int, variant: int) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = int(0.3 * SR)
    y = np.zeros(n)

    def put(t0: float, sig: np.ndarray) -> None:
        i = int(t0 * SR)
        m = min(len(sig), n - i)
        y[i:i + m] += sig[:m]

    thump_t = np.arange(int(0.05 * SR)) / SR
    thump = np.sin(2 * np.pi * 160 * thump_t) * np.exp(-thump_t / 0.012)
    thump[:20] *= np.linspace(0, 1, 20)
    put(0.01, tick(int(0.08 * SR), rng, PLASTIC, 1.0))
    if variant == 1:
        put(0.012, 0.5 * thump)
        put(0.08, tick(int(0.08 * SR), rng, PLASTIC, 0.6))
        put(0.15, 0.35 * thump)
    else:
        put(0.015, 0.35 * thump)
        for t0 in np.sort(rng.uniform(0.06, 0.2, 6)):
            put(t0, tick(int(0.02 * SR), rng, ((4800, 0.002, 1.0), (6900, 0.0015, 0.7), (9100, 0.001, 0.5)),
                         rng.uniform(0.08, 0.25)))
    y = y - np.mean(y)
    y = fft_filter(y, lo=60.0)
    w = np.ones(n)
    k = int(0.05 * SR)
    w[-k:] = 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, k))
    w[:8] = np.linspace(0, 1, 8)
    return finish(y * w, peak_db=-4, highpass=False, one_shot=False)


SOUNDS = {
    "filter/running1": lambda: filter_running(2301, 2.6),
    "filter/running2": lambda: filter_running(2302, 2.4),
    "filter/running3": lambda: filter_running(2303, 2.8),
    "air_pump/hum": lambda: air_pump_hum(2311),
    "air_stone/bubbles1": lambda: air_stone(2321, 2.5),
    "air_stone/bubbles2": lambda: air_stone(2322, 2.2),
    "air_stone/bubbles3": lambda: air_stone(2323, 2.8),
    "sponge_filter/gurgle1": lambda: sponge_gurgle(2331, 2.8),
    "sponge_filter/gurgle2": lambda: sponge_gurgle(2332, 2.6),
    "flow_pump/whir": lambda: flow_pump_whir(2341),
    "feeder/click1": lambda: feeder_click(2351, 1),
    "feeder/click2": lambda: feeder_click(2352, 2),
}
LOOPS = {"air_pump/hum", "flow_pump/whir"}


# ---------------------------------------------------------------------------- report
def spectrogram_png(x: np.ndarray, path: Path) -> None:
    from PIL import Image
    nfft, hop = 1024, 128
    if len(x) < nfft:
        x = np.concatenate([x, np.zeros(nfft - len(x))])
    win = np.hanning(nfft)
    frames = [x[i:i + nfft] * win for i in range(0, len(x) - nfft + 1, hop)]
    s = np.abs(np.fft.rfft(np.array(frames), axis=1)).T
    s = 20 * np.log10(s + 1e-9)
    s = np.clip((s - (s.max() - 90)) / 90, 0, 1)[::-1]
    r = np.clip(3 * s - 1, 0, 1)
    g = np.clip(3 * s - 2, 0, 1) * 0.5 + np.clip(3 * s - 1.2, 0, 1) * 0.5
    b = np.clip(1.5 * s, 0, 1) * (1 - np.clip(3 * s - 2, 0, 1)) + np.clip(3 * s - 2, 0, 1)
    img = (np.stack([r, g, b], -1) * 255).astype(np.uint8)
    im = Image.fromarray(img).resize((max(300, min(900, img.shape[1])), 300))
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def analyse(name: str, path: Path, png_dir: Path) -> None:
    x, sr = sf.read(path, dtype="float64", always_2d=True)
    ch = x.shape[1]
    x = x[:, 0]
    spec = np.abs(np.fft.rfft(x)) ** 2
    f = np.fft.rfftfreq(len(x), 1 / sr)
    tot = spec.sum()
    cen = (f * spec).sum() / tot
    low = spec[f < 200].sum() / tot
    high = spec[f > 4000].sum() / tot
    line = (f"{name:24s} {len(x) / sr:5.2f}s {sr} {ch}ch {path.stat().st_size / 1024:5.1f}KB "
            f"peak {db(np.max(np.abs(x))):6.1f} rms {db(np.sqrt(np.mean(x ** 2))):6.1f} "
            f"cent {cen:6.0f}Hz low/mid/high {low:4.2f}/{1 - low - high:4.2f}/{high:4.2f}")
    if name in LOOPS:
        d = np.abs(np.diff(x))
        seam = abs(x[0] - x[-1])
        line += f" | seam {seam:.4f} (median step {np.median(d):.4f}, p99 {np.percentile(d, 99):.4f}, max {d.max():.4f})"
        # high-band energy around the seam versus elsewhere
        hp = lambda s: np.diff(np.diff(s))
        wrap = np.concatenate([x[-256:], x[:256]])
        mid = x[len(x) // 2 - 256:len(x) // 2 + 256]
        line += f" seam/mid HF ratio {np.sqrt(np.mean(hp(wrap) ** 2) / np.mean(hp(mid) ** 2)):.2f}"
        spectrogram_png(np.concatenate([x, x]), png_dir / f"{name.replace('/', '_')}_x2.png")
    print(line)
    spectrogram_png(x, png_dir / f"{name.replace('/', '_')}.png")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", type=Path, default=OUT)
    ap.add_argument("--report", type=Path, default=None, help="directory for spectrogram PNGs")
    args = ap.parse_args()
    for name, make in SOUNDS.items():
        x = make()
        p = args.out / f"{name}.ogg"
        p.parent.mkdir(parents=True, exist_ok=True)
        # Vorbis can overshoot the PCM peak on sharp bubble transients: decode, and if the
        # decoded peak is above -1.3 dBFS, scale down and encode again
        for _ in range(6):
            sf.write(p, x.astype(np.float32), SR, format="OGG", subtype="VORBIS")
            peak = db(np.max(np.abs(sf.read(p, dtype="float64")[0])))
            if peak <= -1.3:
                break
            x = x * 10 ** ((-1.4 - peak) / 20)
    if args.report:
        for name in SOUNDS:
            analyse(name, args.out / f"{name}.ogg", args.report)


if __name__ == "__main__":
    main()
