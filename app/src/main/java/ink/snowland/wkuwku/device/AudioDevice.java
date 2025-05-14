package ink.snowland.wkuwku.device;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.interfaces.EmAudioDevice;

public class AudioDevice implements EmAudioDevice, AudioManager.OnAudioFocusChangeListener {
    private AudioTrack mAudioTrack;
    private boolean mPlaying = false;
    private boolean mFocusGranted = false;
    private AudioAttributes mAudioAttributes;
    private AudioManager mAudioManager;
    private AudioFocusRequest mFocusRequest = null;
    public AudioDevice(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mAudioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }
    }

    @Override
    public void close() {
        if (mAudioTrack == null) return;
        mAudioTrack.release();
        mAudioTrack = null;
        mPlaying = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        }
    }

    @Override
    public void open(int encoding, int sampleRate, int channels) {
        if (mAudioTrack != null)
            return;
        if (encoding != PCM_16BIT) return;
        int channelMask = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack.Builder()
                .setAudioAttributes(mAudioAttributes)
                .setAudioFormat(new AudioFormat.Builder()
                        .setChannelMask(channelMask)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .build())
                .setBufferSizeInBytes(minBufferSize)
                .build();
    }

    @Override
    public void play(short[] data, int frames) {
        if (mAudioTrack == null) return;
        if (!mFocusGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFocusGranted = mAudioManager.requestAudioFocus(mFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            mFocusGranted = true;
        }
        if (mFocusGranted) {
            if (!mPlaying) {
                mAudioTrack.play();
                mPlaying = true;
            }
            mAudioTrack.write(data, 0, data.length);
        }
    }

    @Override
    public void pause() {
        if (!mPlaying) return;
        mAudioTrack.pause();
        mPlaying = false;
        if (mFocusGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        }
    }

    @Override
    public boolean isOpen() {
        return mAudioTrack != null;
    }

    @Override
    public int getSampleRate() {
        if (mAudioTrack != null) {
            return mAudioTrack.getSampleRate();
        }
        return 0;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        mFocusGranted = focusChange == AudioManager.AUDIOFOCUS_GAIN;
    }
}