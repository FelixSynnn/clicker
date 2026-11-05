package clicker.config;

import clicker.Clicker;
import clicker.clicking.ActivationMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Loads and saves {@code config/clicker.json}.
 *
 * <ul>
 *     <li>Missing file: defaults are used and a new file is written.</li>
 *     <li>Unreadable/corrupted file: it is kept as {@code clicker.json.corrupt-<time>} and defaults are used.</li>
 *     <li>Individual bad values: only that value falls back to its default; everything else is kept.</li>
 *     <li>Changes are saved automatically a moment after the last edit (so dragging a slider writes once),
 *     and immediately when the GUI closes or the game stops. Writes are atomic.</li>
 * </ul>
 *
 * <p>This class has no Minecraft dependencies, so it can be unit tested on its own.
 */
public final class ConfigManager {
	/** How long after the last change the file is written. */
	public static final long SAVE_DEBOUNCE_MILLIS = 400L;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private final Path path;
	private final ClickerConfig config = new ClickerConfig();
	private final List<String> loadWarnings = new ArrayList<>();
	private final List<Consumer<ClickerConfig>> listeners = new ArrayList<>();

	private boolean dirty;
	private long lastChangeMillis;
	private SaveStatus status = SaveStatus.SAVED;
	private long lastSavedMillis;
	private String lastError;
	private boolean recoveredFromCorruption;

	public ConfigManager(Path path) {
		this.path = path;
	}

	public ClickerConfig get() {
		return this.config;
	}

	public Path path() {
		return this.path;
	}

	public SaveStatus status() {
		return this.status;
	}

	public long lastSavedMillis() {
		return this.lastSavedMillis;
	}

	public String lastError() {
		return this.lastError;
	}

	public boolean recoveredFromCorruption() {
		return this.recoveredFromCorruption;
	}

	public List<String> loadWarnings() {
		return List.copyOf(this.loadWarnings);
	}

	/** Called after every change (and after load/reset) so dependent systems can react. */
	public void addListener(Consumer<ClickerConfig> listener) {
		this.listeners.add(listener);
	}

	// ------------------------------------------------------------------ load

	public void load() {
		this.loadWarnings.clear();
		this.recoveredFromCorruption = false;
		ClickerConfig fresh = new ClickerConfig();

		if (!Files.exists(this.path)) {
			this.config.copyFrom(fresh);
			Clicker.LOGGER.info("No config found, creating {}", this.path);
			this.saveNow();
			this.notifyListeners();
			return;
		}

		JsonObject root;
		try (Reader reader = Files.newBufferedReader(this.path, StandardCharsets.UTF_8)) {
			JsonElement element = JsonParser.parseReader(reader);
			if (!element.isJsonObject()) {
				throw new IllegalStateException("top-level value is not a JSON object");
			}
			root = element.getAsJsonObject();
		} catch (Exception exception) {
			Clicker.LOGGER.warn("Config file {} is unreadable ({}); keeping a backup and using defaults", this.path, exception.toString());
			this.backupCorruptFile();
			this.recoveredFromCorruption = true;
			this.config.copyFrom(fresh);
			this.saveNow();
			this.notifyListeners();
			return;
		}

		readInto(root, fresh, this.loadWarnings);
		fresh.validate();
		this.config.copyFrom(fresh);
		for (String warning : this.loadWarnings) {
			Clicker.LOGGER.warn("Config: {}", warning);
		}
		this.status = SaveStatus.SAVED;
		this.lastSavedMillis = System.currentTimeMillis();
		if (!this.loadWarnings.isEmpty()) {
			// Rewrite the file so the repaired values are persisted.
			this.saveNow();
		}
		this.notifyListeners();
	}

	private void backupCorruptFile() {
		Path backup = this.path.resolveSibling(this.path.getFileName() + ".corrupt-" + LocalDateTime.now().format(BACKUP_TIME));
		try {
			Files.move(this.path, backup, StandardCopyOption.REPLACE_EXISTING);
			Clicker.LOGGER.warn("Saved the unreadable config as {}", backup);
		} catch (IOException exception) {
			Clicker.LOGGER.error("Could not back up the unreadable config", exception);
		}
	}

