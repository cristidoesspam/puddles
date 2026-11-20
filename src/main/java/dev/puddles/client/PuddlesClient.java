package dev.puddles.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.puddles.PuddleConfig;
import dev.puddles.PuddleField;
import dev.puddles.Puddles;
import dev.puddles.Wetness;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class PuddlesClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Puddles.MOD_ID, Puddles.MOD_ID));

	@Override
	public void onInitializeClient() {
		PuddleConfig.load();

		ClientTickEvents.END_LEVEL_TICK.register(PuddleField::tick);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			Identifier.fromNamespaceAndPath(Puddles.MOD_ID, "reset"),
			(ResourceManagerReloadListener) manager -> PuddleField.reset());

		KeyMapping openSettingsKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.puddles.settings", InputConstants.UNKNOWN.getValue(), CATEGORY));
		KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.puddles.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				PuddleConfig config = PuddleConfig.get();
				config.enabled = !config.enabled;
				PuddleConfig.save();
				if (client.player != null) {
					client.player.sendOverlayMessage(Component.translatable(
						config.enabled ? "puddles.toggle.on" : "puddles.toggle.off"));
				}
			}
			while (openSettingsKey.consumeClick()) {
				if (client.player != null) {
					client.gui.setScreen(new PuddlesScreen(client.gui.screen()));
				}
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
			ClientCommands.literal("puddles")
				.then(ClientCommands.literal("wet")
					.then(ClientCommands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(command -> {
						int percent = IntegerArgumentType.getInteger(command, "percent");
						Wetness.setPreview(percent / 100f);
						command.getSource().sendFeedback(Component.translatable("puddles.command.wet", percent));
						return 1;
					}))
					.then(ClientCommands.literal("off").executes(command -> {
						Wetness.setPreview(-1f);
						command.getSource().sendFeedback(Component.translatable("puddles.command.wet_off"));
						return 1;
					})))));
	}
}
