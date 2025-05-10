package ink.snowland.wkuwku.util;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageUtils {
    public static void saveRGB565AsBMP(byte[] rgb565Data, int width, int height, String outputPath) throws IOException {
        if (rgb565Data == null || rgb565Data.length != width * height * 2) {
            throw new IllegalArgumentException("Invalid RGB565 data or dimensions");
        }

        int rowSize = (width * 3 + 3) & ~3;
        int imageSize = rowSize * height;
        int fileSize = 54 + imageSize;       // 54 bytes header

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(outputPath))) {
            out.write('B'); out.write('M');
            out.write(fileSize & 0xFF);
            out.write((fileSize >> 8) & 0xFF);
            out.write((fileSize >> 16) & 0xFF);
            out.write((fileSize >> 24) & 0xFF);
            out.write(0); out.write(0); out.write(0); out.write(0); // reserved
            out.write(54 & 0xFF);
            out.write(0); out.write(0); out.write(0);

            out.write(40 & 0xFF);
            out.write(0); out.write(0); out.write(0);
            out.write(width & 0xFF);
            out.write((width >> 8) & 0xFF);
            out.write((width >> 16) & 0xFF);
            out.write((width >> 24) & 0xFF);
            out.write(height & 0xFF);
            out.write((height >> 8) & 0xFF);
            out.write((height >> 16) & 0xFF);
            out.write((height >> 24) & 0xFF);
            out.write(1); out.write(0);
            out.write(24); out.write(0);
            out.write(0); out.write(0); out.write(0); out.write(0);
            out.write(imageSize & 0xFF);
            out.write((imageSize >> 8) & 0xFF);
            out.write((imageSize >> 16) & 0xFF);
            out.write((imageSize >> 24) & 0xFF);
            out.write(0x13); out.write(0x0B); out.write(0); out.write(0); // 2835 = 0x0B13
            out.write(0x13); out.write(0x0B); out.write(0); out.write(0);
            out.write(0); out.write(0); out.write(0); out.write(0);
            out.write(0); out.write(0); out.write(0); out.write(0);

            ByteBuffer buffer = ByteBuffer.wrap(rgb565Data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            byte[] row = new byte[rowSize];
            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    short pixel = buffer.getShort();

                    int r = (pixel >> 11) & 0x1F;  // R
                    int g = (pixel >> 5) & 0x3F;   // G
                    int b = pixel & 0x1F;          // B

                    r = (r << 3) | (r >> 2);
                    g = (g << 2) | (g >> 4);
                    b = (b << 3) | (b >> 2);

                    int offset = x * 3;
                    row[offset] = (byte) b;
                    row[offset + 1] = (byte) g;
                    row[offset + 2] = (byte) r;
                }
                out.write(row);
            }
        }
    }
}
