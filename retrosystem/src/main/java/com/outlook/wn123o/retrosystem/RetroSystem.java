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
    private static native void add(@NonNull String alias, @NonNull String path);

    /**
     * Switch current core (from core list of loaded).
     * @param alias   unique alias
     * @return true if switch success
     */
    private static native boolean use(@NonNull String alias);

    /**
     * Attach a valid surface for render to display video content.
     * @param surface valid surface
     * @return
     */
    private static native boolean attachSurface(@NonNull Surface surface);

    /**
     * Tell render surface size changed.
     * @param vw width
     * @param vh height
     */
    private static native void adjustSurface(int vw, int vh);

    /**
     * When surface destroyed, it should been detach from render.
     */
    private static native void detachSurface();

    /**
     * Use current core launch the game.
     * @param path  The game's path
     * @return true if no error happen
     */
    private static native boolean start(@NonNull String path);

    /**
     * If current have running game, so it will been paused.
     */
    private static native void pause();

    /**
     * If current have paused game, so it will been resumed.
     */
    private static native void resume();

    /**
     * Unload current game (whether it is running or paused).
     */
    private static native void stop();
}