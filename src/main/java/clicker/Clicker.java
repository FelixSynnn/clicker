package clicker;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants for the Clicker mod.
 *
 * <p>Clicker is client-only; the client entrypoint is {@link ClickerClient}.
 */
public final class Clicker {
	public static final String MOD_ID = "clicker";
	public static final String NAME = "Clicker";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private Clicker() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static String version() {
		return FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("dev");
	}
}
