package dev.puddles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PuddleConfig {
	public static final int MIN_FILL = 5;
	public static final int MAX_FILL = 300;
	public static final int MIN_DRY = 10;
	public static final int MAX_DRY = 1200;
	public static final int MIN_AMOUNT = 10;
	public static final int MAX_AMOUNT = 100;
	public static final int MIN_CLARITY = 20;
	public static final int MAX_CLARITY = 100;
	public static final int MIN_RANGE = 12;
	public static final int MAX_RANGE = 48;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static PuddleConfig current = new PuddleConfig();

	public boolean enabled = true;
	public PuddleStyle style = PuddleStyle.BLOCKS;
	public int fillSeconds = 25;
	public int drySeconds = 150;
	public int amount = 45;
	public int clarity = 100;
	public int range = 24;
	public boolean ripples = true;
	public boolean splashes = true;

	public static PuddleConfig get() {
		return current;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("puddles.json");
	}

	public static void load() {
		Path file = file();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				PuddleConfig loaded = GSON.fromJson(reader, PuddleConfig.class);
				if (loaded != null) {
					current = loaded;
					if (current.style == null) {
						current.style = PuddleStyle.BLOCKS;
					}
					current.fillSeconds = Mth.clamp(current.fillSeconds, MIN_FILL, MAX_FILL);
					current.drySeconds = Mth.clamp(current.drySeconds, MIN_DRY, MAX_DRY);
					current.amount = Mth.clamp(current.amount, MIN_AMOUNT, MAX_AMOUNT);
					current.clarity = Mth.clamp(current.clarity, MIN_CLARITY, MAX_CLARITY);
					current.range = Mth.clamp(current.range, MIN_RANGE, MAX_RANGE);
				}
			} catch (Exception e) {
				Puddles.LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		save();
	}

	public static void save() {
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(current, writer);
			}
		} catch (IOException e) {
			Puddles.LOGGER.warn("Could not write {}", file, e);
		}
	}
}
