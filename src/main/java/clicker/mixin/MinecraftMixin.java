package clicker.mixin;

import clicker.ClickerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the click scheduler once per rendered frame.
 *
 * <p>Minecraft only ticks 20 times per second, which would bunch clicks into groups of five at 100 CPS. Hooking the
 * start of every frame spreads clicks evenly at any frame rate, on the client thread, without extra threads.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void clicker$onFrame(boolean renderLevel, CallbackInfo ci) {
		ClickerClient.onFrame((Minecraft) (Object) this);
	}
}
