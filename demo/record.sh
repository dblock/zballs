#!/usr/bin/env bash
# Plays the game on autopilot with synthetic mouse events and renders frames
# off-screen. Sounds are logged rather than played, then mixed from the
# original .au files into the soundtrack of zballs.mp4.
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f build/classes/DemoRecorder.class ]] || ./build.sh
out=$(mktemp -d)
java -cp build/classes DemoRecorder "$out" 15 120

# Skip frames captured before the applet resizes itself to 350 pixels wide, and
# give every frame the duration until the next one so video and sound line up.
python3 - "$out" <<'PY'
import os, struct, sys
out = sys.argv[1]
frames = [line.split() for line in open(os.path.join(out, 'frames.txt'))]
frames = [(int(t), f) for t, f in frames
          if struct.unpack('>I', open(os.path.join(out, f), 'rb').read(24)[16:20])[0] == 350]
with open(os.path.join(out, 'frames.ffconcat'), 'w') as c:
    c.write('ffconcat version 1.0\n')
    for (t, f), (n, _) in zip(frames, frames[1:] + [(frames[-1][0] + 66, None)]):
        c.write(f"file '{f}'\nduration {(n - t) / 1000:.3f}\n")
    c.write(f"file '{frames[-1][1]}'\n")
open(os.path.join(out, 'range'), 'w').write(f'{frames[0][0]} {frames[-1][0] + 66}\n')
PY
read -r start end < "$out/range"
python3 demo/mix.py "$out/sounds.txt" sounds "$start" "$end" "$out/sound.wav"
ffmpeg -loglevel error -y -f concat -i "$out/frames.ffconcat" -i "$out/sound.wav" \
  -vf "fps=30,scale=700:-2:flags=neighbor" -c:v libx264 -pix_fmt yuv420p -crf 20 \
  -af loudnorm=I=-16:TP=-1.5 -ar 44100 -c:a aac -b:a 96k -shortest -movflags +faststart zballs.mp4
rm -rf "$out"
echo "Wrote zballs.mp4."
