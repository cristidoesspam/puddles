package dev.puddles;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Puddles {
	public static final String MOD_ID = "puddles";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Puddles() {}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
