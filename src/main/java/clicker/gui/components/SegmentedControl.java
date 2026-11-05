package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.UiSounds;
import clicker.gui.anim.Anim;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** A row of mutually exclusive options with a sliding selection pill. */
public final class SegmentedControl<T> extends Widget {
	public static final float HEIGHT = 16f;

	private final List<T> options;
	private final Function<T, Component> label;
	private final Supplier<T> getter;
	private final Consumer<T> setter;
	private float pill = -1f;
	private int hoveredIndex = -1;

	public SegmentedControl(List<T> options, Function<T, Component> label, Supplier<T> getter, Consumer<T> setter) {
		this.options = List.copyOf(options);
		this.label = label;
		this.getter = getter;
		this.setter = setter;
		this.h = HEIGHT;
	}

	private int selectedIndex() {
		return Math.max(0, this.options.indexOf(this.getter.get()));
	}

	private int indexAt(double mouseX) {
		float segment = this.w / this.options.size();
		int index = (int) ((mouseX - this.x) / segment);
		return Math.max(0, Math.min(this.options.size() - 1, index));
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		int selected = this.selectedIndex();
		if (this.pill < 0f) {
			this.pill = selected;
		}
		this.pill = Anim.approach(this.pill, selected, ctx.dt, 17f);
		this.hoveredIndex = this.hovered ? this.indexAt(ctx.mouseX) : -1;

		float radius = this.h / 2f;
		r.round(this.x, this.y, this.w, this.h, radius, p.track());
		float segment = this.w / this.options.size();
		float inset = 1.6f;
		float px = this.x + this.pill * segment + inset;
		r.shadow(px, this.y + inset + 0.4f, segment - inset * 2f, this.h - inset * 2f, radius - inset, 2f, p.shadow());
		r.round(px, this.y + inset, segment - inset * 2f, this.h - inset * 2f, radius - inset, p.navActive());

		for (int i = 0; i < this.options.size(); i++) {
			Component text = r.ellipsize(this.label.apply(this.options.get(i)), segment - 6f);
			float closeness = 1f - Math.min(1f, Math.abs(this.pill - i));
			int color = Colors.lerp(i == this.hoveredIndex ? p.text() : p.textSecondary(), p.accentStrong(), closeness);
			r.textCentered(text, this.x + segment * (i + 0.5f), this.y + this.h / 2f - 4f, color);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.isActive() && this.contains(mouseX, mouseY)) {
			T option = this.options.get(this.indexAt(mouseX));
			if (option != this.getter.get()) {
				this.setter.accept(option);
				UiSounds.select();
			}
			return true;
		}
		return false;
	}
}
