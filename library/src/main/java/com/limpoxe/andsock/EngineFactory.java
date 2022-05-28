package com.limpoxe.andsock;

public class EngineFactory {
    private static final String TAG = "EngineFactory";

    public static Engine newEngine(Socket.Options options) {
        if (Socket.Options.PROTOCOL_TCP.equals(options.protocol)) {
            return new TcpEngineImpl(options.mode, options.ip, options.localPort, options.remotePort);
        } else if (Socket.Options.PROTOCOL_UDP_MULTICAST.equals(options.protocol)) {
            return new UdpMultiCastEngineImpl(options.ip, options.localPort, options.remotePort, options.udpBufferSize);
        } else if (Socket.Options.PROTOCOL_UDP_CAST.equals(options.protocol)) {
            //广播地址应该通过IP和子网掩码计算
            if (options.ip != null && options.ip.endsWith(".255")) {
                return new UdpCastEngineImpl(options.ip, options.localPort, options.remotePort, options.udpBufferSize);
            } else {
                LogUtil.log(TAG, options.protocol + " not support with ip " + options.ip);
            }
        } else if (Socket.Options.PROTOCOL_UDP_UNICAST.equals(options.protocol)) {
            return new UdpCastEngineImpl(null, options.localPort, options.remotePort, options.udpBufferSize);
        }
        LogUtil.log(TAG, "new engine failed");
        return null;
    }
}
