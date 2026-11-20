package dev.puddles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

abstract class Puddle extends SingleQuadParticle {
	static final double LIFT = 0.02;
	static final float FILL_RANGE = 0.18f;

	final long key;
	final float size;
	final double top;

	private final float threshold;
	private final BlockPos ground;
	private final BlockState surface;
	private float fill;
	private float previousFill;
	private int ticks;

	Puddle(ClientLevel level, long key, double x, double y, double z, Ground.Spot spot, TextureAtlasSprite sprite, float size, float threshold) {
		super(level, x, y, z, sprite);
		this.hasPhysics = false;
		this.lifetime = Integer.MAX_VALUE;
		this.key = key;
		this.size = size;
		this.top = spot.top();
		this.threshold = threshold;
		this.ground = spot.pos();
		this.surface = spot.state();
		this.alpha = 0f;
	}

	abstract double spread();

	abstract boolean contains(double px, double py, double pz);

	double x() {
		return x;
	}

	double z() {
		return z;
	}

	float fill() {
		return fill;
	}

	float shown(float partialTick) {
		return Mth.lerp(partialTick, previousFill, fill);
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
}
