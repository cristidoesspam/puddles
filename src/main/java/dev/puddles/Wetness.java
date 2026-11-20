package dev.puddles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public final class Wetness {
	private static final float RAIN_THRESHOLD = 0.1f;

	private static float value;
	private static float preview = -1f;

	private Wetness() {}

	public static float get() {
		return preview >= 0f ? preview : value;
	}

	public static void setPreview(float amount) {
		preview = amount < 0f ? -1f : Mth.clamp(amount, 0f, 1f);
	}

	static void reset(float amount) {
		value = amount;
	}

	static void tick(ClientLevel level, PuddleConfig config) {
		float rain = level.getRainLevel(1f);
		if (rain > RAIN_THRESHOLD) {
			value = Math.min(1f, value + rain / (config.fillSeconds * 20f));
		} else {
			value = Math.max(0f, value - 1f / (config.drySeconds * 20f));
		}
	}
}
