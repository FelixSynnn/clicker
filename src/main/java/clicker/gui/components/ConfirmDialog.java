package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.anim.Anim;
import clicker.gui.anim.Easing;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** A small modal confirmation card that fades and rises into place over the window. */
public final class ConfirmDialog extends Container {
	private static final float WIDTH = 210f;

	private final Component title;
	private final Component message;
	private final RoundedButton cancel;
	private final RoundedButton confirm;
	private boolean open;
	private float progress;
	private float boundsX;
	private float boundsY;
	private float boundsW;
	private float boundsH;

	public ConfirmDialog(Component title, Component message, Component confirmLabel, Runnable onConfirm) {
		this.title = title;
		this.message = message;
		this.cancel = this.add(new RoundedButton(() -> Component.translatable("clicker.dialog.cancel"), null,
				RoundedButton.Style.SECONDARY, this::close));
		this.confirm = this.add(new RoundedButton(() -> confirmLabel, Icon.RESET, RoundedButton.Style.DANGER, () -> {
			onConfirm.run();
			this.close();
		}));
	}

	public void open() {
		this.open = true;
	}

	public void close() {
		this.open = false;
	}

	public boolean isOpen() {
		return this.open;
	}

	/** True while open or still animating closed. */
	public boolean isShowing() {
		return this.open || this.progress > 0.01f;
	}

	/** Positions the dialog over the given area (the window). */
	public void layout(UiRenderer r, float areaX, float areaY, float areaW, float areaH) {
		this.boundsX = areaX;
		this.boundsY = areaY;
		this.boundsW = areaW;
		this.boundsH = areaH;
		float textHeight = r.paragraphHeight(this.message, WIDTH - 28f, 2f);
		float height = 14f + 22f + textHeight + 12f + RoundedButton.HEIGHT + 14f;
		float x = areaX + (areaW - WIDTH) / 2f;
		float y = areaY + (areaH - height) / 2f;
		this.bounds(x, y, WIDTH, height);
		float buttonW = (WIDTH - 28f - 6f) / 2f;
		float by = y + height - 14f - RoundedButton.HEIGHT;
		this.cancel.bounds(x + 14f, by, buttonW, RoundedButton.HEIGHT);
		this.confirm.bounds(x + 14f + buttonW + 6f, by, buttonW, RoundedButton.HEIGHT);
	}

	@Override
	public Widget bounds(float x, float y, float w, float h) {
		return super.bounds(x, y, w, h);
	}

	@Override
	protected void draw(UiContext ctx) {
		this.progress = Anim.approach(this.progress, this.open ? 1f : 0f, ctx.dt, 16f);
		if (this.progress <= 0.01f) {
			return;
		}
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		float t = Easing.outCubic(this.progress);
		r.pushAlpha(this.progress);
		r.round(this.boundsX, this.boundsY, this.boundsW, this.boundsH, 14f, Colors.fade(p.window(), 0.72f));
		r.pushOffset(0f, (1f - t) * 8f);
		r.shadow(this.x, this.y + 3f, this.w, this.h, 11f, 14f, p.shadow());
		r.round(this.x, this.y, this.w, this.h, 11f, p.card());
		r.outline(this.x, this.y, this.w, this.h, 11f, 1, p.cardEdge());
		r.round(this.x + 14f, this.y + 14f, 18f, 18f, 6f, Colors.fade(p.danger(), 0.15f));
		r.icon(Icon.RESET, this.x + 17f, this.y + 17f, 12f, p.danger());
		r.text(this.title, this.x + 38f, this.y + 19f, p.text());
		r.paragraph(this.message, this.x + 14f, this.y + 40f, this.w - 28f, p.textSecondary(), 2f);
		this.drawChildren(ctx);
		r.popOffset();
		r.popAlpha();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.open) {
			return false;
		}
		if (!super.mouseClicked(mouseX, mouseY, button) && !this.contains(mouseX, mouseY)) {
			this.close(); // clicking outside dismisses
		}
		return true; // modal: swallow every click
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.open) {
			return false;
		}
		if (event.isEscape()) {
			this.close();
		}
		return true;
	}
}
