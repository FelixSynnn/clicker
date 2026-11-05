package clicker.gui.components;

import clicker.gui.anim.Anim;
import net.minecraft.client.input.KeyEvent;

import java.util.function.BooleanSupplier;

/**
 * Base class for Clicker's lightweight widgets. Bounds are in units; input coordinates are in the same space.
 */
public abstract class Widget {
	protected float x;
	protected float y;
	protected float w;
	protected float h;
	protected boolean visible = true;
	protected BooleanSupplier active = () -> true;
	protected boolean hovered;
	/** Animated hover amount, 0..1. */
	protected float hover;

	public Widget bounds(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		return this;
	}

	public Widget activeWhen(BooleanSupplier condition) {
		this.active = condition;
		return this;
	}

	public boolean isActive() {
		return this.active.getAsBoolean();
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public float x() {
		return this.x;
	}

	public float y() {
		return this.y;
	}

	public float width() {
		return this.w;
	}

	public float height() {
		return this.h;
	}

	public float bottom() {
		return this.y + this.h;
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= this.x && mouseX < this.x + this.w && mouseY >= this.y && mouseY < this.y + this.h;
	}

	public final void render(UiContext ctx) {
		if (!this.visible) {
			return;
		}
		this.hovered = ctx.hoverEnabled && this.isActive() && this.contains(ctx.mouseX, ctx.mouseY);
		this.hover = Anim.approach(this.hover, this.hovered ? 1f : 0f, ctx.dt, 16f);
		boolean dimmed = !this.isActive();
		if (dimmed) {
			ctx.r.pushAlpha(0.45f);
		}
		this.draw(ctx);
		if (dimmed) {
			ctx.r.popAlpha();
		}
	}

	protected abstract void draw(UiContext ctx);

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		return false;
	}

	public boolean keyPressed(KeyEvent event) {
		return false;
	}

	/** True while the widget wants every key press (e.g. a key binding being recorded). */
	public boolean capturesKeyboard() {
		return false;
	}
}
