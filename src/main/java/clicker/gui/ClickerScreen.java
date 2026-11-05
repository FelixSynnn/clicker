package clicker.gui;

import clicker.ClickerClient;
import clicker.config.ClickerConfig;
import clicker.gui.anim.Anim;
import clicker.gui.anim.Easing;
import clicker.gui.components.ConfirmDialog;
import clicker.gui.components.KeybindButton;
import clicker.gui.components.StatusCard;
import clicker.gui.components.UiContext;
import clicker.gui.pages.ClickingPage;
import clicker.gui.pages.DoubleClickPage;
import clicker.gui.pages.HomePage;
import clicker.gui.pages.PageView;
import clicker.gui.pages.ProtectionPage;
import clicker.gui.pages.SettingsPage;
import clicker.gui.pages.ThemesPage;
import clicker.gui.render.PetalField;
import clicker.gui.render.UiRenderer;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Clicker GUI: a centred, rounded window with a sidebar, a header with the live on/off pill, and animated
 * pages. Opens with H (or from Mod Menu), closes with Esc or H, and never pauses the game.
 */
public final class ClickerScreen extends Screen {
	public static final float PREFERRED_WIDTH = 480f;
	public static final float PREFERRED_HEIGHT = 280f;
	public static final float MIN_WIDTH = 340f;
	public static final float MIN_HEIGHT = 220f;
	private static final float RADIUS = 14f;
	private static final float HEADER_HEIGHT = 34f;
	private static final float PAD = 12f;
	private static final long OPEN_NANOS = 240_000_000L;
	private static final long CLOSE_NANOS = 150_000_000L;
	private static final long PAGE_NANOS = 230_000_000L;

	private final @Nullable Screen parent;
	private final UiRenderer renderer = new UiRenderer();
	private final UiContext ctx = new UiContext(this.renderer);
	private final ClickerSidebar sidebar = new ClickerSidebar(this);
	private final Map<Page, PageView> pages = new EnumMap<>(Page.class);
	private final PetalField petals = new PetalField();
	private final ConfirmDialog resetDialog;

	private Page page;
	private long pageChangedAt = Long.MIN_VALUE;
	private long openedAt = Long.MIN_VALUE;
	private long closingAt = Long.MIN_VALUE;
	private float closeFrom = 1f;
	private long lastFrame;
	private boolean closed;

	private int uiScale = 1;
	private long layoutKey = Long.MIN_VALUE;
	private boolean layoutDirty = true;
	private float screenW;
	private float screenH;
	private float winX;
	private float winY;
	private float winW;
	private float winH;
	private float sideW;
	private float contentX;
	private float contentY;
	private float contentW;
	private float contentH;
	private float pillX;
	private float pillY;
	private float pillW;
	private float pillH;
	private float pillHover;

	private float scrollY;
	private float scrollTarget;
	private boolean draggingScrollbar;
	private float lastWindowSlide;
	private float lastPageSlide;

	public ClickerScreen(@Nullable Screen parent) {
		super(Component.translatable("clicker.gui.title"));
		this.parent = parent;
		this.page = Page.byId(ClickerClient.config().get().lastPage);
		this.pages.put(Page.HOME, new HomePage(this));
		this.pages.put(Page.CLICKING, new ClickingPage(this));
		this.pages.put(Page.DOUBLE_CLICK, new DoubleClickPage(this));
		this.pages.put(Page.PROTECTION, new ProtectionPage(this));
		this.pages.put(Page.THEMES, new ThemesPage(this));
		this.pages.put(Page.SETTINGS, new SettingsPage(this));
		this.resetDialog = new ConfirmDialog(
				Component.translatable("clicker.dialog.reset.title"),
				Component.translatable("clicker.dialog.reset.message"),
				Component.translatable("clicker.dialog.reset.confirm"),
				() -> {
					ClickerClient.manager().stopAll();
					ClickerClient.config().resetToDefaults();
					this.layoutDirty = true;
				});
	}

	// ------------------------------------------------------------------ navigation

	public Page page() {
		return this.page;
	}

	public PageView pageView(Page target) {
		return this.pages.get(target);
	}

	public void navigate(Page target) {
		if (target == this.page) {
			return;
		}
		KeybindButton.cancelRecording();
		this.page = target;
		this.pageChangedAt = System.nanoTime();
		this.scrollY = 0f;
		this.scrollTarget = 0f;
		ClickerConfig cfg = ClickerClient.config().get();
		if (!cfg.lastPage.equals(target.id())) {
			cfg.lastPage = target.id();
			ClickerClient.config().markDirty();
		}
	}

