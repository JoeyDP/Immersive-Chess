package be.immersivechess.client.data;

import be.immersivechess.ImmersiveChess;
import net.minecraft.client.texture.NativeImage;

import java.util.Arrays;

/**
 * Many functions are based on https://stackoverflow.com/a/56678483
 */
public class ImageHelper {

    public static double averagePerceivedLuminance(NativeImage image) {
        double[] values = perceivedLuminanceValues(image);
        return Arrays.stream(values).average().getAsDouble();
    }

    public static double[] perceivedLuminanceValues(NativeImage image) {
        double[] values = new double[image.getWidth() * image.getHeight()];
        if (image.getFormat() != NativeImage.Format.RGBA) {
            ImmersiveChess.LOGGER.error("Unexpected color format discovered in texture, " + image.getFormat());
            return new double[]{0.5};
        }
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                values[y * image.getWidth() + x] = rgbaToPerceivedLuminance(image.getColor(x, y));
            }
        }
        return values;
    }

    /**
     * Convert int color to double between 0 and 1. Assume RGBA format, see `NativeImage`.
     */
    public static double rgbaToPerceivedLuminance(int rgba) {
        double luminance = rgbaToLuminance(rgba);
        return luminanceToPerceivedLuminance(luminance);
    }

    public static double luminanceToPerceivedLuminance(double luminance) {
        if (luminance <= (216d / 24389d)) {       // The CIE standard states 0.008856 but 216/24389 is the intent for 0.008856451679036
            return luminance * (24389d / 27d);  // The CIE standard states 903.3, but 24389/27 is the intent, making 903.296296296296296
        } else {
            return Math.pow(luminance, (1d / 3d)) * 116 - 16;
        }
    }

    public static double rgbaToLuminance(int rgba) {
        double vR = ((rgba >> 0) & 0x0000FF) / 255d;
        double vG = ((rgba >> 8) & 0x0000FF) / 255d;
        double vB = ((rgba >> 16) & 0x0000FF) / 255d;
        double vA = ((rgba >> 24) & 0x0000FF) / 255d;

        return 0.2126 * rgbaToLin(vR, vA) + 0.7152 * rgbaToLin(vG, vA) + 0.0722 * rgbaToLin(vB, vA);
    }

    private static double rgbaToLin(double v, double alpha) {
        v = alpha * v + (1 - alpha);
        if (v <= 0.04045) {
            return v / 12.92;
        } else {
            return Math.pow(((v + 0.055) / 1.055), 2.4);
        }
    }

}
