"""
Generate minimal valid placeholder .glb files for all movements declared in
content manifests. Run from the repo root:

    python3 tools/gen_placeholder_glbs.py

Each generated file is a valid GLB (binary glTF 2.0) containing an empty scene.
SceneView loads it without crashing; it renders nothing, which is the correct
behaviour for a placeholder that hasn't been replaced with real 3D content yet.
"""

import json
import os
import struct


def make_minimal_glb() -> bytes:
    gltf_json = '{"asset":{"version":"2.0"}}'
    json_bytes = gltf_json.encode("utf-8")
    # Pad to 4-byte boundary with spaces (required by GLB spec)
    pad = (4 - len(json_bytes) % 4) % 4
    json_bytes += b" " * pad

    chunk_type_json = 0x4E4F534A  # ASCII "JSON"
    json_chunk = struct.pack("<II", len(json_bytes), chunk_type_json) + json_bytes

    magic = 0x46546C67  # ASCII "glTF"
    version = 2
    total_length = 12 + len(json_chunk)
    header = struct.pack("<III", magic, version, total_length)

    return header + json_chunk


def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    assets_root = os.path.join(repo_root, "app", "src", "main", "assets")
    content_root = os.path.join(assets_root, "content")

    disciplines = ["karate", "kung-fu", "yoga", "calisthenics", "gym", "boxing", "muay-thai", "pilates", "judo", "wrestling", "bjj", "taekwondo", "kickboxing", "aikido"]
    glb_data = make_minimal_glb()
    created = 0
    skipped = 0

    for discipline in disciplines:
        manifest_path = os.path.join(content_root, discipline, "manifest.json")
        if not os.path.exists(manifest_path):
            print(f"  SKIP (no manifest): {discipline}")
            continue

        with open(manifest_path) as f:
            manifest = json.load(f)

        for movement in manifest.get("movements", []):
            model_path = movement.get("modelPath", "")
            if not model_path.endswith(".glb"):
                continue

            dest = os.path.join(assets_root, model_path)
            if os.path.exists(dest):
                skipped += 1
                continue

            os.makedirs(os.path.dirname(dest), exist_ok=True)
            with open(dest, "wb") as f:
                f.write(glb_data)
            print(f"  created: {model_path}")
            created += 1

    print(f"\nDone: {created} created, {skipped} already existed.")


if __name__ == "__main__":
    main()
