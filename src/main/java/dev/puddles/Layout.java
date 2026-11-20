package dev.puddles;

import net.minecraft.util.Mth;

final class Layout {
	private static final long SEED = 0x5EED_0F_9D1E5L;

	private Layout() {}

	static long hash(int x, int z, int salt) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ (salt + SEED) * 0x165667B19E3779F9L;
		h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
		h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
		return h ^ (h >>> 31);
	}

	static float rand(int x, int z, int salt) {
		return (hash(x, z, salt) >>> 40) / (float) (1L << 24);
	}

	static float smooth(float edge0, float edge1, float value) {
		float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0f, 1f);
		return t * t * (3f - 2f * t);
	}

	private static float lattice(double x, double z, int salt) {
		int x0 = Mth.floor(x);
		int z0 = Mth.floor(z);
		float fx = smooth(0f, 1f, (float) (x - x0));
		float fz = smooth(0f, 1f, (float) (z - z0));
		float top = Mth.lerp(fx, rand(x0, z0, salt), rand(x0 + 1, z0, salt));
		float bottom = Mth.lerp(fx, rand(x0, z0 + 1, salt), rand(x0 + 1, z0 + 1, salt));
		return Mth.lerp(fz, top, bottom);
	}

	static float region(int x, int z) {
		return 0.65f * lattice(x / 9.0, z / 9.0, 11) + 0.35f * lattice(x / 3.5, z / 3.5, 12);
	}
}
