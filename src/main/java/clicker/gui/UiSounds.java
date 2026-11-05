package clicker.gui;

import clicker.ClickerClient;
import clicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Soft interface sounds, silenced when "Sound effects" is off. */
public final class UiSounds {
	private UiSounds() {
	}

	public static void click() {
		play(1.05f, 0.22f);
	}

	public static void toggle(boolean on) {
		play(on ? 1.22f : 0.9f, 0.24f);
	}

	public static void navigate() {
		play(1.38f, 0.16f);
	}

	public static void select() {
		play(1.5f, 0.18f);
	}

	public static void release() {
		play(1.7f, 0.1f);
	}

	private static void play(float pitch, float volume) {
		ConfigManager config = ClickerClient.config();
		if (config != null && !config.get().sounds) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getSoundManager() != null) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume));
		}
	}
}
