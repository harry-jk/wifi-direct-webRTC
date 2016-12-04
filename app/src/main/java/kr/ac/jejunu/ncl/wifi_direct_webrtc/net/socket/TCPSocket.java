package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.socket;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jinhy on 2016-12-04.
 */

public abstract class TCPSocket implements Socket, Runnable {
    protected final ExecutorService executor;
    protected final Object rawSocketLock;
    protected Socket.ChannelEvent channelEvent;

    protected InetAddress address;
    protected int port;
    protected Thread thread;

    public TCPSocket(InetAddress address, int port, Socket.ChannelEvent channelEvent) {
        this.executor = Executors.newSingleThreadExecutor();
        this.rawSocketLock = new Object();
        this.channelEvent = channelEvent;
        this.address = address;
        this.port = port;
    }

    @Override
    public void start() {
        thread = new Thread(this);
        thread.start();
    }
}
