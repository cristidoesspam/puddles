package dev.puddles;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class Masks {
	private static final Map<Integer, boolean[][]> CACHE = new HashMap<>();

	private Masks() {}

	public static boolean[][] get(int size, int variant) {
		return CACHE.computeIfAbsent(size * 100 + variant * 10, key -> shape(size, variant, new Random(size * 31L + variant * 7L + 3)));
	}

	public static boolean[][] turned(int size, int variant, int turns) {
		boolean[][] base = get(size, variant);
		return CACHE.computeIfAbsent(size * 100 + variant * 10 + 1 + (turns & 3), key -> rotate(base, turns & 3));
	}

	private static boolean[][] rotate(boolean[][] source, int turns) {
		int size = source.length;
		boolean[][] cells = new boolean[size][size];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				cells[y][x] = switch (turns) {
					case 0 -> source[y][x];
					case 1 -> source[size - 1 - x][y];
					case 2 -> source[size - 1 - y][size - 1 - x];
					default -> source[x][size - 1 - y];
				};
			}
		}
		return cells;
	}

	private static void fill(boolean[][] cells, int x0, int y0, int x1, int y1) {
		for (int y = Math.max(0, y0); y < Math.min(cells.length, y1); y++) {
			for (int x = Math.max(0, x0); x < Math.min(cells.length, x1); x++) {
				cells[y][x] = true;
			}
		}
	}

	private static void clear(boolean[][] cells, int x0, int y0, int x1, int y1) {
		for (int y = Math.max(0, y0); y < Math.min(cells.length, y1); y++) {
			for (int x = Math.max(0, x0); x < Math.min(cells.length, x1); x++) {
				cells[y][x] = false;
			}
		}
	}

	private static void squircle(boolean[][] cells, double radius, double shiftX, double shiftY) {
		int size = cells.length;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = Math.abs(x + 0.5 - size / 2.0 - shiftX) / (size / 2.0);
				double dy = Math.abs(y + 0.5 - size / 2.0 - shiftY) / (size / 2.0);
				if (Math.pow(dx, 3) + Math.pow(dy, 3) < Math.pow(radius, 3)) {
					cells[y][x] = true;
				}
			}
		}
	}

	private static boolean filled(boolean[][] cells, int x, int y) {
		return y >= 0 && x >= 0 && y < cells.length && x < cells.length && cells[y][x];
	}

	public static int neighbours(boolean[][] cells, int x, int y) {
		int count = 0;
		count += filled(cells, x - 1, y) ? 1 : 0;
		count += filled(cells, x + 1, y) ? 1 : 0;
		count += filled(cells, x, y - 1) ? 1 : 0;
		count += filled(cells, x, y + 1) ? 1 : 0;
		return count;
	}

	private static boolean[][] shape(int size, int variant, Random random) {
		boolean[][] cells = new boolean[size][size];
		int chunk = Math.max(2, size / 4);
		int corner = random.nextInt(4);
		int cx = (corner & 1) == 0 ? 0 : size - 1;
		int cy = (corner & 2) == 0 ? 0 : size - 1;
		switch (variant) {
			case 0 -> squircle(cells, 0.86, 0, 0);
			case 1 -> {
				squircle(cells, 0.8, 0, 0);
				int length = Math.max(2, size / 3);
				int start = 1 + random.nextInt(Math.max(1, size - length - 1));
				int side = random.nextInt(4);
				for (int along = start; along < Math.min(size, start + length); along++) {
					for (int depth = 0; depth < chunk; depth++) {
						switch (side) {
							case 0 -> cells[depth][along] = true;
							case 1 -> cells[size - 1 - depth][along] = true;
							case 2 -> cells[along][depth] = true;
							default -> cells[along][size - 1 - depth] = true;
						}
					}
				}
			}
			case 2 -> {
				squircle(cells, 0.9, 0, 0);
				int reach = chunk + 1;
				clear(cells, cx == 0 ? 0 : size - reach, cy == 0 ? 0 : size - reach, cx == 0 ? reach : size, cy == 0 ? reach : size);
			}
			case 3 -> {
				fill(cells, 1, 1, size - 1, size - 1);
				int half = Math.max(2, size / 2);
				clear(cells, cx == 0 ? 0 : size - half, cy == 0 ? 0 : size - half, cx == 0 ? half : size, cy == 0 ? half : size);
			}
			case 4 -> {
				int top = Math.max(1, size * 3 / 10);
				fill(cells, 0, top, size, size - top);
				cells[top][0] = false;
				cells[top][size - 1] = false;
				cells[size - top - 1][0] = false;
				cells[size - top - 1][size - 1] = false;
			}
			default -> {
				double shift = size / 8.0;
				double sx = (corner & 1) == 0 ? -shift : shift;
				double sy = (corner & 2) == 0 ? -shift : shift;
				squircle(cells, 0.66, sx, sy);
				if (size >= 8) {
					int ox = cx == 0 ? size - 2 : 0;
					int oy = cy == 0 ? size - 2 : 0;
					fill(cells, ox, oy, ox + 2, oy + 2);
					int px = cx == 0 ? size - 1 : 0;
					int py = cy == 0 ? 1 + random.nextInt(2) : size - 2 - random.nextInt(2);
					fill(cells, px, py, px + 1, py + 2);
				}
			}
		}
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (cells[y][x] && neighbours(cells, x, y) == 0) {
					cells[y][x] = false;
				}
			}
		}
		return cells;
	}
}
