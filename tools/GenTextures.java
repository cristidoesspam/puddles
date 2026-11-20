import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class GenTextures {
	private static final int SIZE = 64;

	public static void main(String[] args) throws IOException {
		File assets = new File(args.length > 0 ? args[0] : "src/main/resources/assets/puddles");
		File particles = new File(assets, "textures/particle");
		particles.mkdirs();
		for (int i = 0; i < 4; i++) {
			ImageIO.write(puddle(17L * i + 5), "png", new File(particles, "puddle_" + i + ".png"));
		}
		ImageIO.write(ripple(), "png", new File(particles, "ripple.png"));
		ImageIO.write(icon(), "png", new File(assets, "icon.png"));
	}

	private static double smooth(double edge0, double edge1, double value) {
		double t = Math.max(0, Math.min(1, (value - edge0) / (edge1 - edge0)));
		return t * t * (3 - 2 * t);
	}

	private static int rgba(double shade, double alpha) {
		int channel = (int) Math.round(Math.max(0, Math.min(1, shade)) * 255);
		int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
		return a << 24 | channel << 16 | channel << 8 | channel;
	}

	private static BufferedImage puddle(long seed) {
		Random random = new Random(seed);
		double[] amplitude = new double[7];
		double[] phase = new double[7];
		for (int k = 2; k <= 6; k++) {
			amplitude[k] = 0.14 / (k - 1) * (0.5 + random.nextDouble());
			phase[k] = random.nextDouble() * Math.PI * 2;
		}
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double dx = (x + 0.5 - SIZE / 2.0) / (SIZE / 2.0);
				double dy = (y + 0.5 - SIZE / 2.0) / (SIZE / 2.0);
				double rho = Math.hypot(dx, dy);
				double theta = Math.atan2(dy, dx);
				double wobble = 1;
				for (int k = 2; k <= 6; k++) {
					wobble += amplitude[k] * Math.cos(k * theta + phase[k]);
				}
				double edge = 0.58 * wobble;
				double halo = edge + 0.26;
				if (rho >= halo) {
					continue;
				}
				double soil = 0.4 * (1 - smooth(edge * 0.85, halo, rho));
				double depth = (edge - rho) / edge;
				double alpha = soil;
				double shade = 0.1;
				if (depth > 0) {
					double body = smooth(0, 0.16, depth);
					double rim = 1 - smooth(0, 0.3, depth);
					double sky = Math.exp(-(Math.pow(dx + 0.2, 2) + Math.pow(dy + 0.25, 2)) / 0.07) * 0.3;
					shade = 0.45 + 0.3 * smooth(0.1, 0.8, depth) - 0.18 * rim + sky * 0.8;
					alpha = Math.max(soil, body);
				}
				image.setRGB(x, y, rgba(shade, alpha));
			}
		}
		return image;
	}

	private static BufferedImage ripple() {
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double dx = (x + 0.5 - SIZE / 2.0) / (SIZE / 2.0);
				double dy = (y + 0.5 - SIZE / 2.0) / (SIZE / 2.0);
				double rho = Math.hypot(dx, dy);
				double ring = Math.exp(-Math.pow((rho - 0.82) / 0.09, 2)) + Math.exp(-Math.pow((rho - 0.5) / 0.08, 2)) * 0.5;
				image.setRGB(x, y, rgba(1, ring * (1 - smooth(0.9, 1, rho))));
			}
		}
		return image;
	}

	private static BufferedImage tinted(BufferedImage source, double red, double green, double blue) {
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int argb = source.getRGB(x, y);
				int r = (int) (((argb >> 16) & 255) * red);
				int g = (int) (((argb >> 8) & 255) * green);
				int b = (int) ((argb & 255) * blue);
				out.setRGB(x, y, (argb & 0xFF000000) | r << 16 | g << 8 | b);
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
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setComposite(AlphaComposite.SrcOver.derive(0.9f));
		g.drawImage(tinted(puddle(9), 0.5, 0.66, 0.95), 6, 10, 116, 116, null);
		g.setComposite(AlphaComposite.SrcOver);
		g.drawImage(ripple(), 34, 38, 64, 64, null);
		g.drawImage(ripple(), 62, 22, 36, 36, null);
		g.dispose();
		return image;
	}
}
