package util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Loads classpath assets or project assets from root/src IDE working directories. */
public final class ImageAssets {
    private ImageAssets() { }

    public static BufferedImage load(String assetPath) {
        try {
            try (InputStream resource = ImageAssets.class.getClassLoader()
                    .getResourceAsStream(assetPath)) {
                if (resource != null) {
                    return decode(resource, assetPath);
                }
            }
            Path working = Path.of("").toAbsolutePath().normalize();
            for (Path directory = working; directory != null; directory = directory.getParent()) {
                BufferedImage found = loadIfPresent(directory.resolve(assetPath));
                if (found != null) return found;

                // VS Code may launch Java with the workspace set to the folder that
                // CONTAINS the extracted project rather than the project itself.
                // Check immediate child project folders as well so world-map assets
                // remain loadable regardless of that launch layout.
                try (var children = Files.list(directory)) {
                    for (Path child : children.filter(Files::isDirectory).toList()) {
                        found = loadIfPresent(child.resolve(assetPath));
                        if (found != null) return found;
                    }
                } catch (IOException ignored) {
                    // Continue with the remaining deterministic lookup locations.
                }
            }
            throw new IOException("Not found on classpath, project root, or workspace children from "
                    + working);
        } catch (IOException | SecurityException error) {
            System.err.println("Cannot load artwork '" + assetPath + "': "
                    + error.getMessage() + ". Using placeholder rendering.");
            return null;
        }
    }

    private static BufferedImage loadIfPresent(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return null;
        try (InputStream input = Files.newInputStream(file)) {
            return decode(input, file.toString());
        }
    }

    private static BufferedImage decode(InputStream input, String source) throws IOException {
        // ImageIO.read(InputStream) may spool to java.io.tmpdir. A full temp
        // drive then looks like corrupt PNG data, even for valid artwork.
        // Keep this decode local and memory-backed; do not change global ImageIO settings.
        try (var imageInput = new MemoryCacheImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported or invalid image: " + source);
            }
            var reader = readers.next();
            try {
                reader.setInput(imageInput);
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }
}
