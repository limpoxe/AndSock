package com.limpoxe.andsock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class SocketFactory {
    private static final String TAG = "SocketFactory";
    private static ServerSocket serverSocket;

    public static java.net.Socket newSocket(String mode, String ip, int localPort, int remotePort) throws IOException {
        if (Socket.Options.MODE_CLIENT.equals(mode)) {
            return newClient(ip, remotePort);
        } else if (Socket.Options.MODE_SERVER.equals(mode)) {
            return newServer(ip, localPort);
        }
        return null;
    }

    private static java.net.Socket newClient(String ip, int port) throws IOException {
        LogUtil.log(TAG, "new Socket " + ip + ":" + port);
        return new java.net.Socket(ip, port);
    }

    private static java.net.Socket newServer(String ip, int port) throws IOException {
        synchronized (SocketFactory.class) {
            if (serverSocket == null || serverSocket.isClosed()) {
                LogUtil.log(TAG, "new ServerSocket " + ip + ":" + port);
                serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ip));
            }
        }
        LogUtil.log(TAG, "ServerSocket accept");
        return serverSocket.accept();
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
