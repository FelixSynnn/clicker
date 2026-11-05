package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A list row with a real item icon, a name, a short description and a switch. */
public final class ItemToggleRow extends Container {
	public static final float HEIGHT = 26f;

	private final Supplier<ItemStack> icon;
	private final Component name;
	private final Component description;
	private final BooleanSupplier highlighted;
	private final ToggleSwitch toggle;

	public ItemToggleRow(Supplier<ItemStack> icon, Component name, Component description, BooleanSupplier highlighted,
			BooleanSupplier getter, Consumer<Boolean> setter) {
		this.icon = icon;
		this.name = name;
		this.description = description;
		this.highlighted = highlighted;
		this.toggle = this.add(new ToggleSwitch(getter, setter));
		this.h = HEIGHT;
	}

	@Override
	public Widget bounds(float x, float y, float w, float h) {
		super.bounds(x, y, w, h);
		this.toggle.at(x + w - ToggleSwitch.WIDTH - 6f, y + (h - ToggleSwitch.HEIGHT) / 2f);
		return this;
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		boolean held = this.highlighted.getAsBoolean();
		if (held) {
			r.round(this.x, this.y, this.w, this.h, 7f, Colors.fade(p.warning(), 0.12f));
		} else if (this.hover > 0.01f) {
			r.round(this.x, this.y, this.w, this.h, 7f, Colors.fade(p.hover(), this.hover));
		}
		r.round(this.x + 5f, this.y + 3f, 20f, 20f, 6f, p.accentSoft());
		r.item(this.icon.get(), this.x + 7f, this.y + 5f, 16f);

		float tx = this.x + 32f;
		float maxText = this.w - 32f - ToggleSwitch.WIDTH - 14f;
		r.text(r.ellipsize(this.name, maxText), tx, this.y + 4f, p.text());
		r.text(r.ellipsize(this.description, maxText), tx, this.y + 14f, p.textMuted());
		if (held) {
			Component chip = Component.translatable("clicker.protection.held");
			float cw = r.width(chip) + 8f;
			float cx = this.x + this.w - ToggleSwitch.WIDTH - 12f - cw;
			r.round(cx, this.y + 7f, cw, 12f, 6f, Colors.fade(p.warning(), 0.2f));
			r.text(chip, cx + 4f, this.y + 9.5f, p.warning());
		}
		this.drawChildren(ctx);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		// Clicking anywhere on the row flips the switch.
		if (button == 0 && this.contains(mouseX, mouseY)) {
			return this.toggle.mouseClicked(this.toggle.x() + 1, this.toggle.y() + 1, button);
		}
		return false;
	}
}
