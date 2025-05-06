package ink.snowland.wkuwku.device;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import ink.snowland.wkuwku.interfaces.EmAudioDevice;

public class AudioDevice implements EmAudioDevice {
    private AudioTrack mAudioTrack;
    private boolean mPlaying = false;
    @Override
    public void close() {
        if (mAudioTrack == null) return;
        mAudioTrack.release();
        mAudioTrack = null;
        mPlaying = false;
    }

    @Override
    public void open(int encoding, int sampleRate, int channels) {
        if (mAudioTrack != null) {
            throw new UnsupportedOperationException("Repeat operations are prohibited!");
        }
        if (encoding != PCM_16BIT) return;
        int channelMask = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setChannelMask(channelMask)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .build())
                .setBufferSizeInBytes(minBufferSize * 2)
                .build();
    }

    @Override
    public void play(short[] data, int frames) {
        if (mAudioTrack == null) return;
        if (!mPlaying) {
            mAudioTrack.play();
            mPlaying = true;
        }
        mAudioTrack.write(data, 0, data.length, AudioTrack.WRITE_NON_BLOCKING);
    }

    @Override
    public void pause() {
        if (!mPlaying) return;
        mAudioTrack.pause();
        mPlaying = false;
    }

    @Override
    public boolean isOpen() {
        return mAudioTrack != null;
    }
}