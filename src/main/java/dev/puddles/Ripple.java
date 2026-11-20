package dev.puddles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

final class Ripple extends SingleQuadParticle {
	private static final int FRAMES = 4;
	private static final Quaternionf FLAT = new Quaternionf().rotateX(-Mth.HALF_PI);

	private static TextureAtlasSprite[] sprites;

	private final float blocks;
	private final float strength;

	Ripple(ClientLevel level, double x, double y, double z, float blocks, float strength, int lifetime) {
		super(level, x, y, z, frames()[0]);
		this.hasPhysics = false;
		this.lifetime = lifetime;
		this.blocks = blocks;
		this.strength = strength;
		this.setColor(0.92f, 0.96f, 1f);
		this.alpha = 0f;
	}

	static void reset() {
		sprites = null;
	}

	private static TextureAtlasSprite[] frames() {
		if (sprites == null) {
			var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
			sprites = new TextureAtlasSprite[FRAMES];
			for (int i = 0; i < FRAMES; i++) {
				sprites[i] = atlas.getSprite(Puddles.id("ripple_" + i));
			}
		}
		return sprites;
	}

	@Override
	public void tick() {
		if (++age >= lifetime) {
			remove();
			return;
		}
		setSprite(frames()[Math.min(FRAMES - 1, age * FRAMES / lifetime)]);
	}

	@Override
	public float getQuadSize(float partialTick) {
		return blocks / 2f;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
		float t = Mth.clamp((age + partialTickTime) / lifetime, 0f, 1f);
		alpha = strength * (1f - t * t);
		extractRotatedQuad(state, camera, FLAT, partialTickTime);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
