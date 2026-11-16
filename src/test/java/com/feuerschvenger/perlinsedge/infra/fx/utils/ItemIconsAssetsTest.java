package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every item type has a drawn 16x16 icon, and no icon in the folder belongs to nothing. */
class ItemIconsAssetsTest {
    private static final Path ITEMS_FOLDER = Path.of("src/main/resources/assets/items");
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private static int[] pngSize(InputStream in) throws IOException {
        byte[] header = in.readNBytes(24);
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            assertEquals(PNG_SIGNATURE[i], header[i], "not a PNG file");
        }
        int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
        int height = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16) | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
        return new int[]{width, height};
    }

    @Test
    void everyItemTypeHasAnIconOfSixteenBySixteenOnTheClasspath() throws IOException {
        for (ItemType type : ItemType.values()) {
            try (InputStream in = ItemIconsAssetsTest.class.getResourceAsStream(AssetPaths.item(type))) {
                assertNotNull(in, "no icon for " + type);
                int[] size = pngSize(in);
                assertEquals(16, size[0], type + " width");
                assertEquals(16, size[1], type + " height");
            }
        }
    }

    @Test
    void noIconFileInTheFolderBelongsToNoItemType() throws IOException {
        Set<String> expected = new HashSet<>();
        for (ItemType type : ItemType.values()) {
            expected.add(type.getSpritePath() + ".png");
        }
        try (Stream<Path> files = Files.list(ITEMS_FOLDER)) {
            Set<String> found = new HashSet<>();
            files.forEach(file -> found.add(file.getFileName().toString()));
            assertEquals(expected, found);
        }
        assertTrue(expected.size() >= 24);
    }
}
