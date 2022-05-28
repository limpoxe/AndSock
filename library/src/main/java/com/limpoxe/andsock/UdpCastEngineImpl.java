package com.limpoxe.andsock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpCastEngineImpl implements Engine {
    private static final String TAG = "UdpCastEngineImpl";

    private final String ip;
    private int localPort;
    private int remotePort;
    private final int datagramPacketLen ;

    private DatagramSocket socket;
    private InetAddress defaultInetAddress;

    public UdpCastEngineImpl(String ip, int localPort, int remotePort, int bufferSize) {
        this.ip = ip;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.datagramPacketLen = bufferSize;
    }

    public boolean open() {
        try {
            if (ip != null) {
                defaultInetAddress = InetAddress.getByName(ip);
            }
        } catch (Exception e) {
        }
        try {
            socket = new DatagramSocket(localPort);
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

    public boolean write(byte b[], int off, int len, InetAddress address) {
        try {
            if (len > datagramPacketLen) {
                LogUtil.log(TAG, " write fail " + len + " > " + datagramPacketLen);
                return false;
            }
            if (defaultInetAddress == null && address == null) {
                LogUtil.log(TAG, " fatal error remote address not set!! ");
                return false;
            }
            byte[] bytes = new byte[datagramPacketLen];
            System.arraycopy(b, off, bytes, 0, len);
            socket.send(new DatagramPacket(bytes, 0, datagramPacketLen, defaultInetAddress!=null?defaultInetAddress:address, remotePort));
            return true;
        } catch (Exception e) {
            LogUtil.log(TAG, " write fail " + e.getMessage());
        }
        return false;
    }

    public int read(byte b[], int off, int len, InetAddress[] addressHolder) {
        try {
            DatagramPacket datagramPacket = new DatagramPacket(b, off, len);
            socket.receive(datagramPacket);
            addressHolder[0] = datagramPacket.getAddress();
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
