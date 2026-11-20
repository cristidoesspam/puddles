# Puddles

A client-side Fabric mod for Minecraft 26.3. When it rains, puddles form on the ground, ripple under the raindrops, splash when you walk through them, and dry up again once the weather clears.
Nothing has to be installed on the server: it works in singleplayer and on any server.

## What you get

- **Puddles that fill up.** While it rains, puddles appear a few at a time and grow, and they are bigger and more common the longer it rains. Rain that stops lets them shrink and dry out, the small ones first.
- **They form where water would stay.**
  - Dirt, mud, clay and paths hold the most water, stone and planks nearly as much, grass and mycelium less, gravel little.
  - Sand, snow, ice, leaves and water never get one.
  - Only open ground gets puddles: nothing forms under a roof or a tree (the top of the roof gets them instead), and a puddle is skipped when it would hang over an edge or a step. Build a roof over one and it disappears.
  - Biomes without rain (deserts, snowy biomes) stay dry.
- **Rain ripples.** Small square pixel rings pop up on a puddle every time a raindrop lands.
- **Splashes.** You, other players and mobs splash when walking through a puddle: spray, a ripple and a soft sound.
- **Blocky, vanilla-style water.** The sprites are chunky pixel art on a grid of eighths of a block, turned only in quarter turns so the pixels line up with the ground, and the water takes the colour of the biome: blue in plains, murky green in swamps.
- **Variety.** Five sizes from three-quarters of a block to two and a half, six shapes (round, bumped, notched, L, strip, and a main puddle with a few drops next to it), flat or deeper-looking shading, and a slightly different shade and clarity for every puddle. Puddles on dirt, paths and mud are a little muddier.
- **It matches the light.** Puddles are lit like the ground around them, so they go dark at night and under a stormy sky.

## Settings

Open them from **Mod Menu** (Config button) or bind a key under *Controls > Puddles*.

| Setting | Default | Meaning |
| --- | --- | --- |
| Puddles | on | Master switch |
| Fill Time | 25 s | Seconds of steady rain for all puddles to fill |
| Dry Time | 150 s | Seconds for all puddles to dry after the rain stops |
| Amount | 45% | How many puddles form |
| Clarity | 75% | How strongly puddles show; lower is more see-through |
| Range | 24 blocks | How far from you puddles are drawn |
| Rain Ripples | on | Rings where raindrops land |
| Splashes | on | Spray, ripples and sound when walking through a puddle |

Settings are saved in `config/puddles.json`. There are also two bindable keys: open settings, and toggle the mod.

`/puddles wet <0-100>` shows puddles at that wetness without any rain, and `/puddles wet off` goes back to the real weather.

## How it works

Puddles are ordinary particles lying flat on the ground (five sizes, six shapes each), so the game does the culling, lighting and drawing. One wetness value rises while it rains and falls while it does not. Every puddle spot is fixed by a hash of its column and has its own wetness at which it appears, so the same ground always gets the same puddles, they appear and vanish in a steady order, and the mod keeps no list of them besides the ones near you. Every half second it checks the columns around you, finds the ground with the world's motion-blocking heightmap and makes the puddles that are due.

## Shader packs

Puddles are ordinary particles, so they work with Iris and shader packs, and nothing has to be set up. Tested with Iris 1.11.7 and Sodium 0.9.2:

- **BSL v10.1.8:** bright, clean puddles.
- **Complementary Reimagined r5.9.3:** works, but paler. It treats translucent particles like tinted glass, and it guesses what a particle is from its colour (a dark blue one looks like a water drop or smoke to it). While a pack is active the mod makes the water colour a little brighter so these guesses do not match. Over yellow or orange ground the result looks bluish grey.

Other packs are untested. The water does not reflect the sky: shader packs only reflect things they know are water.

## Building

```
gradlew build
```

The jar is `build/libs/puddles-1.0.0.jar`. Needs JDK 25, Fabric Loader 0.19.5+, Fabric API 0.163.0+26.3. Mod Menu is optional.

`tools/GenTextures.java` draws the sprites and the icon (`java tools/GenTextures.java src/main/resources/assets/puddles`).

## Limits

- The water is a flat tinted sprite, not a real reflection: it does not mirror the sky, trees or you.
- A puddle only shows when it fits on one level of one kind of ground; on bumpy terrain some spots stay dry.
- Ground under any solid block, glass included, counts as covered.
- Not every shader pack treats particles the same way. See Shader packs above; packs that replace the particle pipeline may draw puddles differently.
