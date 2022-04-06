package com.limpoxe.andsock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TcpEngineImpl implements Engine {
    private static final String TAG = "TcpEngineImpl";

    private final String mode;
    private final String ip;
    private final int localPort;
    private final int remotePort;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public TcpEngineImpl(String mode, String ip, int localPort, int remotePort) {
        this.mode = mode;
        this.ip = ip;
        this.localPort = localPort;
        this.remotePort = remotePort;
    }

    public boolean open() {
        try {
            socket = SocketFactory.newSocket(mode, ip, localPort, remotePort);
            socket.setKeepAlive(true);
            socket.setSoTimeout(0);
            socket.setTcpNoDelay(true);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (Exception e) {
            LogUtil.log(TAG, " open fail " + e.getMessage());
            close();
            return false;
        }
        return true;
    }

    public void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, " outputStream close fail " + e.getMessage());
        }
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, " inputStream close fail " + e.getMessage());
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, " socket close fail " + e.getMessage());
        }
    }

    public boolean write(byte b[], int off, int len) {
        try {
            if (outputStream != null) {
                outputStream.write(b, off, len);
                outputStream.flush();
            }
        } catch (IOException e) {
            LogUtil.log(TAG, " write fail " + e.getMessage());
            return false;
        }
        return true;
    }

    public int read(byte b[], int off, int len) {
        try {
            if (inputStream != null) {
                return inputStream.read(b, off, len);
            }
        } catch (IOException e) {
            LogUtil.log(TAG, " read fail " + e.getMessage());
        }
        return -1;
    }

    @Override
    public int getReadBufferLen() {
        return 1;
    }
}
