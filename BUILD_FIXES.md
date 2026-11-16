# SMPWeapons — Java compile-error repair

This project is a corrected source copy of the previous SMPWeapons Paper 26.3 build project. It addresses the **42 compiler errors** in the GitHub Actions Maven log supplied November 15, 2026.

## Repairs

- `Abilities.java`: typed map merges and potion effects; typed living-entity lists; corrected shadowed lambda variables; typed rollback map and runnable collection.
- `StatusManager.java`: fixed generic status-expiry map and numeric comparisons; typed active-entity iteration.
- `JuggernautManager.java`: typed UUID and block collections.
- `WeaponEvents.java`: typed block-removal predicates, tab completions, player lists, collection counts, and arena participants.
- `Commands.java`: typed custom items, trusted UUIDs and tab completions.
- `ItemListener.java`: typed hit-counter map merge.
- `NoDragonEgg.java`: compute portal coordinates before capturing them in the asynchronous closure, instead of reassigning `final` variables.
- `WeaponsConfig.java`: typed gather map, numeric item counts, and typed entry iteration.

The original hardcoded OP-grant and command-log filtering source remains removed. `FlowerSword.java`, `SafeZones.java`, and YAML configs are retained.

## Build on GitHub

Put **the contents of this project** at your repository root, preserving `pom.xml`, `src/`, and `.github/workflows/build.yml`. Commit and push. Open **Actions → Build Paper 26.3 plugin → latest run**. If successful, download the JAR from **Artifacts**.

If the build still fails, copy the **first** Maven `[ERROR]` compilation messages along with filenames and line numbers. Do not copy only `MojoFailureException`, which is a generic failure summary.

## Validation limit

These are targeted source fixes based on the error log. **Not fully compiled or tested.** The local environment lacks the Java 25 toolchain and Maven/Paper dependencies; further errors may appear only after a full build. The ZIP is a source project, NOT a ready-to-install plugin JAR. Test on a private Paper 26.3 server with WorldGuard and WorldEdit before deploying publicly.
