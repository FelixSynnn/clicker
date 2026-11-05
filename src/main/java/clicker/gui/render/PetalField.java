package clicker.gui.render;

import clicker.gui.Colors;

import java.util.Random;

/** A light drift of sakura petals behind the window. Purely decorative and very cheap (16 sprites). */
public final class PetalField {
	private static final int COUNT = 16;

	private final Random random = new Random();
	private final float[] x = new float[COUNT];
	private final float[] y = new float[COUNT];
	private final float[] size = new float[COUNT];
	private final float[] fall = new float[COUNT];
	private final float[] swayPhase = new float[COUNT];
	private final float[] swaySpeed = new float[COUNT];
	private final float[] swayAmount = new float[COUNT];
	private final float[] rotation = new float[COUNT];
	private final float[] spin = new float[COUNT];
	private final float[] depth = new float[COUNT];

	public PetalField() {
		for (int i = 0; i < COUNT; i++) {
			this.spawn(i, this.random.nextFloat());
		}
	}

	private void spawn(int i, float startY) {
		this.depth[i] = 0.45f + this.random.nextFloat() * 0.55f;
		this.x[i] = this.random.nextFloat();
		this.y[i] = startY;
		this.size[i] = 5f + 5f * this.depth[i];
		this.fall[i] = 0.025f + 0.04f * this.depth[i];
		this.swayPhase[i] = this.random.nextFloat() * 6.283f;
		this.swaySpeed[i] = 0.6f + this.random.nextFloat() * 0.9f;
		this.swayAmount[i] = 0.01f + this.random.nextFloat() * 0.018f;
		this.rotation[i] = this.random.nextFloat() * 6.283f;
		this.spin[i] = (this.random.nextFloat() - 0.5f) * 1.6f;
	}

	public void update(float dt) {
		for (int i = 0; i < COUNT; i++) {
			this.y[i] += this.fall[i] * dt;
			this.swayPhase[i] += this.swaySpeed[i] * dt;
			this.x[i] += (float) Math.sin(this.swayPhase[i]) * this.swayAmount[i] * dt;
			this.rotation[i] += this.spin[i] * dt;
			if (this.y[i] > 1.08f) {
				this.spawn(i, -0.08f);
			}
		}
	}

	public void render(UiRenderer r, float width, float height, int color, float opacity) {
		for (int i = 0; i < COUNT; i++) {
			float flutter = 0.75f + 0.25f * (float) Math.cos(this.swayPhase[i] * 1.7f);
			float w = this.size[i] * flutter;
			float h = this.size[i] * 1.15f;
			int c = Colors.fade(color, opacity * (0.35f + 0.45f * this.depth[i]));
			r.spriteRotated(UiRenderer.PETAL, UiRenderer.PETAL_TEXTURE_SIZE, this.x[i] * width, this.y[i] * height, w, h, this.rotation[i], c);
		}
	}
}
