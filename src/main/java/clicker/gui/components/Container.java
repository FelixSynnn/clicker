package clicker.gui.components;

import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

/** A widget that owns child widgets and forwards input to them (topmost first). */
public class Container extends Widget {
	protected final List<Widget> children = new ArrayList<>();

	public <T extends Widget> T add(T child) {
		this.children.add(child);
		return child;
	}

	public List<Widget> children() {
		return this.children;
	}

	@Override
	protected void draw(UiContext ctx) {
		this.drawChildren(ctx);
	}

	protected void drawChildren(UiContext ctx) {
		for (Widget child : this.children) {
			child.render(ctx);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.isActive()) {
			return false;
		}
		for (int i = this.children.size() - 1; i >= 0; i--) {
			Widget child = this.children.get(i);
			if (child.isVisible() && child.isActive() && child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = false;
		for (Widget child : this.children) {
			handled |= child.mouseReleased(mouseX, mouseY, button);
		}
		return handled;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		for (Widget child : this.children) {
			if (child.mouseDragged(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!this.visible || !this.isActive()) {
			return false;
		}
		for (int i = this.children.size() - 1; i >= 0; i--) {
			Widget child = this.children.get(i);
			if (child.isVisible() && child.isActive() && child.mouseScrolled(mouseX, mouseY, amount)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		for (Widget child : this.children) {
			if (child.isVisible() && child.keyPressed(event)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean capturesKeyboard() {
		for (Widget child : this.children) {
			if (child.capturesKeyboard()) {
				return true;
			}
		}
		return false;
	}
}
