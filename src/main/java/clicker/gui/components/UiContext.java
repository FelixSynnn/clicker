package clicker.gui.components;

import clicker.gui.Palette;
import clicker.gui.render.UiRenderer;

/** Per-frame drawing state handed to every widget. */
public final class UiContext {
	public final UiRenderer r;
	public Palette p;
	/** Mouse position in units (already adjusted for scrolling of the current container). */
	public float mouseX;
	public float mouseY;
	/** Seconds since the previous frame (clamped). */
	public float dt;
	/** False while a modal dialog or an animation should suppress hover effects. */
	public boolean hoverEnabled = true;

	public UiContext(UiRenderer renderer) {
		this.r = renderer;
	}
}
