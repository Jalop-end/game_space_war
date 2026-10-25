# SPACE WAR — Game Design & Architecture Plan

## 1. High-Level Concept
- **Genre**: 4X Galaxy Conquest Strategy (eXplore, eXpand, eXploit, eXterminate).
- **Inspiration**: Master of Orion (deliberate tactical combat, planet management).
- **Visuals**: Retro 16-bit / 32-bit pixel art with crisp scaling.
- **Platforms**: PC (Windows Desktop) and Android (Mobile touch controls).
- **Engine**: Java + libGDX (shared core logic for cross-platform play).

---

## 2. Galaxy Structure & Exploration
- **Central Core**: Massive Supermassive Black Hole (Orange or Blue Accretion Disk).
- **Spiral Arms**: Logarithmic spiral arm distribution of stars.
- **Sizes**:
  - **Small**: 25 Stars, ~2,100 radius, 2 spiral arms.
  - **Medium**: 45 Stars, ~2,800 radius, 3 spiral arms.
  - **Large**: 75 Stars, ~3,600 radius, 4 spiral arms.
- **Stars & Planets**:
  - 4 Star classifications (Giant, Large, Medium, Dwarf).
  - 11 Celestial Biomes (Terran, Volcanic, Ringed Giant, Gas Giant, Ice, Desert, Toxic, Barren, Frozen, Sulfur, Crystal).
  - Moons orbiting major planets with realistic relative speeds.

---

## 3. The 4 Galactic Factions
1. **Solar Federation** (Blue / Steel): Industrial strength, robust shields, balanced fleets.
2. **Cyber Syndicate** (Gold / Teal): High-tech, energy beams, fast thrusters.
3. **Krak'tor Swarm** (Purple / Lime): Bio-mechanical, fast pop growth, regeneration.
4. **Void Crusade** (Black / Crimson): Kinetic railguns, aggressive armor penetration.

---

## 4. Multi-Day Server & Matchmaking Architecture
- **Room Codes**: 4-character codes (e.g., `GALAXY-7`) to connect across devices.
- **Session Tokens**: Players can close the app on phone/PC and resume without losing turn state.
- **Time Controls**: Simultaneous turns (1 hour or 24 hours per turn) or real-time ticks.
- **Bot AI**: Unfilled faction slots automatically played by server-side bot routines.

---

## 5. Two-Stage Combat Model
1. **Stage 1 — Orbital Fleet Battle**: Tactical hex-grid clash (Movement points, Shields, Hull, Weapon ranges).
2. **Stage 2 — Planetary Invasion**: Orbital bombardment of missile silos followed by Troop Transports landing mech and infantry divisions to conquer cities and factories.
