#!/usr/bin/env python3
"""Mixes the soundtrack from the sounds.txt log written by DemoRecorder.

Each clip behaves like a java.applet.AudioClip: play() restarts it from the
beginning and stop() cuts it off. Times are in milliseconds, and start_ms is
the time of the first frame of the video.

Usage: mix.py <sounds.txt> <sounds dir> <start_ms> <end_ms> <out.wav>
"""
import array
import subprocess
import sys
import wave

RATE = 44100
FADE = int(RATE * 0.005)


def decode(path):
    raw = subprocess.run(
        ['ffmpeg', '-loglevel', 'error', '-i', path, '-f', 's16le', '-ac', '1', '-ar', str(RATE), '-'],
        check=True, capture_output=True).stdout
    return array.array('h', raw)


def main(log, sounds, start_ms, end_ms, out):
    start_ms, end_ms = int(start_ms), int(end_ms)
    length = (end_ms - start_ms) * RATE // 1000
    mix = [0.0] * length
    clips = {}
    playing = {}  # clip name -> start sample

    def render(name, begin, end):
        data = clips[name]
        end = min(end, begin + len(data), length)
        for i in range(max(begin, 0), end):
            gain = min(1.0, (end - i) / FADE) if end - begin < len(data) else 1.0
            mix[i] += data[i - begin] * gain

    for line in open(log):
        t, what, name = line.split()
        pos = (int(t) - start_ms) * RATE // 1000
        if name not in clips:
            clips[name] = decode(f'{sounds}/{name}')
        if name in playing:
            render(name, playing.pop(name), pos)
        if what in ('play', 'loop'):
            playing[name] = pos
    for name, begin in playing.items():
        render(name, begin, length)

    peak = max(1.0, max(abs(s) for s in mix) / 32000)
    pcm = array.array('h', (int(s / peak) for s in mix))
    with wave.open(out, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())


if __name__ == '__main__':
    main(*sys.argv[1:])
