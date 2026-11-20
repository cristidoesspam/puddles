package dev.puddles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

final class Water {
	private static final Identifier STILL = Identifier.withDefaultNamespace("block/water_still");

	private static TextureAtlasSprite sprite;

	private Water() {}

	static TextureAtlasSprite sprite() {
		if (sprite == null) {
			sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(STILL);
		}
		return sprite;
	}

	static void reset() {
		sprite = null;
	}
}
