package clicker;

import clicker.clicking.ClickerManager;
import clicker.config.ConfigManager;
import clicker.gui.ThemeManager;
import clicker.gui.hud.ClickerHud;
import clicker.input.ClickerKeybinds;
import clicker.input.InputHandler;
import clicker.protection.ItemProtection;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Client entrypoint: creates Clicker's services and hooks them into Fabric's events. */
public final class ClickerClient implements ClientModInitializer {
	private static ConfigManager config;
	private static ClickerManager manager;
	private static ThemeManager themes;
	private static ClickerKeybinds keybinds;
	private static InputHandler input;

	@Override
	public void onInitializeClient() {
		config = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(Clicker.MOD_ID + ".json"));
		config.load();

		themes = new ThemeManager(config);
		ItemProtection protection = new ItemProtection(config);
		manager = new ClickerManager(config, protection);
		keybinds = ClickerKeybinds.register();
		input = new InputHandler(keybinds, manager, config);

		ClientTickEvents.END_CLIENT_TICK.register(input::onEndTick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> manager.onDisconnect());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.flush());
		HudElementRegistry.addLast(Clicker.id("status"), new ClickerHud());

		Clicker.LOGGER.info("Clicker {} ready (open the GUI with H, toggle with G)", Clicker.version());
	}

	/** Called at the start of every frame by {@code MinecraftMixin}. */
	public static void onFrame(Minecraft minecraft) {
		if (manager != null) {
			manager.onFrame(minecraft);
		}
	}

	public static ConfigManager config() {
		return config;
	}

	public static ClickerManager manager() {
		return manager;
	}

	public static ThemeManager themes() {
		return themes;
	}

	public static ClickerKeybinds keybinds() {
		return keybinds;
	}

	public static InputHandler input() {
		return input;
	}
}
