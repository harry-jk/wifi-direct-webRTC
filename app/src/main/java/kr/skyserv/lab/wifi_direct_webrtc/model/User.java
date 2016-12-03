package kr.skyserv.lab.wifi_direct_webrtc.model;

/**
 * Created by jinhy on 2016-11-22.
 */

public class User {
    private String name;
    private String key;

    public User(String key) {
        this.key = key;
        name = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }
}