	static void readInto(JsonObject root, ClickerConfig target, List<String> warnings) {
		FieldReader r = new FieldReader(warnings);
		JsonObject left = r.object(root, "leftClick");
		JsonObject right = r.object(root, "rightClick");
		readSide(r, left, "leftClick", target.left);
		readSide(r, right, "rightClick", target.right);

		JsonObject doubleClick = r.object(root, "doubleClick");
		target.doubleClickEnabled = r.bool(doubleClick, "doubleClick.enabled", "enabled", target.doubleClickEnabled);

		JsonObject protection = r.object(root, "protection");
		target.protectionEnabled = r.bool(protection, "protection.enabled", "enabled", target.protectionEnabled);
		target.protectElytra = r.bool(protection, "protection.elytra", "elytra", target.protectElytra);
		target.protectGoldenApple = r.bool(protection, "protection.goldenApple", "goldenApple", target.protectGoldenApple);
		target.protectEnchantedGoldenApple = r.bool(protection, "protection.enchantedGoldenApple", "enchantedGoldenApple", target.protectEnchantedGoldenApple);
		target.protectChestplates = r.bool(protection, "protection.chestplates", "chestplates", target.protectChestplates);

		JsonObject ui = r.object(root, "interface");
		target.theme = r.string(ui, "interface.theme", "theme", target.theme);
		target.guiScale = r.integer(ui, "interface.guiScale", "guiScale", target.guiScale, 0, ClickerConfig.MAX_GUI_SCALE);
		target.animations = r.bool(ui, "interface.animations", "animations", target.animations);
		target.sounds = r.bool(ui, "interface.sounds", "sounds", target.sounds);
		String hud = r.string(ui, "interface.hud", "hud", target.hud.key());
		HudMode hudMode = HudMode.fromKey(hud, null);
		if (hudMode == null) {
			warnings.add("interface.hud: unknown value '" + hud + "', using default");
			hudMode = HudMode.WHEN_ACTIVE;
		}
		target.hud = hudMode;
		target.lastPage = r.string(ui, "interface.lastPage", "lastPage", target.lastPage);

		JsonObject state = r.object(root, "state");
		target.rememberState = r.bool(state, "state.remember", "remember", target.rememberState);
		target.lastGlobalState = r.bool(state, "state.enabled", "enabled", target.lastGlobalState);
	}

	private static void readSide(FieldReader r, JsonObject json, String name, ClickerConfig.SideConfig side) {
		side.enabled = r.bool(json, name + ".enabled", "enabled", side.enabled);
		side.cps = r.integer(json, name + ".cps", "cps", side.cps, ClickerConfig.MIN_CPS, ClickerConfig.MAX_CPS);
		String mode = r.string(json, name + ".activation", "activation", side.mode.key());
		ActivationMode parsed = ActivationMode.fromKey(mode, null);
		if (parsed == null) {
			r.warnings.add(name + ".activation: unknown value '" + mode + "', using default");
			parsed = side.mode;
		}
		side.mode = parsed;
		side.doubleClick = r.bool(json, name + ".doubleClick", "doubleClick", side.doubleClick);
		side.doubleClickDelayMs = r.integer(json, name + ".doubleClickDelayMs", "doubleClickDelayMs", side.doubleClickDelayMs,
				ClickerConfig.MIN_DOUBLE_CLICK_DELAY_MS, ClickerConfig.MAX_DOUBLE_CLICK_DELAY_MS);
	}

	// ------------------------------------------------------------------ save

	/** Marks the config as changed; it is written shortly afterwards by {@link #tick()}. */
	public void markDirty() {
		this.dirty = true;
		this.lastChangeMillis = System.currentTimeMillis();
		this.status = SaveStatus.PENDING;
		this.notifyListeners();
	}

	/** Writes pending changes once the debounce delay has passed. Call this regularly (every client tick). */
	public void tick() {
		if (this.dirty && System.currentTimeMillis() - this.lastChangeMillis >= SAVE_DEBOUNCE_MILLIS) {
			this.saveNow();
		}
	}

	/** Writes pending changes immediately, if there are any. */
	public void flush() {
		if (this.dirty) {
			this.saveNow();
		}
	}

	/** Writes the file now. Returns true on success. */
	public boolean saveNow() {
		this.config.validate();
		JsonObject root = toJson(this.config);
		Path temp = this.path.resolveSibling(this.path.getFileName() + ".tmp");
		try {
			Files.createDirectories(this.path.getParent());
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
			try {
				Files.move(temp, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temp, this.path, StandardCopyOption.REPLACE_EXISTING);
			}
			this.dirty = false;
			this.status = SaveStatus.SAVED;
			this.lastSavedMillis = System.currentTimeMillis();
			this.lastError = null;
			return true;
		} catch (IOException exception) {
			this.status = SaveStatus.ERROR;
			this.lastError = exception.getMessage();
			Clicker.LOGGER.error("Could not save the Clicker config to {}", this.path, exception);
			return false;
		}
	}

