package clicker.input;

import clicker.Clicker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

import java.util.List;

/**
 * Clicker's key mappings. They appear in Options &gt; Controls &gt; Key Binds under "Clicker" and are saved by
 * Minecraft in {@code options.txt} like every other control.
 */
public final class ClickerKeybinds {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Clicker.id("main"));

	private final KeyMapping openGui;
	private final KeyMapping toggle;
	private final KeyMapping toggleLeft;
	private final KeyMapping toggleRight;

	private ClickerKeybinds() {
		this.openGui = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.clicker.open_gui", InputConstants.Type.KEYSYM, InputConstants.KEY_H, CATEGORY));
		this.toggle = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.clicker.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_G, CATEGORY));
		this.toggleLeft = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.clicker.toggle_left", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		this.toggleRight = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.clicker.toggle_right", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
	}

	public static ClickerKeybinds register() {
		return new ClickerKeybinds();
	}

	public KeyMapping openGui() {
		return this.openGui;
	}

	public KeyMapping toggle() {
		return this.toggle;
	}

	public KeyMapping toggleLeft() {
		return this.toggleLeft;
	}

	public KeyMapping toggleRight() {
		return this.toggleRight;
	}

	public List<KeyMapping> all() {
		return List.of(this.openGui, this.toggle, this.toggleLeft, this.toggleRight);
	}
}
