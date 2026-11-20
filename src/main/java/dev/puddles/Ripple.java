package dev.puddles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

final class Ripple extends SingleQuadParticle {
	private static final Quaternionf FLAT = new Quaternionf().rotateX(-Mth.HALF_PI);

	private final float reach;
	private final float strength;

	Ripple(ClientLevel level, double x, double y, double z, float reach, float strength, int lifetime) {
		super(level, x, y, z, Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(Puddles.id("ripple")));
		this.hasPhysics = false;
		this.lifetime = lifetime;
		this.reach = reach;
		this.strength = strength;
		this.setColor(0.92f, 0.96f, 1f);
		this.alpha = 0f;
	}

	@Override
	public void tick() {
		if (++age >= lifetime) {
			remove();
		}
	}

	private float progress(float partialTick) {
		return Mth.clamp((age + partialTick) / lifetime, 0f, 1f);
	}

	@Override
	public float getQuadSize(float partialTick) {
		float t = progress(partialTick);
		return reach / 2f * (0.2f + 0.8f * (1f - (1f - t) * (1f - t)));
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
		float t = progress(partialTickTime);
		alpha = strength * (1f - t) * (1f - t) * Math.min(1f, t * 8f + 0.35f);
		extractRotatedQuad(state, camera, FLAT, partialTickTime);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
