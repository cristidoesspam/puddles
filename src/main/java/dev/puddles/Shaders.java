package dev.puddles;

import java.lang.reflect.Method;

final class Shaders {
	static final float SPRITE_FLOOR = 0.88f;

	private static final float MIN_BRIGHTNESS = 1.02f;
	private static final float MIN_GREEN = 0.5f;
	private static final int STEPS = 24;
	private static final float STEP = 0.05f;

	private static boolean looked;
	private static Object api;
	private static Method inUse;
	private static boolean active;

	private Shaders() {}

	static boolean active() {
		return active;
	}

	static void refresh() {
		if (!looked) {
			looked = true;
			try {
				Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				api = type.getMethod("getInstance").invoke(null);
				inUse = type.getMethod("isShaderPackInUse");
			} catch (ReflectiveOperationException | LinkageError e) {
				api = null;
			}
		}
		if (api == null) {
			return;
		}
		try {
			active = (boolean) inUse.invoke(api);
		} catch (ReflectiveOperationException e) {
			active = false;
		}
	}

	static float[] safe(float red, float green, float blue) {
		float floor = SPRITE_FLOOR * SPRITE_FLOOR;
		for (int step = 0; step <= STEPS; step++) {
			float scale = 1f + step * STEP;
			float r = Math.min(1f, red * scale);
			float g = Math.min(1f, green * scale);
			float b = Math.min(1f, blue * scale);
			if ((r * r + g * g + b * b) * floor >= MIN_BRIGHTNESS && g * SPRITE_FLOOR >= MIN_GREEN) {
				return new float[] {r, g, b};
			}
		}
		return new float[] {1f, 1f, 1f};
	}
}
