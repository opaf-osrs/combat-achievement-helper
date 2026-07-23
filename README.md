# Combat Achievement Helper

A RuneLite plugin that reads your Combat Achievement progress live and tells you what to do next —
ordered by what your account can actually do, not by what is worth the most points in the abstract.

## What it does

Three modes in one side panel:

| Mode | Answers |
|------|---------|
| **CAs** | "What can I knock out right now?" — every doable task, ordered by points against a pure-skill difficulty rating |
| **Bosses** | "What's worth going to?" — a boss directory with the points available at each, ranked by points per hour of a trip |
| **Route** | "How do I reach the next tier?" — the quickest set of tasks that closes the points gap, plus the quests and training that would open more |

It reads your real account: completed achievements from the game's own varps, skill levels, quest
state, and per-boss kill counts (from chat, backfilled from the HiScores on login). Nothing is sent
anywhere — every dataset ships with the plugin and every calculation runs locally.

### Readiness

The thing that makes the recommendations usable is that they respect how far you are from the content.
Each task carries curated recommended stats; how far below them you are sinks it down the list, so a
fresh account is not routed at Dagannoth Kings because the tasks there happen to be worth good points.
Where an account genuinely has nothing left in reach, the Route says so and hands over to a training
plan rather than padding itself out with content 40 levels away.

## Building

Requires JDK 11.

```bash
./gradlew build
```

| Task | What it does |
|------|--------------|
| `./gradlew build` | Compiles and runs the test suite |
| `./gradlew run` | Launches a RuneLite dev client with the plugin loaded |
| `./gradlew renderUi` | Renders the real Swing panel to PNGs in `build/ui-preview/` — no game, no display |
| `./gradlew curationExport` | Exports per-task heuristic scores for the curation spreadsheet |

`renderUi` is the fastest way to see a UI change: it paints the production panel off-screen, so the
PNGs match what the live panel draws.

## Layout

```
src/main/java/.../core/     pure domain — no net.runelite.* imports, fully unit tested
src/main/java/.../bridge/   the only code that reads the live client
src/main/java/.../varbit/   Combat Achievement completion, read from packed varps
src/main/resources/         the bundled datasets
src/test/java/              unit tests, plus the headless panel renderer
```

The domain layer never touches the client, which is why the panel can be rendered and the rankings
tested without a game running.

## Data

Everything ships bundled — the plugin makes no network calls for data.

| File | Holds |
|------|-------|
| `combat_achievements.json` | The 637 tasks: tier, type, points, monster, description |
| `task_effort.json` | Skill and quest gates per task |
| `rec_stats.json` | Recommended stats per task — the basis of the readiness model |
| `task_difficulty.json` / `boss_difficulty.json` | Hand-rated 1–10 pure-skill difficulty, per task and per boss |
| `boss_timing.json` | Kill times and respawn timers |
| `quests.json` / `skills_xp.json` | Quest chains and XP rates, for costing what an unlock would take |
| `task_detail.json` | Setup, strategy and item notes shown on a task |
| `tier_rewards.json` | What each tier unlocks |

The task data is generated from the OSRS Wiki; the difficulty ratings, recommended stats and strategy
notes are hand-curated. Both are compiled to the JSON above and shipped with the plugin, so a release
is reproducible and works offline.

## Licence

BSD 2-Clause — see [LICENSE](LICENSE).

The sidebar icon is the in-game Combat Achievements task book, from the
[OSRS Wiki](https://oldschool.runescape.wiki/w/Combat_Achievements). Old School RuneScape is a
trademark of Jagex Ltd; game assets remain their property.
