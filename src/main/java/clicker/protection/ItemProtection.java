package clicker.protection;

import clicker.config.ConfigManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Right-click item protection.
 *
 * <p>It is evaluated at the moment of every generated right click, for both hands, so it reacts instantly to hotbar
 * scrolling, number keys, offhand swaps or items being picked up. Manual clicks are never affected.
 */
public final class ItemProtection {
	private final ConfigManager config;

	public ItemProtection(ConfigManager config) {
		this.config = config;
	}

	/** Returns what currently blocks right-click automation, or null when right clicking is allowed. */
	public @Nullable Match check(@Nullable Player player) {
		if (player == null || !this.config.get().protectionEnabled) {
			return null;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			ProtectedCategory category = this.enabledCategoryOf(stack);
			if (category != null) {
				return new Match(category, stack, hand);
			}
		}
		return null;
	}

	public boolean blocksRightClick(@Nullable Player player) {
		return this.check(player) != null;
	}

	private @Nullable ProtectedCategory enabledCategoryOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (ProtectedCategory category : ProtectedCategory.values()) {
			if (category.isEnabled(this.config.get()) && category.matches(stack)) {
				return category;
			}
		}
		return null;
	}

	/** The category an item belongs to, ignoring whether it is enabled (used for display). */
	public static @Nullable ProtectedCategory classify(ItemStack stack) {
		for (ProtectedCategory category : ProtectedCategory.values()) {
			if (category.matches(stack)) {
				return category;
			}
		}
		return null;
	}

	public record Match(ProtectedCategory category, ItemStack stack, InteractionHand hand) {
	}
}
