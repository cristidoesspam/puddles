package dev.puddles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PuddleField {
	private static final int SCAN_INTERVAL = 10;
	private static final int RIPPLE_CAP = 300;
	private static final int MAX_SPAWNS_PER_SCAN = 120;
	private static final float MAX_CHANCE = 0.3f;
	private static final float RIPPLE_RATE = 0.03f;
	private static final double RIPPLE_RANGE = 20.0;
	private static final double STEP_RANGE = 24.0;
	private static final double STEP_SPEED_SQR = 0.0016;
	private static final double SPLASH_TOP = 0.05;

	private static final Map<Long, Puddle> ACTIVE = new HashMap<>();
	private static final List<Puddle> NEAR = new ArrayList<>();
	private static final ArrayDeque<Ripple> RIPPLES = new ArrayDeque<>();
	private static ClientLevel world;

	private PuddleField() {}

	public static void reset() {
		ACTIVE.values().forEach(Puddle::remove);
		RIPPLES.forEach(Ripple::remove);
		ACTIVE.clear();
		NEAR.clear();
		RIPPLES.clear();
	}

	public static void tick(ClientLevel level) {
		if (level != world) {
			reset();
			world = level;
			Wetness.reset(level.getRainLevel(1f) > 0.1f ? 0.5f : 0f);
		}
		PuddleConfig config = PuddleConfig.get();
		Wetness.tick(level, config);
		Puddle.setOvercast(level.getRainLevel(1f));
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !config.enabled) {
			if (!ACTIVE.isEmpty() || !RIPPLES.isEmpty()) {
				reset();
			}
			return;
		}
		if (level.getGameTime() % SCAN_INTERVAL == 0) {
			scan(level, player, config);
		}
		if (!NEAR.isEmpty()) {
			if (config.ripples) {
				rain(level, config);
			}
			if (config.splashes) {
				steps(level, player);
			}
		}
	}

	private static void scan(ClientLevel level, LocalPlayer player, PuddleConfig config) {
		ACTIVE.values().removeIf(puddle -> !puddle.isAlive());
		RIPPLES.removeIf(ripple -> !ripple.isAlive());
		float wet = Wetness.get();
		int range = config.range;
		int centerX = Mth.floor(player.getX());
		int centerZ = Mth.floor(player.getZ());
		double limit = (range + 3.0) * (range + 3.0);
		ACTIVE.values().removeIf(puddle -> {
			double dx = puddle.x() - player.getX();
			double dz = puddle.z() - player.getZ();
			if (dx * dx + dz * dz > limit) {
				puddle.remove();
				return true;
			}
			return false;
		});
		if (wet > 0.02f) {
			spawn(level, config, wet, centerX, centerZ);
		}
		NEAR.clear();
		for (Puddle puddle : ACTIVE.values()) {
			double dx = puddle.x() - player.getX();
			double dz = puddle.z() - player.getZ();
			if (puddle.fill() > 0.4f && dx * dx + dz * dz < RIPPLE_RANGE * RIPPLE_RANGE) {
				NEAR.add(puddle);
			}
		}
	}

	private static void spawn(ClientLevel level, PuddleConfig config, float wet, int centerX, int centerZ) {
		int range = config.range;
		float amount = config.amount / 100f;
		int spawned = 0;
		for (int dx = -range; dx <= range && spawned < MAX_SPAWNS_PER_SCAN; dx++) {
			for (int dz = -range; dz <= range; dz++) {
				if (dx * dx + dz * dz > range * range) {
					continue;
				}
				int x = centerX + dx;
				int z = centerZ + dz;
				float region = Layout.region(x, z);
				float chance = MAX_CHANCE * amount * Layout.smooth(0.15f, 0.6f, region);
				if (Layout.rand(x, z, 1) >= chance) {
					continue;
				}
				float threshold = 0.05f + 0.8f * Layout.rand(x, z, 2);
				if (wet < threshold || ACTIVE.containsKey(BlockPos.asLong(x, 0, z)) || !Ground.rains(level, x, z)) {
					continue;
				}
				Puddle puddle = Puddle.create(level, x, z, region, threshold);
				if (puddle != null) {
					ACTIVE.put(puddle.key, puddle);
					Minecraft.getInstance().particleEngine.add(puddle);
					spawned++;
				}
			}
		}
	}

	private static void rain(ClientLevel level, PuddleConfig config) {
		float rain = level.getRainLevel(1f);
		if (rain <= 0.1f) {
			return;
		}
		RandomSource random = level.getRandom();
		float expected = NEAR.size() * RIPPLE_RATE * rain;
		int count = Math.min(8, (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0));
		for (int i = 0; i < count; i++) {
			Puddle puddle = NEAR.get(random.nextInt(NEAR.size()));
			double angle = random.nextFloat() * Mth.TWO_PI;
			double distance = Math.sqrt(random.nextFloat()) * puddle.size * 0.27 * puddle.fill();
			addRipple(level, puddle.x() + Math.cos(angle) * distance, puddle.top + Puddle.LIFT + 0.03,
				puddle.z() + Math.sin(angle) * distance, 0.5f + 0.5f * random.nextFloat(), 0.85f * config.clarity / 100f,
				14 + random.nextInt(9));
		}
	}

	private static void steps(ClientLevel level, LocalPlayer player) {
		RandomSource random = level.getRandom();
		double rangeSqr = STEP_RANGE * STEP_RANGE;
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity living) || living.isSpectator() || living.isInvisible() || !living.onGround()
					|| living.isInWater() || living.isPassenger() || living.distanceToSqr(player) > rangeSqr) {
				continue;
			}
			double dx = living.getX() - living.xo;
			double dz = living.getZ() - living.zo;
			if (dx * dx + dz * dz < STEP_SPEED_SQR || (living.tickCount + living.getId()) % 4 != 0) {
				continue;
			}
			Puddle puddle = at(living.getX(), living.getY(), living.getZ());
			if (puddle == null) {
				continue;
			}
			double y = puddle.top + SPLASH_TOP;
			addRipple(level, living.getX(), y, living.getZ(), 0.55f + 0.4f * random.nextFloat(), 0.8f, 12 + random.nextInt(6));
			for (int i = 0; i < 2; i++) {
				level.addParticle(ParticleTypes.SPLASH, living.getX() + (random.nextDouble() - 0.5) * 0.4, y,
					living.getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
			}
			if ((living.tickCount + living.getId()) % 8 == 0) {
				level.playLocalSound(living.getX(), y, living.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS,
					0.12f, 1.5f + random.nextFloat() * 0.4f, false);
			}
		}
	}

	private static Puddle at(double x, double y, double z) {
		int column = Mth.floor(x);
		int row = Mth.floor(z);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				Puddle puddle = ACTIVE.get(BlockPos.asLong(column + dx, 0, row + dz));
				if (puddle != null && puddle.contains(x, y, z)) {
					return puddle;
				}
			}
		}
		return null;
	}

	private static void addRipple(ClientLevel level, double x, double y, double z, float reach, float strength, int lifetime) {
		if (RIPPLES.size() >= RIPPLE_CAP) {
			return;
		}
		Ripple ripple = new Ripple(level, x, y, z, reach, strength, lifetime);
		RIPPLES.add(ripple);
		Minecraft.getInstance().particleEngine.add(ripple);
	}
}
