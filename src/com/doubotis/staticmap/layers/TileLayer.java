/*
 * Copyright (C) 2017 doubotis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.doubotis.staticmap.layers;

import com.doubotis.staticmap.StaticMap;
import com.doubotis.staticmap.geo.Location;
import com.doubotis.staticmap.geo.PointF;
import com.doubotis.staticmap.geo.Tile;
import com.doubotis.staticmap.geo.projection.MercatorProjection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 *
 * @author Christophe
 */
public abstract class TileLayer implements Layer
{

    private float mOpacity = 1.0f;
    
    public void setOpacity(float opacity)
    {
        mOpacity = opacity;
    }
    
    /** Returns the opacity of the layer, between 0 and 1. */
    public float getOpacity()
    {
        return mOpacity;
    }
    
    public abstract Image getTile(int tileX, int tileY, int tileZ);

    @Override
    public void draw(Graphics2D graphics, StaticMap mp)
    {
        // Apply opacity
        float alpha = getOpacity();
        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        graphics.setComposite(composite);
        
        MercatorProjection proj = mp.getProjection();
        int tileSize = proj.getTileSize();
        int tileZ = mp.getZoom();
        
        
        // Get the top left point.
        PointF topLeftPixels = new PointF(0 + mp.getOffset().x,
                0 + mp.getOffset().y);
        Location topLeftLocation = proj.project(topLeftPixels, mp.getZoom());
        Tile topLeftTile = new Tile(
                tileXFromLongitude(topLeftLocation.getLongitude(), mp.getZoom()),
                tileYFromLatitude(topLeftLocation.getLatitude(), mp.getZoom()),
                mp.getZoom());
        
        // Get the bottom right point.
        PointF bottomRightPixels = new PointF(mp.getWidth() + mp.getOffset().x,
                mp.getHeight() + mp.getOffset().y);
        Location bottomRightLocation = proj.project(bottomRightPixels, mp.getZoom());
        Tile bottomRightTile = new Tile(
                tileXFromLongitude(bottomRightLocation.getLongitude(), mp.getZoom()),
                tileYFromLatitude(bottomRightLocation.getLatitude(), mp.getZoom()),
                mp.getZoom());
        
        // Get the top left corner or the top left tile before looping.
        double topLeftCornerLat = latitudeFromTile(topLeftTile.y, mp.getZoom());
        double topLeftCornerLon = longitudeFromTile(topLeftTile.x, mp.getZoom());
        Location topLeftLoc = new Location(topLeftCornerLat, topLeftCornerLon);
        PointF topLeftCorner = proj.unproject(topLeftLoc, mp.getZoom());
        
        int i = 0;
        int j = 0;

        // precompute tile positions so we can build futures
        List<TileData> tiles = new ArrayList<>();
        for (int y = topLeftTile.y; y <= bottomRightTile.y; y++) {
            for (int x = topLeftTile.x; x <= bottomRightTile.x; x++) {
                // Get the "true" pos.
                PointF truePos = new PointF(topLeftCorner.x + (tileSize * i), topLeftCorner.y + (tileSize * j));
                // Get the pos.
                PointF tilePos = new PointF(truePos.x - mp.getOffset().x, truePos.y - mp.getOffset().y);
                tiles.add(new TileData(x, y, tileZ, truePos, tilePos, tileSize));
                i++;
            }
            i = 0;
            j++;
        }
        CompletableFuture<List<TileData>> allTileDataFutures = buildTileDataFutures(tiles);
        try {
            List<TileData> tileData = allTileDataFutures.get();
            for (TileData t : tileData) {
                // Draw the tile.
                graphics.drawImage(t.image, (int) t.tilePos.x, (int) t.tilePos.y, tileSize, tileSize, null);
            }
        } catch (InterruptedException | ExecutionException e) {
            // ignored
        }

        // Reset composite.
        composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
        graphics.setComposite(composite);
    }

    private CompletableFuture<List<TileData>> buildTileDataFutures(List<TileData> tiles) {
        List<CompletableFuture<TileData>> tileDataList = tiles.stream().map(t -> CompletableFuture.supplyAsync(() -> {
            try {
                t.setImage(getTile(t.x, t.y, t.z));
            } catch (Exception e) {
                t.setImage(getDefaultImage(t.tileSize));
            }
            return t;
        })).collect(Collectors.toList());

        CompletableFuture<Void> tileDataFutures = CompletableFuture.allOf(
                tileDataList.toArray(new CompletableFuture[0]));
        // list of all futures that wait for all to complete
        return tileDataFutures.thenApply(
                v -> tileDataList.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    private Image getDefaultImage(int tileSize) {
        BufferedImage buff =
                new BufferedImage(tileSize, tileSize,
                        BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buff.createGraphics();
        g.setColor(Color.decode("#EEEEEEE"));
        g.fillRect(0, 0, tileSize, tileSize);
        return buff;
    }

    // === Static methods ===
    
    public static double longitudeFromTile(int x, int z)
    {
            return (x/Math.pow(2,z)*360-180);
    }

    public static  double latitudeFromTile(int y, int z)
    {
        final double latRadians = StrictMath.PI - (2.0 * StrictMath.PI) * y / (1 << z);
        return StrictMath.atan(StrictMath.exp(latRadians)) / StrictMath.PI * 360.0 - 90.0;
    }

    public static int tileXFromLongitude(double lon, int z)
    {
        return ((int)Math.floor( (lon + 180) / 360 * (1<<z) ));
    }

    public static int tileYFromLatitude(double lat, int z)
    {
        final double alpha = Math.toRadians(lat);

        return (int)StrictMath.floor( (float) ((1.0 - StrictMath.log((StrictMath.sin(alpha) + 1.0) / StrictMath.cos(alpha)) / StrictMath.PI) * 0.5 * (1 << z)));
    }

    private class TileData {
        private int x;
        private int y;
        private int z;
        private PointF truePos;
        private PointF tilePos;
        private int tileSize;
        private Image image;

        public TileData(int x, int y, int z, PointF truePos, PointF tilePos, int tileSize) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.truePos = truePos;
            this.tilePos = tilePos;
            this.tileSize = tileSize;
        }

        public Image getImage() {
            return image;
        }

        public void setImage(Image image) {
            this.image = image;
        }
    }
}
