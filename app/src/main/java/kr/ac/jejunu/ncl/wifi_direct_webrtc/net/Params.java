package kr.ac.jejunu.ncl.wifi_direct_webrtc.net;

/**
 * Created by jinhy on 2016-12-04.
 */

public interface Params {
    String EXTRA_ROOMID =
            "org.appspot.apprtc.ROOMID";
    String EXTRA_LOOPBACK =
            "org.appspot.apprtc.LOOPBACK";
    String EXTRA_VIDEO_CALL =
            "org.appspot.apprtc.VIDEO_CALL";
    String EXTRA_CAMERA2 =
            "org.appspot.apprtc.CAMERA2";
    String EXTRA_VIDEO_WIDTH =
            "org.appspot.apprtc.VIDEO_WIDTH";
    String EXTRA_VIDEO_HEIGHT =
            "org.appspot.apprtc.VIDEO_HEIGHT";
    String EXTRA_VIDEO_FPS =
            "org.appspot.apprtc.VIDEO_FPS";
    String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
            "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    String EXTRA_VIDEO_BITRATE =
            "org.appspot.apprtc.VIDEO_BITRATE";
    String EXTRA_VIDEOCODEC =
            "org.appspot.apprtc.VIDEOCODEC";
    String EXTRA_HWCODEC_ENABLED =
            "org.appspot.apprtc.HWCODEC";
    String EXTRA_CAPTURETOTEXTURE_ENABLED =
            "org.appspot.apprtc.CAPTURETOTEXTURE";
    String EXTRA_AUDIO_BITRATE =
            "org.appspot.apprtc.AUDIO_BITRATE";
    String EXTRA_AUDIOCODEC =
            "org.appspot.apprtc.AUDIOCODEC";
    String EXTRA_NOAUDIOPROCESSING_ENABLED =
            "org.appspot.apprtc.NOAUDIOPROCESSING";
    String EXTRA_AECDUMP_ENABLED =
            "org.appspot.apprtc.AECDUMP";
    String EXTRA_OPENSLES_ENABLED =
            "org.appspot.apprtc.OPENSLES";
    String EXTRA_DISABLE_BUILT_IN_AEC =
            "org.appspot.apprtc.DISABLE_BUILT_IN_AEC";
    String EXTRA_DISABLE_BUILT_IN_AGC =
            "org.appspot.apprtc.DISABLE_BUILT_IN_AGC";
    String EXTRA_DISABLE_BUILT_IN_NS =
            "org.appspot.apprtc.DISABLE_BUILT_IN_NS";
    String EXTRA_ENABLE_LEVEL_CONTROL =
            "org.appspot.apprtc.ENABLE_LEVEL_CONTROL";
    String EXTRA_DISPLAY_HUD =
            "org.appspot.apprtc.DISPLAY_HUD";
    String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
    String EXTRA_CMDLINE =
            "org.appspot.apprtc.CMDLINE";
    String EXTRA_RUNTIME =
            "org.appspot.apprtc.RUNTIME";

    int STAT_CALLBACK_PERIOD = 1000;
}
