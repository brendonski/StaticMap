package com.doubotis.staticmap.layers;

import com.doubotis.staticmap.StaticMap;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestTileLayer {

	@Test
	public void testGetTile() throws IOException {
		TileLayer tileLayer = new FakeTileLayer();
		Image tile = tileLayer.getTile(1, 2, 3);
		File outputfile = new File("saved.png");
		ImageIO.write((BufferedImage) tile, "png", outputfile);
	}

	@Test
	public void testParallelDraw() throws IOException {
		FakeTileLayer tileLayer = new FakeTileLayer();
		StaticMap mp = new StaticMap(1024, 1024);
		mp.setLocation(50.5, 5.5);
		mp.setZoom(14);
		mp.addLayer(tileLayer);
		File f = new File("parallel_tiles.png");
		mp.drawInto(f);
	}

	@Test
	public void testParallelDrawWithSimulatedTimeout() throws IOException {
		FakeTileLayer tileLayer = new FakeTileLayer();
		tileLayer.setTestTimeout(true);
		StaticMap mp = new StaticMap(1024, 1024);
		mp.setLocation(-33.865143, 151.209900);
		mp.setZoom(14);
		mp.addLayer(tileLayer);
		File f = new File("parallel_tiles_timeout.png");
		mp.drawInto(f);
	}

}
