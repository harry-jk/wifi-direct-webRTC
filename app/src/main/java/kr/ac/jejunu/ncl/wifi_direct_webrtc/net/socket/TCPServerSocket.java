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

public class TCPServerSocket extends TCPSocket {
    private ServerSocket serverSocket;
    protected PrintWriter out;
    private Socket rawSocket;

    public TCPServerSocket(InetAddress address, int port, ChannelEvent channelEvent) {
        super(address, port, channelEvent);
    }

    public void connect() {
        final ServerSocket tempSocket;
        try {
            tempSocket = new ServerSocket();
            tempSocket.setReuseAddress(true);
            tempSocket.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            channelEvent.onError("Failed to create server socket: " + e.getMessage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            return;
        }

        synchronized (rawSocketLock) {
            serverSocket = tempSocket;
        }
    }


    @Override
    public void disconnect() {
        try {
            synchronized (rawSocketLock) {
                if (serverSocket != null) {
                    serverSocket.close();
                    serverSocket = null;
                }
            }
        } catch (IOException e) {
            channelEvent.onError("Failed to close server socket: " + e.getMessage());
        }

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
        // Receive connection to temporary variable first, so we don't block.
        while (true) {
            Socket rawSocket = null;
            if(serverSocket == null) {
                connect();
            }
            try {
                if(serverSocket != null) {
                    rawSocket = serverSocket.accept();
                } else if(serverSocket == null) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                if(serverSocket == null) return;
            }
            if(rawSocket != null) {
                final Socket finalRawSocket = rawSocket;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BufferedReader in;

                        synchronized (finalRawSocket) {
                            // Connecting failed, error has already been reported, just exit.
                            if (finalRawSocket == null) {
                                return;
                            }

                            try {
                                out = new PrintWriter(finalRawSocket.getOutputStream(), true);
                                in = new BufferedReader(new InputStreamReader(finalRawSocket.getInputStream()));
                            } catch (IOException e) {
//                                channelEvent.onError("Failed to open IO on rawSocket: " + e.getMessage());
                                return;
                            }
                        }

                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                channelEvent.onConnected(true);
                            }
                        });

                        while (true) {
                            final String message;
                            try {
                                message = in.readLine();
                            } catch (IOException e) {
                                synchronized (finalRawSocket) {
                                    // If socket was closed, this is expected.
                                    if (finalRawSocket == null) {
                                        break;
                                    }
                                }

//                                channelEvent.onError("Failed to read from rawSocket: " + e.getMessage());
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
                }).start();
            }
        }
    }
}
