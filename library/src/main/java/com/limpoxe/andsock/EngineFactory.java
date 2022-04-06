package com.limpoxe.andsock;

public class EngineFactory {
    private static final String TAG = "EngineFactory";

    public static Engine newEngine(Socket.Options options) {
        if (Socket.Options.PROTOCOL_TCP.equals(options.protocol)) {
            return new TcpEngineImpl(options.mode, options.ip, options.localPort, options.remotePort);
        } else if (Socket.Options.PROTOCOL_UDP_MULTICAST.equals(options.protocol)) {
            return new UdpMultiCastEngineImpl(options.mode, options.ip, options.localPort, options.remotePort, options.udpBufferSize);
        } else if (Socket.Options.PROTOCOL_UDP_CAST.equals(options.protocol)) {
            return new UdpCastEngineImpl(options.mode, options.ip, options.localPort, options.remotePort, options.udpBufferSize);
        }
        return null;
    }
}
