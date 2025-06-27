package com.outlook.wn123o.retrosystem;

import android.view.Surface;

import androidx.annotation.NonNull;

public class RetroSystem {
    static {
        System.loadLibrary("retrosystem");
    }

    /**
     * add a retro core lib from the path, and set an alias.
     * @param alias   unique alias
     * @param path  core path
     * @return true if loaded successful.
     */
    private static native void nativeAdd(@NonNull String alias, @NonNull String path);

    /**
     * Switch current core (from core list of loaded).
     * @param alias   unique alias
     * @return true if switch success
     */
    private static native boolean nativeUse(@NonNull String alias);

    /**
     * Attach a valid surface for render to display video content.
     * @param surface valid surface
     * @return
     */
    private static native boolean nativeAttachSurface(@NonNull Surface surface);

    /**
     * Tell render surface size changed.
     * @param vw width
     * @param vh height
     */
    private static native void nativeAdjustSurface(int vw, int vh);

    /**
     * When surface destroyed, it should been detach from render.
     */
    private static native void nativeDetachSurface();

    /**
     * Use current core launch the game.
     * @param path  The game's path
     * @return true if no error happen
     */
    private static native boolean nativeStart(@NonNull String path);

    /**
     * If current have running game, so it will been paused.
     */
    private static native void nativePause();

    /**
     * If current have paused game, so it will been resumed.
     */
    private static native void nativeResume();

    /**
     * Unload current game (whether it is running or paused).
     */
    private static native void nativeStop();

    private static boolean onNativeEnvironmentCallback(int cmd, Object data) {
        return false;
    }

    private static int onNativeAudioCallback(final short[] data, int frames) {
        return frames;
    }

    private static int onNativeInputCallback(int port, int device, int index, int id) {
        return 0;
    }

    private static void onNativeInputPollCallback() {

    }

    private static boolean onNativeRumbleCallback(int port, int effect, int strength) {
        return true;
    }
}