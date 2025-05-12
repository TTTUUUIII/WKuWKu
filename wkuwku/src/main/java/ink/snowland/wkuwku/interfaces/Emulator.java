package ink.snowland.wkuwku.interfaces;


import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.annotations.CallFromJni;
import ink.snowland.wkuwku.common.EmConfig;
import ink.snowland.wkuwku.common.EmMessageExt;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmScheduledThread;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.common.Variable;
import ink.snowland.wkuwku.common.VariableEntry;

public abstract class Emulator {

    public static final int RETRO_ENVIRONMENT_EXPERIMENTAL = 0x10000;
    public static final int RETRO_ENVIRONMENT_SET_ROTATION = 1;
    /* const unsigned * --
     * Sets screen rotation of graphics.
     * Valid values are 0, 1, 2, 3, which rotates screen by 0, 90, 180,
     * 270 degrees counter-clockwise respectively.
     */
    public static final int RETRO_ENVIRONMENT_GET_OVERSCAN = 2;
    /* bool * --
     * NOTE: As of 2019 this callback is considered deprecated in favor of
     * using core options to manage overscan in a more nuanced, core-specific way.
     *
     * Boolean value whether or not the implementation should use overscan,
     * or crop away overscan.
     */
    public static final int RETRO_ENVIRONMENT_GET_CAN_DUPE = 3;
    /* bool * --
     * Boolean value whether or not frontend supports frame duping,
     * passing NULL to video frame callback.
     */

    /* Environ 4, 5 are no longer supported (GET_VARIABLE / SET_VARIABLES),
     * and reserved to avoid possible ABI clash.
     */

    public static final int RETRO_ENVIRONMENT_SET_MESSAGE = 6;
    /* const struct retro_message * --
     * Sets a message to be displayed in implementation-specific manner
     * for a certain amount of 'frames'.
     * Should not be used for trivial messages, which should simply be
     * logged via RETRO_ENVIRONMENT_GET_LOG_INTERFACE (or as a
     * fallback, stderr).
     */
    public static final int RETRO_ENVIRONMENT_SHUTDOWN = 7;
    /* N/A (NULL) --
     * Requests the frontend to shutdown.
     * Should only be used if game has a specific
     * way to shutdown the game from a menu item or similar.
     */
    public static final int RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL = 8;
    /* const unsigned * --
     * Gives a hpublic static final int to the frontend how demanding this implementation
     * is on a system. E.g. reporting a level of 2 means
     * this implementation should run decently on all frontends
     * of level 2 and up.
     *
     * It can be used by the frontend to potentially warn
     * about too demanding implementations.
     *
     * The levels are "floating".
     *
     * This function can be called on a per-game basis,
     * as certain games an implementation can play might be
     * particularly demanding.
     * If called, it should be called in retro_load_game().
     */
    public static final int RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9;
    /* const char ** --
     * Returns the "system" directory of the frontend.
     * This directory can be used to store system specific
     * content such as BIOSes, configuration data, etc.
     * The returned value can be NULL.
     * If so, no such directory is defined,
     * and it's up to the implementation to find a suitable directory.
     *
     * NOTE: Some cores used this folder also for "save" data such as
     * memory cards, etc, for lack of a better place to put it.
     * This is now discouraged, and if possible, cores should try to
     * use the new GET_SAVE_DIRECTORY.
     */
    public static final int RETRO_ENVIRONMENT_SET_PIXEL_FORMAT = 10;
    /* const enum retro_pixel_format * --
     * Sets the internal pixel format used by the implementation.
     * The default pixel format is RETRO_PIXEL_FORMAT_0RGB1555.
     * This pixel format however, is deprecated (see enum retro_pixel_format).
     * If the call returns false, the frontend does not support this pixel
     * format.
     *
     * This function should be called inside retro_load_game() or
     * retro_get_system_av_info().
     */
    public static final int RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS = 11;
    /* const struct retro_input_descriptor * --
     * Sets an array of retro_input_descriptors.
     * It is up to the frontend to present this in a usable way.
     * The array is terminated by retro_input_descriptor::description
     * being set to NULL.
     * This function can be called at any time, but it is recommended
     * to call it as early as possible.
     */
    public static final int RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK = 12;
    /* const struct retro_keyboard_callback * --
     * Sets a callback function used to notify core about keyboard events.
     */
    public static final int RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE = 13;
    /* const struct retro_disk_control_callback * --
     * Sets an interface which frontend can use to eject and insert
     * disk images.
     * This is used for games which consist of multiple images and
     * must be manually swapped out by the user (e.g. PSX).
     */
    public static final int RETRO_ENVIRONMENT_SET_HW_RENDER = 14;
    /* struct retro_hw_render_callback * --
     * Sets an interface to let a libretro core render with
     * hardware acceleration.
     * Should be called in retro_load_game().
     * If successful, libretro cores will be able to render to a
     * frontend-provided framebuffer.
     * The size of this framebuffer will be at least as large as
     * max_width/max_height provided in get_av_info().
     * If HW rendering is used, pass only RETRO_HW_FRAME_BUFFER_VALID or
     * NULL to retro_video_refresh_t.
     */
    public static final int RETRO_ENVIRONMENT_GET_VARIABLE = 15;
    /* struct retro_variable * --
     * Interface to acquire user-defined information from environment
     * that cannot feasibly be supported in a multi-system way.
     * 'key' should be set to a key which has already been set by
     * SET_VARIABLES.
     * 'data' will be set to a value or NULL.
     */
    public static final int RETRO_ENVIRONMENT_SET_VARIABLES = 16;
    /* const struct retro_variable * --
     * Allows an implementation to signal the environment
     * which variables it might want to check for later using
     * GET_VARIABLE.
     * This allows the frontend to present these variables to
     * a user dynamically.
     * This should be called the first time as early as
     * possible (ideally in retro_set_environment).
     * Afterward it may be called again for the core to communicate
     * updated options to the frontend, but the number of core
     * options must not change from the number in the initial call.
     *
     * 'data' points to an array of retro_variable structs
     * terminated by a { NULL, NULL } element.
     * retro_variable::key should be namespaced to not collide
     * with other implementations' keys. E.g. A core called
     * 'foo' should use keys named as 'foo_option'.
     * retro_variable::value should contain a human readable
     * description of the key as well as a '|' delimited list
     * of expected values.
     *
     * The number of possible options should be very limited,
     * i.e. it should be feasible to cycle through options
     * without a keyboard.
     *
     * First entry should be treated as a default.
     *
     * Example entry:
     * { "foo_option", "Speed hack coprocessor X; false|true" }
     *
     * Text before first ';' is description. This ';' must be
     * followed by a space, and followed by a list of possible
     * values split up with '|'.
     *
     * Only strings are operated on. The possible values will
     * generally be displayed and stored as-is by the frontend.
     */
    public static final int RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE = 17;
    /* bool * --
     * Result is set to true if some variables are updated by
     * frontend since last call to RETRO_ENVIRONMENT_GET_VARIABLE.
     * Variables should be queried with GET_VARIABLE.
     */
    public static final int RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME = 18;
    /* const bool * --
     * If true, the libretro implementation supports calls to
     * retro_load_game() with NULL as argument.
     * Used by cores which can run without particular game data.
     * This should be called within retro_set_environment() only.
     */
    public static final int RETRO_ENVIRONMENT_GET_LIBRETRO_PATH = 19;
    /* const char ** --
     * Retrieves the absolute path from where this libretro
     * implementation was loaded.
     * NULL is returned if the libretro was loaded statically
     * (i.e. linked statically to frontend), or if the path cannot be
     * determined.
     * Mostly useful in cooperation with SET_SUPPORT_NO_GAME as assets can
     * be loaded without ugly hacks.
     */

