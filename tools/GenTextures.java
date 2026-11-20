import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class GenTextures {
	private static final int[] SIZES = {6, 8, 12, 16, 20};
	private static final int VARIANTS = 6;
	private static final int FRAMES = 4;
	private static final int RIPPLE_SIZE = 8;

	public static void main(String[] args) throws IOException {
		File assets = new File(args.length > 0 ? args[0] : "src/main/resources/assets/puddles");
		File particles = new File(assets, "textures/particle");
		particles.mkdirs();
		for (int size : SIZES) {
			for (int variant = 0; variant < VARIANTS; variant++) {
				ImageIO.write(puddle(size, variant, size * 31L + variant * 7L + 3), "png", new File(particles, "puddle_" + size + "_" + variant + ".png"));
			}
		}
		for (int frame = 0; frame < FRAMES; frame++) {
			ImageIO.write(ripple(frame), "png", new File(particles, "ripple_" + frame + ".png"));
		}
		ImageIO.write(icon(), "png", new File(assets, "icon.png"));
	}

	private static int argb(double shade, int alpha) {
		int channel = (int) Math.round(Math.max(0, Math.min(1, shade)) * 255);
		return alpha << 24 | channel << 16 | channel << 8 | channel;
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

	private static boolean filled(boolean[][] cells, int x, int y) {
		return y >= 0 && x >= 0 && y < cells.length && x < cells.length && cells[y][x];
	}

	private static int neighbours(boolean[][] cells, int x, int y) {
		int count = 0;
		count += filled(cells, x - 1, y) ? 1 : 0;
		count += filled(cells, x + 1, y) ? 1 : 0;
		count += filled(cells, x, y - 1) ? 1 : 0;
		count += filled(cells, x, y + 1) ? 1 : 0;
		return count;
	}

	private static int[][] layers(boolean[][] cells) {
		int size = cells.length;
		int[][] layer = new int[size][size];
		for (int step = 0; step < size; step++) {
			boolean changed = false;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					if (cells[y][x] && layer[y][x] == step && neighbours(cells, x, y) == 4) {
						boolean deeper = true;
						for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
							int nx = x + d[0];
							int ny = y + d[1];
							deeper &= layer[ny][nx] >= step;
						}
						if (deeper) {
							layer[y][x] = step + 1;
							changed = true;
						}
					}
				}
			}
			if (!changed) {
				break;
			}
		}
		return layer;
	}

	private static BufferedImage puddle(int size, int variant, long seed) {
		Random random = new Random(seed);
		boolean[][] cells = shape(size, variant, random);
		int[][] layer = layers(cells);
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double[] flat = {0.88, 0.92, 0.94, 0.97, 1.0};
		boolean deep = variant % 2 == 1;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (!cells[y][x]) {
					continue;
				}
				boolean edge = neighbours(cells, x, y) < 4;
				double shade;
				if (edge) {
					shade = 0.88;
				} else if (deep) {
					shade = Math.max(0.88, 0.99 - 0.03 * Math.min(layer[y][x], 4) + (random.nextInt(3) - 1) * 0.015);
				} else {
					shade = flat[random.nextInt(flat.length)];
				}
				image.setRGB(x, y, argb(shade, 255));
			}
		}
		return image;
	}

	private static BufferedImage ripple(int frame) {
		BufferedImage image = new BufferedImage(RIPPLE_SIZE, RIPPLE_SIZE, BufferedImage.TYPE_INT_ARGB);
		double ring = 0.5 + frame;
		for (int y = 0; y < RIPPLE_SIZE; y++) {
			for (int x = 0; x < RIPPLE_SIZE; x++) {
				double dx = Math.abs(x + 0.5 - RIPPLE_SIZE / 2.0);
				double dy = Math.abs(y + 0.5 - RIPPLE_SIZE / 2.0);
				boolean onRing = Math.abs(Math.max(dx, dy) - ring) < 0.01;
				boolean corner = ring > 1 && dx == dy;
				boolean broken = frame == FRAMES - 1 && ((int) (dx + dy)) % 2 == 0;
				if (onRing && !corner && !broken) {
					image.setRGB(x, y, argb(1, 255));
				}
			}
		}
		return image;
	}

	private static BufferedImage tinted(BufferedImage source, double red, double green, double blue) {
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int pixel = source.getRGB(x, y);
				int r = (int) (((pixel >> 16) & 255) * red);
				int g = (int) (((pixel >> 8) & 255) * green);
				int b = (int) ((pixel & 255) * blue);
				out.setRGB(x, y, (pixel & 0xFF000000) | r << 16 | g << 8 | b);
			}
		}
		return out;
	}

	private static BufferedImage icon() {
		int size = 128;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Random random = new Random(42);
		for (int y = 0; y < size; y += 8) {
			for (int x = 0; x < size; x += 8) {
				int base = 70 + random.nextInt(34);
				Color color = new Color(base + 22, base + 4, base - 20);
				for (int j = 0; j < 8; j++) {
					for (int i = 0; i < 8; i++) {
						image.setRGB(x + i, y + j, color.getRGB());
					}
				}
			}
		}
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(tinted(puddle(16, 3, 99), 0.247, 0.463, 0.894), 8, 8, 112, 112, null);
		g.drawImage(ripple(2), 40, 40, 48, 48, null);
		g.drawImage(ripple(1), 70, 24, 32, 32, null);
		g.dispose();
		return image;
	}
}
