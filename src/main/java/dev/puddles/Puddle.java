package dev.puddles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

final class Puddle extends SingleQuadParticle {
	static final double LIFT = 0.02;
	static final float FILL_RANGE = 0.18f;

	private static final int[] PIXELS = {16, 24, 32};
	private static final int VARIANTS = 3;
	private static final float GROWTH_STEPS = 5f;

	final long key;
	final float size;
	final double top;

	private final float threshold;
	private final BlockPos ground;
	private final BlockState surface;
	private final Quaternionf rotation;
	private float fill;
	private float previousFill;
	private int ticks;

	private Puddle(ClientLevel level, long key, double x, double z, Ground.Spot spot, int pixels, int variant, float threshold, int turns) {
		super(level, x, spot.top() + LIFT + variant * 0.004, z, Minecraft.getInstance().getAtlasManager()
			.getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(Puddles.id("puddle_" + pixels + "_" + variant)));
		this.hasPhysics = false;
		this.lifetime = Integer.MAX_VALUE;
		this.key = key;
		this.size = pixels / 16f;
		this.top = spot.top();
		this.threshold = threshold;
		this.ground = spot.pos();
		this.surface = spot.state();
		this.rotation = new Quaternionf().rotateY(turns * Mth.HALF_PI).rotateX(-Mth.HALF_PI);
		int water = level.getBiome(spot.pos()).value().getWaterColor();
		this.setColor(ARGB.redFloat(water), ARGB.greenFloat(water), ARGB.blueFloat(water));
		this.alpha = 0f;
	}

	static Puddle create(ClientLevel level, int column, int row, float region, float threshold) {
		Ground.Spot spot = Ground.probe(level, column, row);
		if (spot == null || Layout.rand(column, row, 3) >= spot.weight()) {
			return null;
		}
		double x = column + Math.round((0.5 + (Layout.rand(column, row, 4) - 0.5) * 0.5) * 16) / 16.0;
		double z = row + Math.round((0.5 + (Layout.rand(column, row, 5) - 0.5) * 0.5) * 16) / 16.0;
		float roll = Layout.rand(column, row, 6) - (region - 0.5f) * 0.4f;
		for (int i = roll < 0.35f ? 0 : roll < 0.75f ? 1 : 2; i >= 0; i--) {
			if (isFlat(level, spot, x, z, PIXELS[i] / 16f)) {
				int variant = (int) (Layout.rand(column, row, 7) * VARIANTS) % VARIANTS;
				int turns = (int) (Layout.rand(column, row, 8) * 4) % 4;
				return new Puddle(level, BlockPos.asLong(column, 0, row), x, z, spot, PIXELS[i], variant, threshold, turns);
			}
		}
		return null;
	}

	private static boolean isFlat(ClientLevel level, Ground.Spot spot, double x, double z, float size) {
		double reach = size * 0.45;
		for (int i = 0; i < 8; i++) {
			double angle = i * Mth.PI / 4.0;
			Ground.Spot other = Ground.probe(level, Mth.floor(x + Math.cos(angle) * reach), Mth.floor(z + Math.sin(angle) * reach));
			if (other == null || Math.abs(other.top() - spot.top()) > 1.0E-3) {
				return false;
			}
		}
		return true;
	}

	double x() {
		return x;
	}

	double z() {
		return z;
	}

	float fill() {
		return fill;
	}

	boolean contains(double px, double py, double pz) {
		double radius = size * 0.4 * fill;
		double dx = px - x;
		double dz = pz - z;
		return fill > 0.35f && Math.abs(py - top) < 0.6 && dx * dx + dz * dz < radius * radius;
	}

	@Override
	public void tick() {
		previousFill = fill;
		float target = Mth.clamp((Wetness.get() - threshold) / FILL_RANGE, 0f, 1f);
		fill += Mth.clamp(target - fill, -0.05f, 0.04f);
		if (target <= 0f && fill <= 0.002f) {
			remove();
			return;
		}
		if ((++ticks & 7) == 0 && (level.getBlockState(ground) != surface || !Ground.isOpen(level, ground))) {
			remove();
		}
	}

	@Override
	public float getQuadSize(float partialTick) {
		float stage = Math.round(Mth.lerp(partialTick, previousFill, fill) * GROWTH_STEPS) / GROWTH_STEPS;
		return size / 2f * stage;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
		alpha = PuddleConfig.get().clarity / 100f;
		extractRotatedQuad(state, camera, rotation, partialTickTime);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
