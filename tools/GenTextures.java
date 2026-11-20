import dev.puddles.Masks;

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

	private static int[][] layers(boolean[][] cells) {
		int size = cells.length;
		int[][] layer = new int[size][size];
		for (int step = 0; step < size; step++) {
			boolean changed = false;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					if (cells[y][x] && layer[y][x] == step && Masks.neighbours(cells, x, y) == 4) {
						boolean deeper = true;
						for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
							deeper &= layer[y + d[1]][x + d[0]] >= step;
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
		boolean[][] cells = Masks.get(size, variant);
		int[][] layer = layers(cells);
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double[] flat = {0.88, 0.92, 0.94, 0.97, 1.0};
		boolean deep = variant % 2 == 1;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (!cells[y][x]) {
					continue;
				}
				double shade = deep
					? Math.max(0.88, 0.99 - 0.03 * Math.min(layer[y][x], 4) + (random.nextInt(3) - 1) * 0.015)
					: flat[random.nextInt(flat.length)];
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

	private static void water(BufferedImage image, boolean[][] cells, int left, int top, int pixel) {
		Random random = new Random(7);
		double[] tint = {0.247, 0.463, 0.894};
		for (int y = 0; y < cells.length; y++) {
			for (int x = 0; x < cells.length; x++) {
				if (!cells[y][x]) {
					continue;
				}
				double shade = 0.69 + (random.nextDouble() - 0.5) * 0.28;
				int x0 = left + x * pixel;
				int y0 = top + y * pixel;
				int under = image.getRGB(x0, y0);
				int out = 0xFF000000;
				for (int c = 0; c < 3; c++) {
					int shift = 16 - 8 * c;
					int value = (int) Math.round(0.7 * tint[c] * shade * 255 + 0.3 * ((under >> shift) & 255));
					out |= Math.min(255, value) << shift;
				}
				for (int j = 0; j < pixel; j++) {
					for (int i = 0; i < pixel; i++) {
						image.setRGB(x0 + i, y0 + j, out);
					}
				}
			}
		}
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
		water(image, Masks.turned(16, 1, 1), 0, 0, 8);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(ripple(2), 34, 38, 36, 36, null);
		g.drawImage(ripple(1), 76, 62, 28, 28, null);
		g.dispose();
		return image;
	}
}
