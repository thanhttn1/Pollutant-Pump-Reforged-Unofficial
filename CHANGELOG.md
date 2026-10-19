# Changelog

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
* Hardening for busy servers:
  * The fan model keeps its on/off look for at least 20 ticks, so a pump hovering around "just enough energy" cannot flood nearby players with block updates (the update flag stays "clients only" on purpose).
  * Energy is written to the chunk at most once per second instead of on every cable insertion.
  * Tower walks (notify pump, break tower) are capped by the world height.
  * Config maximums lowered to `pumpRadius` 16 and `pumpHeightRange` 8, which bounds the worst-case scan to about 18,500 positions per attempt instead of about 139,000.

### Added
* Addon API: `PumpPollutantSuckedEvent.Pre` / `.Post`, `PumpCapabilities.PUMP_STATUS`, `PumpTags.PUMP_PIPES`.
* Optional `keepEnergyWhenBroken` using the new `stored_energy` data component.
* Optional compat: Jade tooltip, JEI info pages, Advanced Chimneys pipes in the tower tag.
* English and Vietnamese translations.

### Fixed
* A pump without a pipe above it used to scan around the world origin (0, 0, 0).
* Pipes above a broken pipe were lost in survival, they are now dropped at the broken position.
* Energy could be extracted from the pump by cables.