    /* Environment 20 was an obsolete version of SET_AUDIO_CALLBACK.
     * It was not used by any known core at the time,
     * and was removed from the API. */
    public static final int RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK = 21;
    /* const struct retro_frame_time_callback * --
     * Lets the core know how much time has passed since last
     * invocation of retro_run().
     * The frontend can tamper with the timing to fake fast-forward,
     * slow-motion, frame stepping, etc.
     * In this case the delta time will use the reference value
     * in frame_time_callback..
     */
    public static final int RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK = 22;
    /* const struct retro_audio_callback * --
     * Sets an interface which is used to notify a libretro core about audio
     * being available for writing.
     * The callback can be called from any thread, so a core using this must
     * have a thread safe audio implementation.
     * It is intended for games where audio and video are completely
     * asynchronous and audio can be generated on the fly.
     * This interface is not recommended for use with emulators which have
     * highly synchronous audio.
     *
     * The callback only notifies about writability; the libretro core still
     * has to call the normal audio callbacks
     * to write audio. The audio callbacks must be called from within the
     * notification callback.
     * The amount of audio data to write is up to the implementation.
     * Generally, the audio callback will be called continously in a loop.
     *
     * Due to thread safety guarantees and lack of sync between audio and
     * video, a frontend  can selectively disallow this interface based on
     * internal configuration. A core using this interface must also
     * implement the "normal" audio interface.
     *
     * A libretro core using SET_AUDIO_CALLBACK should also make use of
     * SET_FRAME_TIME_CALLBACK.
     */
    public static final int RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE = 23;
    /* struct retro_rumble_interface * --
     * Gets an interface which is used by a libretro core to set
     * state of rumble motors in controllers.
     * A strong and weak motor is supported, and they can be
     * controlled indepedently.
     * Should be called from either retro_init() or retro_load_game().
     * Should not be called from retro_set_environment().
     * Returns false if rumble functionality is unavailable.
     */
    public static final int RETRO_ENVIRONMENT_GET_INPUT_DEVICE_CAPABILITIES = 24;
    /* uint64_t * --
     * Gets a bitmask telling which device type are expected to be
     * handled properly in a call to retro_input_state_t.
     * Devices which are not handled or recognized always return
     * 0 in retro_input_state_t.
     * Example bitmask: caps = (1 << RETRO_DEVICE_JOYPAD) | (1 << RETRO_DEVICE_ANALOG).
     * Should only be called in retro_run().
     */
    public static final int RETRO_ENVIRONMENT_GET_SENSOR_INTERFACE = (25 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_sensor_interface * --
     * Gets access to the sensor interface.
     * The purpose of this interface is to allow
     * setting state related to sensors such as polling rate,
     * enabling/disable it entirely, etc.
     * Reading sensor state is done via the normal
     * input_state_callback API.
     */
    public static final int RETRO_ENVIRONMENT_GET_CAMERA_INTERFACE = (26 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_camera_callback * --
     * Gets an interface to a video camera driver.
     * A libretro core can use this interface to get access to a
     * video camera.
     * New video frames are delivered in a callback in same
     * thread as retro_run().
     *
     * GET_CAMERA_INTERFACE should be called in retro_load_game().
     *
     * Depending on the camera implementation used, camera frames
     * will be delivered as a raw framebuffer,
     * or as an OpenGL texture directly.
     *
     * The core has to tell the frontend here which types of
     * buffers can be handled properly.
     * An OpenGL texture can only be handled when using a
     * libretro GL core (SET_HW_RENDER).
     * It is recommended to use a libretro GL core when
     * using camera interface.
     *
     * The camera is not started automatically. The retrieved start/stop
     * functions must be used to explicitly
     * start and stop the camera driver.
     */
    public static final int RETRO_ENVIRONMENT_GET_LOG_INTERFACE = 27;
    /* struct retro_log_callback * --
     * Gets an interface for logging. This is useful for
     * logging in a cross-platform way
     * as certain platforms cannot use stderr for logging.
     * It also allows the frontend to
     * show logging information in a more suitable way.
     * If this interface is not used, libretro cores should
     * log to stderr as desired.
     */
    public static final int RETRO_ENVIRONMENT_GET_PERF_INTERFACE = 28;
    /* struct retro_perf_callback * --
     * Gets an interface for performance counters. This is useful
     * for performance logging in a cross-platform way and for detecting
     * architecture-specific features, such as SIMD support.
     */
    public static final int RETRO_ENVIRONMENT_GET_LOCATION_INTERFACE = 29;
    /* struct retro_location_callback * --
     * Gets access to the location interface.
     * The purpose of this interface is to be able to retrieve
     * location-based information from the host device,
     * such as current latitude / longitude.
     */
    public static final int RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY = 30;
    /* Old name, kept for compatibility. */
    public static final int RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY = 30;
    /* const char ** --
     * Returns the "core assets" directory of the frontend.
     * This directory can be used to store specific assets that the
     * core relies upon, such as art assets,
     * input data, etc etc.
     * The returned value can be NULL.
     * If so, no such directory is defined,
     * and it's up to the implementation to find a suitable directory.
     */
    public static final int RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY = 31;
    /* const char ** --
     * Returns the "save" directory of the frontend, unless there is no
     * save directory available. The save directory should be used to
     * store SRAM, memory cards, high scores, etc, if the libretro core
     * cannot use the regular memory interface (retro_get_memory_data()).
     *
     * If the frontend cannot designate a save directory, it will return
     * NULL to indicate that the core should attempt to operate without a
     * save directory set.
     *
     * NOTE: early libretro cores used the system directory for save
     * files. Cores that need to be backwards-compatible can still check
     * GET_SYSTEM_DIRECTORY.
     */
    public static final int RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO = 32;
    /* const struct retro_system_av_info * --
     * Sets a new av_info structure. This can only be called from
     * within retro_run().
     * This should *only* be used if the core is completely altering the
     * internal resolutions, aspect ratios, timings, sampling rate, etc.
     * Calling this can require a full reinitialization of video/audio
     * drivers in the frontend,
     *
     * so it is important to call it very sparingly, and usually only with
     * the users explicit consent.
     * An eventual driver reinitialize will happen so that video and
     * audio callbacks
     * happening after this call within the same retro_run() call will
     * target the newly initialized driver.
     *
     * This callback makes it possible to support configurable resolutions
     * in games, which can be useful to
     * avoid setting the "worst case" in max_width/max_height.
     *
     * ***HIGHLY RECOMMENDED*** Do not call this callback every time
     * resolution changes in an emulator core if it's
     * expected to be a temporary change, for the reasons of possible
     * driver reinitialization.
     * This call is not a free pass for not trying to provide
     * correct values in retro_get_system_av_info(). If you need to change
     * things like aspect ratio or nominal width/height,
     * use RETRO_ENVIRONMENT_SET_GEOMETRY, which is a softer variant
     * of SET_SYSTEM_AV_INFO.
     *
     * If this returns false, the frontend does not acknowledge a
     * changed av_info struct.
     */
    public static final int RETRO_ENVIRONMENT_SET_PROC_ADDRESS_CALLBACK = 33;
    /* const struct retro_get_proc_address_interface * --
     * Allows a libretro core to announce support for the
     * get_proc_address() interface.
     * This interface allows for a standard way to extend libretro where
     * use of environment calls are too indirect,
     * e.g. for cases where the frontend wants to call directly into the core.
     *
     * If a core wants to expose this interface, SET_PROC_ADDRESS_CALLBACK
     * **MUST** be called from within retro_set_environment().
     */
    public static final int RETRO_ENVIRONMENT_SET_SUBSYSTEM_INFO = 34;
    /* const struct retro_subsystem_info * --
     * This environment call introduces the concept of libretro "subsystems".
     * A subsystem is a variant of a libretro core which supports
     * different kinds of games.
     * The purpose of this is to support e.g. emulators which might
     * have special needs, e.g. Super Nintendo's Super GameBoy, Sufami Turbo.
     * It can also be used to pick among subsystems in an explicit way
     * if the libretro implementation is a multi-system emulator itself.
     *
     * Loading a game via a subsystem is done with retro_load_game_special(),
     * and this environment call allows a libretro core to expose which
     * subsystems are supported for use with retro_load_game_special().
     * A core passes an array of retro_game_special_info which is terminated
     * with a zeroed out retro_game_special_info struct.
     *
     * If a core wants to use this functionality, SET_SUBSYSTEM_INFO
     * **MUST** be called from within retro_set_environment().
     */
    public static final int RETRO_ENVIRONMENT_SET_CONTROLLER_INFO = 35;
    /* const struct retro_controller_info * --
     * This environment call lets a libretro core tell the frontend
     * which controller subclasses are recognized in calls to
     * retro_set_controller_port_device().
     *
     * Some emulators such as Super Nintendo support multiple lightgun
     * types which must be specifically selected from. It is therefore
     * sometimes necessary for a frontend to be able to tell the core
     * about a special kind of input device which is not specifcally
     * provided by the Libretro API.
     *
     * In order for a frontend to understand the workings of those devices,
     * they must be defined as a specialized subclass of the generic device
     * types already defined in the libretro API.
     *
     * The core must pass an array of const struct retro_controller_info which
     * is terminated with a blanked out struct. Each element of the
     * retro_controller_info struct corresponds to the ascending port index
     * that is passed to retro_set_controller_port_device() when that function
     * is called to indicate to the core that the frontend has changed the
     * active device subclass. SEE ALSO: retro_set_controller_port_device()
     *
     * The ascending input port indexes provided by the core in the struct
     * are generally presented by frontends as ascending User # or Player #,
     * such as Player 1, Player 2, Player 3, etc. Which device subclasses are
     * supported can vary per input port.
     *
     * The first inner element of each entry in the retro_controller_info array
     * is a retro_controller_description struct that specifies the names and
     * codes of all device subclasses that are available for the corresponding
     * User or Player, beginning with the generic Libretro device that the
     * subclasses are derived from. The second inner element of each entry is the
     * total number of subclasses that are listed in the retro_controller_description.
     *
     * NOTE: Even if special device types are set in the libretro core,
     * libretro should only poll input based on the base input device types.
     */
    public static final int RETRO_ENVIRONMENT_SET_MEMORY_MAPS = (36 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* const struct retro_memory_map * --
     * This environment call lets a libretro core tell the frontend
     * about the memory maps this core emulates.
     * This can be used to implement, for example, cheats in a core-agnostic way.
     *
     * Should only be used by emulators; it doesn't make much sense for
     * anything else.
     * It is recommended to expose all relevant pointers through
     * retro_get_memory_* as well.
     */
    public static final int RETRO_ENVIRONMENT_SET_GEOMETRY = 37;
    /* const struct retro_game_geometry * --
     * This environment call is similar to SET_SYSTEM_AV_INFO for changing
     * video parameters, but provides a guarantee that drivers will not be
     * reinitialized.
     * This can only be called from within retro_run().
     *
     * The purpose of this call is to allow a core to alter nominal
     * width/heights as well as aspect ratios on-the-fly, which can be
     * useful for some emulators to change in run-time.
     *
     * max_width/max_height arguments are ignored and cannot be changed
     * with this call as this could potentially require a reinitialization or a
     * non-constant time operation.
     * If max_width/max_height are to be changed, SET_SYSTEM_AV_INFO is required.
     *
     * A frontend must guarantee that this environment call completes in
     * constant time.
     */
    public static final int RETRO_ENVIRONMENT_GET_USERNAME = 38;
    /* const char **
     * Returns the specified username of the frontend, if specified by the user.
     * This username can be used as a nickname for a core that has online facilities
     * or any other mode where personalization of the user is desirable.
     * The returned value can be NULL.
     * If this environ callback is used by a core that requires a valid username,
     * a default username should be specified by the core.
     */
    public static final int RETRO_ENVIRONMENT_GET_LANGUAGE = 39;
    /* unsigned * --
     * Returns the specified language of the frontend, if specified by the user.
     * It can be used by the core for localization purposes.
     */
    public static final int RETRO_ENVIRONMENT_GET_CURRENT_SOFTWARE_FRAMEBUFFER = (40 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_framebuffer * --
     * Returns a preallocated framebuffer which the core can use for rendering
     * the frame into when not using SET_HW_RENDER.
     * The framebuffer returned from this call must not be used
     * after the current call to retro_run() returns.
     *
     * The goal of this call is to allow zero-copy behavior where a core
     * can render directly into video memory, avoiding extra bandwidth cost by copying
     * memory from core to video memory.
     *
     * If this call succeeds and the core renders into it,
     * the framebuffer pointer and pitch can be passed to retro_video_refresh_t.
     * If the buffer from GET_CURRENT_SOFTWARE_FRAMEBUFFER is to be used,
     * the core must pass the exact
     * same pointer as returned by GET_CURRENT_SOFTWARE_FRAMEBUFFER;
     * i.e. passing a pointer which is offset from the
     * buffer is undefined. The width, height and pitch parameters
     * must also match exactly to the values obtained from GET_CURRENT_SOFTWARE_FRAMEBUFFER.
     *
     * It is possible for a frontend to return a different pixel format
     * than the one used in SET_PIXEL_FORMAT. This can happen if the frontend
     * needs to perform conversion.
     *
     * It is still valid for a core to render to a different buffer
     * even if GET_CURRENT_SOFTWARE_FRAMEBUFFER succeeds.
     *
     * A frontend must make sure that the pointer obtained from this function is
     * writeable (and readable).
     */
    public static final int RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE = (41 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* const struct retro_hw_render_interface ** --
     * Returns an API specific rendering interface for accessing API specific data.
     * Not all HW rendering APIs support or need this.
     * The contents of the returned pointer is specific to the rendering API
     * being used. See the various headers like libretro_vulkan.h, etc.
     *
     * GET_HW_RENDER_INTERFACE cannot be called before context_reset has been called.
     * Similarly, after context_destroyed callback returns,
     * the contents of the HW_RENDER_INTERFACE are invalidated.
     */
    public static final int RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS = (42 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* const bool * --
     * If true, the libretro implementation supports achievements
     * either via memory descriptors set with RETRO_ENVIRONMENT_SET_MEMORY_MAPS
     * or via retro_get_memory_data/retro_get_memory_size.
     *
     * This must be called before the first call to retro_run.
     */
    public static final int RETRO_ENVIRONMENT_SET_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE = 43 | (RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* const struct retro_hw_render_context_negotiation_interface * --
     * Sets an interface which lets the libretro core negotiate with frontend how a context is created.
     * The semantics of this interface depends on which API is used in SET_HW_RENDER earlier.
     * This interface will be used when the frontend is trying to create a HW rendering context,
     * so it will be used after SET_HW_RENDER, but before the context_reset callback.
     */
    public static final int RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS = 44;
    /* uint64_t * --
     * Sets quirk flags associated with serialization. The frontend will zero any flags it doesn't
     * recognize or support. Should be set in either retro_init or retro_load_game, but not both.
     */
    public static final int RETRO_ENVIRONMENT_SET_HW_SHARED_CONTEXT = (44 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* N/A (null) * --
     * The frontend will try to use a 'shared' hardware context (mostly applicable
     * to OpenGL) when a hardware context is being set up.
     *
     * Returns true if the frontend supports shared hardware contexts and false
     * if the frontend does not support shared hardware contexts.
     *
     * This will do nothing on its own until SET_HW_RENDER env callbacks are
     * being used.
     */
    public static final int RETRO_ENVIRONMENT_GET_VFS_INTERFACE = (45 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_vfs_interface_info * --
     * Gets access to the VFS interface.
     * VFS presence needs to be queried prior to load_game or any
     * get_system/save/other_directory being called to let front end know
     * core supports VFS before it starts handing out paths.
     * It is recomended to do so in retro_set_environment
     */
    public static final int RETRO_ENVIRONMENT_GET_LED_INTERFACE = (46 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_led_interface * --
     * Gets an interface which is used by a libretro core to set
     * state of LEDs.
     */
    public static final int RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE = (47 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* public static final int * --
     * Tells the core if the frontend wants audio or video.
     * If disabled, the frontend will discard the audio or video,
     * so the core may decide to skip generating a frame or generating audio.
     * This is mainly used for increasing performance.
     * Bit 0 (value 1): Enable Video
     * Bit 1 (value 2): Enable Audio
     * Bit 2 (value 4): Use Fast Savestates.
     * Bit 3 (value 8): Hard Disable Audio
     * Other bits are reserved for future use and will default to zero.
     * If video is disabled:
     * * The frontend wants the core to not generate any video,
     *   including presenting frames via hardware acceleration.
     * * The frontend's video frame callback will do nothing.
     * * After running the frame, the video output of the next frame should be
     *   no different than if video was enabled, and saving and loading state
     *   should have no issues.
     * If audio is disabled:
     * * The frontend wants the core to not generate any audio.
     * * The frontend's audio callbacks will do nothing.
     * * After running the frame, the audio output of the next frame should be
     *   no different than if audio was enabled, and saving and loading state
     *   should have no issues.
     * Fast Savestates:
     * * Guaranteed to be created by the same binary that will load them.
     * * Will not be written to or read from the disk.
     * * Suggest that the core assumes loading state will succeed.
     * * Suggest that the core updates its memory buffers in-place if possible.
     * * Suggest that the core skips clearing memory.
     * * Suggest that the core skips resetting the system.
     * * Suggest that the core may skip validation steps.
     * Hard Disable Audio:
     * * Used for a secondary core when running ahead.
     * * Indicates that the frontend will never need audio from the core.
     * * Suggests that the core may stop synthesizing audio, but this should not
     *   compromise emulation accuracy.
     * * Audio output for the next frame does not matter, and the frontend will
     *   never need an accurate audio state in the future.
     * * State will never be saved when using Hard Disable Audio.
     */
    public static final int RETRO_ENVIRONMENT_GET_MIDI_INTERFACE = (48 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_midi_interface ** --
     * Returns a MIDI interface that can be used for raw data I/O.
     */

    public static final int RETRO_ENVIRONMENT_GET_FASTFORWARDING = (49 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* bool * --
     * Boolean value that indicates whether or not the frontend is in
     * fastforwarding mode.
     */

    public static final int RETRO_ENVIRONMENT_GET_TARGET_REFRESH_RATE = (50 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* float * --
     * Float value that lets us know what target refresh rate
     * is curently in use by the frontend.
     *
     * The core can use the returned value to set an ideal
     * refresh rate/framerate.
     */

    public static final int RETRO_ENVIRONMENT_GET_INPUT_BITMASKS = (51 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* bool * --
     * Boolean value that indicates whether or not the frontend supports
     * input bitmasks being returned by retro_input_state_t. The advantage
     * of this is that retro_input_state_t has to be only called once to
     * grab all button states instead of multiple times.
     *
     * If it returns true, you can pass RETRO_DEVICE_ID_JOYPAD_MASK as 'id'
     * to retro_input_state_t (make sure 'device' is set to RETRO_DEVICE_JOYPAD).
     * It will return a bitmask of all the digital buttons.
     */

    public static final int RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION = 52;
    /* unsigned * --
     * Unsigned value is the API version number of the core options
     * interface supported by the frontend. If callback return false,
     * API version is assumed to be 0.
     *
     * In legacy code, core options are set by passing an array of
     * retro_variable structs to RETRO_ENVIRONMENT_SET_VARIABLES.
     * This may be still be done regardless of the core options
     * interface version.
     *
     * If version is >= 1 however, core options may instead be set by
     * passing an array of retro_core_option_definition structs to
     * RETRO_ENVIRONMENT_SET_CORE_OPTIONS, or a 2D array of
     * retro_core_option_definition structs to RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL.
     * This allows the core to additionally set option sublabel information
     * and/or provide localisation support.
     *
     * If version is >= 2, core options may instead be set by passing
     * a retro_core_options_v2 struct to RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2,
     * or an array of retro_core_options_v2 structs to
     * RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL. This allows the core
     * to additionally set optional core option category information
     * for frontends with core option category support.
     */

    public static final int RETRO_ENVIRONMENT_SET_CORE_OPTIONS = 53;
    /* const struct retro_core_option_definition ** --
     * Allows an implementation to signal the environment
     * which variables it might want to check for later using
     * GET_VARIABLE.
     * This allows the frontend to present these variables to
     * a user dynamically.
     * This should only be called if RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION
     * returns an API version of >= 1.
     * This should be called instead of RETRO_ENVIRONMENT_SET_VARIABLES.
     * This should be called the first time as early as
     * possible (ideally in retro_set_environment).
     * Afterwards it may be called again for the core to communicate
     * updated options to the frontend, but the number of core
     * options must not change from the number in the initial call.
     *
     * 'data' points to an array of retro_core_option_definition structs
     * terminated by a { NULL, NULL, NULL, {{0}}, NULL } element.
     * retro_core_option_definition::key should be namespaced to not collide
     * with other implementations' keys. e.g. A core called
     * 'foo' should use keys named as 'foo_option'.
     * retro_core_option_definition::desc should contain a human readable
     * description of the key.
     * retro_core_option_definition::info should contain any additional human
     * readable information text that a typical user may need to
     * understand the functionality of the option.
     * retro_core_option_definition::values is an array of retro_core_option_value
     * structs terminated by a { NULL, NULL } element.
     * > retro_core_option_definition::values[index].value is an expected option
     *   value.
     * > retro_core_option_definition::values[index].label is a human readable
     *   label used when displaying the value on screen. If NULL,
     *   the value itself is used.
     * retro_core_option_definition::default_value is the default core option
     * setting. It must match one of the expected option values in the
     * retro_core_option_definition::values array. If it does not, or the
     * default value is NULL, the first entry in the
     * retro_core_option_definition::values array is treated as the default.
     *
     * The number of possible option values should be very limited,
     * and must be less than RETRO_NUM_CORE_OPTION_VALUES_MAX.
     * i.e. it should be feasible to cycle through options
     * without a keyboard.
     *
     * Example entry:
     * {
     *     "foo_option",
     *     "Speed hack coprocessor X",
     *     "Provides increased performance at the expense of reduced accuracy",
     * 	  {
     *         { "false",    NULL },
     *         { "true",     NULL },
     *         { "unstable", "Turbo (Unstable)" },
     *         { NULL, NULL },
     *     },
     *     "false"
     * }
     *
     * Only strings are operated on. The possible values will
     * generally be displayed and stored as-is by the frontend.
     */

    public static final int RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL = 54;
    /* const struct retro_core_options_intl * --
     * Allows an implementation to signal the environment
     * which variables it might want to check for later using
     * GET_VARIABLE.
     * This allows the frontend to present these variables to
     * a user dynamically.
     * This should only be called if RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION
     * returns an API version of >= 1.
     * This should be called instead of RETRO_ENVIRONMENT_SET_VARIABLES.
     * This should be called instead of RETRO_ENVIRONMENT_SET_CORE_OPTIONS.
     * This should be called the first time as early as
     * possible (ideally in retro_set_environment).
     * Afterwards it may be called again for the core to communicate
     * updated options to the frontend, but the number of core
     * options must not change from the number in the initial call.
     *
     * This is fundamentally the same as RETRO_ENVIRONMENT_SET_CORE_OPTIONS,
     * with the addition of localisation support. The description of the
     * RETRO_ENVIRONMENT_SET_CORE_OPTIONS callback should be consulted
     * for further details.
     *
     * 'data' points to a retro_core_options_intl struct.
     *
     * retro_core_options_intl::us is a pointer to an array of
     * retro_core_option_definition structs defining the US English
     * core options implementation. It must popublic static final int to a valid array.
     *
     * retro_core_options_intl::local is a pointer to an array of
     * retro_core_option_definition structs defining core options for
     * the current frontend language. It may be NULL (in which case
     * retro_core_options_intl::us is used by the frontend). Any items
     * missing from this array will be read from retro_core_options_intl::us
     * instead.
     *
     * NOTE: Default core option values are always taken from the
     * retro_core_options_intl::us array. Any default values in
     * retro_core_options_intl::local array will be ignored.
     */

    public static final int RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY = 55;
    /* struct retro_core_option_display * --
     *
     * Allows an implementation to signal the environment to show
     * or hide a variable when displaying core options. This is
     * considered a *suggestion*. The frontend is free to ignore
     * this callback, and its implementation not considered mandatory.
     *
     * 'data' points to a retro_core_option_display struct
     *
     * retro_core_option_display::key is a variable identifier
     * which has already been set by SET_VARIABLES/SET_CORE_OPTIONS.
     *
     * retro_core_option_display::visible is a boolean, specifying
     * whether variable should be displayed
     *
     * Note that all core option variables will be set visible by
     * default when calling SET_VARIABLES/SET_CORE_OPTIONS.
     */

    public static final int RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER = 56;
    /* unsigned * --
     *
     * Allows an implementation to ask frontend preferred hardware
     * context to use. Core should use this information to deal
     * with what specific context to request with SET_HW_RENDER.
     *
     * 'data' points to an unsigned variable
     */

    public static final int RETRO_ENVIRONMENT_GET_DISK_CONTROL_INTERFACE_VERSION = 57;
    /* unsigned * --
     * Unsigned value is the API version number of the disk control
     * interface supported by the frontend. If callback return false,
     * API version is assumed to be 0.
     *
     * In legacy code, the disk control interface is defined by passing
     * a struct of type retro_disk_control_callback to
     * RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE.
     * This may be still be done regardless of the disk control
     * interface version.
     *
     * If version is >= 1 however, the disk control interface may
     * instead be defined by passing a struct of type
     * retro_disk_control_ext_callback to
     * RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE.
     * This allows the core to provide additional information about
     * disk images to the frontend and/or enables extra
     * disk control functionality by the frontend.
     */

    public static final int RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE = 58;
    /* const struct retro_disk_control_ext_callback * --
     * Sets an interface which frontend can use to eject and insert
     * disk images, and also obtain information about individual
     * disk image files registered by the core.
     * This is used for games which consist of multiple images and
     * must be manually swapped out by the user (e.g. PSX, floppy disk
     * based systems).
     */

    public static final int RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION = 59;
    /* unsigned * --
     * Unsigned value is the API version number of the message
     * interface supported by the frontend. If callback returns
     * false, API version is assumed to be 0.
     *
     * In legacy code, messages may be displayed in an
     * implementation-specific manner by passing a struct
     * of type retro_message to RETRO_ENVIRONMENT_SET_MESSAGE.
     * This may be still be done regardless of the message
     * interface version.
     *
     * If version is >= 1 however, messages may instead be
     * displayed by passing a struct of type retro_message_ext
     * to RETRO_ENVIRONMENT_SET_MESSAGE_EXT. This allows the
     * core to specify message logging level, priority and
     * destination (OSD, logging interface or both).
     */

    public static final int RETRO_ENVIRONMENT_SET_MESSAGE_EXT = 60;
    /* const struct retro_message_ext * --
     * Sets a message to be displayed in an implementation-specific
     * manner for a certain amount of 'frames'. Additionally allows
     * the core to specify message logging level, priority and
     * destination (OSD, logging interface or both).
     * Should not be used for trivial messages, which should simply be
     * logged via RETRO_ENVIRONMENT_GET_LOG_INTERFACE (or as a
     * fallback, stderr).
     */

    public static final int RETRO_ENVIRONMENT_GET_INPUT_MAX_USERS = 61;
    /* unsigned * --
     * Unsigned value is the number of active input devices
     * provided by the frontend. This may change between
     * frames, but will remain constant for the duration
     * of each frame.
     * If callback returns true, a core need not poll any
     * input device with an index greater than or equal to
     * the number of active devices.
     * If callback returns false, the number of active input
     * devices is unknown. In this case, all input devices
     * should be considered active.
     */

    public static final int RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK = 62;
    /* const struct retro_audio_buffer_status_callback * --
     * Lets the core know the occupancy level of the frontend
     * audio buffer. Can be used by a core to attempt frame
     * skipping in order to avoid buffer under-runs.
     * A core may pass NULL to disable buffer status reporting
     * in the frontend.
     */

    public static final int RETRO_ENVIRONMENT_SET_MINIMUM_AUDIO_LATENCY = 63;
    /* const unsigned * --
     * Sets minimum frontend audio latency in milliseconds.
     * Resultant audio latency may be larger than set value,
     * or smaller if a hardware limit is encountered. A frontend
     * is expected to honour requests up to 512 ms.
     *
     * - If value is less than current frontend
     *   audio latency, callback has no effect
     * - If value is zero, default frontend audio
     *   latency is set
     *
     * May be used by a core to increase audio latency and
     * therefore decrease the probability of buffer under-runs
     * (crackling) when performing 'intensive' operations.
     * A core utilising RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK
     * to implement audio-buffer-based frame skipping may achieve
     * optimal results by setting the audio latency to a 'high'
     * (typically 6x or 8x) integer multiple of the expected
     * frame time.
     *
     * WARNING: This can only be called from within retro_run().
     * Calling this can require a full reinitialization of audio
     * drivers in the frontend, so it is important to call it very
     * sparingly, and usually only with the users explicit consent.
     * An eventual driver reinitialize will happen so that audio
     * callbacks happening after this call within the same retro_run()
     * call will target the newly initialized driver.
     */

    public static final int RETRO_ENVIRONMENT_SET_FASTFORWARDING_OVERRIDE = 64;
    /* const struct retro_fastforwarding_override * --
     * Used by a libretro core to override the current
     * fastforwarding mode of the frontend.
     * If NULL is passed to this function, the frontend
     * will return true if fastforwarding override
     * functionality is supported (no change in
     * fastforwarding state will occur in this case).
     */

    public static final int RETRO_ENVIRONMENT_SET_CONTENT_INFO_OVERRIDE = 65;
    /* const struct retro_system_content_info_override * --
     * Allows an implementation to override 'global' content
     * info parameters reported by retro_get_system_info().
     * Overrides also affect subsystem content info parameters
     * set via RETRO_ENVIRONMENT_SET_SUBSYSTEM_INFO.
     * This function must be called inside retro_set_environment().
     * If callback returns false, content info overrides
     * are unsupported by the frontend, and will be ignored.
     * If callback returns true, extended game info may be
     * retrieved by calling RETRO_ENVIRONMENT_GET_GAME_INFO_EXT
     * in retro_load_game() or retro_load_game_special().
     *
     * 'data' points to an array of retro_system_content_info_override
     * structs terminated by a { NULL, false, false } element.
     * If 'data' is NULL, no changes will be made to the frontend;
     * a core may therefore pass NULL in order to test whether
     * the RETRO_ENVIRONMENT_SET_CONTENT_INFO_OVERRIDE and
     * RETRO_ENVIRONMENT_GET_GAME_INFO_EXT callbacks are supported
     * by the frontend.
     *
     * For struct member descriptions, see the definition of
     * struct retro_system_content_info_override.
     *
     * Example:
     *
     * - struct retro_system_info:
     * {
     *    "My Core",                      // library_name
     *    "v1.0",                         // library_version
     *    "m3u|md|cue|iso|chd|sms|gg|sg", // valid_extensions
     *    true,                           // need_fullpath
     *    false                           // block_extract
     * }
     *
     * - Array of struct retro_system_content_info_override:
     * {
     *    {
     *       "md|sms|gg", // extensions
     *       false,       // need_fullpath
     *       true         // persistent_data
     *    },
     *    {
     *       "sg",        // extensions
     *       false,       // need_fullpath
     *       false        // persistent_data
     *    },
     *    { NULL, false, false }
     * }
     *
     * Result:
     * - Files of type m3u, cue, iso, chd will not be
     *   loaded by the frontend. Frontend will pass a
     *   valid path to the core, and core will handle
     *   loading internally
     * - Files of type md, sms, gg will be loaded by
     *   the frontend. A valid memory buffer will be
     *   passed to the core. This memory buffer will
     *   remain valid until retro_deinit() returns
     * - Files of type sg will be loaded by the frontend.
     *   A valid memory buffer will be passed to the core.
     *   This memory buffer will remain valid until
     *   retro_load_game() (or retro_load_game_special())
     *   returns
     *
     * NOTE: If an extension is listed multiple times in
     * an array of retro_system_content_info_override
     * structs, only the first instance will be registered
     */

    public static final int RETRO_ENVIRONMENT_GET_GAME_INFO_EXT = 66;
    /* const struct retro_game_info_ext ** --
     * Allows an implementation to fetch extended game
     * information, providing additional content path
     * and memory buffer status details.
     * This function may only be called inside
     * retro_load_game() or retro_load_game_special().
     * If callback returns false, extended game information
     * is unsupported by the frontend. In this case, only
     * regular retro_game_info will be available.
     * RETRO_ENVIRONMENT_GET_GAME_INFO_EXT is guaranteed
     * to return true if RETRO_ENVIRONMENT_SET_CONTENT_INFO_OVERRIDE
     * returns true.
     *
     * 'data' points to an array of retro_game_info_ext structs.
     *
     * For struct member descriptions, see the definition of
     * struct retro_game_info_ext.
     *
     * - If function is called inside retro_load_game(),
     *   the retro_game_info_ext array is guaranteed to
     *   have a size of 1 - i.e. the returned pointer may
     *   be used to access directly the members of the
     *   first retro_game_info_ext struct, for example:
     *
     *      struct retro_game_info_ext *game_info_ext;
     *      if (environ_cb(RETRO_ENVIRONMENT_GET_GAME_INFO_EXT, &game_info_ext))
     *         printf("Content Directory: %s\n", game_info_ext->dir);
     *
     * - If the function is called inside retro_load_game_special(),
     *   the retro_game_info_ext array is guaranteed to have a
     *   size equal to the num_info argument passed to
     *   retro_load_game_special()
     */

    public static final int RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2 = 67;
    /* const struct retro_core_options_v2 * --
     * Allows an implementation to signal the environment
     * which variables it might want to check for later using
     * GET_VARIABLE.
     * This allows the frontend to present these variables to
     * a user dynamically.
     * This should only be called if RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION
     * returns an API version of >= 2.
     * This should be called instead of RETRO_ENVIRONMENT_SET_VARIABLES.
     * This should be called instead of RETRO_ENVIRONMENT_SET_CORE_OPTIONS.
     * This should be called the first time as early as
     * possible (ideally in retro_set_environment).
     * Afterwards it may be called again for the core to communicate
     * updated options to the frontend, but the number of core
     * options must not change from the number in the initial call.
     * If RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION returns an API
     * version of >= 2, this callback is guaranteed to succeed
     * (i.e. callback return value does not indicate success)
     * If callback returns true, frontend has core option category
     * support.
     * If callback returns false, frontend does not have core option
     * category support.
     *
     * 'data' points to a retro_core_options_v2 struct, containing
     * of two pointers:
     * - retro_core_options_v2::categories is an array of
     *   retro_core_option_v2_category structs terminated by a
     *   { NULL, NULL, NULL } element. If retro_core_options_v2::categories
     *   is NULL, all core options will have no category and will be shown
     *   at the top level of the frontend core option interface. If frontend
     *   does not have core option category support, categories array will
     *   be ignored.
     * - retro_core_options_v2::definitions is an array of
     *   retro_core_option_v2_definition structs terminated by a
     *   { NULL, NULL, NULL, NULL, NULL, NULL, {{0}}, NULL }
     *   element.
     *
     * >> retro_core_option_v2_category notes:
     *
     * - retro_core_option_v2_category::key should contain string
     *   that uniquely identifies the core option category. Valid
     *   key characters are [a-z, A-Z, 0-9, _, -]
     *   Namespace collisions with other implementations' category
     *   keys are permitted.
     * - retro_core_option_v2_category::desc should contain a human
     *   readable description of the category key.
     * - retro_core_option_v2_category::info should contain any
     *   additional human readable information text that a typical
     *   user may need to understand the nature of the core option
     *   category.
     *
     * Example entry:
     * {
     *     "advanced_settings",
     *     "Advanced",
     *     "Options affecting low-level emulation performance and accuracy."
     * }
     *
     * >> retro_core_option_v2_definition notes:
     *
     * - retro_core_option_v2_definition::key should be namespaced to not
     *   collide with other implementations' keys. e.g. A core called
     *   'foo' should use keys named as 'foo_option'. Valid key characters
     *   are [a-z, A-Z, 0-9, _, -].
     * - retro_core_option_v2_definition::desc should contain a human readable
     *   description of the key. Will be used when the frontend does not
     *   have core option category support. Examples: "Aspect Ratio" or
     *   "Video > Aspect Ratio".
     * - retro_core_option_v2_definition::desc_categorized should contain a
     *   human readable description of the key, which will be used when
     *   frontend has core option category support. Example: "Aspect Ratio",
     *   where associated retro_core_option_v2_category::desc is "Video".
     *   If empty or NULL, the string specified by
     *   retro_core_option_v2_definition::desc will be used instead.
     *   retro_core_option_v2_definition::desc_categorized will be ignored
     *   if retro_core_option_v2_definition::category_key is empty or NULL.
     * - retro_core_option_v2_definition::info should contain any additional
     *   human readable information text that a typical user may need to
     *   understand the functionality of the option.
     * - retro_core_option_v2_definition::info_categorized should contain
     *   any additional human readable information text that a typical user
     *   may need to understand the functionality of the option, and will be
     *   used when frontend has core option category support. This is provided
     *   to accommodate the case where info text references an option by
     *   name/desc, and the desc/desc_categorized text for that option differ.
     *   If empty or NULL, the string specified by
     *   retro_core_option_v2_definition::info will be used instead.
     *   retro_core_option_v2_definition::info_categorized will be ignored
     *   if retro_core_option_v2_definition::category_key is empty or NULL.
     * - retro_core_option_v2_definition::category_key should contain a
     *   category identifier (e.g. "video" or "audio") that will be
     *   assigned to the core option if frontend has core option category
     *   support. A categorized option will be shown in a subsection/
     *   submenu of the frontend core option interface. If key is empty
     *   or NULL, or if key does not match one of the
     *   retro_core_option_v2_category::key values in the associated
     *   retro_core_option_v2_category array, option will have no category
     *   and will be shown at the top level of the frontend core option
     *   interface.
     * - retro_core_option_v2_definition::values is an array of
     *   retro_core_option_value structs terminated by a { NULL, NULL }
     *   element.
     * --> retro_core_option_v2_definition::values[index].value is an
     *     expected option value.
     * --> retro_core_option_v2_definition::values[index].label is a
     *     human readable label used when displaying the value on screen.
     *     If NULL, the value itself is used.
     * - retro_core_option_v2_definition::default_value is the default
     *   core option setting. It must match one of the expected option
     *   values in the retro_core_option_v2_definition::values array. If
     *   it does not, or the default value is NULL, the first entry in the
     *   retro_core_option_v2_definition::values array is treated as the
     *   default.
     *
     * The number of possible option values should be very limited,
     * and must be less than RETRO_NUM_CORE_OPTION_VALUES_MAX.
     * i.e. it should be feasible to cycle through options
     * without a keyboard.
     *
     * Example entries:
     *
     * - Uncategorized:
     *
     * {
     *     "foo_option",
     *     "Speed hack coprocessor X",
     *     NULL,
     *     "Provides increased performance at the expense of reduced accuracy.",
     *     NULL,
     *     NULL,
     * 	  {
     *         { "false",    NULL },
     *         { "true",     NULL },
     *         { "unstable", "Turbo (Unstable)" },
     *         { NULL, NULL },
     *     },
     *     "false"
     * }
     *
     * - Categorized:
     *
     * {
     *     "foo_option",
     *     "Advanced > Speed hack coprocessor X",
     *     "Speed hack coprocessor X",
     *     "Setting 'Advanced > Speed hack coprocessor X' to 'true' or 'Turbo' provides increased performance at the expense of reduced accuracy",
     *     "Setting 'Speed hack coprocessor X' to 'true' or 'Turbo' provides increased performance at the expense of reduced accuracy",
     *     "advanced_settings",
     * 	  {
     *         { "false",    NULL },
     *         { "true",     NULL },
     *         { "unstable", "Turbo (Unstable)" },
     *         { NULL, NULL },
     *     },
     *     "false"
     * }
     *
     * Only strings are operated on. The possible values will
     * generally be displayed and stored as-is by the frontend.
     */

    public static final int RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL = 68;
    /* const struct retro_core_options_v2_intl * --
     * Allows an implementation to signal the environment
     * which variables it might want to check for later using
     * GET_VARIABLE.
     * This allows the frontend to present these variables to
     * a user dynamically.
     * This should only be called if RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION
     * returns an API version of >= 2.
     * This should be called instead of RETRO_ENVIRONMENT_SET_VARIABLES.
     * This should be called instead of RETRO_ENVIRONMENT_SET_CORE_OPTIONS.
     * This should be called instead of RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL.
     * This should be called instead of RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2.
     * This should be called the first time as early as
     * possible (ideally in retro_set_environment).
     * Afterwards it may be called again for the core to communicate
     * updated options to the frontend, but the number of core
     * options must not change from the number in the initial call.
     * If RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION returns an API
     * version of >= 2, this callback is guaranteed to succeed
     * (i.e. callback return value does not indicate success)
     * If callback returns true, frontend has core option category
     * support.
     * If callback returns false, frontend does not have core option
     * category support.
     *
     * This is fundamentally the same as RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2,
     * with the addition of localisation support. The description of the
     * RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2 callback should be consulted
     * for further details.
     *
     * 'data' points to a retro_core_options_v2_intl struct.
     *
     * - retro_core_options_v2_intl::us is a pointer to a
     *   retro_core_options_v2 struct defining the US English
     *   core options implementation. It must popublic static final int to a valid struct.
     *
     * - retro_core_options_v2_intl::local is a pointer to a
     *   retro_core_options_v2 struct defining core options for
     *   the current frontend language. It may be NULL (in which case
     *   retro_core_options_v2_intl::us is used by the frontend). Any items
     *   missing from this struct will be read from
     *   retro_core_options_v2_intl::us instead.
     *
     * NOTE: Default core option values are always taken from the
     * retro_core_options_v2_intl::us struct. Any default values in
     * the retro_core_options_v2_intl::local struct will be ignored.
     */

    public static final int RETRO_ENVIRONMENT_SET_CORE_OPTIONS_UPDATE_DISPLAY_CALLBACK = 69;
    /* const struct retro_core_options_update_display_callback * --
     * Allows a frontend to signal that a core must update
     * the visibility of any dynamically hidden core options,
     * and enables the frontend to detect visibility changes.
     * Used by the frontend to update the menu display status
     * of core options without requiring a call of retro_run().
     * Must be called in retro_set_environment().
     */

    public static final int RETRO_ENVIRONMENT_SET_VARIABLE = 70;
    /* const struct retro_variable * --
     * Allows an implementation to notify the frontend
     * that a core option value has changed.
     *
     * retro_variable::key and retro_variable::value
     * must match strings that have been set previously
     * via one of the following:
     *
     * - RETRO_ENVIRONMENT_SET_VARIABLES
     * - RETRO_ENVIRONMENT_SET_CORE_OPTIONS
     * - RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL
     * - RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2
     * - RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL
     *
     * After changing a core option value via this
     * callback, RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE
     * will return true.
     *
     * If data is NULL, no changes will be registered
     * and the callback will return true; an
     * implementation may therefore pass NULL in order
     * to test whether the callback is supported.
     */

    public static final int RETRO_ENVIRONMENT_GET_THROTTLE_STATE = (71 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_throttle_state * --
     * Allows an implementation to get details on the actual rate
     * the frontend is attempting to call retro_run().
     */

    public static final int RETRO_ENVIRONMENT_GET_SAVESTATE_CONTEXT = (72 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* public static final int * --
     * Tells the core about the context the frontend is asking for savestate.
     * (see enum retro_savestate_context)
     */

    public static final int RETRO_ENVIRONMENT_GET_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_SUPPORT = (73 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_hw_render_context_negotiation_interface * --
     * Before calling SET_HW_RNEDER_CONTEXT_NEGOTIATION_INTERFACE, a core can query
     * which version of the interface is supported.
     *
     * Frontend looks at interface_type and returns the maximum supported
     * context negotiation interface version.
     * If the interface_type is not supported or recognized by the frontend, a version of 0
     * must be returned in interface_version and true is returned by frontend.
     *
     * If this environment call returns true with interface_version greater than 0,
     * a core can always use a negotiation interface version larger than what the frontend returns, but only
     * earlier versions of the interface will be used by the frontend.
     * A frontend must not reject a negotiation interface version that is larger than
     * what the frontend supports. Instead, the frontend will use the older entry points that it recognizes.
     * If this is incompatible with a particular core's requirements, it can error out early.
     *
     * Backwards compatibility note:
     * This environment call was introduced after Vulkan v1 context negotiation.
     * If this environment call is not supported by frontend - i.e. the environment call returns false -
     * only Vulkan v1 context negotiation is supported (if Vulkan HW rendering is supported at all).
     * If a core uses Vulkan negotiation interface with version > 1, negotiation may fail unexpectedly.
     * All future updates to the context negotiation interface implies that frontend must support
     * this environment call to query support.
     */

    public static final int RETRO_ENVIRONMENT_GET_JIT_CAPABLE = 74;
    /* bool * --
     * Result is set to true if the frontend has already verified JIT can be
     * used, mainly for use iOS/tvOS. On other platforms the result is true.
     */

    public static final int RETRO_ENVIRONMENT_GET_MICROPHONE_INTERFACE = (75 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_microphone_interface * --
     * Returns an interface that can be used to receive input from the microphone driver.
     *
     * Returns true if microphone support is available,
     * even if no microphones are plugged in.
     * Returns false if mic support is disabled or unavailable.
     *
     * This callback can be invoked at any time,
     * even before the microphone driver is ready.
     */

    public static final int RETRO_ENVIRONMENT_SET_NETPACKET_INTERFACE = 76;
    /* const struct retro_netpacket_callback * --
     * When set, a core gains control over network packets sent and
     * received during a multiplayer session. This can be used to
     * emulate multiplayer games that were originally played on two
     * or more separate consoles or computers connected together.
     *
     * The frontend will take care of connecting players together,
     * and the core only needs to send the actual data as needed for
     * the emulation, while handshake and connection management happen
     * in the background.
     *
     * When two or more players are connected and this interface has
     * been set, time manipulation features (such as pausing, slow motion,
     * fast forward, rewinding, save state loading, etc.) are disabled to
     * avoid interrupting communication.
     *
     * Should be set in either retro_init or retro_load_game, but not both.
     *
     * When not set, a frontend may use state serialization-based
     * multiplayer, where a deterministic core supporting multiple
     * input devices does not need to take any action on its own.
     */

    public static final int RETRO_ENVIRONMENT_GET_DEVICE_POWER = (77 | RETRO_ENVIRONMENT_EXPERIMENTAL);
    /* struct retro_device_power * --
     * Returns the device's current power state as reported by the frontend.
     * This is useful for emulating the battery level in handheld consoles,
     * or for reducing power consumption when on battery power.
     *
     * The return value indicates whether the frontend can provide this information,
     * even if the parameter is NULL.
     *
     * If the frontend does not support this functionality,
     * then the provided argument will remain unchanged.
     *
     * Note that this environment call describes the power state for the entire device,
     * not for individual peripherals like controllers.
     */

    /* 0RGB1555, native endian.
     * 0 bit must be set to 0.
     * This pixel format is default for compatibility concerns only.
     * If a 15/16-bit pixel format is desired, consider using RGB565. */
    public static final int RETRO_PIXEL_FORMAT_0RGB1555 = 0;

    /* XRGB8888, native endian.
     * X bits are ignored. */
    public static final int RETRO_PIXEL_FORMAT_XRGB8888 = 1;

    /* RGB565, native endian.
     * This pixel format is the recommended format to use if a 15/16-bit
     * format is desired as it is the pixel format that is typically
     * available on a wide range of low-power devices.
     *
     * It is also natively supported in APIs like OpenGL ES. */
    public static final int RETRO_PIXEL_FORMAT_RGB565 = 2;

    /* Ensure sizeof() == sizeof(int). */
    public static final int RETRO_PIXEL_FORMAT_UNKNOWN = Integer.MAX_VALUE;

    public static final int RETRO_LANGUAGE_ENGLISH = 0;
    public static final int RETRO_LANGUAGE_JAPANESE = 1;
    public static final int RETRO_LANGUAGE_FRENCH = 2;
    public static final int RETRO_LANGUAGE_SPANISH = 3;
    public static final int RETRO_LANGUAGE_GERMAN = 4;
    public static final int RETRO_LANGUAGE_ITALIAN = 5;
    public static final int RETRO_LANGUAGE_DUTCH = 6;
    public static final int RETRO_LANGUAGE_PORTUGUESE_BRAZIL = 7;
    public static final int RETRO_LANGUAGE_PORTUGUESE_PORTUGAL = 8;
    public static final int RETRO_LANGUAGE_RUSSIAN = 9;
    public static final int RETRO_LANGUAGE_KOREAN = 10;
    public static final int RETRO_LANGUAGE_CHINESE_TRADITIONAL = 11;
    public static final int RETRO_LANGUAGE_CHINESE_SIMPLIFIED = 12;
    public static final int RETRO_LANGUAGE_ESPERANTO = 13;
    public static final int RETRO_LANGUAGE_POLISH = 14;
    public static final int RETRO_LANGUAGE_VIETNAMESE = 15;
    public static final int RETRO_LANGUAGE_ARABIC = 16;
    public static final int RETRO_LANGUAGE_GREEK = 17;
    public static final int RETRO_LANGUAGE_TURKISH = 18;
    public static final int RETRO_LANGUAGE_SLOVAK = 19;
    public static final int RETRO_LANGUAGE_PERSIAN = 20;
    public static final int RETRO_LANGUAGE_HEBREW = 21;
    public static final int RETRO_LANGUAGE_ASTURIAN = 22;
    public static final int RETRO_LANGUAGE_FINNISH = 23;
    public static final int RETRO_LANGUAGE_INDONESIAN = 24;
    public static final int RETRO_LANGUAGE_SWEDISH = 25;
    public static final int RETRO_LANGUAGE_UKRAINIAN = 26;
    public static final int RETRO_LANGUAGE_CZECH = 27;
    public static final int RETRO_LANGUAGE_CATALAN_VALENCIA = 28;
    public static final int RETRO_LANGUAGE_CATALAN = 29;
    public static final int RETRO_LANGUAGE_BRITISH_ENGLISH = 30;
    public static final int RETRO_LANGUAGE_HUNGARIAN = 31;
    public static final int RETRO_LANGUAGE_BELARUSIAN = 32;
    public static final int RETRO_LANGUAGE_LAST = 33;

    /* Ensure sizeof(enum) == sizeof(int) */
    public static final int RETRO_LANGUAGE_DUMMY = Integer.MAX_VALUE;

    /* Input disabled. */
    public static final int RETRO_DEVICE_NONE = 0;

    /* The JOYPAD is called RetroPad. It is essentially a Super Nintendo
     * controller, but with additional L2/R2/L3/R3 buttons, similar to a
     * PS1 DualShock. */
    public static final int RETRO_DEVICE_JOYPAD = 1;

    /* The mouse is a simple mouse, similar to Super Nintendo's mouse.
     * X and Y coordinates are reported relatively to last poll (poll callback).
     * It is up to the libretro implementation to keep track of where the mouse
     * pointer is supposed to be on the screen.
     * The frontend must make sure not to interfere with its own hardware
     * mouse pointer.
     */
    public static final int RETRO_DEVICE_MOUSE = 2;

    /* KEYBOARD device lets one poll for raw key pressed.
     * It is poll based, so input callback will return with the current
     * pressed state.
     * For event/text based keyboard input, see
     * RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK.
     */
    public static final int RETRO_DEVICE_KEYBOARD = 3;

    /* LIGHTGUN device is similar to Guncon-2 for PlayStation 2.
     * It reports X/Y coordinates in screen space (similar to the pointer)
     * in the range [-0x8000, 0x7fff] in both axes, with zero being center and
     * -0x8000 being out of bounds.
     * As well as reporting on/off screen state. It features a trigger,
     * start/select buttons, auxiliary action buttons and a
     * directional pad. A forced off-screen shot can be requested for
     * auto-reloading function in some games.
     */
    public static final int RETRO_DEVICE_LIGHTGUN = 4;

    /* The ANALOG device is an extension to JOYPAD (RetroPad).
     * Similar to DualShock2 it adds two analog sticks and all buttons can
     * be analog. This is treated as a separate device type as it returns
     * axis values in the full analog range of [-0x7fff, 0x7fff],
     * although some devices may return -0x8000.
     * Positive X axis is right. Positive Y axis is down.
     * Buttons are returned in the range [0, 0x7fff].
     * Only use ANALOG type when polling for analog values.
     */
    public static final int RETRO_DEVICE_ANALOG = 5;

    /* Abstracts the concept of a pointing mechanism, e.g. touch.
     * This allows libretro to query in absolute coordinates where on the
     * screen a mouse (or something similar) is being placed.
     * For a touch centric device, coordinates reported are the coordinates
     * of the press.
     *
     * Coordinates in X and Y are reported as:
     * [-0x7fff, 0x7fff]: -0x7fff corresponds to the far left/top of the screen,
     * and 0x7fff corresponds to the far right/bottom of the screen.
     * The "screen" is here defined as area that is passed to the frontend and
     * later displayed on the monitor.
     *
     * The frontend is free to scale/resize this screen as it sees fit, however,
     * (X, Y) = (-0x7fff, -0x7fff) will correspond to the top-left pixel of the
     * game image, etc.
     *
     * To check if the pointer coordinates are valid (e.g. a touch display
     * actually being touched), PRESSED returns 1 or 0.
     *
     * If using a mouse on a desktop, PRESSED will usually correspond to the
     * left mouse button, but this is a frontend decision.
     * PRESSED will only return 1 if the pointer is inside the game screen.
     *
     * For multi-touch, the index variable can be used to successively query
     * more presses.
     * If index = 0 returns true for _PRESSED, coordinates can be extracted
     * with _X, _Y for index = 0. One can then query _PRESSED, _X, _Y with
     * index = 1, and so on.
     * Eventually _PRESSED will return false for an index. No further presses
     * are registered at this point. */
    public static final int RETRO_DEVICE_POINTER = 6;

    /* Buttons for the RetroPad (JOYPAD).
     * The placement of these is equivalent to placements on the
     * Super Nintendo controller.
     * L2/R2/L3/R3 buttons correspond to the PS1 DualShock.
     * Also used as id values for RETRO_DEVICE_INDEX_ANALOG_BUTTON */
    public static final int RETRO_DEVICE_ID_JOYPAD_B = 0;
    public static final int RETRO_DEVICE_ID_JOYPAD_Y = 1;
    public static final int RETRO_DEVICE_ID_JOYPAD_SELECT = 2;
    public static final int RETRO_DEVICE_ID_JOYPAD_START = 3;
    public static final int RETRO_DEVICE_ID_JOYPAD_UP = 4;
    public static final int RETRO_DEVICE_ID_JOYPAD_DOWN = 5;
    public static final int RETRO_DEVICE_ID_JOYPAD_LEFT = 6;
    public static final int RETRO_DEVICE_ID_JOYPAD_RIGHT = 7;
    public static final int RETRO_DEVICE_ID_JOYPAD_A = 8;
    public static final int RETRO_DEVICE_ID_JOYPAD_X = 9;
    public static final int RETRO_DEVICE_ID_JOYPAD_L = 10;
    public static final int RETRO_DEVICE_ID_JOYPAD_R = 11;
    public static final int RETRO_DEVICE_ID_JOYPAD_L2 = 12;
    public static final int RETRO_DEVICE_ID_JOYPAD_R2 = 13;
    public static final int RETRO_DEVICE_ID_JOYPAD_L3 = 14;
    public static final int RETRO_DEVICE_ID_JOYPAD_R3 = 15;

    public static final int RETRO_DEVICE_ID_JOYPAD_MASK = 256;

    /* Index / Id values for ANALOG device. */
    public static final int RETRO_DEVICE_INDEX_ANALOG_LEFT = 0;
    public static final int RETRO_DEVICE_INDEX_ANALOG_RIGHT = 1;
    public static final int RETRO_DEVICE_INDEX_ANALOG_BUTTON = 2;
    public static final int RETRO_DEVICE_ID_ANALOG_X = 0;
    public static final int RETRO_DEVICE_ID_ANALOG_Y = 1;

    /* Id values for MOUSE. */
    public static final int RETRO_DEVICE_ID_MOUSE_X = 0;
    public static final int RETRO_DEVICE_ID_MOUSE_Y = 1;
    public static final int RETRO_DEVICE_ID_MOUSE_LEFT = 2;
    public static final int RETRO_DEVICE_ID_MOUSE_RIGHT = 3;
    public static final int RETRO_DEVICE_ID_MOUSE_WHEELUP = 4;
    public static final int RETRO_DEVICE_ID_MOUSE_WHEELDOWN = 5;
    public static final int RETRO_DEVICE_ID_MOUSE_MIDDLE = 6;
    public static final int RETRO_DEVICE_ID_MOUSE_HORIZ_WHEELUP = 7;
    public static final int RETRO_DEVICE_ID_MOUSE_HORIZ_WHEELDOWN = 8;
    public static final int RETRO_DEVICE_ID_MOUSE_BUTTON_4 = 9;
    public static final int RETRO_DEVICE_ID_MOUSE_BUTTON_5 = 10;

    /* Id values for LIGHTGUN. */
    public static final int RETRO_DEVICE_ID_LIGHTGUN_SCREEN_X = 13; /*Absolute Position*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_SCREEN_Y = 14; /*Absolute*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_IS_OFFSCREEN = 15; /*Status Check*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_TRIGGER = 2;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_RELOAD = 16; /*Forced off-screen shot*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_AUX_A = 3;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_AUX_B = 4;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_START = 6;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_SELECT = 7;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_AUX_C = 8;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_DPAD_UP = 9;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_DPAD_DOWN = 10;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_DPAD_LEFT = 11;
    public static final int RETRO_DEVICE_ID_LIGHTGUN_DPAD_RIGHT = 12;
    /* deprecated */
    public static final int RETRO_DEVICE_ID_LIGHTGUN_X = 0; /*Relative Position*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_Y = 1; /*Relative*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_CURSOR = 3; /*Use Aux:A*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_TURBO = 4; /*Use Aux:B*/
    public static final int RETRO_DEVICE_ID_LIGHTGUN_PAUSE = 5; /*Use Start*/

    /* Id values for POINTER. */
    public static final int RETRO_DEVICE_ID_POINTER_X = 0;
    public static final int RETRO_DEVICE_ID_POINTER_Y = 1;
    public static final int RETRO_DEVICE_ID_POINTER_PRESSED = 2;
    public static final int RETRO_DEVICE_ID_POINTER_COUNT = 3;

    public static final int VIDEO_DEVICE = 1;
    public static final int AUDIO_DEVICE = 2;
    public static final int INPUT_DEVICE = 3;

    public static final int SAVE_MEMORY_RAM = 1;
    public static final int SAVE_STATE = 2;

    public static final int LOAD_STATE = 2;

    public static final int SYSTEM_DIR = 1;
    public static final int SAVE_DIR = 2;
    protected final String TAG = getClass().getSimpleName();
    private static final int STATE_INVALID = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;

    private final byte[] mLock = new byte[0];
    private volatile int mState = STATE_INVALID;
    private EmScheduledThread mMainThread;

    protected final EmConfig config;
    protected final Map<String, EmOption> options = new HashMap<>();
    protected EmSystemAvInfo systemAvInfo;
    protected String systemDir;
    protected String saveDir;
    protected EmAudioDevice audioDevice;
    protected EmVideoDevice videoDevice;
    protected EmInputDevice inputDevice0;
    protected EmInputDevice inputDevice1;
    protected EmInputDevice inputDevice2;
    protected EmInputDevice inputDevice3;
    private OnEmulatorEventListener mEventListener;

    public boolean run(@NonNull File rom) {
        if (!rom.exists() || !rom.canRead()) return false;
        if (mState == STATE_INVALID) {
            onPowerOn();
        }
        if (!onLoadGame(rom.getAbsolutePath())) {
            onPowerOff();
            mState = STATE_INVALID;
            return false;
        }
        assert mMainThread == null;
        mMainThread = new EmScheduledThread() {
            @Override
            public void next() {
                synchronized (mLock) {
                    if (mState == STATE_RUNNING) {
                        onNext();
                    } else if (mState == STATE_PAUSED) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace(System.err);
                        }
                    } else {
                        interrupt();
                    }
                }
            }
        };
        mState = STATE_RUNNING;
        if (systemAvInfo == null)
            systemAvInfo = getSystemAvInfo();
        mMainThread.schedule(systemAvInfo.timing.fps);
        return true;
    }

    public void pause() {
        if (mState != STATE_RUNNING) return;
        mState = STATE_PAUSED;
        if (audioDevice != null) {
            audioDevice.pause();
        }
    }

    public void resume() {
        if (mState != STATE_PAUSED) return;
        synchronized (mLock) {
            mState = STATE_RUNNING;
            mLock.notify();
        }
    }

    public void reset() {
        if (mState == STATE_INVALID) return;
        onReset();
    }

    public void suspend() {
        if (mMainThread != null) {
            mMainThread.cancel();
            mMainThread = null;
        }
        if (audioDevice != null) {
            audioDevice.close();
            audioDevice = null;
        }
        if (videoDevice != null) {
            videoDevice = null;
        }
        mEventListener = null;
        if (mState == STATE_INVALID) return;
        onPowerOff();
        mState = STATE_INVALID;
    }

    public void setSystemDirectory(int type, @NonNull File dir) {
        if (!dir.exists() || !dir.isDirectory())
            throw new InvalidParameterException(dir + " not exists or not is a directory!");
        switch (type) {
            case SYSTEM_DIR:
                systemDir = dir.getAbsolutePath();
                break;
            case SAVE_DIR:
                saveDir = dir.getAbsolutePath();
                break;
            default:
                /*ignored*/
        }
    }

    public void attachDevice(int target, @Nullable EmulatorDevice device) {
        switch (target) {
            case AUDIO_DEVICE:
                audioDevice = (EmAudioDevice) device;
                break;
            case VIDEO_DEVICE:
                videoDevice = (EmVideoDevice) device;
                break;
            case INPUT_DEVICE:
                if (device instanceof EmInputDevice) {
                    EmInputDevice it = (EmInputDevice) device;
                    if (it.port == 1) {
                        inputDevice1 = it;
                    } else if (it.port == 2) {
                        inputDevice2 = it;
                    } else if (it.port == 3) {
                        inputDevice3 = it;
                    } else {
                        inputDevice0 = it;
                    }
                }
                break;
            default:
        }
    }

    public Emulator(@NonNull Resources res, @XmlRes int configResId) throws XmlPullParserException, IOException {
        config = EmConfig.fromXmlConfig(res, configResId);
        config.options.forEach(option -> options.put(option.key, option));
    }

    public void setOption(@NonNull EmOption option) {
        if (!option.enable) return;
        EmOption opt = options.get(option.key);
        if (opt != null) {
            opt.val = option.val;
        }
    }

    public Collection<EmOption> getOptions() {
        return config.options.stream()
                .map(EmOption::clone)
                .collect(Collectors.toList());
    }

    public boolean isSupportedSystem(@NonNull EmSystem system) {
        return config.systems.contains(system);
    }

    public boolean save(int type, @NonNull File file) {
        if (type == SAVE_STATE)
            return onSaveState(file.getAbsolutePath());
        return false;
    }

    public boolean load(int type, @Nullable File file) {
        if (file == null) return true;
        if (type == LOAD_STATE) {
            onLoadState(file.getAbsolutePath());
        }
        return false;
    }

    public List<EmSystem> getSupportedSystems() {
        return config.systems;
    }

    @CallFromJni
    protected boolean onEnvironment(int cmd, Object data) {
        if (cmd == RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE) {
            return false;
        }
        Variable variable;
        VariableEntry entry;
        switch (cmd) {
            case RETRO_ENVIRONMENT_GET_VARIABLE:
                entry = (VariableEntry) data;
                EmOption option = options.get(entry.key);
                if (option != null) {
                    entry.value = option.val;
                } else {
                    Log.d(TAG, entry.key);
                }
                break;
            case RETRO_ENVIRONMENT_SET_VARIABLE:
            case RETRO_ENVIRONMENT_SET_VARIABLES:
                if (data != null) {
                    /*pass*/
                }
                break;
            case RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION:
                variable = (Variable) data;
                variable.value = 1;
                break;
            case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
                if (mEventListener != null)
                    mEventListener.onShowMessage((EmMessageExt) data);
                break;
            case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
                variable = (Variable) data;
                if ((int) variable.value == RETRO_PIXEL_FORMAT_XRGB8888)
                    videoDevice.setPixelFormat(EmVideoDevice.PIXEL_FORMAT_RGBA);
                break;
            case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
                break;
            case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
                variable = (Variable) data;
                variable.value = systemDir;
                break;
            case RETRO_ENVIRONMENT_GET_LANGUAGE:
                variable = (Variable) data;
                variable.value = RETRO_LANGUAGE_ENGLISH;
                break;
            case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
//                List<InputDescriptor> descriptors = (ArrayList<InputDescriptor>) data;
                break;
            case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
//                ArrayList<ControllerDescription> descriptors = (ArrayList<ControllerDescription>) data;
                break;
            case RETRO_ENVIRONMENT_SET_GEOMETRY:
//                EmGameGeometry geometry = (EmGameGeometry) data;
                break;
            default:
                return false;
        }
        return true;
    }
    @CallFromJni
    protected void onVideoRefresh(final byte[] data, int width, int height, int pitch) {
        if (videoDevice == null) return;
        videoDevice.refresh(data, width, height, pitch);
    }

    @CallFromJni
    protected void onAudioSampleBatch(final short[] data, int frames) {
        if (audioDevice == null) return;
        if (!audioDevice.isOpen()) {
            int sampleRate = (int) systemAvInfo.timing.sampleRate;
            if (sampleRate == 0)
                sampleRate = 48000;
            audioDevice.open(EmAudioDevice.PCM_16BIT, sampleRate, 2);
        }
        if (audioDevice.isOpen()) {
            audioDevice.play(data, frames);
        }
    }

    @CallFromJni
    protected int onInputState(int port, int device, int index, int id) {
        EmInputDevice it = null;
        if (port == 0) {
            it = inputDevice0;
        } else if (port == 1) {
            it = inputDevice1;
        } else if (port == 2) {
            it = inputDevice2;
        } else if (port == 3) {
            it = inputDevice3;
        }
        if (it != null && it.device == device) {
            return it.getState(id);
        }
        return 0;
    }

    @CallFromJni
    protected void onInputPoll() {

    }

    public void setEmulatorEventListener(@Nullable OnEmulatorEventListener listener) {
        mEventListener = listener;
    }

    public abstract String getTag();
    public abstract EmSystemInfo getSystemInfo();
    protected abstract EmSystemAvInfo getSystemAvInfo();
    public abstract void onPowerOn();
    public abstract boolean onLoadGame(@NonNull String fullPath);
    public abstract void onNext();
    public abstract void onReset();
    public abstract void onLoadState(@NonNull String fullPath);
    public abstract boolean onSaveState(@NonNull String savePath);
    public abstract void onPowerOff();

    public interface OnEmulatorEventListener {
        void onShowMessage(EmMessageExt msg);
    }
}
