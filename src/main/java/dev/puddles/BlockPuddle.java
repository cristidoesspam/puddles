package dev.puddles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BlockPuddle extends Puddle {
	private static final Identifier WATER = Identifier.withDefaultNamespace("block/water_still");
	private static final Quaternionf FLAT = new Quaternionf().rotateX(-Mth.HALF_PI);

	private static TextureAtlasSprite sprite;

	private final float opacity;
	private final float red;
	private final float green;
	private final float blue;

	private BlockPuddle(ClientLevel level, int column, int row, Ground.Spot spot, float threshold, int rgb, float opacity) {
		super(level, BlockPos.asLong(column, 0, row), column + 0.5, spot.top() + LIFT, row + 0.5, spot, water(), 1f, threshold);
		this.opacity = opacity;
		this.red = ARGB.redFloat(rgb);
		this.green = ARGB.greenFloat(rgb);
		this.blue = ARGB.blueFloat(rgb);
	}

	static void reset() {
		sprite = null;
	}

	private static TextureAtlasSprite water() {
		if (sprite == null) {
			sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(WATER);
		}
		return sprite;
	}

	static List<BlockPuddle> create(ClientLevel level, Map<Long, Puddle> active, int column, int row, float threshold) {
		Ground.Spot origin = Ground.probe(level, column, row);
		if (origin == null || Layout.rand(column, row, 3) >= origin.weight()) {
			return null;
		}
		int[][] cells = Shapes.pick(Layout.rand(column, row, 6), (int) (Layout.rand(column, row, 8) * 4) % 4);
		int water = level.getBiome(origin.pos()).value().getWaterColor();
		float opacity = 0.9f + 0.1f * Layout.rand(column, row, 22);
		List<BlockPuddle> group = new ArrayList<>(cells.length);
		for (int[] cell : cells) {
			int x = column + cell[0];
			int z = row + cell[1];
			Ground.Spot spot = cell[0] == 0 && cell[1] == 0 ? origin : Ground.probe(level, x, z);
			if (spot == null || Math.abs(spot.top() - origin.top()) > 1.0E-3 || active.containsKey(BlockPos.asLong(x, 0, z))
					|| !Ground.rains(level, x, z)) {
				return null;
			}
			group.add(new BlockPuddle(level, x, z, spot, threshold, water, opacity));
		}
		return group;
	}

	@Override
	double spread() {
		return 0.35;
	}

	@Override
	boolean contains(double px, double py, double pz) {
		return fill() > 0.35f && Math.abs(py - top) < 0.6 && Math.abs(px - x) < 0.5 && Math.abs(pz - z) < 0.5;
	}

	@Override
	public float getQuadSize(float partialTick) {
		return 0.5f;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
		alpha = PuddleConfig.get().clarity / 100f * opacity * Layout.smooth(0f, 1f, Mth.clamp(shown(partialTickTime) * 2f, 0f, 1f));
		setColor(red, green, blue);
		extractRotatedQuad(state, camera, FLAT, partialTickTime);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT_TERRAIN;
	}
}
