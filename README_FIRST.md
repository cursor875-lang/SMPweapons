# SECURITY FIX IN THIS COPY

The former AdminAccess.java had hardcoded account grants and automatic operator escalation; the unused CommandLogFilterInstaller.java attempted command-log suppression. Both classes and the AdminAccess startup registration have been removed from this copy. This does not certify the rest of the plugin as secure or tested.

# SMPWeapons 2.0 – Java Paper 26.3 project

This is a **source-code project**, not a compiled or server-tested `.jar`.

It restores the original `SMPWeapons-2.zip` Java sources (22 items, original game-event framework, Juggernaut, original ability implementations), adds a **23rd item: Flower Sword**, and integrates strict WorldGuard `spawn2` checks and modular YAML settings. It does **not** depend on Skript, SkBee or skript-reflect.

## How to build

**Requirements:** Java **25**, Maven 3.9+, Paper **26.3** server, WorldEdit **7.4.6**, and WorldGuard **7.0.21**. The JAR includes only your plugin classes and YAML resources; WorldGuard/WorldEdit/Paper must be installed on the server.

```bash
cd SMPWeapons_Paper26_3_Java
mvn clean package
```

The result, **if compilation succeeds**, will be `target/smpweapons-2.0.0.jar`. Do not place this source folder directly into `plugins`.

If you don't have Java 25/Maven on your computer, upload the project contents to a GitHub repository and run the included **Actions → Build Paper 26.3 plugin** workflow. The build artifact contains the JAR only when the workflow succeeds. **No compiled JAR is included in this ZIP.**

## Install

1. Back up your server and test in a **separate Paper 26.3 test server** first.
2. Install WorldEdit and WorldGuard builds compatible with 26.3.
3. Remove old `SMPWeapons` JARs and the old SMPWeapons `.sk` scripts to prevent duplicate abilities. If you have a separate `flowersword.sk`, remove that too.
4. Copy the compiled `smpweapons-2.0.0.jar` into `plugins/` and restart.
5. Create or confirm the `spawn2` WorldGuard region in every world where it should exist.
6. Edit the generated `plugins/SMPWeapons/*.yml` files and use `/smpweaponsconfig reload` to reload YAML.

Useful WorldGuard region flags (optional additional protection):

```text
/rg flag spawn2 pvp deny
/rg flag spawn2 invincible allow
/rg flag spawn2 mob-damage deny
```

## Commands

| Command | Purpose |
|---|---|
| `/smpweapons list` | List all custom item IDs |
| `/smpweapons give <player> flower_sword` | Give the native Java Flower Sword |
| `/smpweapons give <player> all` | Give all 23 original/new items |
| `/smpweaponsconfig reload` | Reload all YAML settings |
| `/smpweaponsconfig regioncheck` | Confirm the configured region is present |
| `/weaponevent start flower_sword [player]` | Start Flower Festival event for the sword |
| `/weaponevent list` | List events |
| `/trust`, `/untrust`, `/trustlist` | Existing trusted-player system |
| `/ability <name>` | F-key ability backup command (original system) |

`/smpweaponsconfig` and `/smpweapons give` require `smpweapons.admin` (default: OP).

## Flower Sword

- Diamond Sword; configurable **Sharpness V**, **Fire Aspect III**, unbreakable.
- Hostile monsters will not target/attack the holder (when the passive is enabled).
- Every **5th** successful melee hit: permanent, naturally breakable cobweb (only on empty, WorldGuard-authorized ground) and Slowness II for three seconds.
- **Shift + Left-click** activates **Flowering Sanctuary**. Left-click **air or a block**; Sneak + hit another entity also works.
- The sanctuary is a **stationary** 5-block-radius field for 10 seconds; owner gets Regeneration II when inside; enemies inside get Blindness, Slowness and repeated damage. It spawns green/pink particles and temporary random flowers which are cleaned up.
- 30-second cooldown (all values adjustable in `flower-sword.yml`).
- Cannot be further enchanted, anvil-combined/renamed, or upgraded to Netherite via normal tables.

## WorldGuard safety

`protection.yml` defaults to region `spawn2`. The plugin cancels damage inside this region, prevents harmful new effects there, disallows abilities starting there, checks direct player/target regions for targeting, denies custom block edits in the region, and checks build permission against WorldGuard's `Flags.BUILD` for mining, temporary walls, cobwebs and flowers. Movement and projectile protections additionally block ability pushes/teleports/projectiles entering `spawn2`. Attribute-based custom debuffs clear on region entry.

**Note:** Some protections are intentionally broader than the custom weapons alone: `cancel-damage-inside` cancels ordinary damage inside `spawn2` as well, and `remove-negative-effects-on-entry` can strip unrelated negative effects. Adjust these if your other plugins depend on them.

**Important:** These checks are an extra safety layer, not a substitute for correct WorldGuard flags and manual tests of every original ability; the original plugin has many complex features.

## YAML files

| File | Configurable settings |
|---|---|
| `config.yml` | Legacy compatibility options |
| `weapons.yml` | **All 23** items: availability, material, display name, lore, enchantment levels and unbreakable |
| `abilities.yml` | All original cooldowns, enabled flags and select tuned damage/radius/duration settings |
| `passives.yml` | Configurable hit thresholds, lifesteal and selected passive stats |
| `events.yml` | Event enable toggle and gather-message settings |
| `protection.yml` | `spawn2` ID and WorldGuard protection toggles |
| `flower-sword.yml` | **Every custom Flower Sword ability parameter**: hits, damage, duration, range, potions, particles, flower choices/count, cooldown, locks |
| `messages.yml` | Editable base messages for future polish (some original Java messages remain hardcoded) |

**Not every numeric literal** from the 5,000+ lines of original Java source is configurable yet. The source retains original defaults where YAML settings are not wired. New item names/enchantments/lore affect newly generated items; existing PDC-tagged items keep their old metadata.

## Verification status

- Source was checked for Java syntax issues as far as locally possible (a parse attempt showed missing Bukkit dependencies, not a Java syntax error).
- All YAML files were parsed successfully, and all 23 item definitions were found.
- This environment cannot run **JDK 25 + Maven + Paper 26.3 API dependencies**, so **compilation, reload and gameplay have not been verified**. Build/test via the included Java 25 CI workflow before deployment.
- The project contains no bundled Paper/WorldGuard/WorldEdit JARs; Maven fetches these dependencies during build.
