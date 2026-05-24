# 3D Asset & Animation Strategy

**Status: PAUSED** — placeholder GLBs are in place, app compiles and renders. Resume this when ready to ship beta-quality visuals.

---

## Current State

Every movement in every discipline has a **48-byte placeholder GLB** committed to `app/src/main/assets/content/<discipline>/models/<movement>.glb`. These are valid GLB 2.0 files containing an empty scene — SceneView loads them without crashing but renders nothing.

As of Sprint 120:
- **35 disciplines**, **525 movements**, **525 placeholder GLBs**
- All model paths follow the pattern: `content/<discipline>/models/<movement_id>.glb`
- The `gen_placeholder_glbs.py` script regenerates any missing stubs from manifests

---

## Character Model

**Chosen base model:** Fitness Character by iPoly3D  
**Source:** `https://poly.pizza/m/eMOTyGEAxj`  
**License:** CC0 (Public Domain) — safe to commit to a public repo, no attribution required

This single rigged human character will serve as the base for all disciplines. Discipline-appropriate character variations (gi, shorts, armour, etc.) are deferred — one neutral athletic character covers all 525 movements for now.

---

## Animation Sources

### What is needed
525 movements do **not** require 525 unique animations. Many techniques share the same underlying motion (a jab in boxing ≈ a jab in MMA ≈ a front punch in karate). Estimate: **~100–120 distinct animation clips** mapped across 525 slots via the `defaultClip` field already in each manifest.

### Chosen sources (public repo safe)

| Source | License | Coverage | Format | Notes |
|---|---|---|---|---|
| **CMU Motion Capture Database** | Public domain | ~60% — common punches, kicks, throws, groundwork | BVH → GLB | `mocap.cs.cmu.edu` — best free quality |
| **MDM (Motion Diffusion Model)** | MIT (output owned by you) | ~35% — technique-specific moves | BVH → GLB | Text-to-motion AI, runs free on Google Colab |
| **Poly Pizza CC0 models** | CC0 | ~5% — fill remaining gaps | GLB | Static poses only if no animation found |

### Why Mixamo was ruled out
Mixamo (Adobe) has the best animation quality and largest library but:
- Prohibits redistribution of raw animation files
- A public GitHub repo makes committed GLBs publicly downloadable → ToS violation
- Safe only in a permanently private repo

### Why GitHub LFS was ruled out for delivery
- Free tier: 1GB bandwidth/month (~7 app installs before quota exhausted)
- Designed for developer workflows, not end-user app delivery
- LFS files in a public repo are still publicly downloadable

---

## Storage & Delivery Architecture

### Decision
Real animated GLBs will **not** be committed to git. They will be hosted on **GitHub Releases** and downloaded by the app on first use.

### Why GitHub Releases
- Binary files up to 2GB, hosted free on GitHub's infrastructure
- Stable, permanent download URLs (unlike LFS which uses temporary authenticated URLs)
- No bandwidth cap on public releases
- Assets are "on GitHub" without being in git history — keeps repo size small
- Decouples asset updates from app releases (upload new GLB, no new APK needed)

### App-side caching
```
First launch:
  manifest says modelPath = "content/mma/models/jab.glb"
  → check Context.filesDir / "assets" / "content/mma/models/jab.glb"
  → not found → download from GitHub Releases CDN URL
  → save to filesDir
  → pass local File to SceneView

Subsequent launches:
  → file exists in filesDir → load directly, no network call
```

A `GlbAssetRepository` Kotlin class handles this transparently. The rendering layer never needs to know whether an asset is local or remote.

---

## Animation Pipeline (when resuming)

### Step 1 — CMU Motion Capture → GLB (batch)
1. Download relevant BVH files from `mocap.cs.cmu.edu`
2. Open Blender → import Poly Pizza character (FBX/GLB)
3. Run `tools/retarget_bvh_to_glb.py` (Blender Python batch script — to be written)
4. Script imports each BVH, retargets to the character, exports one GLB per animation
5. Output: `assets_real/content/<discipline>/models/<movement>.glb`

### Step 2 — MDM text-to-motion (for technique-specific moves)
1. Open Google Colab notebook: `github.com/GuyTevet/motion-diffusion-model`
2. For each uncovered movement, run with the pre-written text prompt (see mapping table below)
3. Export BVH → convert to GLB via Blender
4. Add to `assets_real/`

### Step 3 — Upload to GitHub Releases
1. Create a GitHub Release tagged `assets-v1`
2. Attach all GLBs from `assets_real/` as release assets
3. Update the base URL constant in `GlbAssetRepository.kt`

### Step 4 — Test & ship
1. Run app on device, verify assets download and cache correctly
2. Remove placeholder GLBs from repo (or leave as fallback — SceneView won't crash on them)

---

## Movement → Animation Mapping (partial, to be completed)

When resuming, use this table to identify which CMU clip or MDM prompt covers each movement. Common mappings:

| Movement type | CMU subject/motion | MDM prompt template |
|---|---|---|
| Jab / front punch | Subject 14, motion 30 | `"person throwing a jab punch with right hand"` |
| Cross / rear punch | Subject 14, motion 30 | `"person throwing a straight right cross punch"` |
| Hook | Subject 14, motion 31 | `"person throwing a left hook punch"` |
| Front kick | Subject 38, motion 1 | `"person throwing a front kick"` |
| Round kick | Subject 38, motion 2 | `"person throwing a roundhouse kick"` |
| Takedown / throw | Subject 90, motion 3 | `"person performing a double leg takedown"` |
| Guard position | — | `"person in a martial arts guard stance"` |
| Ground control | — | `"person maintaining mount position on ground"` |
| Weapon strike (overhead) | — | `"person performing an overhead stick strike"` |
| Spinning movement | Subject 13, motion 18 | `"person performing a spinning strike"` |

**Full mapping table:** To be written as `tools/movement_animation_map.csv` when work resumes.

---

## Files to Create When Resuming

| File | Purpose |
|---|---|
| `tools/retarget_bvh_to_glb.py` | Blender Python batch script: BVH → GLB with character |
| `tools/movement_animation_map.csv` | Full mapping of 525 movement IDs to CMU/MDM sources |
| `tools/upload_release_assets.py` | GitHub CLI wrapper to upload GLBs to a release |
| `rendering/rendering-scene-view/src/.../GlbAssetRepository.kt` | Kotlin: resolves modelPath, downloads from GitHub Releases, caches to filesDir |

---

## What Is NOT Blocked

The following work can continue at full sprint cadence regardless of animation status:

- Adding new disciplines and manifests (sprint cadence)
- UI development (browse, detail, settings screens)
- Theme system
- Navigation
- Alpha build via GitHub Actions
- Any Kotlin/Compose code work

The placeholder GLBs mean SceneView renders an empty scene — the app does not crash and all non-rendering functionality is fully testable.
