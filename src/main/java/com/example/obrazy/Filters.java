package com.example.obrazy;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class Filters {

    private static final ForkJoinPool customPool = new ForkJoinPool(4);

    public Image scaleImage(Image source, int width, int height) {
        if (width == 0 || height == 0) {

            double ratio = source.getWidth() / source.getHeight();
            if (width == 0) {
                width = (int)(height * ratio);
            } else {
                height = (int)(width / ratio);
            }
        }

        ImageView imageView = new ImageView(source);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        return imageView.snapshot(params, null);
    }

    public WritableImage applyContour(Image source) throws Exception {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();

        WritableImage result = new WritableImage(width, height);
        PixelReader reader = source.getPixelReader();
        PixelWriter writer = result.getPixelWriter();

        double[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        double[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        customPool.submit(() -> IntStream.range(1, height - 1).parallel().forEach(y -> {
            for (int x = 1; x < width - 1; x++) {
                double pixelX = 0;
                double pixelY = 0;

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        Color color = reader.getColor(x + j, y + i);
                        double gray = 0.299 * color.getRed() +
                                0.587 * color.getGreen() +
                                0.114 * color.getBlue();
                        pixelX += gray * sobelX[i + 1][j + 1];
                        pixelY += gray * sobelY[i + 1][j + 1];
                    }
                }

                double magnitude = Math.sqrt(pixelX * pixelX + pixelY * pixelY);
                double edgeValue = magnitude > 0.2 ? 1.0 : 0.0;
                writer.setColor(x, y, new Color(edgeValue, edgeValue, edgeValue, 1.0));
            }
        })).get();

        return result;
    }

    public WritableImage applyThreshold(Image source, int threshold) throws Exception {
        BufferedImage tmp = SwingFXUtils.fromFXImage(source, null);

        BufferedImage result = new BufferedImage(
                tmp.getWidth(),
                tmp.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        result.getGraphics().drawImage(tmp, 0, 0, null);

       customPool.submit(() ->
                IntStream.range(0, result.getHeight()).parallel().forEach(y -> {
                    for (int x = 0; x < result.getWidth(); x++) {
                        int rgb = result.getRGB(x, y);
                        int gray = rgb & 0xFF;
                        int newValue = gray > threshold ? 255 : 0;
                        int rgbValue = (newValue << 16) | (newValue << 8) | newValue;
                        result.setRGB(x, y, rgbValue);
                    }
                })
        ).get();

        return SwingFXUtils.toFXImage(result, null);
    }

    public WritableImage applyNegative(Image source) throws Exception {

        BufferedImage result = SwingFXUtils.fromFXImage(source, null);

        // negatyw
        customPool.submit(() ->
                IntStream.range(0, result.getHeight()).parallel().forEach(y -> {
                    for (int x = 0; x < result.getWidth(); x++) {
                        int rgb = result.getRGB(x, y);
                        int r = 255 - ((rgb >> 16) & 0xFF);
                        int g = 255 - ((rgb >> 8) & 0xFF);
                        int b = 255 - (rgb & 0xFF);
                        int newRgb = (r << 16) | (g << 8) | b;
                        result.setRGB(x, y, newRgb);
                    }
                })).get();

        return SwingFXUtils.toFXImage(result, null);
    }
}