	public void confirmReset() {
		this.resetDialog.open();
	}

	public boolean isClosing() {
		return this.closingAt != Long.MIN_VALUE;
	}

	/** The UI scale currently in use (physical pixels per unit). */
	public int uiScale() {
		return this.uiScale;
	}

	// ------------------------------------------------------------------ lifecycle

	@Override
	protected void init() {
		if (this.openedAt == Long.MIN_VALUE) {
			this.openedAt = System.nanoTime();
		}
		this.layoutDirty = true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		if (this.isClosing()) {
			return;
		}
		KeybindButton.cancelRecording();
		if (!Anim.enabled()) {
			this.finishClose();
			return;
		}
		this.closeFrom = this.openProgress(System.nanoTime());
		this.closingAt = System.nanoTime();
	}

	private void finishClose() {
		if (this.closed) {
			return;
		}
		this.closed = true;
		ClickerClient.config().flush();
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void removed() {
		KeybindButton.cancelRecording();
		ClickerClient.config().flush();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (this.minecraft.level == null) {
			this.renderPanorama(graphics, partialTick);
		}
		// Vanilla blur behind the window; the themed tint is drawn in render() so it can fade with the window.
		this.renderBlurredBackground(graphics);
	}

	// ------------------------------------------------------------------ layout

	/** Picks a whole-number scale so text stays crisp and the window fits comfortably. */
	static int chooseScale(int requested, boolean auto, int framebufferW, int framebufferH) {
		int maxFit = 1;
		for (int s = 1; s <= 8; s++) {
			if (MIN_WIDTH * s <= framebufferW * 0.98f && MIN_HEIGHT * s <= framebufferH * 0.96f) {
				maxFit = s;
			}
		}
		int wanted = Math.max(1, requested);
		if (!auto) {
			return Math.min(wanted, maxFit);
		}
		for (int s = Math.min(wanted, maxFit); s >= 2; s--) {
			if (PREFERRED_WIDTH * s <= framebufferW * 0.86f && PREFERRED_HEIGHT * s <= framebufferH * 0.84f) {
				return s;
			}
		}
		return Math.min(wanted, maxFit);
	}

	private void updateLayout() {
		Window window = this.minecraft.getWindow();
		int fbW = Math.max(1, window.getWidth());
		int fbH = Math.max(1, window.getHeight());
		int setting = ClickerClient.config().get().guiScale;
		int scale = chooseScale(setting == 0 ? window.getGuiScale() : setting, setting == 0, fbW, fbH);
		long key = ((long) scale << 40) ^ ((long) fbW << 20) ^ fbH;
		if (key != this.layoutKey) {
			this.layoutKey = key;
			this.layoutDirty = true;
		}
		if (!this.layoutDirty) {
			return;
		}
		this.uiScale = scale;
		this.screenW = fbW / (float) scale;
		this.screenH = fbH / (float) scale;
		this.winW = Math.max(MIN_WIDTH, Math.min(PREFERRED_WIDTH, this.screenW - 24f));
		this.winH = Math.max(MIN_HEIGHT, Math.min(PREFERRED_HEIGHT, this.screenH - 24f));
		this.winX = Math.round((this.screenW - this.winW) / 2f);
		this.winY = Math.round((this.screenH - this.winH) / 2f);
		this.sideW = this.winW >= 400f ? 112f : 100f;
		this.sidebar.layout(this.winX + 6f, this.winY + 6f, this.sideW - 6f, this.winH - 12f);
		this.contentX = this.winX + this.sideW + PAD;
		this.contentY = this.winY + PAD + HEADER_HEIGHT;
		this.contentW = this.winW - this.sideW - PAD * 2f;
		this.contentH = this.winH - HEADER_HEIGHT - PAD * 2f;
	}

	private void layoutPages() {
		for (PageView view : this.pages.values()) {
			view.layout(this.renderer, this.contentX, this.contentY, this.contentW);
		}
		this.resetDialog.layout(this.renderer, this.winX, this.winY, this.winW, this.winH);
		this.layoutDirty = false;
	}

	// ------------------------------------------------------------------ animation helpers

	private float openProgress(long now) {
		if (!Anim.enabled()) {
			return this.isClosing() ? 0f : 1f;
		}
		if (this.isClosing()) {
			float t = (now - this.closingAt) / (float) CLOSE_NANOS;
			return this.closeFrom * (1f - Easing.inCubic(t));
		}
		if (this.openedAt == Long.MIN_VALUE) {
			return 1f;
		}
		return Easing.outCubic((now - this.openedAt) / (float) OPEN_NANOS);
	}

	private float pageProgress(long now) {
		if (!Anim.enabled() || this.pageChangedAt == Long.MIN_VALUE) {
			return 1f;
		}
		return Easing.outCubic((now - this.pageChangedAt) / (float) PAGE_NANOS);
	}

	private double unitsX(double guiX) {
		return guiX * this.minecraft.getWindow().getGuiScale() / this.uiScale;
	}

	private double unitsY(double guiY) {
		return guiY * this.minecraft.getWindow().getGuiScale() / this.uiScale;
	}

	/** Precise mouse position in units (sub-GUI-pixel accuracy for smooth hover). */
	private float mouseUnitsX() {
		Window window = this.minecraft.getWindow();
		double physical = this.minecraft.mouseHandler.xpos() * window.getWidth() / Math.max(1, window.getScreenWidth());
		return (float) (physical / this.uiScale);
	}

	private float mouseUnitsY() {
		Window window = this.minecraft.getWindow();
		double physical = this.minecraft.mouseHandler.ypos() * window.getHeight() / Math.max(1, window.getScreenHeight());
		return (float) (physical / this.uiScale);
	}

	private boolean inViewport(double x, double y) {
		return x >= this.winX + this.sideW && x < this.winX + this.winW && y >= this.contentY - 4f && y < this.winY + this.winH - 4f;
	}

	private float maxScroll() {
		return Math.max(0f, this.pages.get(this.page).contentHeight() - this.contentH);
	}

	// ------------------------------------------------------------------ render

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		float dt = this.lastFrame == 0L ? 0f : Math.min(0.1f, (now - this.lastFrame) / 1_000_000_000f);
		this.lastFrame = now;

		this.updateLayout();
		this.renderer.begin(graphics, this.uiScale);
		try {
			if (this.layoutDirty) {
				this.layoutPages();
			}
			float open = this.openProgress(now);
			if (this.isClosing() && open <= 0.001f) {
				this.finishClose();
				return;
			}
			Palette p = ClickerClient.themes().palette();
			this.ctx.p = p;
			this.ctx.dt = dt;
			UiRenderer r = this.renderer;

			// Themed tint over the blurred world, plus drifting petals.
			r.pushAlpha(open);
			r.gradientV(0f, 0f, this.screenW, this.screenH, p.backdropTop(), p.backdropBottom());
			if (Anim.enabled()) {
				this.petals.update(dt);
				this.petals.render(r, this.screenW, this.screenH, p.petal(), 1f);
			}
			r.popAlpha();

			float windowSlide = (1f - open) * 10f;
			this.lastWindowSlide = windowSlide;
			float mx = this.mouseUnitsX();
			float my = this.mouseUnitsY() - windowSlide;
			boolean interactive = !this.isClosing() && !this.resetDialog.isShowing();

			r.pushAlpha(open);
			r.pushOffset(0f, windowSlide);
			this.ctx.mouseX = mx;
			this.ctx.mouseY = my;
			this.ctx.hoverEnabled = interactive;

			r.shadow(this.winX, this.winY + 5f, this.winW, this.winH, RADIUS, 20f, p.shadow());
			r.round(this.winX, this.winY, this.winW, this.winH, RADIUS, p.window());
			r.outline(this.winX, this.winY, this.winW, this.winH, RADIUS, 1, p.windowEdge());

			this.sidebar.render(this.ctx, this.page);
			this.drawHeader(r, p, mx, my, interactive);
			this.drawContent(r, p, mx, my, now, interactive);

			this.ctx.mouseX = mx;
			this.ctx.mouseY = my;
			this.ctx.hoverEnabled = !this.isClosing();
			this.resetDialog.render(this.ctx);

			r.popOffset();
			r.popAlpha();
		} finally {
			this.renderer.end();
		}
	}

