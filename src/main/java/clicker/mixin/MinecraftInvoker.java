package clicker.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's own attack and use-item routines.
 *
 * <p>Generated clicks call these directly instead of faking mouse events, so they never feed back into the
 * physical input state (no recursion, no duplicated key presses) and they behave exactly like a real click:
 * attack cooldown, miss penalty, reach and item-use rules all stay vanilla.
 */
@Mixin(Minecraft.class)
public interface MinecraftInvoker {
	@Invoker("startAttack")
	boolean clicker$startAttack();

	@Invoker("startUseItem")
	void clicker$startUseItem();
}
