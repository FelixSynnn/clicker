package clicker.gui.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** A runtime-generated texture with clamped edges and a chosen filter. */
final class ClickerTexture extends DynamicTexture {
	ClickerTexture(String name, NativeImage image, boolean smooth) {
		super(() -> name, image);
		this.sampler = RenderSystem.getSamplerCache().getClampToEdge(smooth ? FilterMode.LINEAR : FilterMode.NEAREST);
	}
}
