# Changelog

## 1.1.1 (2026-11-03)

### Added
* Sneak-placing a pipe beside pipes links it to all of them, so a pipe placed into the gap of a broken branch rejoins both halves. Sneak + empty-hand right-click on the side of a pipe links or unlinks it with the pipe next to it. The hitbox of a tube-only pipe now ends at the block centre where the model ends.

### Fixed
* The top and bottom of the junction hub, and the top of a tower pipe that carries an arm, no longer show the black `pump_top` circle that looked like a hole; they use plain metal or the grate. The lowest pipe of a branch going down is closed with the grate as well.

## 1.1.0 (2026-11-02)

### Added
* **Branch pipes**: sneak-placing a pipe against the side of another pipe links them. Open ends of branches become extra suction points (rotating with the tower top), so one pump can pump several floors. Costs `branchEnergyPercent` (200 %) of `energyPerOperation` plus `branchEnergyPercentPerPipe` (10 %) per further pipe. New config keys `branchPipesEnabled`, `maxBranchPipes`, `branchEnergyPercent`, `branchEnergyPercentPerPipe`. The pipe block gained four boolean block state properties (`north`, `east`, `south`, `west`); existing pipes load unchanged.
* Startup check (`PotrBindingGuard`) that the installed Pollution of the Realms still has the methods this mod calls. It stops loading with a readable message, or only logs when `strictDependencyCheck` is false.

### Changed
* The required mod ranges now have an upper bound (`potr_version_range`, `forgeendertech_version_range` in `gradle.properties`).
* The pump backs off only after the tower top and every branch end came up empty in a row.
* The pump energy reading is clamped to the capacity, so lowering `energyCapacity` no longer shows more than the maximum.

### Removed
* The empty Mixin config and its `[[mixins]]` entry.

### Notes
* Modern Industrialization and NTM Decentralized Edition already feed the pump through their own Forge Energy bridges; see the README.

## 1.0.2 (2026-10-22)

### Changed
* A changed tower now also pulls the next suction attempt forward (a random delay within one work interval). Before, only the idle backoff counter was cleared while the already scheduled attempt, up to `maxIdleIntervalTicks` away, stayed in place.
* The classes used by the first suction (including the Pollution of the Realms ones) are loaded during mod loading instead of in the middle of the first server tick that needs them. If Pollution of the Realms is incompatible, the error now appears at startup instead of inside a block entity tick.

## 1.0.1 (2026-10-20)

### Changed

* The fan model keeps its on/off look for at least 20 ticks, so a pump hovering around "just enough energy" cannot flood nearby players with block updates (the update flag stays "clients only" on purpose).
* Energy is written to the chunk at most once per second instead of on every cable insertion.
* Tower walks (notify pump, break tower) are capped by the world height.
* Attempts end early when the pump has no possible destination or when no chunk section in the scan box can hold pollution (palette check), so pumps over clean air no longer scan block by block.
* The chunks of the scan box are looked up once per attempt, and an attempt gives up after 8 clouds that could not be delivered or were vetoed by an addon.
* Pumps start with a random delay, so pumps loaded together no longer scan on the same tick.
* After a failed attempt the wait doubles up to `maxIdleIntervalTicks` (default 240), and resets after a success or when the tower changes.
* The fan model stays on for 100 ticks after the last moment of sufficient energy, so trickle-fed pumps do not flicker.
* A cloud standing right beside the pump is no longer re-released to another side (it wasted energy and let short towers re-suck their own output).
* Config maximums lowered to `pumpRadius` 16 and `pumpHeightRange` 8, which bounds the worst-case scan to about 18,500 positions per attempt instead of about 139,000. Existing config values above the new maximums are reset to the default (NeoForge logs a warning), so re-check these two keys after updating.

## 1.0.0 (2026-10-19)

Initial unofficial port of **Pollutant Pump** 1.4.0 (bright_spark, GPLv3) to NeoForge 1.21.1.

### Ported
* Pollutant Pump block and Pump Pipe block, with the original models, textures, recipes and animated fan.
* Pipe tower mechanics: extend by right-clicking with a pipe, breaking a pipe removes the pipes above it.
* Suction logic: moves one cloud of air pollution from the area around the top pipe into an adjacent Filter Frame, or back into the open air.
* Forge Energy support.

### Changed
* Rewritten in Java 21 for NeoForge 21.1.x (the original is Kotlin for Forge 1.16.5).
* Configuration moved from a SERVER spec to a COMMON `ModConfigSpec` with new keys (`pumpRadius`, `pumpHeightRange`, `workIntervalTicks`, `energyPerOperation`, `energyCapacity`).
* Energy is spent per successful suction (default 40 FE) instead of every tick while powered.
* The scan area is walked in random order without allocating position lists, and never loads chunks.
* Pipes are identified by the `pump_pipes` block tag instead of by class.

### Added
* Addon API: `PumpPollutantSuckedEvent.Pre` / `.Post`, `PumpCapabilities.PUMP_STATUS`, `PumpTags.PUMP_PIPES`.
* Optional `keepEnergyWhenBroken` using the new `stored_energy` data component.
* Optional compat: Jade tooltip, JEI info pages, Advanced Chimneys pipes in the tower tag.
* English and Vietnamese translations.

### Fixed
* A pump without a pipe above it used to scan around the world origin (0, 0, 0).
* Pipes above a broken pipe were lost in survival, they are now dropped at the broken position.
* Energy could be extracted from the pump by cables.
