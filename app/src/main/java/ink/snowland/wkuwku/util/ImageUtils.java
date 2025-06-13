package ink.snowland.wkuwku.util;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageUtils {
    public static final int FORMAT_RGB565   = 1;
    public static final int FORMAT_RGBA8888 = 2;
    public static void saveAsPng(int pixelFormat, final byte[] data, int width, int height, File file) {
        final Bitmap bitmap;
        if (pixelFormat == FORMAT_RGB565) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
        } else if (pixelFormat == FORMAT_RGBA8888){
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            final int[] pixels = new int[data.length / 4];
            for (int i = 0; i < width * height; ++i) {
                int r = data[4 * i] & 0xFF;
                int g = data[4 * i + 1] & 0xFF;
                int b = data[4 * i + 2] & 0xFF;
//                int a = data[4 * i + 3] & 0xFF;
                pixels[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } else {
            throw new UnsupportedOperationException("Unsupported format " + pixelFormat);
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