	private void drawHeader(UiRenderer r, Palette p, float mx, float my, boolean interactive) {
		float hx = this.contentX;
		float hy = this.winY + PAD + 1f;
		Component title = Component.translatable(this.page.titleKey());
		r.text(title, hx, hy, p.text(), 1.25f);
		float subY = hy + r.lineHeight(1.25f) + 4f;

		// Live on/off pill with the toggle key; clicking it toggles Clicker.
		boolean on = ClickerClient.manager().isGlobalEnabled();
		Component state = Component.translatable(on ? "clicker.status.active" : "clicker.status.off");
		Component key = ClickerClient.keybinds().toggle().getTranslatedKeyMessage();
		float keyW = r.width(key) + 8f;
		this.pillH = 16f;
		this.pillW = 18f + r.width(state) + 6f + keyW + 4f;
		this.pillX = this.winX + this.winW - PAD - this.pillW;
		this.pillY = this.winY + PAD + 1f;
		boolean hovered = interactive && mx >= this.pillX && mx < this.pillX + this.pillW && my >= this.pillY && my < this.pillY + this.pillH;
		this.pillHover = Anim.approach(this.pillHover, hovered ? 1f : 0f, this.ctx.dt, 16f);
		int bg = on ? Colors.lerp(p.accent(), p.accentStrong(), this.pillHover * 0.5f) : Colors.lerp(p.track(), p.accentSoft(), this.pillHover);
		int fg = on ? p.onAccent() : p.textSecondary();
		if (on) {
			r.shadow(this.pillX, this.pillY + 1.2f, this.pillW, this.pillH, this.pillH / 2f, 4f, Colors.fade(p.accentStrong(), 0.35f));
		}
		r.round(this.pillX, this.pillY, this.pillW, this.pillH, this.pillH / 2f, bg);
		float pulse = on ? 0.55f + 0.45f * (float) Math.sin(System.nanoTime() / 1.6e8) : 1f;
		r.circle(this.pillX + 9f, this.pillY + this.pillH / 2f, 2.2f, Colors.fade(on ? p.onAccent() : p.textMuted(), pulse));
		r.text(state, this.pillX + 15f, this.pillY + 4f, fg);
		float kx = this.pillX + this.pillW - 3f - keyW;
		r.round(kx, this.pillY + 2.5f, keyW, this.pillH - 5f, 4f, on ? Colors.fade(p.onAccent(), 0.22f) : p.window());
		r.textCentered(key, kx + keyW / 2f, this.pillY + 4f, fg);

		Component subtitle = Component.translatable(this.page.subtitleKey());
		r.text(r.ellipsize(subtitle, this.pillX - hx - 8f), hx, subY, p.textMuted());
	}

