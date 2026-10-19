# Pollutant Pump Reforged Unofficial

An unofficial port of the **Pollutant Pump** mod by **bright_spark**, for the NeoForge mod loader and Minecraft **1.21.1**.

Pollutant Pump is an addon for **Pollution of the Realms** that adds a powered pump and a pipe tower which suck clouds of air pollution out of the sky and feed them into Filter Frames.

## Credits and licensing

| | |
|---|---|
| Original mod | [Pollutant Pump](https://github.com/thebrightspark/PollutantPump) by **bright_spark**, licensed under the **GNU GPL v3** |
| This port | **thanhtnt2** (GitHub: @thanhttn1), derivative work, licensed under the **GNU GPL v3** (`GPL-3.0-only`) |
| Required mods | Pollution of the Realms and ForgeEnderTech by EnderLanky (All Rights Reserved) |

* This project is an **unofficial** port of the original mod by bright_spark. And it is not affiliated with or endorsed by bright_spark or the authors of Pollution of the Realms.
* The source is a rewrite of the original Kotlin/Forge 1.16.5 code in Java 21 for NeoForge 1.21.1. Textures and models come from the original mod. Every derived source file carries a notice that it was modified, as required by GPLv3 section 5.
* The full license text is in [`LICENSE.txt`](LICENSE.txt). It is also packed into the mod jar under `META-INF/`.
* As an unofficial port building upon GPLv3-licensed code, the compiled mod as a whole is, and will always remain, licensed under the **GNU General Public License v3 (GPLv3).**
* The template files of the Gradle project come from the NeoForged MDK, see [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt) (MIT).
* The new API code written for this port (located in `thanhttn1.pollutantpump.reforged.api`) is also fully open and licensed under GPLv3 as part of this project. While the author of the port reserves the theoretical right to dual-license these specific new files for other projects, this port itself strictly adheres to the GPLv3 upstream requirements.
* **No code, asset or binary of Pollution of the Realms, ForgeEnderTech, Advanced Chimneys, Jade or JEI is included in this repository.** They are only used as compile-time and dev-runtime dependencies from your own `libs/` folder.

## How it works

1. Place a **Pollutant Pump** and stack **Pump Pipes** on top of it (right-click a pipe with another pipe to extend the tower; breaking a pipe removes all pipes above it).
2. Power the pump with Forge Energy.
3. Every `workIntervalTicks` the pump looks for one cloud of air pollution inside the box around the **top pipe**, and moves it into a Filter Frame touching the pump. If no filter has room the cloud is released into an empty block next to the pump (can be disabled).

Compared with the 1.16.5 original this port also fixes a few things: a pump without pipes no longer scans around the world origin, pipes are returned when a tower is broken in survival, and the pump no longer lets cables drain its energy.

## Configuration

`config/pollutantpumpreforgedunofficial-common.toml`

| Key | Default | Meaning |
|---|---|---|
| `pumpRadius` | 5 | Horizontal range (X/Z) around the top pipe |
| `pumpHeightRange` | 1 | Vertical range (Y) around the top pipe |
| `workIntervalTicks` | 60 | Ticks between two suction attempts |
| `energyPerOperation` | 40 | FE spent per successful suction |
| `energyCapacity` | 1000 | FE the pump can store |
| `keepEnergyWhenBroken` | false | Dropped pump item keeps its energy (data component `stored_energy`) |
| `releaseIntoOpenAir` | true | Release pollution that no filter can hold next to the pump |

## Addon API

Stable API lives in `thanhttn1.pollutantpump.reforged.api`:

* `PumpPollutantSuckedEvent.Pre` (cancellable, `setAmount`) and `.Post`, posted on `NeoForge.EVENT_BUS` on the logical server.
* `PumpCapabilities.PUMP_STATUS` block capability returning `IPumpStatus`.
* `PumpTags.PUMP_PIPES` block tag: add your own pipe block through a JSON tag.

```java
NeoForge.EVENT_BUS.addListener((PumpPollutantSuckedEvent.Pre event) -> {
    if (event.getLevel().dimension() == Level.NETHER) {
        event.setCanceled(true); // pumps do not work in the Nether
    }
});
```

## Optional compat (`integration/compat`)

| Mod | What it adds |
|---|---|
| Jade | Tooltip with pump state, tower top, range and cost (Jade Addons is **not** needed) |
| JEI | Info pages for the pump and the pipe |
| Advanced Chimneys | `adchimneys:pipe` is an optional entry of the `pump_pipes` tag, so its pipes can extend a pump tower |

To add another mod: add a constant to `CompatModule`, then put its code in its own sub-package.

## Building

Requires **JDK 21**.

1. Put these jars in the `libs/` folder (they are git-ignored, never commit them):
   * Pollution of the Realms for NeoForge 1.21.1 (`AdPother-...jar`), required
   * ForgeEnderTech for NeoForge 1.21.1 (`forgeendertech-...jar`), required
   * Jade, JEI, Advanced Chimneys: optional, only needed to test those modules in the dev game
2. `./gradlew build` (Windows: `gradlew.bat build`). The mod jar is created in `build/libs/`.
3. `./gradlew runClient` starts a test client.
