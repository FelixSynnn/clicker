package clicker.config;

import clicker.clicking.ActivationMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
	@TempDir
	Path dir;

	private Path file() {
		return this.dir.resolve("clicker.json");
	}

	@Test
	void missingFileCreatesDefaults() {
		ConfigManager manager = new ConfigManager(this.file());
		manager.load();
		assertTrue(Files.exists(this.file()));
		ClickerConfig cfg = manager.get();
		assertEquals(10, cfg.left.cps);
		assertEquals(10, cfg.right.cps);
		assertTrue(cfg.left.enabled);
		assertFalse(cfg.right.enabled);
		assertEquals(50, cfg.left.doubleClickDelayMs);
		assertEquals("sakura", cfg.theme);
		assertTrue(cfg.protectionEnabled);
	}

	@Test
	void everySettingSurvivesARestart() {
		ConfigManager first = new ConfigManager(this.file());
		first.load();
		ClickerConfig cfg = first.get();
		cfg.left.cps = 37;
		cfg.left.mode = ActivationMode.TOGGLE;
		cfg.left.doubleClick = false;
		cfg.left.doubleClickDelayMs = 123;
		cfg.right.enabled = true;
		cfg.right.cps = 100;
		cfg.right.doubleClickDelayMs = 1;
		cfg.doubleClickEnabled = true;
		cfg.protectionEnabled = false;
		cfg.protectChestplates = false;
		cfg.theme = "lavender";
		cfg.guiScale = 3;
		cfg.animations = false;
		cfg.sounds = false;
		cfg.hud = HudMode.ALWAYS;
		cfg.lastPage = "settings";
		cfg.rememberState = true;
		cfg.lastGlobalState = true;
		first.markDirty();
		first.flush();

		ConfigManager second = new ConfigManager(this.file());
		second.load();
		ClickerConfig loaded = second.get();
		assertEquals(37, loaded.left.cps);
		assertEquals(ActivationMode.TOGGLE, loaded.left.mode);
		assertFalse(loaded.left.doubleClick);
		assertEquals(123, loaded.left.doubleClickDelayMs);
		assertTrue(loaded.right.enabled);
		assertEquals(100, loaded.right.cps);
		assertEquals(1, loaded.right.doubleClickDelayMs);
		assertTrue(loaded.doubleClickEnabled);
		assertFalse(loaded.protectionEnabled);
		assertFalse(loaded.protectChestplates);
		assertTrue(loaded.protectElytra);
		assertEquals("lavender", loaded.theme);
		assertEquals(3, loaded.guiScale);
		assertFalse(loaded.animations);
		assertFalse(loaded.sounds);
		assertEquals(HudMode.ALWAYS, loaded.hud);
		assertEquals("settings", loaded.lastPage);
		assertTrue(loaded.rememberState);
		assertTrue(loaded.lastGlobalState);
		assertTrue(second.loadWarnings().isEmpty());
	}

	@Test
	void corruptedFileIsBackedUpAndDefaultsAreUsed() throws IOException {
		Files.writeString(this.file(), "{ this is not json ");
		ConfigManager manager = new ConfigManager(this.file());
		manager.load();
		assertTrue(manager.recoveredFromCorruption());
		assertEquals(10, manager.get().left.cps);
		try (Stream<Path> files = Files.list(this.dir)) {
			assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("clicker.json.corrupt-")),
					"the unreadable file is kept as a backup");
		}
		// The rewritten file is valid again.
		ConfigManager again = new ConfigManager(this.file());
		again.load();
		assertFalse(again.recoveredFromCorruption());
	}

	@Test
	void badValuesOnlyResetThemselves() throws IOException {
		Files.writeString(this.file(), """
				{
				  "leftClick": { "enabled": false, "cps": "fast", "activation": "toggle", "doubleClickDelayMs": 9999 },
				  "rightClick": { "enabled": true, "cps": 250, "activation": "sideways" },
				  "protection": 5,
				  "interface": { "theme": "rose", "hud": "sometimes", "guiScale": -3 }
				}
				""");
		ConfigManager manager = new ConfigManager(this.file());
		manager.load();
		ClickerConfig cfg = manager.get();
		assertFalse(cfg.left.enabled, "valid values next to invalid ones are kept");
		assertEquals(10, cfg.left.cps, "a non-number falls back to the default");
		assertEquals(ActivationMode.TOGGLE, cfg.left.mode);
		assertEquals(500, cfg.left.doubleClickDelayMs, "out-of-range values are clamped");
		assertTrue(cfg.right.enabled);
		assertEquals(100, cfg.right.cps);
		assertEquals(ActivationMode.HOLD, cfg.right.mode, "unknown mode falls back");
		assertTrue(cfg.protectionEnabled, "a malformed section falls back to its defaults");
		assertEquals("rose", cfg.theme);
		assertEquals(HudMode.WHEN_ACTIVE, cfg.hud);
		assertEquals(0, cfg.guiScale);
		assertFalse(manager.loadWarnings().isEmpty());
		assertFalse(manager.recoveredFromCorruption());
	}

	@Test
	void changesAreSavedAfterTheDebounce() throws InterruptedException, IOException {
		ConfigManager manager = new ConfigManager(this.file());
		manager.load();
		manager.get().left.cps = 64;
		manager.markDirty();
		manager.tick();
		assertEquals(ConfigManager.SaveStatus.PENDING, manager.status(), "not written mid-drag");
		Thread.sleep(ConfigManager.SAVE_DEBOUNCE_MILLIS + 100);
		manager.tick();
		assertEquals(ConfigManager.SaveStatus.SAVED, manager.status());
		assertTrue(Files.readString(this.file()).contains("\"cps\": 64"));
	}

	@Test
	void resetRestoresDefaults() {
		ConfigManager manager = new ConfigManager(this.file());
		manager.load();
		manager.get().left.cps = 99;
		manager.get().theme = "midnight";
		manager.resetToDefaults();
		assertEquals(10, manager.get().left.cps);
		assertEquals("sakura", manager.get().theme);
		ConfigManager reloaded = new ConfigManager(this.file());
		reloaded.load();
		assertEquals(10, reloaded.get().left.cps);
	}
}
