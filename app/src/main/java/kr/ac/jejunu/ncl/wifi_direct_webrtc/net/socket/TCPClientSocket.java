package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.net.Socket;

/**
 * Created by jinhy on 2016-12-04.
 */

public class TCPClientSocket extends TCPSocket {
    private Socket rawSocket;
    private PrintWriter out;

    public TCPClientSocket(InetAddress address, int port, ChannelEvent channelEvent) {
        super(address, port, channelEvent);
    }

    private Socket connect() {
        try {
            return new Socket(address, port);
        } catch (IOException e) {
            channelEvent.onError("Failed to connect: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void disconnect() {
        try {
            synchronized (rawSocketLock) {
                if (rawSocket != null) {
                    rawSocket.close();
                    rawSocket = null;
                    out = null;

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            channelEvent.onClose();
                        }
                    });
                }
            }
        } catch (IOException e) {
            channelEvent.onError("Failed to close rawSocket: " + e.getMessage());
        }
    }

    @Override
    public void send(String message) {
        synchronized (rawSocketLock) {
            if (out == null) {
                channelEvent.onError("Sending data on closed socket.");
                return;
            }
            out.write(message + "\n");
            out.flush();
        }
    }

    @Override
    public void run() {
        java.net.Socket tempSocket = connect();
        BufferedReader in;

        synchronized (rawSocketLock) {
            rawSocket = tempSocket;

            // Connecting failed, error has already been reported, just exit.
            if (rawSocket == null) {
                return;
            }

            try {
                out = new PrintWriter(rawSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(rawSocket.getInputStream()));
            } catch (IOException e) {
                channelEvent.onError("Failed to open IO on rawSocket: " + e.getMessage());
                return;
            }
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                channelEvent.onConnected(false);
            }
        });

        while (true) {
            final String message;
            try {
                message = in.readLine();
            } catch (IOException e) {
                synchronized (rawSocketLock) {
                    // If socket was closed, this is expected.
                    if (rawSocket == null) {
                        break;
                    }
                }

                channelEvent.onError("Failed to read from rawSocket: " + e.getMessage());
                break;
            }

            // No data received, rawSocket probably closed.
            if (message == null) {
                break;
            }

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    channelEvent.onMessage(message);
                }
            });
        }
        // Close the rawSocket if it is still open.
        disconnect();
    }
}
