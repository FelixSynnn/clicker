package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.UiSounds;
import clicker.gui.anim.Anim;
import clicker.gui.render.UiRenderer;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/**
 * A smooth integer slider. Drag or click the track, scroll the wheel (Shift for x10), or use the arrow keys while
 * hovering for precise steps. The knob glides to its value and shows a value bubble while dragging.
 */
public final class CustomSlider extends Widget {
	public static final float HEIGHT = 12f;
	private static final float TRACK = 4f;
	private static final float KNOB_RADIUS = 4.6f;

	private final int min;
	private final int max;
	private final IntSupplier getter;
	private final IntConsumer setter;
	private final IntFunction<Component> bubble;
	private float display;
	private float grow;
	private float bubbleAlpha;
	private boolean dragging;
	private boolean initialised;

	public CustomSlider(int min, int max, IntSupplier getter, IntConsumer setter, IntFunction<Component> bubble) {
		this.min = min;
		this.max = max;
		this.getter = getter;
		this.setter = setter;
		this.bubble = bubble;
		this.h = HEIGHT;
	}

	private float fraction(int value) {
		return (value - this.min) / (float) (this.max - this.min);
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		float target = this.fraction(this.getter.getAsInt());
		if (!this.initialised) {
			this.display = target;
			this.initialised = true;
		}
		this.display = Anim.approach(this.display, target, ctx.dt, this.dragging ? 45f : 17f);
		this.grow = Anim.approach(this.grow, this.hovered || this.dragging ? 1f : 0f, ctx.dt, 16f);
		this.bubbleAlpha = Anim.approach(this.bubbleAlpha, this.dragging ? 1f : 0f, ctx.dt, 18f);

		float cy = this.y + this.h / 2f;
		float left = this.x + KNOB_RADIUS;
		float span = this.w - KNOB_RADIUS * 2f;
		float knobX = left + span * this.display;

		r.round(this.x, cy - TRACK / 2f, this.w, TRACK, TRACK / 2f, p.track());
		float fill = knobX - this.x;
		if (fill > 0.5f) {
			r.round(this.x, cy - TRACK / 2f, Math.max(TRACK, fill), TRACK, TRACK / 2f, Colors.lerp(p.accent(), p.accentStrong(), this.grow * 0.35f));
		}

		float radius = KNOB_RADIUS + this.grow * 0.7f;
		r.shadow(knobX - radius, cy - radius + 0.6f, radius * 2f, radius * 2f, radius, 1.8f, p.shadow());
		r.circle(knobX, cy, radius, p.knob());
		r.circle(knobX, cy, 1.7f + this.grow * 0.5f, p.accent());

		if (this.bubbleAlpha > 0.01f) {
			Component label = this.bubble.apply(this.getter.getAsInt());
			float bw = r.width(label) + 8f;
			float bh = 12f;
			float bx = knobX - bw / 2f;
			float by = this.y - bh - 3f - (1f - this.bubbleAlpha) * 3f;
			r.pushAlpha(this.bubbleAlpha);
			r.shadow(bx, by, bw, bh, 4f, 3f, p.shadow());
			r.round(bx, by, bw, bh, 4f, p.accentStrong());
			r.textCentered(label, knobX, by + 2.5f, p.onAccent());
			r.popAlpha();
		}
	}

	private void setFromMouse(double mouseX) {
		float left = this.x + KNOB_RADIUS;
		float span = this.w - KNOB_RADIUS * 2f;
		float t = (float) Math.max(0.0, Math.min(1.0, (mouseX - left) / span));
		int value = Math.round(this.min + t * (this.max - this.min));
		if (value != this.getter.getAsInt()) {
			this.setter.accept(value);
		}
	}

	private void step(int delta) {
		int value = Math.max(this.min, Math.min(this.max, this.getter.getAsInt() + delta));
		if (value != this.getter.getAsInt()) {
			this.setter.accept(value);
		}
	}

	@Override
	public boolean contains(double mouseX, double mouseY) {
		// A slightly taller hit area makes the thin track easy to grab.
		return mouseX >= this.x - 2 && mouseX < this.x + this.w + 2 && mouseY >= this.y - 3 && mouseY < this.y + this.h + 3;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.isActive() && this.contains(mouseX, mouseY)) {
			this.dragging = true;
			this.setFromMouse(mouseX);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		if (this.dragging) {
			this.setFromMouse(mouseX);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.dragging) {
			this.dragging = false;
			UiSounds.release();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (this.isActive() && this.contains(mouseX, mouseY) && amount != 0) {
			int stepSize = net.minecraft.client.Minecraft.getInstance().hasShiftDown() ? 10 : 1;
			this.step(amount > 0 ? stepSize : -stepSize);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.hovered || !this.isActive()) {
			return false;
		}
		int stepSize = event.hasShiftDown() ? 10 : 1;
		if (event.isLeft() || event.isDown()) {
			this.step(-stepSize);
			return true;
		}
		if (event.isRight() || event.isUp()) {
			this.step(stepSize);
			return true;
		}
		return false;
	}
}
