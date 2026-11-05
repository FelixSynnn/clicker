package clicker.gui;

import clicker.gui.render.Icon;

/** The sidebar pages, in display order. */
public enum Page {
	HOME("home", Icon.HOME),
	CLICKING("clicking", Icon.CLICK),
	DOUBLE_CLICK("double_click", Icon.DOUBLE),
	PROTECTION("protection", Icon.SHIELD),
	THEMES("themes", Icon.PALETTE),
	SETTINGS("settings", Icon.GEAR);

	private final String id;
	private final Icon icon;

	Page(String id, Icon icon) {
		this.id = id;
		this.icon = icon;
	}

	public String id() {
		return this.id;
	}

	public Icon icon() {
		return this.icon;
	}

	public String titleKey() {
		return "clicker.page." + this.id;
	}

	public String subtitleKey() {
		return "clicker.page." + this.id + ".subtitle";
	}

	public static Page byId(String id) {
		for (Page page : values()) {
			if (page.id.equals(id)) {
				return page;
			}
		}
		return HOME;
	}
}
