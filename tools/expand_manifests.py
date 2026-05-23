"""
Expand each discipline manifest from 12 to 15 movements.
Run from the repo root: python3 tools/expand_manifests.py
"""
import json
import os

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "content")

EXTENSIONS = {
    "karate": [
        {
            "id": "karate.mae-tobi-geri",
            "name": "Jumping Front Kick",
            "description": "An airborne version of the front kick executed by leaping off the rear leg and chambering the front knee before extending. The additional height increases reach and momentum, making it effective against taller opponents or as a surprise technique.",
            "modelPath": "content/karate/models/mae_tobi_geri.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.8, "fps": 30},
                {"name": "kick", "durationSeconds": 1.4, "fps": 30}
            ],
            "muscles": ["quadriceps", "hip_flexors", "core", "calves"],
            "difficulty": "advanced",
            "tags": ["kick", "jump"],
            "cameraPreset": "side",
            "prerequisites": ["karate.mae-geri"],
            "commonMistakes": [
                {"description": "Telegraphing the jump by bending the knees too slowly before takeoff"},
                {"description": "Failing to chamber the knee fully before the kick extends"}
            ]
        },
        {
            "id": "karate.nagashi-uke",
            "name": "Flowing Block",
            "description": "A sweeping parry that redirects an incoming strike by flowing the arm outward in a circular arc. Unlike a hard block, nagashi-uke uses the attacker's momentum against them, making it energy-efficient and suitable for continuous counter-attacking.",
            "modelPath": "content/karate/models/nagashi_uke.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.6, "fps": 30},
                {"name": "block", "durationSeconds": 1.0, "fps": 30}
            ],
            "muscles": ["shoulders", "back", "core"],
            "difficulty": "intermediate",
            "tags": ["block", "defense"],
            "cameraPreset": "front",
            "prerequisites": ["karate.jodan-uke"],
            "commonMistakes": [
                {"description": "Using a rigid stop rather than flowing through the arc"},
                {"description": "Crossing the centreline and exposing the body"}
            ]
        },
        {
            "id": "karate.haito-uchi",
            "name": "Ridge Hand Strike",
            "description": "A strike delivered with the inner ridge of the hand, formed by folding the thumb inward and extending the fingers. The weapon surface is the bony ridge along the index finger's base, and strikes target the temple, neck, or bridge of the nose.",
            "modelPath": "content/karate/models/haito_uchi.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.6, "fps": 30},
                {"name": "strike", "durationSeconds": 0.9, "fps": 30}
            ],
            "muscles": ["shoulders", "chest", "core"],
            "difficulty": "intermediate",
            "tags": ["strike", "hand"],
            "cameraPreset": "front",
            "prerequisites": ["karate.gyaku-zuki"],
            "commonMistakes": [
                {"description": "Allowing the thumb to protrude, risking injury on impact"},
                {"description": "Swinging the arm without rotating the hips, losing power"}
            ]
        }
    ],
    "kung-fu": [
        {
            "id": "kung-fu.tiger-claw",
            "name": "Tiger Claw Strike",
            "description": "A close-range raking and gripping attack modelled on the tiger's paw. The fingers curve into a claw shape and strike downward or forward, targeting the face, throat, or weapon hand. Training develops crushing grip strength and toughens the fingertips.",
            "modelPath": "content/kung-fu/models/tiger-claw.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.7, "fps": 30},
                {"name": "strike", "durationSeconds": 1.0, "fps": 30}
            ],
            "muscles": ["shoulders", "chest", "core"],
            "difficulty": "intermediate",
            "tags": ["strike", "close-range"],
            "cameraPreset": "front",
            "prerequisites": ["kung-fu.palm-strike"],
            "commonMistakes": [
                {"description": "Keeping fingers too stiff instead of curving naturally into the claw shape"},
                {"description": "Striking with the fingertips instead of the full claw surface"}
            ]
        },
        {
            "id": "kung-fu.praying-mantis",
            "name": "Praying Mantis Hook",
            "description": "A hooking strike and deflection technique drawn from the Praying Mantis style. The wrist bends into a tight hook and simultaneously traps or redirects the opponent's arm while a secondary strike is delivered to an exposed target.",
            "modelPath": "content/kung-fu/models/praying-mantis.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.8, "fps": 30},
                {"name": "strike", "durationSeconds": 1.2, "fps": 30}
            ],
            "muscles": ["shoulders", "back", "core"],
            "difficulty": "advanced",
            "tags": ["strike", "combination"],
            "cameraPreset": "three_quarter",
            "prerequisites": ["kung-fu.leopard-punch"],
            "commonMistakes": [
                {"description": "Forming the hook with the entire hand rather than just the wrist"},
                {"description": "Losing shoulder alignment when executing the simultaneous deflect-and-strike"}
            ]
        },
        {
            "id": "kung-fu.drop-stance",
            "name": "Drop Stance",
            "description": "A deep low stance in which the practitioner squats entirely onto one leg while the other extends along the ground. Used to dodge high attacks, transition under an opponent's guard, or set up sweeping counterstrikes from an unexpected angle.",
            "modelPath": "content/kung-fu/models/drop-stance.glb",
            "defaultClip": "hold",
            "clips": [
                {"name": "hold", "durationSeconds": 2.0, "fps": 30},
                {"name": "transition", "durationSeconds": 1.0, "fps": 30}
            ],
            "muscles": ["quadriceps", "glutes", "hip_flexors"],
            "difficulty": "intermediate",
            "tags": ["stance", "evasion"],
            "cameraPreset": "side",
            "prerequisites": ["kung-fu.horse-stance"],
            "commonMistakes": [
                {"description": "Allowing the knee of the supporting leg to drift inward under load"},
                {"description": "Placing the extended foot with the sole up instead of heel on the ground"}
            ]
        }
    ],
    "yoga": [
        {
            "id": "yoga.cat-cow",
            "name": "Cat-Cow Flow",
            "description": "A rhythmic spinal mobilisation moving between two complementary positions. Cat arches the spine upward while the chin drops; Cow lets the belly descend as the chest lifts. Coordinating each transition with the breath warms the spine and establishes the breath-movement link foundational to all yoga.",
            "modelPath": "content/yoga/models/cat_cow.glb",
            "defaultClip": "flow",
            "clips": [
                {"name": "flow", "durationSeconds": 4.0, "fps": 24}
            ],
            "muscles": ["back", "core"],
            "difficulty": "beginner",
            "tags": ["warmup", "fundamental"],
            "cameraPreset": "side",
            "prerequisites": [],
            "commonMistakes": [
                {"description": "Moving the head independently instead of letting it follow the natural spinal curve"},
                {"description": "Rushing the transitions instead of linking each movement to an inhale or exhale"}
            ]
        },
        {
            "id": "yoga.plank",
            "name": "Plank Pose",
            "description": "A straight-arm hold with the body parallel to the floor, supported by hands and toes. The entire posterior chain and core must engage simultaneously to maintain a rigid plank shape. It is the foundational upper-body strengthening posture that precedes arm balances and transitions between poses.",
            "modelPath": "content/yoga/models/plank.glb",
            "defaultClip": "hold",
            "clips": [
                {"name": "hold", "durationSeconds": 5.0, "fps": 24}
            ],
            "muscles": ["core", "shoulders", "chest"],
            "difficulty": "beginner",
            "tags": ["strength", "fundamental"],
            "cameraPreset": "side",
            "prerequisites": ["yoga.downward-dog"],
            "commonMistakes": [
                {"description": "Letting the hips sag or pike upward, breaking the straight line from head to heel"},
                {"description": "Shifting the shoulders forward past the wrists instead of stacking them directly above"}
            ]
        },
        {
            "id": "yoga.wheel-pose",
            "name": "Wheel Pose",
            "description": "A full spinal backbend in which the body forms an arched bridge, balanced on hands and feet with the torso reaching upward. Demands simultaneous shoulder flexibility, thoracic extension, and hip flexor length. One of the deepest backbends in the Hatha yoga canon.",
            "modelPath": "content/yoga/models/wheel_pose.glb",
            "defaultClip": "hold",
            "clips": [
                {"name": "hold", "durationSeconds": 3.0, "fps": 24}
            ],
            "muscles": ["back", "shoulders", "glutes", "quadriceps"],
            "difficulty": "advanced",
            "tags": ["backbend", "strength"],
            "cameraPreset": "side",
            "prerequisites": ["yoga.bridge"],
            "commonMistakes": [
                {"description": "Externally rotating the feet, which closes the hip and strains the lower back"},
                {"description": "Failing to press the chest forward through the arms, collapsing into a passive arch"}
            ]
        }
    ],
    "calisthenics": [
        {
            "id": "calisthenics.skin-the-cat",
            "name": "Skin the Cat",
            "description": "A hanging shoulder-mobility drill in which the legs are brought up, through, and behind the body while hanging from a bar or rings, then reversed. Develops an exceptional range of motion in the shoulder girdle and builds the posterior chain strength needed for levers and iron cross work.",
            "modelPath": "content/calisthenics/models/skin-the-cat.glb",
            "defaultClip": "swing",
            "clips": [
                {"name": "swing", "durationSeconds": 3.0, "fps": 30}
            ],
            "muscles": ["back", "shoulders", "core"],
            "difficulty": "intermediate",
            "tags": ["pull", "flexibility"],
            "cameraPreset": "side",
            "prerequisites": ["calisthenics.pull-up"],
            "commonMistakes": [
                {"description": "Using momentum to swing through instead of controlling each phase"},
                {"description": "Allowing the elbows to flare out rather than keeping the arms externally rotated"}
            ]
        },
        {
            "id": "calisthenics.back-lever",
            "name": "Back Lever",
            "description": "A horizontal hold in which the body is suspended face-down, parallel to the ground, from a bar or rings by the arms extended overhead. The entire posterior chain and shoulder girdle must resist gravity simultaneously. Considered the entry-level horizontal lever before moving to the front lever.",
            "modelPath": "content/calisthenics/models/back-lever.glb",
            "defaultClip": "hold",
            "clips": [
                {"name": "hold", "durationSeconds": 3.0, "fps": 30}
            ],
            "muscles": ["back", "shoulders", "core", "glutes"],
            "difficulty": "advanced",
            "tags": ["hold", "advanced"],
            "cameraPreset": "side",
            "prerequisites": ["calisthenics.skin-the-cat"],
            "commonMistakes": [
                {"description": "Allowing the hips to drop, breaking the straight line from shoulders to feet"},
                {"description": "Bending the elbows instead of maintaining fully extended arms throughout"}
            ]
        },
        {
            "id": "calisthenics.tuck-planche",
            "name": "Tuck Planche",
            "description": "A horizontal hold with the body suspended in front of the hands by the arms, with the knees tucked toward the chest to reduce the moment arm. The shoulders push forward past the wrists while the entire body fights gravity with no support from the legs.",
            "modelPath": "content/calisthenics/models/tuck-planche.glb",
            "defaultClip": "hold",
            "clips": [
                {"name": "hold", "durationSeconds": 3.0, "fps": 30}
            ],
            "muscles": ["shoulders", "chest", "core"],
            "difficulty": "advanced",
            "tags": ["hold", "advanced"],
            "cameraPreset": "side",
            "prerequisites": ["calisthenics.push-up", "calisthenics.l-sit"],
            "commonMistakes": [
                {"description": "Not protacting the scapulae enough, causing the hips to stay too high"},
                {"description": "Holding the breath instead of breathing steadily throughout the hold"}
            ]
        }
    ],
    "gym": [
        {
            "id": "gym.incline-bench-press",
            "name": "Incline Bench Press",
            "description": "A bench press performed on a 30-45 degree incline to shift emphasis toward the upper chest and anterior deltoid. The elevated angle reduces lower pec recruitment while increasing demands on the clavicular head of the pectoralis major and the front shoulder.",
            "modelPath": "content/gym/models/incline-bench-press.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.8, "fps": 30},
                {"name": "press", "durationSeconds": 2.0, "fps": 30}
            ],
            "muscles": ["chest", "shoulders", "core"],
            "difficulty": "intermediate",
            "tags": ["press", "compound"],
            "cameraPreset": "side",
            "prerequisites": ["gym.bench-press"],
            "commonMistakes": [
                {"description": "Setting the bench angle too steep, turning the movement into a shoulder press"},
                {"description": "Allowing the wrists to extend backward under the bar instead of staying neutral"}
            ]
        },
        {
            "id": "gym.good-morning",
            "name": "Good Morning",
            "description": "A hip-hinge movement with a barbell on the upper back, in which the torso descends toward horizontal by pushing the hips back and softening the knees. Primarily trains the posterior chain under eccentric load, building the hamstring and lower back strength that supports the deadlift.",
            "modelPath": "content/gym/models/good-morning.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.8, "fps": 30},
                {"name": "lift", "durationSeconds": 2.2, "fps": 30}
            ],
            "muscles": ["hamstrings", "glutes", "back"],
            "difficulty": "intermediate",
            "tags": ["hinge", "compound"],
            "cameraPreset": "side",
            "prerequisites": ["gym.romanian-deadlift"],
            "commonMistakes": [
                {"description": "Rounding the lower back instead of maintaining a neutral spine throughout"},
                {"description": "Using too much knee bend, converting the hinge into a squat pattern"}
            ]
        },
        {
            "id": "gym.bulgarian-split-squat",
            "name": "Bulgarian Split Squat",
            "description": "A single-leg squat in which the rear foot is elevated on a bench, isolating the front leg to perform the full movement. The front knee tracks over the foot while the hip descends straight down. Develops unilateral leg strength, hip mobility, and addresses asymmetries between limbs.",
            "modelPath": "content/gym/models/bulgarian-split-squat.glb",
            "defaultClip": "ready",
            "clips": [
                {"name": "ready", "durationSeconds": 0.8, "fps": 30},
                {"name": "squat", "durationSeconds": 2.0, "fps": 30}
            ],
            "muscles": ["quadriceps", "glutes", "hip_flexors"],
            "difficulty": "intermediate",
            "tags": ["squat", "compound"],
            "cameraPreset": "side",
            "prerequisites": ["gym.squat"],
            "commonMistakes": [
                {"description": "Placing the front foot too close to the bench, forcing excessive forward knee travel"},
                {"description": "Letting the torso collapse forward instead of staying upright with a tall spine"}
            ]
        }
    ]
}


def extend_manifest(discipline_id: str, new_movements: list) -> None:
    path = os.path.join(ASSETS, discipline_id, "manifest.json")
    with open(path) as f:
        manifest = json.load(f)

    existing_ids = {m["id"] for m in manifest["movements"]}
    added = 0
    for mv in new_movements:
        if mv["id"] not in existing_ids:
            manifest["movements"].append(mv)
            added += 1

    with open(path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"  {discipline_id}: +{added} movements -> {len(manifest['movements'])} total")


def main():
    print("Extending manifests to 15 movements per discipline...")
    for disc, movements in EXTENSIONS.items():
        extend_manifest(disc, movements)
    print("\nDone. Run 'python3 tools/gen_placeholder_glbs.py' to scaffold new GLB files.")


if __name__ == "__main__":
    main()
