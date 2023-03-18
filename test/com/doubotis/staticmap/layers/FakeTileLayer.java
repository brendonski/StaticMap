package com.doubotis.staticmap.layers;

import java.awt.*;
import java.awt.image.BufferedImage;

public class FakeTileLayer extends TileLayer {

	private static final int TILE_SIZE = 256;

	private static final long TIMEOUT_MS = 2000;

	private static final long MAX_TIMEOUT_MS = 4000;

	private boolean testTimeout = false;

	public boolean isTestTimeout() {
		return testTimeout;
	}

	public void setTestTimeout(boolean testTimeout) {
		this.testTimeout = testTimeout;
	}

	@Override
	public Image getTile(int tileX, int tileY, int tileZ) {
		BufferedImage buff =
				new BufferedImage(TILE_SIZE, TILE_SIZE,
						BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = buff.createGraphics();
		g.setBackground(Color.WHITE);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
		g.setColor(Color.BLACK);
		g.setFont(new Font("TimesRoman", Font.BOLD, 24));
		drawCenteredString(String.format("%d-%d-%d", tileX, tileY, tileZ), TILE_SIZE, TILE_SIZE, g);
		// random wait up to max timeout
		long millis = (long) (Math.random() * MAX_TIMEOUT_MS);
		if (isTestTimeout()) {
			// simulate a failed image load
			long start = System.currentTimeMillis();
			long now = start;
			while (now < start + millis) {
				if (now - start > TIMEOUT_MS) {
					throw new RuntimeException("Timeout");
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					//ignore
				}
				now = System.currentTimeMillis();
			}
		}
		return buff;
	}

	private void drawCenteredString(String s, int w, int h, Graphics g) {
		FontMetrics fm = g.getFontMetrics();
		int x = (w - fm.stringWidth(s)) / 2;
		int y = (fm.getAscent() + (h- (fm.getAscent() + fm.getDescent())) / 2);
		g.drawString(s, x, y);
	}

}
