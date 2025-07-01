package com.outlook.wn123o.retrosystem.common;

public class SystemInfo {
    /* All pointers are owned by libretro implementation, and pointers must
     * remain valid until it is unloaded. */

    public final String name;      /* Descriptive name of library. Should not
     * contain any version numbers, etc. */
    public final String version;   /* Descriptive version of core. */

    public final String validExtensions;  /* A string listing probably content
     * extensions the core will be able to
     * load, separated with pipe.
     * I.e. "bin|rom|iso".
     * Typically used for a GUI to filter
     * out extensions. */

    /* Libretro cores that need to have direct access to their content
     * files, including cores which use the path of the content files to
     * determine the paths of other files, should set need_fullpath to true.
     *
     * Cores should strive for setting need_fullpath to false,
     * as it allows the frontend to perform patching, etc.
     *
     * If need_fullpath is true and retro_load_game() is called:
     *    - retro_game_info::path is guaranteed to have a valid path
     *    - retro_game_info::data and retro_game_info::size are invalid
     *
     * If need_fullpath is false and retro_load_game() is called:
     *    - retro_game_info::path may be NULL
     *    - retro_game_info::data and retro_game_info::size are guaranteed
     *      to be valid
     *
     * See also:
     *    - RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY
     *    - RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY
     */
    public boolean needFullpath;

    /* If true, the frontend is not allowed to extract any archives before
     * loading the real content.
     * Necessary for certain libretro implementations that load games
     * from zipped archives. */
    public boolean blockExtract;

    public SystemInfo(String name, String version, String validExtensions) {
        this.name = name;
        this.version = version;
        this.validExtensions = validExtensions;
    }
}
