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

	private static final int[] PIXELS = {6, 8, 12, 16, 20};
	private static final float[] SIZE_CUTS = {0.15f, 0.4f, 0.68f, 0.9f};
	private static final float[] MUD = {0.46f, 0.34f, 0.22f};
	private static final int CELLS_PER_BLOCK = 8;
	private static final int VARIANTS = 6;
	private static final float GROWTH_STEPS = 5f;

	final long key;
	final float size;
	final double top;

	private final float threshold;
	private final BlockPos ground;
	private final BlockState surface;
	private final Quaternionf rotation;
	private final float opacity;
	private final float[] plain;
	private final float[] safe;
	private float fill;
	private float previousFill;
	private int ticks;

	private Puddle(ClientLevel level, long key, double x, double z, Ground.Spot spot, int pixels, int variant, float threshold, int turns, int rgb, float opacity) {
		super(level, x, spot.top() + LIFT + variant % 3 * 0.004, z, Minecraft.getInstance().getAtlasManager()
			.getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(Puddles.id("puddle_" + pixels + "_" + variant)));
		this.hasPhysics = false;
		this.lifetime = Integer.MAX_VALUE;
		this.key = key;
		this.size = pixels / (float) CELLS_PER_BLOCK;
		this.top = spot.top();
		this.threshold = threshold;
		this.ground = spot.pos();
		this.surface = spot.state();
		this.rotation = new Quaternionf().rotateY(turns * Mth.HALF_PI).rotateX(-Mth.HALF_PI);
		this.opacity = opacity;
		this.plain = new float[] {ARGB.redFloat(rgb), ARGB.greenFloat(rgb), ARGB.blueFloat(rgb)};
		this.safe = Shaders.safe(plain[0], plain[1], plain[2]);
		this.alpha = 0f;
	}

	static Puddle create(ClientLevel level, int column, int row, float region, float threshold) {
		Ground.Spot spot = Ground.probe(level, column, row);
		if (spot == null || Layout.rand(column, row, 3) >= spot.weight()) {
			return null;
		}
		double x = column + Math.round((0.5 + (Layout.rand(column, row, 4) - 0.5) * 0.5) * CELLS_PER_BLOCK) / (double) CELLS_PER_BLOCK;
		double z = row + Math.round((0.5 + (Layout.rand(column, row, 5) - 0.5) * 0.5) * CELLS_PER_BLOCK) / (double) CELLS_PER_BLOCK;
		float roll = Layout.rand(column, row, 6) - (region - 0.5f) * 0.4f;
		int choice = 0;
		while (choice < SIZE_CUTS.length && roll >= SIZE_CUTS[choice]) {
			choice++;
		}
		for (int i = choice; i >= 0; i--) {
			if (isFlat(level, spot, x, z, PIXELS[i] / (float) CELLS_PER_BLOCK)) {
				int variant = (int) (Layout.rand(column, row, 7) * VARIANTS) % VARIANTS;
				int turns = (int) (Layout.rand(column, row, 8) * 4) % 4;
				return new Puddle(level, BlockPos.asLong(column, 0, row), x, z, spot, PIXELS[i], variant, threshold, turns, tint(level, spot, column, row),
					0.85f + 0.15f * Layout.rand(column, row, 22));
			}
		}
		return null;
	}

	private static int tint(ClientLevel level, Ground.Spot spot, int column, int row) {
		int water = level.getBiome(spot.pos()).value().getWaterColor();
		float muddy = spot.mud() * (0.4f + 0.6f * Layout.rand(column, row, 21));
		float light = 0.88f + 0.24f * Layout.rand(column, row, 20);
		return ARGB.color(255,
			Mth.clamp((int) (Mth.lerp(muddy, ARGB.red(water), MUD[0] * 255f) * light), 0, 255),
			Mth.clamp((int) (Mth.lerp(muddy, ARGB.green(water), MUD[1] * 255f) * light), 0, 255),
			Mth.clamp((int) (Mth.lerp(muddy, ARGB.blue(water), MUD[2] * 255f) * light), 0, 255));
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
		alpha = PuddleConfig.get().clarity / 100f * opacity;
		float[] color = Shaders.active() ? safe : plain;
		setColor(color[0], color[1], color[2]);
		extractRotatedQuad(state, camera, rotation, partialTickTime);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
