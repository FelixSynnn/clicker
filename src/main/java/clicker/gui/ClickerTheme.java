package clicker.gui;

import java.util.List;

/**
 * A theme preset. Presets are the way to customise Clicker's look; each one restyles every surface, control,
 * text colour, accent, hover state and navigation indicator consistently.
 */
public record ClickerTheme(String id, boolean dark, Palette palette) {
	private static final int SUCCESS = 0xFF5DBE8E;
	private static final int WARNING = 0xFFF0A24A;
	private static final int DANGER = 0xFFE2566F;
	private static final int SUCCESS_DARK = 0xFF6FD3A0;
	private static final int WARNING_DARK = 0xFFF5B25C;
	private static final int DANGER_DARK = 0xFFFF7088;

	/** Soft pink, white and rose: the signature look. */
	public static final ClickerTheme SAKURA = new ClickerTheme("sakura", false, Palette.builder()
			.backdrop(0x30FFD1E1, 0x60F4A6C1)
			.window(0xFFFFF7FA, 0xFFFFE6EE)
			.sidebar(0xFFFFEDF3)
			.card(0xFFFFFFFF, 0xFFFBE3EB)
			.hover(0x14D9578A)
			.text(0xFF4A2C3A, 0xFF8B6676, 0xFFBAA0AC)
			.accent(0xFFF27BA5, 0xFFD9578A, 0xFFFFE1EC, 0xFFFFFFFF)
			.controls(0xFFF3DCE5, 0xFFFFFFFF, 0xFFFFFFFF)
			.shadow(0x38B0577A)
			.status(SUCCESS, WARNING, DANGER)
			.petal(0xFFFFB3CB)
			.build());

	/** Night-time blossoms: a deep plum background with pastel pink accents. */
	public static final ClickerTheme MIDNIGHT_SAKURA = new ClickerTheme("midnight_sakura", true, Palette.builder()
			.backdrop(0x80120A12, 0xB01A0C17)
			.window(0xFF1E1620, 0xFF2E2231)
			.sidebar(0xFF241A27)
			.card(0xFF2B2030, 0xFF3A2B3F)
			.hover(0x18FF9EC4)
			.text(0xFFF8E9F0, 0xFFCBB0BE, 0xFF8D7685)
			.accent(0xFFFF9EC4, 0xFFFF7FB0, 0xFF472A3D, 0xFF2B1222)
			.controls(0xFF3F3044, 0xFFFFF4F8, 0xFF34253B)
			.shadow(0x80000000)
			.status(SUCCESS_DARK, WARNING_DARK, DANGER_DARK)
			.petal(0xC0FF9EC4)
			.build());

	/** Airy white with the lightest touch of pink. */
	public static final ClickerTheme BLOSSOM = new ClickerTheme("blossom", false, Palette.builder()
			.backdrop(0x28FFFFFF, 0x50FFE3EE)
			.window(0xFFFFFFFF, 0xFFF5E8ED)
			.sidebar(0xFFFBF4F7)
			.card(0xFFFFFCFD, 0xFFF4E5EB)
			.hover(0x12E57AA3)
			.text(0xFF3E3037, 0xFF86707B, 0xFFB5A3AB)
			.accent(0xFFF59BBD, 0xFFE5739F, 0xFFFDEBF2, 0xFFFFFFFF)
			.controls(0xFFEFE3E8, 0xFFFFFFFF, 0xFFFFFFFF)
			.shadow(0x2E9E7A8A)
			.status(SUCCESS, WARNING, DANGER)
			.petal(0xFFF8C6D8)
			.build());

	/** Deeper pinks with muted red accents. */
	public static final ClickerTheme ROSE = new ClickerTheme("rose", false, Palette.builder()
			.backdrop(0x40B5485F, 0x70742237)
			.window(0xFFFFF5F6, 0xFFF6D9DE)
			.sidebar(0xFFFBE4E8)
			.card(0xFFFFFFFF, 0xFFF3D6DB)
			.hover(0x14B32D52)
			.text(0xFF4B1F2B, 0xFF8B5664, 0xFFB78F99)
			.accent(0xFFD6456A, 0xFFB32D52, 0xFFFADDE4, 0xFFFFFFFF)
			.controls(0xFFEED4DA, 0xFFFFFFFF, 0xFFFFFFFF)
			.shadow(0x40902040)
			.status(SUCCESS, WARNING, DANGER)
			.petal(0xFFE88AA2)
			.build());

	/** Soft purple and lavender. */
	public static final ClickerTheme LAVENDER = new ClickerTheme("lavender", false, Palette.builder()
			.backdrop(0x30E9DFFF, 0x60C3AEF0)
			.window(0xFFFBF8FF, 0xFFECE4FB)
			.sidebar(0xFFF3EDFE)
			.card(0xFFFFFFFF, 0xFFE9E0F8)
			.hover(0x148B6AD8)
			.text(0xFF352C4A, 0xFF756B8F, 0xFFA9A0C0)
			.accent(0xFFA98CEC, 0xFF8B6AD8, 0xFFEEE6FD, 0xFFFFFFFF)
			.controls(0xFFE5DCF4, 0xFFFFFFFF, 0xFFFFFFFF)
			.shadow(0x387A5FB0)
			.status(SUCCESS, WARNING, DANGER)
			.petal(0xFFCDB8F5)
			.build());

	/** Dark charcoal with subtle pink highlights. */
	public static final ClickerTheme MIDNIGHT = new ClickerTheme("midnight", true, Palette.builder()
			.backdrop(0x900A0A0C, 0xB0111114)
			.window(0xFF18181C, 0xFF27272D)
			.sidebar(0xFF1E1E23)
			.card(0xFF232329, 0xFF303037)
			.hover(0x14E88FB0)
			.text(0xFFEDEDF2, 0xFFA9A9B6, 0xFF6F6F7B)
			.accent(0xFFE88FB0, 0xFFD9739A, 0xFF3A2A33, 0xFF1C1116)
			.controls(0xFF36363E, 0xFFF4F4F7, 0xFF2B2B32)
			.shadow(0x90000000)
			.status(SUCCESS_DARK, WARNING_DARK, DANGER_DARK)
			.petal(0x90E88FB0)
			.build());

	public static final List<ClickerTheme> PRESETS = List.of(SAKURA, MIDNIGHT_SAKURA, BLOSSOM, ROSE, LAVENDER, MIDNIGHT);

	public String translationKey() {
		return "clicker.theme." + this.id;
	}

	public String descriptionKey() {
		return "clicker.theme." + this.id + ".desc";
	}

	public static ClickerTheme byId(String id) {
		for (ClickerTheme theme : PRESETS) {
			if (theme.id.equals(id)) {
				return theme;
			}
		}
		return SAKURA;
	}

	public static boolean exists(String id) {
		for (ClickerTheme theme : PRESETS) {
			if (theme.id.equals(id)) {
				return true;
			}
		}
		return false;
	}
}