	private void drawContent(UiRenderer r, Palette p, float mx, float my, long now, boolean interactive) {
		PageView view = this.pages.get(this.page);
		float max = this.maxScroll();
		this.scrollTarget = Math.max(0f, Math.min(max, this.scrollTarget));
		this.scrollY = Anim.approach(this.scrollY, this.scrollTarget, this.ctx.dt, 18f);

		float t = this.pageProgress(now);
		float pageSlide = (1f - t) * 8f;
		this.lastPageSlide = pageSlide;

		float clipX = this.winX + this.sideW + 1f;
		float clipY = this.contentY - 6f;
		float clipW = this.winW - this.sideW - 2f;
		float clipH = this.winY + this.winH - 3f - clipY;
		r.pushScissor(clipX, clipY, clipW, clipH);
		r.pushAlpha(t);
		r.pushOffset(0f, pageSlide - this.scrollY);
		this.ctx.mouseX = mx;
		this.ctx.mouseY = my - pageSlide + this.scrollY;
		this.ctx.hoverEnabled = interactive && this.inViewport(mx, my);
		view.render(this.ctx);
		r.popOffset();
		r.popAlpha();
		r.popScissor();

		if (max > 0.5f) {
			float trackX = this.winX + this.winW - 6f;
			float trackY = this.contentY;
			float trackH = this.contentH;
			float thumbH = Math.max(18f, trackH * this.contentH / (this.contentH + max));
			float thumbY = trackY + (trackH - thumbH) * (this.scrollY / max);
			r.round(trackX, trackY, 2.5f, trackH, 1.25f, Colors.fade(p.track(), 0.8f));
			r.round(trackX, thumbY, 2.5f, thumbH, 1.25f, this.draggingScrollbar ? p.accentStrong() : p.accent());
			// Soft fades hint that there is more content above/below.
			if (this.scrollY > 1f) {
				r.gradientV(this.contentX - 4f, this.contentY - 6f, this.contentW + 8f, 8f, p.window(), Colors.fade(p.window(), 0f));
			}
			if (this.scrollY < max - 1f) {
				r.gradientV(this.contentX - 4f, this.winY + this.winH - 14f, this.contentW + 8f, 10f, Colors.fade(p.window(), 0f), p.window());
			}
		}
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.isClosing()) {
			return true;
		}
		double mx = this.unitsX(event.x());
		double my = this.unitsY(event.y()) - this.lastWindowSlide;
		int button = event.button();
		double contentMouseY = my - this.lastPageSlide + this.scrollY;

