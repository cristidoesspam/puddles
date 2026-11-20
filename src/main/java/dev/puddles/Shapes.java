package dev.puddles;

final class Shapes {
	private static final int[][][] SHAPES = {
		{{0, 0}},
		{{0, 0}, {1, 0}},
		{{0, 0}, {1, 0}, {2, 0}},
		{{0, 0}, {1, 0}, {0, 1}},
		{{0, 0}, {1, 0}, {0, 1}, {1, 1}},
		{{0, 0}, {1, 0}, {2, 0}, {0, 1}},
		{{0, 0}, {1, 0}, {2, 0}, {1, 1}},
		{{0, 0}, {1, 0}, {0, 1}, {1, 1}, {2, 0}}
	};
	private static final float[] CUTS = {0.3f, 0.52f, 0.62f, 0.76f, 0.84f, 0.9f, 0.95f};

	private Shapes() {}

	static int[][] pick(float roll, int turns) {
		int index = 0;
		while (index < CUTS.length && roll >= CUTS[index]) {
			index++;
		}
		int[][] source = SHAPES[index];
		int[][] cells = new int[source.length][];
		for (int i = 0; i < source.length; i++) {
			int dx = source[i][0];
			int dz = source[i][1];
			cells[i] = switch (turns & 3) {
				case 0 -> new int[] {dx, dz};
				case 1 -> new int[] {-dz, dx};
				case 2 -> new int[] {-dx, -dz};
				default -> new int[] {dz, -dx};
			};
		}
		return cells;
	}
}
