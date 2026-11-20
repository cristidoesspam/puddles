package dev.puddles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

final class Puddle extends SingleQuadParticle {
	static final int VARIANTS = 4;
	static final double LIFT = 0.02;
	static final float FILL_RANGE = 0.18f;

	private static final float[] SUNNY = {0.4f, 0.64f, 1f};
	private static final float[] OVERCAST = {0.55f, 0.65f, 0.8f};
	private static final float MAX_ALPHA = 0.95f;

	private static float overcast;

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

	private Puddle(ClientLevel level, long key, double x, double z, Ground.Spot spot, int variant, float size, float threshold, float yaw) {
		super(level, x, spot.top() + LIFT + variant * 0.004, z, Minecraft.getInstance().getAtlasManager()
			.getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(Puddles.id("puddle_" + variant)));
		this.hasPhysics = false;
		this.lifetime = Integer.MAX_VALUE;
		this.key = key;
		this.size = size;
		this.top = spot.top();
		this.threshold = threshold;
		this.ground = spot.pos();
		this.surface = spot.state();
		this.rotation = new Quaternionf().rotateY(yaw).rotateX(-Mth.HALF_PI);
		this.alpha = 0f;
	}

	static void setOvercast(float amount) {
		overcast = Mth.clamp(amount, 0f, 1f);
	}

	static Puddle create(ClientLevel level, int column, int row, float region, float threshold) {
		Ground.Spot spot = Ground.probe(level, column, row);
		if (spot == null || Layout.rand(column, row, 3) >= spot.weight()) {
			return null;
		}
		double x = column + 0.5 + (Layout.rand(column, row, 4) - 0.5) * 0.5;
		double z = row + 0.5 + (Layout.rand(column, row, 5) - 0.5) * 0.5;
		float size = (1.15f + 1.4f * Layout.rand(column, row, 6)) * (0.8f + 0.5f * region);
		if (!isFlat(level, spot, x, z, size)) {
			size *= 0.55f;
			if (!isFlat(level, spot, x, z, size)) {
				return null;
			}
		}
		int variant = (int) (Layout.rand(column, row, 7) * VARIANTS) % VARIANTS;
		float yaw = Layout.rand(column, row, 8) * Mth.TWO_PI;
		return new Puddle(level, BlockPos.asLong(column, 0, row), x, z, spot, variant, size, threshold, yaw);
	}

	private static boolean isFlat(ClientLevel level, Ground.Spot spot, double x, double z, float size) {
		double reach = size * 0.4;
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
		double radius = size * 0.3 * fill;
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
		float eased = Layout.smooth(0f, 1f, Mth.lerp(partialTick, previousFill, fill));
		return size / 2f * eased;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
		float shown = Mth.lerp(partialTickTime, previousFill, fill);
		alpha = MAX_ALPHA * PuddleConfig.get().clarity / 100f * Mth.clamp(shown * 2f, 0f, 1f);
		setColor(Mth.lerp(overcast, SUNNY[0], OVERCAST[0]), Mth.lerp(overcast, SUNNY[1], OVERCAST[1]), Mth.lerp(overcast, SUNNY[2], OVERCAST[2]));
		extractRotatedQuad(state, camera, rotation, partialTickTime);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
