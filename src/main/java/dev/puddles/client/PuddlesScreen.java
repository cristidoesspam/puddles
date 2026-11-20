package dev.puddles.client;

import dev.puddles.PuddleConfig;
import dev.puddles.PuddleStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class PuddlesScreen extends OptionsSubScreen {
	private static final String KEY = "puddles.option.";

	private final PuddleConfig config = PuddleConfig.get();

	public PuddlesScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("puddles.screen.title"));
	}

	@Override
	protected void addOptions() {
		list.addBig(toggle("enabled", config.enabled, v -> config.enabled = v));
		list.addBig(style());
		list.addBig(slider("fill", PuddleConfig.MIN_FILL, PuddleConfig.MAX_FILL, config.fillSeconds, "seconds", v -> config.fillSeconds = v));
		list.addBig(slider("dry", PuddleConfig.MIN_DRY, PuddleConfig.MAX_DRY, config.drySeconds, "seconds", v -> config.drySeconds = v));
		list.addBig(slider("amount", PuddleConfig.MIN_AMOUNT, PuddleConfig.MAX_AMOUNT, config.amount, "percent", v -> config.amount = v));
		list.addBig(slider("clarity", PuddleConfig.MIN_CLARITY, PuddleConfig.MAX_CLARITY, config.clarity, "percent", v -> config.clarity = v));
		list.addBig(slider("range", PuddleConfig.MIN_RANGE, PuddleConfig.MAX_RANGE, config.range, "blocks", v -> config.range = v));
		list.addSmall(
			toggle("ripples", config.ripples, v -> config.ripples = v),
			toggle("splashes", config.splashes, v -> config.splashes = v));
	}

	@Override
	public void removed() {
		PuddleConfig.save();
		super.removed();
	}

	private OptionInstance<PuddleStyle> style() {
		return new OptionInstance<>(KEY + "style", tooltip("style"),
			(caption, value) -> Component.translatable(KEY + "style." + value.getSerializedName()),
			new OptionInstance.Enum<>(List.of(PuddleStyle.values()), StringRepresentable.fromEnum(PuddleStyle::values)),
			config.style, v -> config.style = v);
	}

	private static <T> OptionInstance.TooltipSupplier<T> tooltip(String name) {
		Tooltip tip = Tooltip.create(Component.translatable(KEY + name + ".tooltip"));
		return value -> tip;
	}

	private static OptionInstance<Boolean> toggle(String name, boolean value, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(KEY + name, tooltip(name), value, setter::accept);
	}

	private static OptionInstance<Integer> slider(String name, int min, int max, int value, String unit, IntConsumer setter) {
		IntFunction<Component> label = v -> Component.translatable(KEY + unit + ".value", Component.translatable(KEY + name), v);
		return new OptionInstance<>(KEY + name, tooltip(name), (caption, v) -> label.apply(v),
			new OptionInstance.IntRange(min, max), value, setter::accept);
	}
}
