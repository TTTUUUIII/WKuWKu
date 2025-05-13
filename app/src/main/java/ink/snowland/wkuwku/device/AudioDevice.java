package ink.snowland.wkuwku.device;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

import ink.snowland.wkuwku.interfaces.EmAudioDevice;

public class AudioDevice implements EmAudioDevice {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private AudioTrack mAudioTrack;
    private boolean mPlaying = false;
    @Override
    public void close() {
        if (mAudioTrack == null) return;
        mHandler.post(() -> {
            if (mAudioTrack != null) {
                mAudioTrack.release();
                mAudioTrack = null;
                mPlaying = false;
            }
        });
    }

    @Override
    public void open(int encoding, int sampleRate, int channels) {
        if (mAudioTrack != null)
            return;
        if (encoding != PCM_16BIT) return;
        int channelMask = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
        mHandler.post(() -> {
            mAudioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setChannelMask(channelMask)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .build())
                    .setBufferSizeInBytes(minBufferSize)
                    .build();
        });
    }

    @Override
    public void play(short[] data, int frames) {
        if (mAudioTrack == null) return;
        mHandler.post(() -> {
            if (!mPlaying) {
                mAudioTrack.play();
                mPlaying = true;
            }
            mAudioTrack.write(data, 0, data.length, AudioTrack.WRITE_NON_BLOCKING);
        });
    }

    @Override
    public void pause() {
        if (!mPlaying) return;
        mHandler.post(() -> {
            if (mPlaying) {
                mAudioTrack.pause();
                mPlaying = false;
            }
        });
    }

    @Override
    public boolean isOpen() {
        return mAudioTrack != null;
    }
}