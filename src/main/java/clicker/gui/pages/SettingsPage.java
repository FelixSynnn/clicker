package clicker.gui.pages;

import clicker.Clicker;
import clicker.ClickerClient;
import clicker.config.ClickerConfig;
import clicker.config.ConfigManager;
import clicker.config.HudMode;
import clicker.gui.ClickerScreen;
import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.components.KeybindButton;
import clicker.gui.components.Label;
import clicker.gui.components.RoundedButton;
import clicker.gui.components.RoundedPanel;
import clicker.gui.components.SegmentedControl;
import clicker.gui.components.ToggleSwitch;
import clicker.gui.components.UiContext;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Interface options, key bindings and configuration status/reset. */
public final class SettingsPage extends PageView {
	private static final List<Integer> SCALES = List.of(0, 1, 2, 3, 4);

	private final InterfacePanel ui;
	private final KeybindPanel keys;
	private final ConfigPanel configPanel;

	public SettingsPage(ClickerScreen screen) {
		super(screen);
		this.ui = this.add(new InterfacePanel());
		this.keys = this.add(new KeybindPanel(screen));
		this.configPanel = this.add(new ConfigPanel(screen));
	}

	@Override
	public void layout(UiRenderer r, float x, float y, float width) {
		boolean twoColumns = width >= 300f;
		float col = twoColumns ? (width - GAP) / 2f : width;
		float uiHeight = this.ui.layoutPanel(x, y, col);
		float bottom;
		if (twoColumns) {
			float keysHeight = this.keys.layoutPanel(x + col + GAP, y, col);
			float tallest = Math.max(uiHeight, keysHeight);
			this.ui.bounds(x, y, col, tallest);
			this.keys.bounds(x + col + GAP, y, col, tallest);
			bottom = y + tallest;
		} else {
			float keysHeight = this.keys.layoutPanel(x, y + uiHeight + GAP, col);
			bottom = y + uiHeight + GAP + keysHeight;
		}
		float configHeight = this.configPanel.layoutPanel(r, x, bottom + GAP, width);
		this.contentHeight = bottom + GAP + configHeight - y;
	}

	/** Lays out "label ...... control" rows. */
	private static float row(Label label, float x, float y, float w, ToggleSwitch toggle) {
		label.bounds(x, y + 2f, w - ToggleSwitch.WIDTH - 6f, 9f);
		toggle.at(x + w - ToggleSwitch.WIDTH, y);
		return y + ToggleSwitch.HEIGHT + 7f;
	}

	private static final class InterfacePanel extends RoundedPanel {
		private final Label scaleLabel;
		private final SegmentedControl<Integer> scale;
		private final Label animationsLabel;
		private final ToggleSwitch animations;
		private final Label soundsLabel;
		private final ToggleSwitch sounds;
		private final Label hudLabel;
		private final SegmentedControl<HudMode> hud;
		private final Label rememberLabel;
		private final ToggleSwitch remember;

		InterfacePanel() {
			this.header(Icon.GEAR, () -> Component.translatable("clicker.settings.interface"), null);
			this.scaleLabel = this.add(new Label(() -> Component.translatable("clicker.settings.gui_scale"), Palette::textSecondary));
			this.scale = this.add(new SegmentedControl<>(SCALES,
					value -> value == 0 ? Component.translatable("clicker.settings.gui_scale.auto") : Component.literal(value + "x"),
					() -> config().guiScale,
					value -> {
						config().guiScale = value;
						changed();
					}));
			this.animationsLabel = this.add(new Label(() -> Component.translatable("clicker.settings.animations"), Palette::text));
			this.animations = this.add(new ToggleSwitch(() -> config().animations, value -> {
				config().animations = value;
				changed();
			}));
			this.soundsLabel = this.add(new Label(() -> Component.translatable("clicker.settings.sounds"), Palette::text));
			this.sounds = this.add(new ToggleSwitch(() -> config().sounds, value -> {
				config().sounds = value;
				changed();
			}));
			this.hudLabel = this.add(new Label(() -> Component.translatable("clicker.settings.hud"), Palette::textSecondary));
			this.hud = this.add(new SegmentedControl<>(List.of(HudMode.values()),
					mode -> Component.translatable(mode.translationKey()),
					() -> config().hud,
					mode -> {
						config().hud = mode;
						changed();
					}));
			this.rememberLabel = this.add(new Label(() -> Component.translatable("clicker.settings.remember"), Palette::text));
			this.remember = this.add(new ToggleSwitch(() -> config().rememberState, value -> {
				ClickerConfig cfg = config();
				cfg.rememberState = value;
				cfg.lastGlobalState = value && manager().isGlobalEnabled();
				changed();
			}));
		}

