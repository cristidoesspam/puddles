import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class GenTextures {
	private static final int[] SIZES = {16, 24, 32};
	private static final int VARIANTS = 3;
	private static final int FRAMES = 4;

	public static void main(String[] args) throws IOException {
		File assets = new File(args.length > 0 ? args[0] : "src/main/resources/assets/puddles");
		File particles = new File(assets, "textures/particle");
		particles.mkdirs();
		for (int size : SIZES) {
			for (int variant = 0; variant < VARIANTS; variant++) {
				ImageIO.write(puddle(size, size * 31L + variant * 7L + 3), "png", new File(particles, "puddle_" + size + "_" + variant + ".png"));
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

	private static BufferedImage puddle(int size, long seed) {
		Random random = new Random(seed);
		double[] amplitude = new double[5];
		double[] phase = new double[5];
		for (int k = 2; k <= 4; k++) {
			amplitude[k] = 0.09 * (0.6 + random.nextDouble()) / (k - 1);
			phase[k] = random.nextDouble() * Math.PI * 2;
		}
		boolean[][] inside = new boolean[size][size];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = (x + 0.5 - size / 2.0) / (size / 2.0);
				double dy = (y + 0.5 - size / 2.0) / (size / 2.0);
				double theta = Math.atan2(dy, dx);
				double wobble = 1;
				for (int k = 2; k <= 4; k++) {
					wobble += amplitude[k] * Math.cos(k * theta + phase[k]);
				}
				inside[y][x] = Math.hypot(dx, dy) < 0.86 * wobble;
			}
		}
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double[] shades = {0.8, 0.86, 0.92, 0.92, 1.0};
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (!inside[y][x]) {
					continue;
				}
				boolean edge = x == 0 || y == 0 || x == size - 1 || y == size - 1
					|| !inside[y - 1][x] || !inside[y + 1][x] || !inside[y][x - 1] || !inside[y][x + 1];
				double shade = edge ? 0.7 : shades[random.nextInt(shades.length)];
				image.setRGB(x, y, argb(shade, 255));
			}
		}
		for (int i = 0; i < size / 8; i++) {
			int x = size / 4 + random.nextInt(size / 4 + 1);
			int y = size / 4 + random.nextInt(size / 4 + 1);
			if (inside[y][x] && inside[y][Math.min(size - 1, x + 1)]) {
				image.setRGB(x, y, argb(1, 255));
				image.setRGB(Math.min(size - 1, x + 1), y, argb(1, 255));
			}
		}
		return image;
	}

	private static BufferedImage ripple(int frame) {
		double radius = 2.0 + frame * 1.6;
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double distance = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
				boolean broken = frame == FRAMES - 1 && (x + y) % 2 == 0;
				if (Math.abs(distance - radius) < 0.6 && !broken) {
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
		g.drawImage(tinted(puddle(32, 99), 0.247, 0.463, 0.894), 16, 16, 96, 96, null);
		g.drawImage(ripple(2), 40, 40, 64, 64, null);
		g.drawImage(ripple(0), 24, 24, 48, 48, null);
		g.dispose();
		return image;
	}
}