		KeybindButton recording = KeybindButton.recording();
		if (recording != null) {
			recording.mouseClicked(mx, contentMouseY, button);
			return true;
		}
		if (this.resetDialog.isShowing()) {
			this.resetDialog.mouseClicked(mx, my, button);
			return true;
		}
		if (this.sidebar.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0 && mx >= this.pillX && mx < this.pillX + this.pillW && my >= this.pillY && my < this.pillY + this.pillH) {
			ClickerClient.input().toggleGlobal(this.minecraft);
			return true;
		}
		if (this.inViewport(mx, my)) {
			float max = this.maxScroll();
			if (button == 0 && max > 0.5f && mx >= this.winX + this.winW - 9f) {
				this.draggingScrollbar = true;
				this.dragScrollbar(my);
				return true;
			}
			this.pages.get(this.page).mouseClicked(mx, contentMouseY, button);
		}
		return true;
	}

	private void dragScrollbar(double my) {
		float max = this.maxScroll();
		float thumbH = Math.max(18f, this.contentH * this.contentH / (this.contentH + max));
		float t = (float) ((my - this.contentY - thumbH / 2f) / Math.max(1f, this.contentH - thumbH));
		this.scrollTarget = Math.max(0f, Math.min(1f, t)) * max;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.draggingScrollbar = false;
		double mx = this.unitsX(event.x());
		double my = this.unitsY(event.y()) - this.lastWindowSlide;
		this.pages.get(this.page).mouseReleased(mx, my - this.lastPageSlide + this.scrollY, event.button());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		double mx = this.unitsX(event.x());
		double my = this.unitsY(event.y()) - this.lastWindowSlide;
		if (this.draggingScrollbar) {
			this.dragScrollbar(my);
			return true;
		}
		this.pages.get(this.page).mouseDragged(mx, my - this.lastPageSlide + this.scrollY, event.button());
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		double mx = this.unitsX(mouseX);
		double my = this.unitsY(mouseY) - this.lastWindowSlide;
		if (this.resetDialog.isShowing() || !this.inViewport(mx, my)) {
			return true;
		}
		float max = this.maxScroll();
		if (max > 0.5f) {
			// Scrollable pages scroll; sliders only take the wheel on pages that fit without scrolling.
			this.scrollTarget = Math.max(0f, Math.min(max, this.scrollTarget - (float) vertical * 22f));
			return true;
		}
		this.pages.get(this.page).mouseScrolled(mx, my - this.lastPageSlide + this.scrollY, vertical);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		KeybindButton recording = KeybindButton.recording();
		if (recording != null) {
			recording.keyPressed(event);
			return true;
		}
		if (this.resetDialog.isOpen()) {
			this.resetDialog.keyPressed(event);
			return true;
		}
		if (event.isEscape() || ClickerClient.keybinds().openGui().matches(event)) {
			this.onClose();
			return true;
		}
		if (ClickerClient.keybinds().toggle().matches(event)) {
			ClickerClient.input().toggleGlobal(this.minecraft);
			return true;
		}
		int digit = event.getDigit();
		if (digit >= 1 && digit <= Page.values().length) {
			Page target = Page.values()[digit - 1];
			if (target != this.page) {
				UiSounds.navigate();
				this.navigate(target);
			}
			return true;
		}
		if (this.pages.get(this.page).keyPressed(event)) {
			return true;
		}
		return super.keyPressed(event);
	}

	/** Used by the HUD to avoid drawing on top of the open GUI. */
	public static boolean isOpen() {
		return net.minecraft.client.Minecraft.getInstance().screen instanceof ClickerScreen;
	}

	/** The status shown in the header pill. */
	public StatusCard.Status headerStatus() {
		return StatusText.global();
	}

	/** True once the open and page animations have finished. */
	public boolean isSettled() {
		long now = System.nanoTime();
		return !this.isClosing() && this.openProgress(now) >= 0.999f && this.pageProgress(now) >= 0.999f
				&& Math.abs(this.scrollY - this.scrollTarget) < 0.01f;
	}

	/** Window bounds in units: x, y, width, height, sidebar width (used by the in-game tests). */
	public float[] windowBounds() {
		return new float[]{this.winX, this.winY, this.winW, this.winH, this.sideW};
	}

	/** Converts a point in units (page content space) to GLFW cursor coordinates (used by the in-game tests). */
	public double[] toCursor(float unitX, float unitY, boolean pageContent) {
		Window window = this.minecraft.getWindow();
		float y = pageContent ? unitY - this.scrollY + this.lastPageSlide : unitY;
		y += this.lastWindowSlide;
		double sx = (double) window.getScreenWidth() / Math.max(1, window.getWidth());
		double sy = (double) window.getScreenHeight() / Math.max(1, window.getHeight());
		return new double[]{unitX * this.uiScale * sx, y * this.uiScale * sy};
	}
}
