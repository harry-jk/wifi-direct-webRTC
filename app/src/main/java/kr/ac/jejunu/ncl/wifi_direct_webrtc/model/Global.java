package kr.ac.jejunu.ncl.wifi_direct_webrtc.model;

import org.webrtc.EglBase;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.util.Util;

/**
 * Created by jinhy on 2016-12-05.
 */

public class Global {
    private static Global instance;
    private User user;
    private EglBase rootEglBase;

    public Global() {
        String address = Util.getMacAddr();
        user = new User(address);
        user.setIp("127.0.0.1");
    }

    public static Global getInstance() {
        if(instance == null) {
            instance = new Global();
        }
        return instance;
    }

    public User getUser() {
        return user;
    }

    public EglBase getRootEglBase() {
        if(rootEglBase == null) rootEglBase = EglBase.create();
        return rootEglBase;
    }
}
