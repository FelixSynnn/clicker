package clicker.gui.pages;

import clicker.ClickerClient;
import clicker.clicking.ClickerManager;
import clicker.config.ClickerConfig;
import clicker.config.ConfigManager;
import clicker.gui.ClickerScreen;
import clicker.gui.components.Container;
import clicker.gui.render.UiRenderer;

/** Base class for a sidebar page. Pages lay out their widgets for a given content width. */
public abstract class PageView extends Container {
	protected static final float GAP = 8f;

	protected final ClickerScreen screen;
	protected float contentHeight;

	protected PageView(ClickerScreen screen) {
		this.screen = screen;
	}

	/** Positions every widget inside a column starting at (x, y) with the given width. */
	public abstract void layout(UiRenderer r, float x, float y, float width);

	public float contentHeight() {
		return this.contentHeight;
	}

	/** Every widget on this page, depth-first in drawing order (used by the in-game tests). */
	public java.util.List<clicker.gui.components.Widget> allWidgets() {
		java.util.List<clicker.gui.components.Widget> out = new java.util.ArrayList<>();
		collect(this, out);
		return out;
	}

	private static void collect(Container container, java.util.List<clicker.gui.components.Widget> out) {
		for (clicker.gui.components.Widget child : container.children()) {
			out.add(child);
			if (child instanceof Container nested) {
				collect(nested, out);
			}
		}
	}

	protected static ConfigManager configManager() {
		return ClickerClient.config();
	}

	protected static ClickerConfig config() {
		return ClickerClient.config().get();
	}

	protected static ClickerManager manager() {
		return ClickerClient.manager();
	}

	protected static void changed() {
		ClickerClient.config().markDirty();
	}
}
