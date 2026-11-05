package clicker.protection;

import clicker.config.ClickerConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

import java.util.function.Supplier;

/** Item groups that pause right-click automation while held. */
public enum ProtectedCategory {
	ELYTRA("elytra", () -> Items.ELYTRA),
	GOLDEN_APPLE("golden_apple", () -> Items.GOLDEN_APPLE),
	ENCHANTED_GOLDEN_APPLE("enchanted_golden_apple", () -> Items.ENCHANTED_GOLDEN_APPLE),
	CHESTPLATES("chestplates", () -> Items.DIAMOND_CHESTPLATE);

	private final String key;
	private final Supplier<Item> iconItem;
	private ItemStack icon;

	ProtectedCategory(String key, Supplier<Item> iconItem) {
		this.key = key;
		this.iconItem = iconItem;
	}

	public String key() {
		return this.key;
	}

	public String translationKey() {
		return "clicker.protection." + this.key;
	}

	public String descriptionKey() {
		return "clicker.protection." + this.key + ".desc";
	}

	/** A representative stack for drawing this category's icon. */
	public ItemStack icon() {
		if (this.icon == null) {
			this.icon = new ItemStack(this.iconItem.get());
		}
		return this.icon;
	}

	public boolean matches(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		return switch (this) {
			// The glider component also covers modded or data-driven elytra.
			case ELYTRA -> stack.is(Items.ELYTRA) || stack.has(DataComponents.GLIDER);
			case GOLDEN_APPLE -> stack.is(Items.GOLDEN_APPLE);
			case ENCHANTED_GOLDEN_APPLE -> stack.is(Items.ENCHANTED_GOLDEN_APPLE);
			// Every vanilla chestplate is in #minecraft:chest_armor (leather, copper, chainmail, iron, gold,
			// diamond, netherite); the equippable check also catches modded chestplates.
			case CHESTPLATES -> stack.is(ItemTags.CHEST_ARMOR) || isChestEquippable(stack);
		};
	}

	private static boolean isChestEquippable(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null
				&& equippable.slot() == EquipmentSlot.CHEST
				&& !stack.is(Items.ELYTRA)
				&& !stack.has(DataComponents.GLIDER);
	}

	public boolean isEnabled(ClickerConfig config) {
		return switch (this) {
			case ELYTRA -> config.protectElytra;
			case GOLDEN_APPLE -> config.protectGoldenApple;
			case ENCHANTED_GOLDEN_APPLE -> config.protectEnchantedGoldenApple;
			case CHESTPLATES -> config.protectChestplates;
		};
	}

	public void setEnabled(ClickerConfig config, boolean enabled) {
		switch (this) {
			case ELYTRA -> config.protectElytra = enabled;
			case GOLDEN_APPLE -> config.protectGoldenApple = enabled;
			case ENCHANTED_GOLDEN_APPLE -> config.protectEnchantedGoldenApple = enabled;
			case CHESTPLATES -> config.protectChestplates = enabled;
		}
	}
}
