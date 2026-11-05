package clicker.input;

import clicker.clicking.ClickType;
import clicker.clicking.ClickerManager;
import clicker.config.ConfigManager;
import clicker.gui.ClickerScreen;
import clicker.gui.UiSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Handles Clicker's key mappings once per client tick.
 *
 * <p>Key mappings only register presses while no screen is open, so typing an "h" in chat never opens the GUI.
 */
public final class InputHandler {
	private static final int ON_COLOR = 0xF27BA5;

	private final ClickerKeybinds keys;
	private final ClickerManager manager;
	private final ConfigManager config;

	public InputHandler(ClickerKeybinds keys, ClickerManager manager, ConfigManager config) {
		this.keys = keys;
		this.manager = manager;
		this.config = config;
	}

	public void onEndTick(Minecraft minecraft) {
		while (this.keys.openGui().consumeClick()) {
			if (minecraft.screen == null) {
				minecraft.setScreen(new ClickerScreen(null));
			}
		}
		while (this.keys.toggle().consumeClick()) {
			this.toggleGlobal(minecraft);
		}
		while (this.keys.toggleLeft().consumeClick()) {
			this.toggleSide(minecraft, ClickType.LEFT);
		}
		while (this.keys.toggleRight().consumeClick()) {
			this.toggleSide(minecraft, ClickType.RIGHT);
		}
		this.config.tick();
	}

	public void toggleGlobal(Minecraft minecraft) {
		boolean enabled = this.manager.toggleGlobal();
		UiSounds.toggle(enabled);
		this.announce(minecraft, Component.translatable(enabled ? "clicker.message.enabled" : "clicker.message.disabled"), enabled);
	}

	private void toggleSide(Minecraft minecraft, ClickType type) {
		boolean enabled = this.manager.toggleSide(type);
		UiSounds.toggle(enabled);
		Component side = Component.translatable(type.translationKey());
		this.announce(minecraft, Component.translatable(enabled ? "clicker.message.side_enabled" : "clicker.message.side_disabled", side), enabled);
	}

	private void announce(Minecraft minecraft, Component message, boolean positive) {
		if (minecraft.player == null) {
			return;
		}
		Component styled = positive
				? message.copy().withStyle(style -> style.withColor(ON_COLOR))
				: message.copy().withStyle(ChatFormatting.GRAY);
		minecraft.gui.setOverlayMessage(styled, false);
	}
}
