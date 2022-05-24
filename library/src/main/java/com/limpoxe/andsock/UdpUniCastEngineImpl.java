package com.limpoxe.andsock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpUniCastEngineImpl implements Engine {
    private static final String TAG = "UdpUniCastEngineImpl";

    private final String mode;
    private final String ip;
    private int localPort;
    private int remotePort;
    private final int datagramPacketLen ;

    private DatagramSocket socket;
    private InetAddress inetAddress;

    public UdpUniCastEngineImpl(String mode, String ip, int localPort, int remotePort, int bufferSize) {
        this.mode = mode;
        this.ip = ip;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.datagramPacketLen = bufferSize;
    }

    public boolean open() {
        try {
            socket = new DatagramSocket(localPort);
            inetAddress = InetAddress.getByName(ip);
            LogUtil.log(TAG, "ReceiveBufferSize=" + socket.getReceiveBufferSize() + " SendBufferSize=" + socket.getSendBufferSize() + " SoTimeout=" + socket.getSoTimeout());
        } catch (Exception e) {
            LogUtil.log(TAG, " open fail " + e.getMessage());
            close();
            return false;
        }
        return true;
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, " outputStream close fail " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public boolean write(byte b[], int off, int len) {
        try {
            if (len > datagramPacketLen) {
                LogUtil.log(TAG, " write fail " + len + " > " + datagramPacketLen);
                return false;
            }
            byte[] bytes = new byte[datagramPacketLen];
            System.arraycopy(b, off, bytes, 0, len);
            socket.send(new DatagramPacket(bytes, 0, datagramPacketLen, inetAddress, remotePort));
        } catch (Exception e) {
            LogUtil.log(TAG, " write fail " + e.getMessage());
            return false;
        }
        return true;
    }

    public int read(byte b[], int off, int len) {
        try {
            socket.receive(new DatagramPacket(b, off, len));
            return len;
        } catch (Exception e) {
            LogUtil.log(TAG, " read fail " + e.getMessage());
        }
        return -1;
    }

    @Override
    public int getReadBufferLen() {
        return datagramPacketLen;
    }
}
