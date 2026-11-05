package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.UiSounds;
import clicker.gui.anim.Anim;
import clicker.gui.anim.Easing;
import clicker.gui.render.UiRenderer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** An animated on/off switch. The knob slides and briefly stretches as it travels. */
public final class ToggleSwitch extends Widget {
	public static final float WIDTH = 24f;
	public static final float HEIGHT = 13f;

	private final BooleanSupplier getter;
	private final Consumer<Boolean> setter;
	private float progress;
	private boolean initialised;

	public ToggleSwitch(BooleanSupplier getter, Consumer<Boolean> setter) {
		this.getter = getter;
		this.setter = setter;
		this.w = WIDTH;
		this.h = HEIGHT;
	}

	public ToggleSwitch at(float x, float y) {
		this.bounds(x, y, WIDTH, HEIGHT);
		return this;
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		float target = this.getter.getAsBoolean() ? 1f : 0f;
		if (!this.initialised) {
			this.progress = target;
			this.initialised = true;
		}
		this.progress = Anim.approach(this.progress, target, ctx.dt, 15f);
		float t = Easing.smoothstep(this.progress);

		int track = Colors.lerp(p.track(), p.accent(), t);
		if (this.hover > 0f) {
			int hoverTarget = t > 0.5f ? p.accentStrong() : Colors.lerp(p.track(), p.textMuted(), 0.5f);
			track = Colors.lerp(track, hoverTarget, this.hover * 0.35f);
		}
		r.round(this.x, this.y, this.w, this.h, this.h / 2f, track);

		float knobR = this.h / 2f - 1.7f;
		float left = this.x + this.h / 2f;
		float right = this.x + this.w - this.h / 2f;
		float cx = Anim.lerp(left, right, t);
		float cy = this.y + this.h / 2f;
		// Stretch the knob a little mid-travel for a soft, springy feel.
		float stretch = 3.2f * (float) Math.sin(Math.PI * this.progress) * (target != this.progress ? 1f : 0f);
		r.shadow(cx - knobR - stretch / 2f, cy - knobR + 0.4f, knobR * 2f + stretch, knobR * 2f, knobR, 1.4f, p.shadow());
		r.round(cx - knobR - stretch / 2f, cy - knobR, knobR * 2f + stretch, knobR * 2f, knobR, p.knob());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.isActive() && this.contains(mouseX, mouseY)) {
			boolean next = !this.getter.getAsBoolean();
			this.setter.accept(next);
			UiSounds.toggle(next);
			return true;
		}
		return false;
	}
}
