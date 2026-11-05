package clicker.clicking;

/** Which mouse action a clicker side automates. */
public enum ClickType {
	LEFT("left"),
	RIGHT("right");

	private final String key;

	ClickType(String key) {
		this.key = key;
	}

	/** Stable lowercase id, used in the config file and translation keys. */
	public String key() {
		return this.key;
	}

	public String translationKey() {
		return "clicker.side." + this.key;
	}
}
