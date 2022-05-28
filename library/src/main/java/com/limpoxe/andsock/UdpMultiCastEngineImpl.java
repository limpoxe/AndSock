package com.limpoxe.andsock;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class UdpMultiCastEngineImpl implements Engine {
    private static final String TAG = "UdpMultiCastEngineImpl";

    private final String ip;
    private int localPort;
    private int remotePort;
    private final int datagramPacketLen ;

    private MulticastSocket socket;
    private InetAddress inetAddress;

    public UdpMultiCastEngineImpl(String ip, int localPort, int remotePort, int bufferSize) {
        this.ip = ip;//239.0.0.0 ~ 239.255.255.255
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.datagramPacketLen = bufferSize;
    }

    public boolean open() {
        try {
            socket = new MulticastSocket(localPort);
            inetAddress = InetAddress.getByName(ip);
            socket.joinGroup(inetAddress);
            socket.setLoopbackMode(false);
            LogUtil.log(TAG, "ReceiveBufferSize=" + socket.getReceiveBufferSize() + " SendBufferSize=" + socket.getSendBufferSize() + " TimeToLive="  +socket.getTimeToLive() + " SoTimeout=" + socket.getSoTimeout());
        } catch (Exception e) {
            LogUtil.log(TAG, " open fail " + e.getMessage());
            close();
            return false;
        }
        return true;
    }

    public void close() {
        try {
            if (socket != null && inetAddress != null) {
                socket.leaveGroup(inetAddress);
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
            if (address == null) {
                LogUtil.log(TAG, " fatal error remote address not set!! ");
                return false;
            }
            byte[] bytes = new byte[datagramPacketLen];
            System.arraycopy(b, off, bytes, 0, len);
            socket.send(new DatagramPacket(bytes, 0, datagramPacketLen, inetAddress, remotePort));
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
