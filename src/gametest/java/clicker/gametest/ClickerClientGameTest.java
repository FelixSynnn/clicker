package clicker.gametest;

import clicker.ClickerClient;
import clicker.clicking.ActivationMode;
import clicker.clicking.ClickStats;
import clicker.clicking.ClickType;
import clicker.clicking.ClickerManager;
import clicker.config.ClickerConfig;
import clicker.config.ConfigManager;
import clicker.gui.ClickerScreen;
import clicker.gui.ClickerTheme;
import clicker.gui.Page;
import clicker.gui.components.CustomSlider;
import clicker.gui.components.SegmentedControl;
import clicker.gui.components.ToggleSwitch;
import clicker.gui.components.Widget;
import clicker.protection.ProtectedCategory;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * End-to-end verification inside a real Minecraft 1.21.11 client.
 *
 * <p>Every check drives the game the way a player would (simulated keyboard and mouse through vanilla's own input
 * handlers) and asserts on real game state. A report is written to {@code clicker-gametest-report.txt} and
 * screenshots to {@code screenshots/} in the test run directory.
 */
public final class ClickerClientGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("Clicker GameTest");

	private final List<String> report = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> {
			mc.options.framerateLimit().set(260);
			ClickerClient.config().resetToDefaults();
			ClickerClient.manager().setGlobalEnabled(false);
		});
		// A common real-world resolution; Clicker's Auto scale picks 3x here.
		ctx.getInput().resizeWindow(1920, 1080);
		ctx.waitTicks(5);

		this.testModMenu(ctx);

		try (TestSingleplayerContext sp = ctx.worldBuilder().setUseConsistentSettings(true).create()) {
			sp.getClientWorld().waitForChunksRender();
			this.setupArena(ctx, sp);

			this.testGuiOpensWithH(ctx);
			this.testGuiPagesAndThemes(ctx);
			this.testGuiMouseInteraction(ctx);
			this.testGuiClosesWithKeys(ctx);
			this.testGlobalToggleWithG(ctx);
			this.testToggleModeRate(ctx);
			this.testLeftClicksReachTheGame(ctx, sp);
			this.testHoldMode(ctx);
			this.testScreensPauseClicking(ctx);
			this.testDoubleClick(ctx);
			this.testProtection(ctx, sp);
			this.testHud(ctx);

			ctx.runOnClient(mc -> ClickerClient.manager().setGlobalEnabled(true));
		}
		this.testDisconnectSwitchesOff(ctx);
		this.testConfigPersistence(ctx);
		this.writeReport();
	}

	// ------------------------------------------------------------------ helpers

	private void pass(String message) {
		this.report.add("PASS  " + message);
		LOG.info("PASS  {}", message);
	}

	private void check(boolean condition, String message) {
		if (!condition) {
			this.report.add("FAIL  " + message);
			this.writeReport();
			throw new AssertionError(message);
		}
		this.pass(message);
	}

	private void writeReport() {
		try {
			Path file = FabricLoader.getInstance().getGameDir().resolve("clicker-gametest-report.txt");
			Files.write(file, this.report);
		} catch (IOException e) {
			LOG.error("Could not write the report", e);
		}
	}

	private void configure(ClientGameTestContext ctx, Consumer<ClickerConfig> change) {
		ctx.runOnClient(mc -> {
			change.accept(ClickerClient.config().get());
			ClickerClient.config().markDirty();
		});
		ctx.waitTick();
	}

	private void setGlobal(ClientGameTestContext ctx, boolean on) {
		ctx.runOnClient(mc -> ClickerClient.manager().setGlobalEnabled(on));
	}

	private record Sample(long primary, long secondary, double seconds) {
		long total() {
			return this.primary + this.secondary;
		}

		double cps() {
			return this.total() / this.seconds;
		}
	}

	private Sample measure(ClientGameTestContext ctx, ClickType type, int ticks) {
		long[] before = this.counts(ctx, type);
		long start = System.nanoTime();
		ctx.waitTicks(ticks);
		long end = System.nanoTime();
		long[] after = this.counts(ctx, type);
		return new Sample(after[0] - before[0], after[1] - before[1], (end - start) / 1e9);
	}

	private long[] counts(ClientGameTestContext ctx, ClickType type) {
		return ctx.computeOnClient(mc -> {
			ClickStats stats = ClickerClient.manager().side(type).stats();
			return new long[]{stats.primaryClicks(), stats.secondaryClicks()};
		});
	}

	private ClickerManager.SideStatus status(ClientGameTestContext ctx, ClickType type) {
		return ctx.computeOnClient(mc -> ClickerClient.manager().side(type).status());
	}

	private static ServerPlayer serverPlayer(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private void aim(ClientGameTestContext ctx, TestSingleplayerContext sp, float yaw, float pitch) {
		sp.getServer().runCommand(String.format(Locale.ROOT, "execute as @p at @s run tp @s ~ ~ ~ %.1f %.1f", yaw, pitch));
		ctx.waitTicks(4);
	}

	private ClickerScreen screen(ClientGameTestContext ctx) {
		return ctx.computeOnClient(mc -> (ClickerScreen) mc.screen);
	}

	private void waitSettled(ClientGameTestContext ctx) {
		ctx.waitFor(mc -> mc.screen instanceof ClickerScreen s && s.isSettled(), 100);
		ctx.waitTicks(2);
	}

	private void clickAt(ClientGameTestContext ctx, double[] cursor) {
		ctx.getInput().setCursorPos(cursor[0], cursor[1]);
		ctx.waitTick();
		ctx.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		ctx.waitTick();
	}

	private <T extends Widget> T findWidget(ClientGameTestContext ctx, Page page, Class<T> type, int index) {
		return ctx.computeOnClient(mc -> {
			ClickerScreen screen = (ClickerScreen) mc.screen;
			int seen = 0;
			for (Widget widget : screen.pageView(page).allWidgets()) {
				if (type.isInstance(widget)) {
					if (seen++ == index) {
						return type.cast(widget);
					}
				}
			}
			throw new AssertionError("No " + type.getSimpleName() + " #" + index + " on " + page);
		});
	}

	private double[] cursorOf(ClientGameTestContext ctx, Widget widget, float fx, float fy) {
		return ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).toCursor(
				widget.x() + widget.width() * fx, widget.y() + widget.height() * fy, true));
	}

	// ------------------------------------------------------------------ tests

	private void testModMenu(ClientGameTestContext ctx) {
		Screen fromModMenu = ctx.computeOnClient(mc -> {
			for (EntrypointContainer<Object> container : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class)) {
				if (!container.getProvider().getMetadata().getId().equals("clicker")) {
					continue;
				}
				try {
					Object api = container.getEntrypoint();
					Object factory = api.getClass().getMethod("getModConfigScreenFactory").invoke(api);
					Method create = Class.forName("com.terraformersmc.modmenu.api.ConfigScreenFactory").getMethod("create", Screen.class);
					return (Screen) create.invoke(factory, mc.screen);
				} catch (ReflectiveOperationException e) {
					throw new AssertionError("Mod Menu factory failed", e);
				}
			}
			return null;
		});
		this.check(fromModMenu instanceof ClickerScreen, "Mod Menu's Configure button opens the Clicker GUI");
		this.check(ctx.computeOnClient(mc -> FabricLoader.getInstance().getModContainer("clicker")
				.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("")).equals("1.0.0"),
				"Mod metadata (name, version 1.0.0, description, icon) is available to Mod Menu");

		Screen modsScreen = ctx.computeOnClient(mc -> {
			try {
				return (Screen) Class.forName("com.terraformersmc.modmenu.api.ModMenuApi").getMethod("createModsScreen", Screen.class).invoke(null, mc.screen);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("Could not open Mod Menu", e);
			}
		});
		ctx.setScreen(() -> modsScreen);
		ctx.waitTicks(10);
		ctx.getInput().typeChars("Clicker");
		ctx.waitTicks(10);
		ctx.takeScreenshot("modmenu_search");
		ctx.setScreen(() -> fromModMenu);
		ctx.waitTicks(15);
		ctx.takeScreenshot("modmenu_opens_clicker_gui");
		ctx.setScreen(() -> null);
		ctx.waitTicks(2);
	}

	private void setupArena(ClientGameTestContext ctx, TestSingleplayerContext sp) {
		sp.getServer().runCommand("gamemode survival @a");
		sp.getServer().runCommand("difficulty peaceful");
		sp.getServer().runCommand("time set noon");
		sp.getServer().runCommand("execute at @p run fill ~-5 ~ ~-5 ~5 ~5 ~8 air");
		sp.getServer().runCommand("execute at @p run fill ~-5 ~-1 ~-5 ~5 ~-1 ~8 smooth_quartz");
		this.aim(ctx, sp, 0f, 0f);
		ctx.waitTicks(10);
		this.check(ctx.computeOnClient(mc -> mc.player != null && mc.screen == null), "Joined a singleplayer world");
	}

	private void testGuiOpensWithH(ClientGameTestContext ctx) {
		ctx.getInput().pressKey(GLFW.GLFW_KEY_H);
		ctx.waitForScreen(ClickerScreen.class);
		this.waitSettled(ctx);
		this.check(true, "Pressing H in-game opens the Clicker GUI");
		this.check(!ctx.computeOnClient(Minecraft::isPaused), "The GUI does not pause the game");
		this.check(ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).page()) == Page.HOME, "The GUI opens on the Home page by default");
	}

	private void testGuiPagesAndThemes(ClientGameTestContext ctx) {
		ctx.takeScreenshot("gui_home_sakura");
		int[] keys = {GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6};
		Page[] expected = {Page.CLICKING, Page.DOUBLE_CLICK, Page.PROTECTION, Page.THEMES, Page.SETTINGS};
		for (int i = 0; i < keys.length; i++) {
			ctx.getInput().pressKey(keys[i]);
			this.waitSettled(ctx);
			Page shown = ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).page());
			this.check(shown == expected[i], "Number key " + (i + 2) + " shows the " + expected[i] + " page");
			ctx.takeScreenshot("gui_page_" + expected[i].id());
		}

		// Sidebar navigation with a real mouse click.
		float[] bounds = this.screen(ctx).windowBounds();
		float itemX = bounds[0] + 6f + (bounds[4] - 6f) / 2f;
		float itemY = bounds[1] + 6f + 44f + Page.HOME.ordinal() * 24f + 11f;
		double[] cursor = ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).toCursor(itemX, itemY, false));
		this.clickAt(ctx, cursor);
		this.waitSettled(ctx);
		this.check(ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).page()) == Page.HOME, "Clicking 'Home' in the sidebar navigates there");

		for (ClickerTheme theme : ClickerTheme.PRESETS) {
			ctx.runOnClient(mc -> ClickerClient.themes().select(theme));
			ctx.waitTicks(14);
			this.check(ctx.computeOnClient(mc -> ClickerClient.themes().current() == theme
					&& ClickerClient.config().get().theme.equals(theme.id())), "Theme preset '" + theme.id() + "' applies and is stored");
			ctx.takeScreenshot("theme_" + theme.id());
		}
		ctx.getInput().pressKey(GLFW.GLFW_KEY_5);
		this.waitSettled(ctx);
		ctx.takeScreenshot("gui_page_themes_midnight");
		ctx.runOnClient(mc -> ClickerClient.themes().select(ClickerTheme.SAKURA));
		ctx.waitTicks(14);

		// Small windows: the layout falls back to one column and scrolls.
		ctx.getInput().resizeWindow(854, 480);
		ctx.waitTicks(5);
		ctx.getInput().pressKey(GLFW.GLFW_KEY_2);
		this.waitSettled(ctx);
		int smallScale = ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).uiScale());
		ctx.takeScreenshot("gui_small_window_clicking");
		this.check(smallScale >= 1, "The GUI adapts to an 854x480 window (scale " + smallScale + "x)");
		ctx.getInput().resizeWindow(1920, 1080);
		ctx.waitTicks(5);
		int bigScale = ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).uiScale());
		this.check(bigScale == 3, "Auto scale picks 3x at 1920x1080 (got " + bigScale + "x)");
		ctx.getInput().pressKey(GLFW.GLFW_KEY_1);
		this.waitSettled(ctx);
	}

	private void testGuiMouseInteraction(ClientGameTestContext ctx) {
		ctx.getInput().pressKey(GLFW.GLFW_KEY_2);
		this.waitSettled(ctx);

		// Widgets on the Clicking page, in drawing order: left card first, then right card.
		ToggleSwitch rightEnabled = this.findWidget(ctx, Page.CLICKING, ToggleSwitch.class, 1);
		boolean before = ctx.computeOnClient(mc -> ClickerClient.config().get().right.enabled);
		this.clickAt(ctx, this.cursorOf(ctx, rightEnabled, 0.5f, 0.5f));
		boolean after = ctx.computeOnClient(mc -> ClickerClient.config().get().right.enabled);
		this.check(before != after, "Clicking the right-click toggle switch flips 'Right Click enabled'");

		CustomSlider leftCps = this.findWidget(ctx, Page.CLICKING, CustomSlider.class, 0);
		double[] start = this.cursorOf(ctx, leftCps, 0.25f, 0.5f);
		double[] end = this.cursorOf(ctx, leftCps, 0.75f, 0.5f);
		ctx.getInput().setCursorPos(start[0], start[1]);
		ctx.waitTick();
		ctx.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		ctx.waitTick();
		int atStart = ctx.computeOnClient(mc -> ClickerClient.config().get().left.cps);
		for (int i = 1; i <= 5; i++) {
			double t = i / 5.0;
			ctx.getInput().setCursorPos(start[0] + (end[0] - start[0]) * t, start[1]);
			ctx.waitTick();
		}
		ctx.takeScreenshot("gui_slider_drag");
		ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		ctx.waitTick();
		int atEnd = ctx.computeOnClient(mc -> ClickerClient.config().get().left.cps);
		this.check(atStart >= 20 && atStart <= 32, "Pressing the CPS slider at 25% sets about 26 CPS (got " + atStart + ")");
		this.check(atEnd >= 70 && atEnd <= 80, "Dragging the CPS slider to 75% sets about 75 CPS (got " + atEnd + ")");

		@SuppressWarnings("unchecked")
		SegmentedControl<ActivationMode> leftMode = this.findWidget(ctx, Page.CLICKING, SegmentedControl.class, 0);
		this.clickAt(ctx, this.cursorOf(ctx, leftMode, 0.25f, 0.5f));
		this.check(ctx.computeOnClient(mc -> ClickerClient.config().get().left.mode) == ActivationMode.TOGGLE,
				"Clicking 'Toggle' in the activation selector switches left click to Toggle mode");
		this.clickAt(ctx, this.cursorOf(ctx, leftMode, 0.75f, 0.5f));
		this.check(ctx.computeOnClient(mc -> ClickerClient.config().get().left.mode) == ActivationMode.HOLD,
				"Clicking 'Hold' switches it back to Hold mode");

		// Header pill toggles Clicker.
		ClickerScreen screen = this.screen(ctx);
		float[] b = screen.windowBounds();
		double[] pill = ctx.computeOnClient(mc -> ((ClickerScreen) mc.screen).toCursor(b[0] + b[2] - 12f - 10f, b[1] + 12f + 9f, false));
		this.clickAt(ctx, pill);
		this.check(ctx.computeOnClient(mc -> ClickerClient.manager().isGlobalEnabled()), "Clicking the header status pill switches Clicker on");
		this.check(this.status(ctx, ClickType.LEFT) == ClickerManager.SideStatus.WAITING, "Nothing is clicked while the GUI is open");
		ctx.takeScreenshot("gui_clicking_active");
		this.clickAt(ctx, pill);
		this.check(!ctx.computeOnClient(mc -> ClickerClient.manager().isGlobalEnabled()), "Clicking the pill again switches it off");
	}

	private void testGuiClosesWithKeys(ClientGameTestContext ctx) {
		ctx.getInput().pressKey(GLFW.GLFW_KEY_H);
		ctx.waitForScreen(null);
		this.check(true, "Pressing H again closes the GUI (with its closing animation)");
		ctx.getInput().pressKey(GLFW.GLFW_KEY_H);
		ctx.waitForScreen(ClickerScreen.class);
		this.waitSettled(ctx);
		ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		ctx.waitForScreen(null);
		this.check(true, "Esc closes the GUI");
		this.check(ctx.computeOnClient(mc -> ClickerClient.config().status() == ConfigManager.SaveStatus.SAVED),
				"Closing the GUI saves the configuration");
	}

	private void testGlobalToggleWithG(ClientGameTestContext ctx) {
		this.check(!ctx.computeOnClient(mc -> ClickerClient.manager().isGlobalEnabled()), "Clicker starts switched off");
		ctx.getInput().pressKey(GLFW.GLFW_KEY_G);
		ctx.waitTicks(2);
		this.check(ctx.computeOnClient(mc -> ClickerClient.manager().isGlobalEnabled()), "Pressing G switches Clicker on");
		ctx.takeScreenshot("g_pressed_actionbar_and_hud");
		ctx.getInput().pressKey(GLFW.GLFW_KEY_G);
		ctx.waitTicks(2);
		this.check(!ctx.computeOnClient(mc -> ClickerClient.manager().isGlobalEnabled()), "Pressing G again switches Clicker off");
	}

	private void testToggleModeRate(ClientGameTestContext ctx, int cps, int ticks) {
		this.configure(ctx, c -> {
			c.left.enabled = true;
			c.left.mode = ActivationMode.TOGGLE;
			c.left.cps = cps;
			c.right.enabled = false;
			c.doubleClickEnabled = false;
		});
		this.setGlobal(ctx, true);
		ctx.waitTicks(3);
		Sample sample = this.measure(ctx, ClickType.LEFT, ticks);
		this.setGlobal(ctx, false);
		double measured = sample.cps();
		this.check(Math.abs(measured - cps) <= Math.max(2.0, cps * 0.2),
				String.format(Locale.ROOT, "Toggle mode at %d CPS measured %.1f CPS in-game (%d clicks in %.2f s)", cps, measured, sample.total(), sample.seconds));
	}

	private void testToggleModeRate(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> LOG.info("Client FPS during tests: {}", mc.getFps()));
		// Aim at the sky so the measurement is not affected by blocks or entities.
		this.testToggleModeRate(ctx, 1, 80);
		this.testToggleModeRate(ctx, 10, 60);
		this.testToggleModeRate(ctx, 50, 60);
		this.testToggleModeRate(ctx, 100, 60);
		Sample off = this.measure(ctx, ClickType.LEFT, 20);
		this.check(off.total() == 0, "No clicks at all while Clicker is switched off");
		this.check(this.status(ctx, ClickType.LEFT) == ClickerManager.SideStatus.OFF, "Left side reports OFF when Clicker is off");
	}

	private void testLeftClicksReachTheGame(ClientGameTestContext ctx, TestSingleplayerContext sp) {
		this.aim(ctx, sp, 0f, 0f);
		sp.getServer().runCommand("execute at @p run summon minecraft:armor_stand ~ ~ ~2.5");
		ctx.waitTicks(6);
		int stands = sp.getServer().computeOnServer(server -> server.overworld().getEntities(EntityType.ARMOR_STAND, e -> true).size());
		this.check(stands == 1, "An armor stand stands in front of the player");
		this.configure(ctx, c -> {
			c.left.enabled = true;
			c.left.mode = ActivationMode.TOGGLE;
			c.left.cps = 20;
		});
		this.setGlobal(ctx, true);
		ctx.waitTicks(20);
		this.setGlobal(ctx, false);
		int left = sp.getServer().computeOnServer(server -> server.overworld().getEntities(EntityType.ARMOR_STAND, e -> true).size());
		this.check(left == 0, "Generated left clicks hit the armor stand twice within 5 ticks and broke it (survival)");
	}

	private void testHoldMode(ClientGameTestContext ctx) {
		this.configure(ctx, c -> {
			c.left.enabled = true;
			c.left.mode = ActivationMode.HOLD;
			c.left.cps = 20;
		});
		this.setGlobal(ctx, true);
		ctx.waitTicks(3);
		Sample idle = this.measure(ctx, ClickType.LEFT, 20);
		this.check(idle.total() == 0, "Hold mode: no clicks while the attack button is not held");
		this.check(this.status(ctx, ClickType.LEFT) == ClickerManager.SideStatus.WAITING, "Hold mode reports 'Ready' while waiting for the button");

		ctx.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		ctx.waitTicks(3);
		this.check(ctx.computeOnClient(mc -> mc.options.keyAttack.isDown()), "Holding the physical left button is seen through the attack key mapping");
		Sample held = this.measure(ctx, ClickType.LEFT, 60);
		this.check(Math.abs(held.cps() - 20) <= 4,
				String.format(Locale.ROOT, "Holding left click keeps clicking at 20 CPS (measured %.1f)", held.cps()));
		ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		ctx.waitTicks(3);
		Sample released = this.measure(ctx, ClickType.LEFT, 20);
		this.check(released.total() == 0, "Releasing the button stops clicking immediately");
		this.setGlobal(ctx, false);
	}

	private void testScreensPauseClicking(ClientGameTestContext ctx) {
		this.configure(ctx, c -> {
			c.left.enabled = true;
			c.left.mode = ActivationMode.TOGGLE;
			c.left.cps = 20;
		});
		this.setGlobal(ctx, true);
		ctx.waitTicks(3);
		ctx.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		ctx.waitTicks(3);
		Sample inInventory = this.measure(ctx, ClickType.LEFT, 20);
		this.check(inInventory.total() == 0, "No clicks while the inventory is open");
		ctx.setScreen(() -> new PauseScreen(true));
		ctx.waitTicks(3);
		Sample paused = this.measure(ctx, ClickType.LEFT, 20);
		this.check(paused.total() == 0, "No clicks while the game is paused");
		ctx.setScreen(() -> null);
		ctx.waitTicks(3);
		Sample resumed = this.measure(ctx, ClickType.LEFT, 20);
		this.check(resumed.total() > 10, "Clicking resumes after the screen closes (" + resumed.total() + " clicks)");
		this.setGlobal(ctx, false);
	}

	private void testDoubleClick(ClientGameTestContext ctx) {
		this.configure(ctx, c -> {
			c.left.enabled = true;
			c.left.mode = ActivationMode.TOGGLE;
			c.left.cps = 5;
			c.left.doubleClick = true;
			c.left.doubleClickDelayMs = 50;
			c.doubleClickEnabled = true;
		});
		this.setGlobal(ctx, true);
		ctx.waitTicks(3);
		Sample pairs = this.measure(ctx, ClickType.LEFT, 60);
		this.check(pairs.primary() >= 12 && pairs.primary() <= 18, "Double click: about 15 first clicks in 3 s at 5 CPS (got " + pairs.primary() + ")");
		this.check(Math.abs(pairs.secondary() - pairs.primary()) <= 1,
				"Double click: every click gets a second click (" + pairs.primary() + " first, " + pairs.secondary() + " second)");

		this.configure(ctx, c -> c.left.doubleClickDelayMs = 300);
		ctx.waitTicks(10);
		Sample longDelay = this.measure(ctx, ClickType.LEFT, 60);
		this.check(Math.abs(longDelay.secondary() - longDelay.primary()) <= 2,
				"Double click with a 300 ms delay (longer than the 200 ms interval) still pairs every click ("
						+ longDelay.primary() + "/" + longDelay.secondary() + ")");

		this.configure(ctx, c -> c.doubleClickEnabled = false);
		ctx.waitTicks(10);
		Sample single = this.measure(ctx, ClickType.LEFT, 40);
		this.check(single.secondary() == 0 && single.primary() > 0, "Turning double click off returns to single clicks");

		this.configure(ctx, c -> {
			c.doubleClickEnabled = true;
			c.left.doubleClick = false;
		});
		ctx.waitTicks(10);
		Sample sideOff = this.measure(ctx, ClickType.LEFT, 40);
		this.check(sideOff.secondary() == 0, "The per-side double click switch is respected");
		this.setGlobal(ctx, false);
		this.configure(ctx, c -> {
			c.doubleClickEnabled = false;
			c.left.doubleClick = true;
			c.left.doubleClickDelayMs = 50;
		});
	}

	private int heldSlotCount(TestSingleplayerContext sp, int slot) {
		return sp.getServer().computeOnServer(server -> serverPlayer(server).getInventory().getItem(slot).getCount());
	}

	private ItemStack chestSlot(TestSingleplayerContext sp) {
		return sp.getServer().computeOnServer(server -> serverPlayer(server).getItemBySlot(EquipmentSlot.CHEST).copy());
	}

	private void selectSlot(ClientGameTestContext ctx, int slot) {
		ctx.getInput().pressKey(GLFW.GLFW_KEY_1 + slot);
		ctx.waitTicks(3);
	}

	private void expectProtected(ClientGameTestContext ctx, TestSingleplayerContext sp, int slot, ProtectedCategory category, String what) {
		this.selectSlot(ctx, slot);
		Sample sample = this.measure(ctx, ClickType.RIGHT, 20);
		ProtectedCategory matched = ctx.computeOnClient(mc -> {
			var match = ClickerClient.manager().protectionMatch();
			return match == null ? null : match.category();
		});
		this.check(sample.total() == 0 && this.status(ctx, ClickType.RIGHT) == ClickerManager.SideStatus.PROTECTED && matched == category,
				"Right clicking pauses while holding " + what);
	}

	private void testProtection(ClientGameTestContext ctx, TestSingleplayerContext sp) {
		this.aim(ctx, sp, 0f, -90f);
		sp.getServer().runCommand("clear @p");
		sp.getServer().runCommand("item replace entity @p hotbar.0 with minecraft:golden_apple 64");
		sp.getServer().runCommand("item replace entity @p hotbar.1 with minecraft:snowball 16");
		sp.getServer().runCommand("item replace entity @p hotbar.2 with minecraft:enchanted_golden_apple 8");
		sp.getServer().runCommand("item replace entity @p hotbar.3 with minecraft:elytra");
		sp.getServer().runCommand("item replace entity @p hotbar.4 with minecraft:diamond_chestplate");
		sp.getServer().runCommand("item replace entity @p hotbar.5 with minecraft:leather_chestplate");
		sp.getServer().runCommand("item replace entity @p hotbar.6 with minecraft:netherite_chestplate");
		sp.getServer().runCommand("item replace entity @p hotbar.7 with minecraft:copper_chestplate");
		sp.getServer().runCommand("item replace entity @p hotbar.8 with minecraft:stick");
		ctx.waitTicks(5);

		this.configure(ctx, c -> {
			c.left.enabled = false;
			c.right.enabled = true;
			c.right.mode = ActivationMode.TOGGLE;
			c.right.cps = 10;
			c.protectionEnabled = true;
			for (ProtectedCategory category : ProtectedCategory.values()) {
				category.setEnabled(c, true);
			}
		});
		this.selectSlot(ctx, 0);
		this.setGlobal(ctx, true);
		ctx.waitTicks(3);

		this.expectProtected(ctx, sp, 0, ProtectedCategory.GOLDEN_APPLE, "a golden apple");
		this.check(this.heldSlotCount(sp, 0) == 64, "No golden apple was used");
		ctx.takeScreenshot("protection_paused_golden_apple_hud");

		// Switch hotbar slots while Clicker keeps running: snowballs are allowed, then back to the protected apple.
		long[] beforeSwitch = this.counts(ctx, ClickType.RIGHT);
		this.selectSlot(ctx, 1);
		ctx.waitTicks(10);
		boolean resumed = this.status(ctx, ClickType.RIGHT) == ClickerManager.SideStatus.CLICKING;
		this.selectSlot(ctx, 0);
		ctx.waitTicks(3);
		long[] afterSwitch = this.counts(ctx, ClickType.RIGHT);
		long snowballClicks = (afterSwitch[0] - beforeSwitch[0]) + (afterSwitch[1] - beforeSwitch[1]);
		int thrown = 16 - this.heldSlotCount(sp, 1);
		this.check(resumed && snowballClicks >= 4,
				"Switching to an allowed item (snowball) resumes right clicking (" + snowballClicks + " clicks)");
		this.check(thrown >= 4 && Math.abs(thrown - snowballClicks) <= 1,
				"Each generated right click threw one snowball in-game (" + snowballClicks + " clicks, " + thrown + " thrown)");

		this.expectProtected(ctx, sp, 0, ProtectedCategory.GOLDEN_APPLE, "a golden apple again after switching back");
		this.expectProtected(ctx, sp, 2, ProtectedCategory.ENCHANTED_GOLDEN_APPLE, "an enchanted golden apple");
		this.check(this.heldSlotCount(sp, 2) == 8, "No enchanted golden apple was used");
		this.expectProtected(ctx, sp, 3, ProtectedCategory.ELYTRA, "an elytra");
		this.expectProtected(ctx, sp, 4, ProtectedCategory.CHESTPLATES, "a diamond chestplate");
		this.expectProtected(ctx, sp, 5, ProtectedCategory.CHESTPLATES, "a leather chestplate");
		this.expectProtected(ctx, sp, 6, ProtectedCategory.CHESTPLATES, "a netherite chestplate");
		this.expectProtected(ctx, sp, 7, ProtectedCategory.CHESTPLATES, "a copper chestplate");
		this.check(this.chestSlot(sp).isEmpty(), "No chestplate or elytra was equipped while protected");

		this.selectSlot(ctx, 8);
		Sample stick = this.measure(ctx, ClickType.RIGHT, 10);
		this.check(stick.total() > 0, "A stick is not protected: right clicking continues");

		sp.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:golden_apple");
		ctx.waitTicks(4);
		Sample offhand = this.measure(ctx, ClickType.RIGHT, 20);
		this.check(offhand.total() == 0, "A golden apple in the OFFHAND also pauses right clicking");
		sp.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:air");
		ctx.waitTicks(4);

		// Per-category switch: golden apples allowed, enchanted ones still protected.
		this.configure(ctx, c -> c.protectGoldenApple = false);
		this.selectSlot(ctx, 0);
		Sample appleAllowed = this.measure(ctx, ClickType.RIGHT, 10);
		this.check(appleAllowed.total() > 0, "Turning off the golden apple group allows right clicking with golden apples");
		this.expectProtected(ctx, sp, 2, ProtectedCategory.ENCHANTED_GOLDEN_APPLE, "an enchanted golden apple (its group is still on)");
		this.configure(ctx, c -> c.protectGoldenApple = true);

		// Manual input is never blocked: holding right click with a golden apple still eats it normally.
		this.setGlobal(ctx, true);
		this.selectSlot(ctx, 0);
		ctx.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		ctx.waitTicks(5);
		boolean eating = ctx.computeOnClient(mc -> mc.player.isUsingItem() && mc.player.getUseItem().is(Items.GOLDEN_APPLE));
		Sample manual = this.measure(ctx, ClickType.RIGHT, 5);
		ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		ctx.waitTicks(3);
		this.check(eating && manual.total() == 0, "Manual right click still works (eating starts) while generated clicks stay paused");

		// Negative control: with protection off, the generated right clicks would equip the chestplate.
		this.configure(ctx, c -> c.protectionEnabled = false);
		this.selectSlot(ctx, 4);
		this.measure(ctx, ClickType.RIGHT, 10);
		ItemStack worn = this.chestSlot(sp);
		this.check(worn.is(Items.DIAMOND_CHESTPLATE), "Control: with protection OFF the clicks do equip the chestplate (so protection is what prevented it)");
		this.setGlobal(ctx, false);
		this.configure(ctx, c -> c.protectionEnabled = true);
		sp.getServer().runCommand("item replace entity @p armor.chest with minecraft:air");
	}

	private void testHud(ClientGameTestContext ctx) {
		this.configure(ctx, c -> {
			c.left.enabled = true;
			c.left.mode = ActivationMode.HOLD;
			c.right.enabled = true;
			c.right.mode = ActivationMode.HOLD;
		});
		this.setGlobal(ctx, true);
		this.selectSlot(ctx, 8);
		ctx.waitTicks(10);
		ctx.takeScreenshot("hud_active");
		this.setGlobal(ctx, false);
	}

	private void testDisconnectSwitchesOff(ClientGameTestContext ctx) {
		ctx.waitTicks(5);
		this.check(!ctx.computeOnClient(mc -> ClickerClient.manager().isGlobalEnabled()),
				"Leaving the world switches Clicker off (unless 'Remember on/off' is enabled)");
		this.check(ctx.computeOnClient(mc -> ClickerClient.manager().side(ClickType.LEFT).status()) == ClickerManager.SideStatus.OFF
				|| ctx.computeOnClient(mc -> mc.level == null), "No automation runs outside a world");
	}

	private void testConfigPersistence(ClientGameTestContext ctx) {
		Path path = ctx.computeOnClient(mc -> {
			ClickerConfig c = ClickerClient.config().get();
			c.left.cps = 42;
			c.left.mode = ActivationMode.TOGGLE;
			c.right.cps = 77;
			c.right.enabled = true;
			c.doubleClickEnabled = true;
			c.left.doubleClickDelayMs = 123;
			c.protectElytra = false;
			c.hud = clicker.config.HudMode.ALWAYS;
			ClickerClient.themes().select(ClickerTheme.LAVENDER);
			ClickerClient.config().flush();
			return ClickerClient.config().path();
		});
		ConfigManager reloaded = new ConfigManager(path);
		reloaded.load();
		ClickerConfig c = reloaded.get();
		this.check(c.left.cps == 42 && c.left.mode == ActivationMode.TOGGLE && c.right.cps == 77 && c.right.enabled
						&& c.doubleClickEnabled && c.left.doubleClickDelayMs == 123 && !c.protectElytra
						&& c.hud == clicker.config.HudMode.ALWAYS && c.theme.equals("lavender"),
				"Settings written by the game reload identically from " + path.getFileName());

		Path corrupt = path.resolveSibling("clicker-corrupt-test.json");
		try {
			Files.writeString(corrupt, "{ not valid json");
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		ConfigManager recovered = new ConfigManager(corrupt);
		recovered.load();
		this.check(recovered.recoveredFromCorruption() && recovered.get().left.cps == ClickerConfig.DEFAULT_CPS,
				"A corrupted config file is backed up and replaced with defaults instead of crashing");
		ctx.runOnClient(mc -> {
			ClickerClient.themes().select(ClickerTheme.SAKURA);
			ClickerClient.config().resetToDefaults();
		});
	}
}
