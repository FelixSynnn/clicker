package clicker.gui.components;

import clicker.gui.Palette;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;

import java.util.function.Function;
import java.util.function.Supplier;

/** A single line of text, optionally right-aligned or wrapped into a paragraph. */
public final class Label extends Widget {
	public enum Align {
		LEFT,
		CENTER,
		RIGHT
	}

	private final Supplier<Component> text;
	private final Function<Palette, Integer> color;
	private Align align = Align.LEFT;
	private boolean wrap;
	private float size = 1f;

	public Label(Supplier<Component> text, Function<Palette, Integer> color) {
		this.text = text;
		this.color = color;
		this.h = 9f;
	}

	public Label align(Align align) {
		this.align = align;
		return this;
	}

	/** Wraps the text to the label width; {@link #measure} returns the wrapped height. */
	public Label wrap() {
		this.wrap = true;
		return this;
	}

	public Label size(float factor) {
		this.size = factor;
		return this;
	}

	public float measure(UiRenderer r, float width) {
		if (this.wrap) {
			return r.paragraphHeight(this.text.get(), width, 2f);
		}
		return 9f * r.textScale(this.size);
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Component value = this.text.get();
		int c = this.color.apply(ctx.p);
		if (this.wrap) {
			r.paragraph(value, this.x, this.y, this.w, c, 2f);
			return;
		}
		Component fitted = this.w > 0 ? r.ellipsize(value, this.w / r.textScale(this.size)) : value;
		switch (this.align) {
			case CENTER -> r.textCentered(fitted, this.x + this.w / 2f, this.y, c, this.size);
			case RIGHT -> r.textRight(fitted, this.x + this.w, this.y, c, this.size);
			default -> r.text(fitted, this.x, this.y, c, this.size);
		}
	}
}