	/** Restores every setting to its default and saves. Keybinds are untouched (they live in options.txt). */
	public void resetToDefaults() {
		this.config.copyFrom(new ClickerConfig());
		this.saveNow();
		this.notifyListeners();
	}

	static JsonObject toJson(ClickerConfig config) {
		JsonObject root = new JsonObject();
		root.addProperty("configVersion", ClickerConfig.CONFIG_VERSION);
		root.add("leftClick", sideToJson(config.left));
		root.add("rightClick", sideToJson(config.right));

		JsonObject doubleClick = new JsonObject();
		doubleClick.addProperty("enabled", config.doubleClickEnabled);
		root.add("doubleClick", doubleClick);

		JsonObject protection = new JsonObject();
		protection.addProperty("enabled", config.protectionEnabled);
		protection.addProperty("elytra", config.protectElytra);
		protection.addProperty("goldenApple", config.protectGoldenApple);
		protection.addProperty("enchantedGoldenApple", config.protectEnchantedGoldenApple);
		protection.addProperty("chestplates", config.protectChestplates);
		root.add("protection", protection);

		JsonObject ui = new JsonObject();
		ui.addProperty("theme", config.theme);
		ui.addProperty("guiScale", config.guiScale);
		ui.addProperty("animations", config.animations);
		ui.addProperty("sounds", config.sounds);
		ui.addProperty("hud", config.hud.key());
		ui.addProperty("lastPage", config.lastPage);
		root.add("interface", ui);

		JsonObject state = new JsonObject();
		state.addProperty("remember", config.rememberState);
		state.addProperty("enabled", config.lastGlobalState);
		root.add("state", state);
		return root;
	}

	private static JsonObject sideToJson(ClickerConfig.SideConfig side) {
		JsonObject json = new JsonObject();
		json.addProperty("enabled", side.enabled);
		json.addProperty("cps", side.cps);
		json.addProperty("activation", side.mode.key());
		json.addProperty("doubleClick", side.doubleClick);
		json.addProperty("doubleClickDelayMs", side.doubleClickDelayMs);
		return json;
	}

	private void notifyListeners() {
		for (Consumer<ClickerConfig> listener : this.listeners) {
			listener.accept(this.config);
		}
	}

	public enum SaveStatus {
		SAVED,
		PENDING,
		ERROR
	}

	/** Type-checked field access that records a warning and keeps the default for any bad value. */
	private static final class FieldReader {
		final List<String> warnings;

		FieldReader(List<String> warnings) {
			this.warnings = warnings;
		}

		JsonObject object(JsonObject parent, String key) {
			JsonElement element = parent.get(key);
			if (element == null) {
				return new JsonObject();
			}
			if (!element.isJsonObject()) {
				this.warnings.add(key + ": expected an object, using defaults for this section");
				return new JsonObject();
			}
			return element.getAsJsonObject();
		}

		boolean bool(JsonObject json, String name, String key, boolean fallback) {
			JsonPrimitive primitive = this.primitive(json, name, key);
			if (primitive == null) {
				return fallback;
			}
			if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			}
			this.warnings.add(name + ": expected true/false, using default");
			return fallback;
		}

		int integer(JsonObject json, String name, String key, int fallback, int min, int max) {
			JsonPrimitive primitive = this.primitive(json, name, key);
			if (primitive == null) {
				return fallback;
			}
			if (primitive.isNumber()) {
				double value = primitive.getAsDouble();
				if (!Double.isFinite(value)) {
					this.warnings.add(name + ": not a finite number, using default");
					return fallback;
				}
				long rounded = Math.round(value);
				if (rounded < min || rounded > max) {
					this.warnings.add(name + ": " + rounded + " is outside " + min + "-" + max + ", clamped");
				}
				return (int) Math.max(min, Math.min(max, rounded));
			}
			this.warnings.add(name + ": expected a number, using default");
			return fallback;
		}

		String string(JsonObject json, String name, String key, String fallback) {
			JsonPrimitive primitive = this.primitive(json, name, key);
			if (primitive == null) {
				return fallback;
			}
			if (primitive.isString()) {
				return primitive.getAsString();
			}
			this.warnings.add(name + ": expected text, using default");
			return fallback;
		}

		private JsonPrimitive primitive(JsonObject json, String name, String key) {
			JsonElement element = json.get(key);
			if (element == null || element.isJsonNull()) {
				return null;
			}
			if (!element.isJsonPrimitive()) {
				this.warnings.add(name + ": unexpected value type, using default");
				return null;
			}
			return element.getAsJsonPrimitive();
		}
	}
}
