package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.UiSounds;
import clicker.gui.anim.Anim;
import clicker.gui.render.UiRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Shows a key binding and lets you change it in place: click, then press a key (or a side mouse button).
 * Escape cancels, Backspace/Delete unbinds. Changes are saved to Minecraft's options like the Controls screen does.
 */
public final class KeybindButton extends Widget {
	public static final float HEIGHT = 16f;

	/** The button currently recording a key, if any; it receives all input until it finishes. */
	private static KeybindButton recording;

	private final KeyMapping mapping;
	private boolean listening;
	private float listen;

	public KeybindButton(KeyMapping mapping) {
		this.mapping = mapping;
		this.h = HEIGHT;
	}

	public static KeybindButton recording() {
		return recording != null && recording.listening ? recording : null;
	}

	public static void cancelRecording() {
		if (recording != null) {
			recording.listening = false;
			recording = null;
		}
	}

	public boolean isListening() {
		return this.listening;
	}

	public void cancel() {
		this.setListening(false);
	}

	private void setListening(boolean value) {
		this.listening = value;
		if (value) {
			if (recording != null && recording != this) {
				recording.listening = false;
			}
			recording = this;
		} else if (recording == this) {
			recording = null;
		}
	}

	private boolean hasConflict() {
		if (this.mapping.isUnbound()) {
			return false;
		}
		for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
			if (other != this.mapping && other.getCategory() != KeyMapping.Category.DEBUG && other.same(this.mapping)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		this.listen = Anim.approach(this.listen, this.listening ? 1f : 0f, ctx.dt, 16f);
		int bg = Colors.lerp(Colors.lerp(p.track(), p.accentSoft(), this.hover), p.accent(), this.listen);
		r.round(this.x, this.y, this.w, this.h, this.h / 2f, bg);

		Component label;
		int color;
		if (this.listening) {
			label = Component.translatable("clicker.keybind.listening");
			float blink = 0.6f + 0.4f * (float) Math.sin(System.nanoTime() / 1.4e8);
			color = Colors.fade(p.onAccent(), blink);
		} else if (this.mapping.isUnbound()) {
			label = Component.translatable("clicker.keybind.unbound");
			color = p.textMuted();
		} else {
			label = this.mapping.getTranslatedKeyMessage();
			color = this.hasConflict() ? p.warning() : p.accentStrong();
		}
		r.textCentered(r.ellipsize(label, this.w - 8f), this.x + this.w / 2f, this.y + this.h / 2f - 4f, color);
	}

	private void apply(InputConstants.Key key) {
		Minecraft minecraft = Minecraft.getInstance();
		this.mapping.setKey(key);
		KeyMapping.resetMapping();
		minecraft.options.save();
		this.setListening(false);
		UiSounds.select();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.listening) {
			if (button >= 2) {
				this.apply(InputConstants.Type.MOUSE.getOrCreate(button));
			} else {
				this.setListening(false);
			}
			return true;
		}
		if (button == 0 && this.contains(mouseX, mouseY)) {
			this.setListening(true);
			UiSounds.click();
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.listening) {
			return false;
		}
		if (event.isEscape()) {
			this.setListening(false);
		} else if (event.key() == InputConstants.KEY_BACKSPACE || event.key() == InputConstants.KEY_DELETE) {
			this.apply(InputConstants.UNKNOWN);
		} else {
			this.apply(InputConstants.getKey(event));
		}
		return true;
	}

	@Override
	public boolean capturesKeyboard() {
		return this.listening;
	}
}
