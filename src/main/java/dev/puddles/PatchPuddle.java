package dev.puddles;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PatchPuddle extends Puddle {
	private static final Quaternionf FLAT = new Quaternionf().rotateX(-Mth.HALF_PI);
	private static final int[] CELLS = {6, 8, 12, 16, 20};
	private static final float[] SIZE_CUTS = {0.15f, 0.4f, 0.68f, 0.9f};
	private static final int PER_BLOCK = 8;
	private static final int VARIANTS = 6;

	private record Tile(double x, double z, float size, float a, float b, float c, float d, float rank) {}

	private final boolean[][] mask;
	private final int originX;
	private final int originZ;
	private final Tile[] tiles;
	private final float opacity;
	private final float red;
	private final float green;
	private final float blue;
	private float half;
	private float quadU0;
	private float quadU1;
	private float quadV0;
	private float quadV1;

	private PatchPuddle(ClientLevel level, long key, double x, double z, Ground.Spot spot, int cells, int variant, float threshold, boolean[][] mask,
			int originX, int originZ, int rgb, float opacity) {
		super(level, key, x, spot.top() + LIFT + variant % 3 * 0.004, z, spot, Water.sprite(), cells / (float) PER_BLOCK, threshold);
		this.mask = mask;
		this.originX = originX;
		this.originZ = originZ;
		this.tiles = tiles(mask, originX, originZ, x, z, cells / (float) PER_BLOCK);
		this.opacity = opacity;
		this.red = ARGB.redFloat(rgb);
		this.green = ARGB.greenFloat(rgb);
		this.blue = ARGB.blueFloat(rgb);
	}

	static PatchPuddle create(ClientLevel level, int column, int row, float region, float threshold) {
		Ground.Spot spot = Ground.probe(level, column, row);
		if (spot == null || Layout.rand(column, row, 3) >= spot.weight()) {
			return null;
		}
		double x = column + Math.round((0.5 + (Layout.rand(column, row, 4) - 0.5) * 0.5) * PER_BLOCK) / (double) PER_BLOCK;
		double z = row + Math.round((0.5 + (Layout.rand(column, row, 5) - 0.5) * 0.5) * PER_BLOCK) / (double) PER_BLOCK;
		float roll = Layout.rand(column, row, 6) - (region - 0.5f) * 0.4f;
		int choice = 0;
		while (choice < SIZE_CUTS.length && roll >= SIZE_CUTS[choice]) {
			choice++;
		}
		int variant = (int) (Layout.rand(column, row, 7) * VARIANTS) % VARIANTS;
		int turns = (int) (Layout.rand(column, row, 8) * 4) % 4;
		for (int i = choice; i >= 0; i--) {
			boolean[][] mask = Masks.turned(CELLS[i], variant, turns);
			int originX = (int) Math.round(x * PER_BLOCK) - CELLS[i] / 2;
			int originZ = (int) Math.round(z * PER_BLOCK) - CELLS[i] / 2;
			if (isFlat(level, spot, mask, originX, originZ)) {
				int water = level.getBiome(spot.pos()).value().getWaterColor();
				return new PatchPuddle(level, BlockPos.asLong(column, 0, row), x, z, spot, CELLS[i], variant, threshold, mask, originX, originZ, water,
					0.9f + 0.1f * Layout.rand(column, row, 22));
			}
		}
		return null;
	}

	private static boolean isFlat(ClientLevel level, Ground.Spot spot, boolean[][] mask, int originX, int originZ) {
		Set<Long> seen = new HashSet<>();
		for (int j = 0; j < mask.length; j++) {
			for (int i = 0; i < mask.length; i++) {
				if (!mask[j][i]) {
					continue;
				}
				int column = Math.floorDiv(originX + i, PER_BLOCK);
				int row = Math.floorDiv(originZ + j, PER_BLOCK);
				if (seen.add(BlockPos.asLong(column, 0, row))) {
					Ground.Spot other = Ground.probe(level, column, row);
					if (other == null || Math.abs(other.top() - spot.top()) > 1.0E-3) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static Tile[] tiles(boolean[][] mask, int originX, int originZ, double centerX, double centerZ, float extent) {
		int cells = mask.length;
		List<Tile> tiles = new ArrayList<>();
		for (int column = Math.floorDiv(originX, PER_BLOCK); column <= Math.floorDiv(originX + cells - 1, PER_BLOCK); column++) {
			for (int row = Math.floorDiv(originZ, PER_BLOCK); row <= Math.floorDiv(originZ + cells - 1, PER_BLOCK); row++) {
				boolean[][] free = new boolean[PER_BLOCK][PER_BLOCK];
				for (int j = 0; j < PER_BLOCK; j++) {
					for (int i = 0; i < PER_BLOCK; i++) {
						int mx = column * PER_BLOCK + i - originX;
						int mz = row * PER_BLOCK + j - originZ;
						free[j][i] = mx >= 0 && mz >= 0 && mx < cells && mz < cells && mask[mz][mx];
					}
				}
				for (int side = PER_BLOCK; side >= 1; side--) {
					for (int j = 0; j + side <= PER_BLOCK; j++) {
						for (int i = 0; i + side <= PER_BLOCK; i++) {
							if (take(free, i, j, side)) {
								double x = column + (i + side / 2.0) / PER_BLOCK;
								double z = row + (j + side / 2.0) / PER_BLOCK;
								float rank = Mth.clamp((float) (Math.hypot(x - centerX, z - centerZ) / (extent * 0.6)), 0f, 1f);
								tiles.add(new Tile(x, z, side / (float) PER_BLOCK, i / (float) PER_BLOCK, (i + side) / (float) PER_BLOCK,
									j / (float) PER_BLOCK, (j + side) / (float) PER_BLOCK, rank));
							}
						}
					}
				}
			}
		}
		return tiles.toArray(new Tile[0]);
	}

	private static boolean take(boolean[][] free, int i, int j, int side) {
		for (int dj = 0; dj < side; dj++) {
			for (int di = 0; di < side; di++) {
				if (!free[j + dj][i + di]) {
					return false;
				}
			}
		}
		for (int dj = 0; dj < side; dj++) {
			for (int di = 0; di < side; di++) {
				free[j + dj][i + di] = false;
			}
		}
		return true;
	}

	@Override
	double spread() {
		return size * 0.3;
	}

	@Override
	boolean contains(double px, double py, double pz) {
		int i = (int) Math.floor(px * PER_BLOCK) - originX;
		int j = (int) Math.floor(pz * PER_BLOCK) - originZ;
		return fill() > 0.35f && Math.abs(py - top) < 0.6 && i >= 0 && j >= 0 && i < mask.length && j < mask.length && mask[j][i];
	}

	@Override
	public float getQuadSize(float partialTick) {
		return half;
	}

	@Override
	protected float getU0() {
		return quadU0;
	}

	@Override
	protected float getU1() {
		return quadU1;
	}

	@Override
	protected float getV0() {
		return quadV0;
	}

	@Override
	protected float getV1() {
		return quadV1;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
		Vec3 eye = camera.position();
		float shown = shown(partialTickTime);
		float strength = PuddleConfig.get().clarity / 100f * opacity;
		float u0 = sprite.getU0();
		float v0 = sprite.getV0();
		float width = sprite.getU1() - u0;
		float height = sprite.getV1() - v0;
		setColor(red, green, blue);
		for (Tile tile : tiles) {
			float visible = Layout.smooth(tile.rank() * 0.75f, tile.rank() * 0.75f + 0.25f, shown);
			if (visible <= 0f) {
				continue;
			}
			alpha = strength * visible;
			half = tile.size() / 2f;
			quadU0 = u0 + tile.a() * width;
			quadU1 = u0 + tile.b() * width;
			quadV0 = v0 + tile.c() * height;
			quadV1 = v0 + tile.d() * height;
			extractRotatedQuad(state, FLAT, (float) (tile.x() - eye.x()), (float) (y - eye.y()), (float) (tile.z() - eye.z()), partialTickTime);
		}
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT_TERRAIN;
	}
}
