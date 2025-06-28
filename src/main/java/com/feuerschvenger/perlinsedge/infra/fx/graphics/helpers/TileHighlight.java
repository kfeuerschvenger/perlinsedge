package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

public record TileHighlight(double centerX, double centerY, double tileWidth, double tileHeight) {

    @Override
    public double centerX() {
        return centerX;
    }

    @Override
    public double centerY() {
        return centerY;
    }

    @Override
    public double tileWidth() {
        return tileWidth;
    }

    @Override
    public double tileHeight() {
        return tileHeight;
    }

}
