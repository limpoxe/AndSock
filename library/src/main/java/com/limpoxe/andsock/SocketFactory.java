package com.limpoxe.andsock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class SocketFactory {
    private static final String TAG = "SocketFactory";
    private static ServerSocket serverSocket;

    public static java.net.Socket newSocket(String mode, String ip, int port) throws IOException {
        if (Socket.Options.MODE_SERVER.equals(mode)) {
            synchronized (SocketFactory.class) {
                if (serverSocket == null || serverSocket.isClosed()) {
                    LogUtil.log(TAG, "new ServerSocket " + ip + ":" + port);
                    serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ip));
                }
                LogUtil.log(TAG, "ServerSocket accept");
                return serverSocket.accept();
            }
        } else {
            LogUtil.log(TAG, "new Socket " + ip + ":" + port);
            return new java.net.Socket(ip, port);
        }
    }

    public static void close() {
        synchronized (SocketFactory.class) {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    LogUtil.log(TAG, "close ServerSocket");
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}