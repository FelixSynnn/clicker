package clicker.gui;

import clicker.ClickerClient;
import clicker.clicking.ClickType;
import clicker.clicking.ClickerManager;
import clicker.config.ClickerConfig;
import clicker.gui.components.StatusCard;
import clicker.protection.ItemProtection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Shared wording for live status, used by the GUI pages and the HUD. */
public final class StatusText {
	private StatusText() {
	}

	public static StatusCard.Status side(ClickType type) {
		ClickerManager.Side side = ClickerClient.manager().side(type);
		return switch (side.status()) {
			case CLICKING -> new StatusCard.Status(Component.translatable("clicker.status.clicking"), StatusCard.Kind.ACTIVE);
			case WAITING -> new StatusCard.Status(Component.translatable("clicker.status.ready"), StatusCard.Kind.READY);
			case PROTECTED -> new StatusCard.Status(Component.translatable("clicker.status.paused"), StatusCard.Kind.PAUSED);
			case DISABLED -> new StatusCard.Status(Component.translatable("clicker.status.disabled"), StatusCard.Kind.OFF);
			case OFF -> new StatusCard.Status(Component.translatable("clicker.status.off"), StatusCard.Kind.OFF);
		};
	}

	public static StatusCard.Status global() {
		return ClickerClient.manager().isGlobalEnabled()
				? new StatusCard.Status(Component.translatable("clicker.status.active"), StatusCard.Kind.ACTIVE)
				: new StatusCard.Status(Component.translatable("clicker.status.off"), StatusCard.Kind.OFF);
	}

	public static Component mode(ClickType type) {
		return Component.translatable(ClickerClient.config().get().side(type).mode.translationKey());
	}

	/** e.g. "Hold mode · double 50 ms" */
	public static Component sideDetail(ClickType type) {
		ClickerConfig cfg = ClickerClient.config().get();
		ClickerConfig.SideConfig side = cfg.side(type);
		if (!side.enabled) {
			return Component.translatable("clicker.home.side_disabled");
		}
		MutableComponent text = Component.translatable("clicker.home.mode_line", mode(type));
		if (cfg.doubleClickActive(type)) {
			text = text.append(Component.literal(" · ")).append(Component.translatable("clicker.home.double_line", side.doubleClickDelayMs));
		}
		ClickerManager.Side runtime = ClickerClient.manager().side(type);
		if (runtime.isClicking()) {
			int live = runtime.stats().clicksPerSecond(System.nanoTime());
			text = Component.translatable("clicker.home.live_line", live);
		} else if (runtime.status() == ClickerManager.SideStatus.PROTECTED) {
			ItemProtection.Match match = ClickerClient.manager().protectionMatch();
			if (match != null) {
				text = Component.translatable("clicker.home.holding", match.stack().getHoverName());
			}
		}
		return text;
	}

	public static Component interval(int cps) {
		int ms = Math.round(1000f / Math.max(1, cps));
		return Component.translatable("clicker.clicking.interval", ms);
	}
}