		float layoutPanel(float x, float y, float w) {
			this.bounds(x, y, w, 0f);
			float cx = this.contentLeft();
			float cw = this.contentWidth();
			float top = this.contentTop();
			this.scaleLabel.bounds(cx, top, cw, 9f);
			top += 12f;
			this.scale.bounds(cx, top, cw, SegmentedControl.HEIGHT);
			top += SegmentedControl.HEIGHT + 9f;
			top = row(this.animationsLabel, cx, top, cw, this.animations);
			top = row(this.soundsLabel, cx, top, cw, this.sounds);
			top = row(this.rememberLabel, cx, top, cw, this.remember);
			top += 1f;
			this.hudLabel.bounds(cx, top, cw, 9f);
			top += 12f;
			this.hud.bounds(cx, top, cw, SegmentedControl.HEIGHT);
			top += SegmentedControl.HEIGHT + PADDING;
			float height = top - y;
			this.bounds(x, y, w, height);
			return height;
		}
	}

	private static final class KeybindPanel extends RoundedPanel {
		private final List<Label> labels = new ArrayList<>();
		private final List<KeybindButton> buttons = new ArrayList<>();
		private final Label hint;
		private final RoundedButton controls;

		KeybindPanel(ClickerScreen screen) {
			this.header(Icon.KEYBOARD, () -> Component.translatable("clicker.settings.keybinds"), null);
			for (KeyMapping mapping : ClickerClient.keybinds().all()) {
				this.labels.add(this.add(new Label(() -> Component.translatable(mapping.getName()), Palette::text)));
				this.buttons.add(this.add(new KeybindButton(mapping)));
			}
			this.hint = this.add(new Label(() -> Component.translatable("clicker.settings.keybinds.hint"), Palette::textMuted).wrap());
			this.controls = this.add(new RoundedButton(() -> Component.translatable("clicker.settings.keybinds.controls"), null,
					RoundedButton.Style.SECONDARY,
					() -> Minecraft.getInstance().setScreen(new KeyBindsScreen(screen, Minecraft.getInstance().options))));
		}

		float layoutPanel(float x, float y, float w) {
			this.bounds(x, y, w, 0f);
			float cx = this.contentLeft();
			float cw = this.contentWidth();
			float top = this.contentTop();
			float buttonW = Math.min(52f, cw * 0.38f);
			for (int i = 0; i < this.buttons.size(); i++) {
				this.labels.get(i).bounds(cx, top + 3.5f, cw - buttonW - 6f, 9f);
				this.buttons.get(i).bounds(cx + cw - buttonW, top, buttonW, KeybindButton.HEIGHT);
				top += KeybindButton.HEIGHT + 5f;
			}
			top += 1f;
			float hintHeight = Minecraft.getInstance().font.split(Component.translatable("clicker.settings.keybinds.hint"), (int) cw).size() * 11f - 2f;
			this.hint.bounds(cx, top, cw, hintHeight);
			top += hintHeight + 7f;
			this.controls.bounds(cx, top, cw, RoundedButton.HEIGHT);
			top += RoundedButton.HEIGHT + PADDING;
			float height = top - y;
			this.bounds(x, y, w, height);
			return height;
		}
	}

	private static final class ConfigPanel extends RoundedPanel {
		private final RoundedButton reset;

		ConfigPanel(ClickerScreen screen) {
			this.header(Icon.INFO, () -> Component.translatable("clicker.settings.config"), ConfigPanel::statusLine);
			this.reset = this.add(new RoundedButton(() -> Component.translatable("clicker.settings.reset"), Icon.RESET,
					RoundedButton.Style.DANGER, screen::confirmReset));
		}

		private static Component statusLine() {
			ConfigManager manager = configManager();
			return switch (manager.status()) {
				case SAVED -> Component.translatable("clicker.settings.config.saved", manager.path().getFileName().toString());
				case PENDING -> Component.translatable("clicker.settings.config.pending");
				case ERROR -> Component.translatable("clicker.settings.config.error", String.valueOf(manager.lastError()));
			};
		}

		float layoutPanel(UiRenderer r, float x, float y, float w) {
			float buttonW = Math.max(84f, this.reset.preferredWidth(r));
			float height = PADDING * 2f + 18f;
			this.bounds(x, y, w, height);
			this.reset.bounds(x + w - PADDING - buttonW, y + (height - RoundedButton.HEIGHT) / 2f, buttonW, RoundedButton.HEIGHT);
			return height;
		}

		@Override
		protected void drawExtras(UiContext ctx) {
			UiRenderer r = ctx.r;
			Palette p = ctx.p;
			ConfigManager manager = configManager();
			int dot = switch (manager.status()) {
				case SAVED -> p.success();
				case PENDING -> p.warning();
				case ERROR -> p.danger();
			};
			// A small status dot on the icon badge.
			r.circle(this.x + PADDING + 17f, this.y + PADDING + 1f, 3f, p.card());
			r.circle(this.x + PADDING + 17f, this.y + PADDING + 1f, 2.1f, dot);
			Component version = Component.translatable("clicker.settings.version", Clicker.version());
			float vx = this.reset.x() - 8f - r.width(version);
			if (vx > this.x + 150f) {
				r.text(version, vx, this.y + this.h / 2f - 4f, Colors.fade(p.textMuted(), 0.9f));
			}
		}
	}
}
