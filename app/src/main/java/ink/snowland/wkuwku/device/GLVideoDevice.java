package ink.snowland.wkuwku.device;

import static android.opengl.GLES30.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.interfaces.EmVideoDevice;
import ink.snowland.wkuwku.util.ImageUtils;
import ink.snowland.wkuwku.util.SettingsManager;

public class GLVideoDevice implements EmVideoDevice {
    private static final String VIDEO_RATIO = "app_video_ratio";
    private final byte[] mLock = new byte[0];
    private int mVideoWidth;
    private int mVideoHeight;
    private volatile ByteBuffer mFrameBuffer = null;
    private RenderImpl mRender;
    private final String mVertexShaderSource;
    private final String mFragmentShaderSource;
    private int mPixelFormat = PIXEL_FORMAT_RGB565;
    private int mBytesPerPixel = 2;

    public GLVideoDevice(Context context) {
        mVertexShaderSource = onGetShaderSource(context, GL_VERTEX_SHADER);
        mFragmentShaderSource = onGetShaderSource(context, GL_FRAGMENT_SHADER);
    }

    @Override
    public void refresh(byte[] data, int width, int height, int pitch) {
        mVideoWidth = width;
        mVideoHeight = height;
        allocFrameBuffer();
        fillFrameBuffer(data, pitch);
    }

    @Override
    public void setPixelFormat(int format) {
        mPixelFormat = format;
        if (mPixelFormat == PIXEL_FORMAT_RGBA) {
            mBytesPerPixel = 4;
        }
    }

    public GLSurfaceView.Renderer getRenderer() {
        if (mRender == null)
            mRender = new RenderImpl();
        return mRender;
    }

    public void exportAsPNG(File file) {
        if (mFrameBuffer == null) return;
        final byte[] pixels;
        synchronized (mLock) {
            pixels = mFrameBuffer.array();
        }
        if (mPixelFormat == PIXEL_FORMAT_RGB565) {
            ImageUtils.saveAsPng(Bitmap.Config.RGB_565, pixels, mVideoWidth, mVideoHeight, file);
        } else {
            ImageUtils.saveAsPng(Bitmap.Config.ARGB_8888, pixels, mVideoWidth, mVideoHeight, file);
        }
    }

    private class RenderImpl implements GLSurfaceView.Renderer {
        private final Buffer mVertexesBuffer = ByteBuffer.allocateDirect(VERTEXES.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEXES)
                .position(0);
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private int mProgram;

        /*[VAO, VBO, Texture1]*/
        private final int[] mBuffers = new int[3];
        private volatile boolean mFirstRender = true;

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            createProgram();
            glGenVertexArrays(1, mBuffers, 0);
            glGenBuffers(1, mBuffers, 1);
            glBindVertexArray(mBuffers[0]);
            glBindBuffer(GL_ARRAY_BUFFER, mBuffers[1]);
            glBufferData(GL_ARRAY_BUFFER, VERTEXES.length * Float.BYTES, mVertexesBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            glEnableVertexAttribArray(1);
            glBindVertexArray(0);

            glGenTextures(1, mBuffers, 2);
            glBindTexture(GL_TEXTURE_2D, mBuffers[2]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glBindTexture(GL_TEXTURE_2D, 0);
            // Set the camera position (View matrix)
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            glViewport(0, 0, width, height);

            String v = SettingsManager.getString(VIDEO_RATIO);
            if (v.isEmpty() || v.equals("covered")) {
                Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -1, 1, 3, 7);
            } else {
                // this projection matrix is applied to object coordinates
                // in the onDrawFrame() method
                float ratio = (float) width / height;
                if (height > width) {
                    ratio = (float) height / width;
                    Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, 3, 7);
                } else {
                    Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
                }
            }
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            glClear(GL_COLOR_BUFFER_BIT);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, mBuffers[2]);
            int internalFormat = GL_RGB565;
            int format = GL_RGB;
            int type = GL_UNSIGNED_SHORT_5_6_5;
            if (mPixelFormat == PIXEL_FORMAT_RGBA) {
                internalFormat = GL_RGBA;
                format = GL_RGBA;
                type = GL_UNSIGNED_BYTE;
            }
            synchronized (mLock) {
                if (mFirstRender) {
                    glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, mVideoWidth, mVideoHeight, 0, format, type, mFrameBuffer);
                    mFirstRender = true;
                } else {
                    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, mVideoWidth, mVideoHeight, format, type, mFrameBuffer);
                }
            }
            glUseProgram(mProgram);
            glUniformMatrix4fv(glGetUniformLocation(mProgram, "projection"), 1, false, mProjectionMatrix, 0);
            glUniformMatrix4fv(glGetUniformLocation(mProgram, "view"), 1, false, mViewMatrix, 0);
            glBindVertexArray(mBuffers[0]);
            glDrawArrays(GL_TRIANGLES, 0, 6);
            glBindVertexArray(0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        private void createProgram() {
            int vertexShader = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vertexShader, mVertexShaderSource);
            glCompileShader(vertexShader);
            int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fragmentShader, mFragmentShaderSource);
            glCompileShader(fragmentShader);
            mProgram = glCreateProgram();
            glAttachShader(mProgram, vertexShader);
            glAttachShader(mProgram, fragmentShader);
            glLinkProgram(mProgram);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
        }
    }

    private void fillFrameBuffer(final byte[] data, final int pitch) {
        synchronized (mLock) {
            mFrameBuffer.rewind();
            for (int i = 0; i < mVideoHeight; ++i) {
                mFrameBuffer.put(data, i * pitch, mVideoWidth * mBytesPerPixel);
            }
            mFrameBuffer.flip();
        }
    }

    private void allocFrameBuffer() {
        int size = mVideoWidth * mVideoHeight * mBytesPerPixel;
        if (mFrameBuffer == null || mFrameBuffer.capacity() != size) {
            mFrameBuffer = ByteBuffer.allocateDirect(size)
                    .order(ByteOrder.nativeOrder());
            mFrameBuffer.position(0);
        }
    }

    private static final float[] VERTEXES = new float[]{
            // positions   // texCoords
            -1.0f, 1.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,

            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 1.0f
    };

    protected String onGetShaderSource(Context context, final int target) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final int resId;
        if (target == GL_VERTEX_SHADER) {
            resId = R.raw.default_vertex_shader;
        } else if (target == GL_FRAGMENT_SHADER) {
            resId = R.raw.default_frgment_shader;
        } else {
            throw new UnsupportedOperationException();
        }
        try (InputStream inputStream = context.getResources().openRawResource(resId)) {
            byte[] buffer = new byte[512];
            int readNumInBytes;
            while ((readNumInBytes = inputStream.read(buffer)) != -1) {
                if (readNumInBytes > 0) {
                    outputStream.write(buffer, 0, readNumInBytes);
                }
            }
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString();
    }
}
