package com.kauan.gamelog.catalog;

public final class IgdbImages {
    private IgdbImages() {}

    public static String cover(String imageId) {
        return imageId == null ? null : "https://images.igdb.com/igdb/image/upload/t_cover_big/" + imageId + ".jpg";
    }
}
