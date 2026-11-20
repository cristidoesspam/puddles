package dev.puddles;

import net.minecraft.util.StringRepresentable;

public enum PuddleStyle implements StringRepresentable {
	PATCHES("patches"),
	BLOCKS("blocks"),
	PIXELS("pixels");

	private final String id;

	PuddleStyle(String id) {
		this.id = id;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
