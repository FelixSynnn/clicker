package clicker.compat;

import clicker.gui.ClickerScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu entry point. Mod Menu shows Clicker's name, description, version and icon from fabric.mod.json; its
 * "Configure" button simply opens the Clicker GUI. Settings are not duplicated here: the GUI (H) is the one place to
 * configure everything. This class is only loaded when Mod Menu is installed.
 */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ClickerScreen::new;
	}
}
