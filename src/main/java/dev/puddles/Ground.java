package dev.puddles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

final class Ground {
	static final double MIN_HEIGHT = 0.8;

	record Spot(BlockPos pos, BlockState state, double top, float weight) {}

	private Ground() {}

	static Spot probe(ClientLevel level, int x, int z) {
		if (!level.hasChunk(x >> 4, z >> 4)) {
			return null;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
		if (y < level.getMinY()) {
			return null;
		}
		BlockPos pos = new BlockPos(x, y, z);
		BlockState state = level.getBlockState(pos);
		float weight = weight(state);
		if (weight <= 0f) {
			return null;
		}
		var shape = state.getCollisionShape(level, pos);
		if (shape.isEmpty() || shape.max(Direction.Axis.Y) < MIN_HEIGHT) {
			return null;
		}
		BlockState above = level.getBlockState(pos.above());
		if (!above.getFluidState().isEmpty() || !(above.isAir() || above.is(BlockTags.REPLACEABLE) && !above.is(BlockTags.SNOW))) {
			return null;
		}
		return new Spot(pos, state, y + shape.max(Direction.Axis.Y), weight);
	}

	static boolean rains(ClientLevel level, int x, int z) {
		if (!level.hasChunk(x >> 4, z >> 4)) {
			return false;
		}
		BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
		return level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()) == Biome.Precipitation.RAIN;
	}

	static boolean isOpen(ClientLevel level, BlockPos ground) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, ground.getX(), ground.getZ()) - 1 == ground.getY()
			&& rains(level, ground.getX(), ground.getZ());
	}

	private static float weight(BlockState state) {
		if (!state.getFluidState().isEmpty() || state.is(BlockTags.SAND) || state.is(BlockTags.LEAVES) || state.is(BlockTags.ICE)
				|| state.is(BlockTags.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
			return 0f;
		}
		if (state.is(Blocks.DIRT_PATH) || state.is(Blocks.MUD) || state.is(Blocks.CLAY) || state.is(Blocks.DIRT)
				|| state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
			return 1f;
		}
		if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
			return 0.6f;
		}
		if (state.is(Blocks.GRAVEL)) {
			return 0.4f;
		}
		return 0.85f;
	}
}
