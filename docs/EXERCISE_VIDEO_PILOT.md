# AI exercise video pilot

Goal: realistic, impressive motion for the 3D-pilot exercises (squat, barbell bench press, barbell row,
conventional deadlift, barbell overhead press) by generating short clips **between each exercise's
existing start and movement stills**. Working muscles are **not** baked into the video: both apps draw
them from `catalog/exercise-muscles.json` in a corner overlay, so they are always correct and in sync.

The apps stay offline. Videos are made by the maintainer in their own Gemini (Veo), Kling or Sora account,
checked, reviewed and bundled like the photos. Each exercise guide then offers **3D motion · Video · Photos**
(Video appears only when a clip exists).

## 1. Prepare keyframes and prompts
```powershell
python scripts/prepare_video_keyframes.py
```
Writes `artifacts/video-keyframes/<ID>/start.png`, `end.png` (1080×1080, seamless background) and
`prompt.txt` (prompt, negative prompt, settings) for each pilot exercise. `artifacts/` is git-ignored.

## 2. Generate (per exercise)
Use **image-to-video with a start frame and an end frame**:
- **Gemini / Veo** (Flow or Gemini "frames to video"), **Kling** ("start and end frame"), or another tool
  that accepts both frames. Sora image-to-video with only a start frame works but drifts more.
- Start frame = `start.png`, end frame = `end.png`, **4 seconds, 1:1, no audio**.
- Paste the prompt and negative prompt from `prompt.txt`. Generate 2–4 takes and keep the cleanest.
- Check the tool's terms allow commercial use of the output for your plan.

## 3. Finalize (needs ffmpeg)
```powershell
python scripts/finalize_exercise_video.py SQUAT C:\path\to\download.mp4 --tool "Veo 3 (Gemini)"
```
Centre-crops to 720×720 at 30 fps, plays the clip forward then reversed for a seamless loop, encodes
silent H.264 MP4 under 1.6 MB, writes `assets/exercises/res/raw/exercise_video_squat.mp4` and records size,
hash, duration and tool in `catalog/exercise-video-manifest.json` with `reviewed: false`.
Use `--no-reverse` only if the source already loops cleanly.

## 4. Review every frame (release gate)
Reject and regenerate if any of these appear:
- Knees or elbows bending the wrong way, joints popping, limbs stretching or merging.
- Hands leaving or passing through the bar; bar bending, tilting or passing through the body.
- Plates changing size, count or shape; equipment morphing; feet sliding when they should be planted.
- Form that contradicts the guide cues: rounded back on deadlift/row, knees caving on squat,
  bar path drifting far forward on press, bouncing off the chest on bench.
- Camera moves, cuts, flicker, text, logos, watermarks.
Then a **qualified reviewer** (coach/physio) approves the clip and its cues; set `"reviewed": true` and a
short `"reviewNote"` in the manifest. `check_exercise_video.py --release` refuses unreviewed clips.

## 5. Check and commit
```powershell
python scripts/check_exercise_video.py
```
CI runs this on every push: files and manifest must match 1:1, hashes and sizes must match, each clip
must be a silent 720×720 MP4 of 3–9 s within the budget, for a pilot exercise.

## Notes
- Budget: about 0.2–1.6 MB per clip; five clips add at most about 8 MB to each app.
- Android plays with `VideoView` (no new dependency, never takes audio focus); iPhone uses a muted
  `AVPlayerLooper` with an ambient, mix-with-others audio session, so music and podcasts keep playing.
- Reduced motion pauses on the first frame. Clips are never mirrored in Arabic.
- To add more exercises later, add them to `catalog/exercise-video-prompts.json` first.
