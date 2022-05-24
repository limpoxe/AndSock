package com.limpoxe.andsock;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Manager {
    private static final String TAG = "Manager";

    private final ScheduledExecutorService Timer;
    private final Socket socket;
    private final Socket.Options options;
    private Socket.Req req;
    private Socket.ConnectStateListener connectStateListener;
    private Socket.HeartBeatListener heartBeatListener;

    private int pingTimeoutTimes;
    private ScheduledFuture pingScheduledFuture = null;
    private Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            if (socket.connected()) {
                //发空包作为心跳
                LogUtil.log(TAG, "send heartbeat req[" + options.heartbeatDelay + "]");
                socket.send(options.heartbeatReq, new Socket.Ack() {
                    @Override
                    public void onAckArrive(Packet ack) {
                        pingTimeoutTimes = 0;
                        LogUtil.log(TAG, "heartbeat success");
                        if (heartBeatListener != null) {
                            heartBeatListener.onBeat();
                        }
                    }
                    @Override
                    public void onTimeout(Packet req) {
                        pingTimeoutTimes++;
                        LogUtil.log(TAG, "heartbeat timeout, count=" + pingTimeoutTimes);
                        if (heartBeatListener != null) {
                            heartBeatListener.onTimeout();
                        }
                        if (pingTimeoutTimes >= options.heartbeatTimeoutMaxTimes) {
                            LogUtil.log(TAG, "heartbeatTimeoutMaxTimes=" + options.heartbeatTimeoutMaxTimes + ", try disconnect");
                            pingTimeoutTimes = 0;
                            socket.disconnect();
                        }
                    }
                });
            }
        }
    };

    private volatile int autoConnectRetryTimes;
    private Runnable connectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!socket.connected()) {
                if (options.autoConnectRetryMaxTimes == 0 || autoConnectRetryTimes < options.autoConnectRetryMaxTimes) {
                    LogUtil.log(TAG, options.mode + " not connected, try connect[" + options.autoConnectDelay + "]" + autoConnectRetryTimes);
                    autoConnectRetryTimes++;
                    socket.connect();
                }
            }
        }
    };

    Manager(Socket socket, ScheduledExecutorService timer, Socket.Options options) {
        this.socket = socket;
        this.options = options;
        this.Timer = timer;
    }

    void registerConnectStateChange(Socket.ConnectStateListener listener) {
        this.connectStateListener = listener;
    }

    void unregisterConnectStateChange(Socket.ConnectStateListener listener) {
        this.connectStateListener = null;
    }

    void registerHeartBeatListener(Socket.HeartBeatListener listener) {
        this.heartBeatListener = listener;
    }

    void unregisterHeartBeatListener(Socket.HeartBeatListener listener) {
        this.heartBeatListener = null;
    }

    void registerReqListener(Socket.Req reqListener) {
        this.req = reqListener;
    }

    void unregisterReqListener(Socket.Req reqListener) {
        this.req = null;
    }

    private boolean isHeartbeatReq(Packet packet) {
        if ((packet.data == null || packet.data.length == 0)
                && (options.heartbeatReq == null || options.heartbeatReq.length == 0)) {
            return true;
        }
        if (!(packet.data == null || packet.data.length == 0)
               &&  !(options.heartbeatReq == null || options.heartbeatReq.length == 0)) {
            if (packet.data.length == options.heartbeatReq.length) {
                for (int i = 0; i < options.heartbeatReq.length; i++) {
                    if (packet.data[i] != options.heartbeatReq[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    void onReqArrive(Packet packet) {
        if (isHeartbeatReq(packet)) {
            LogUtil.log(TAG, "heartbeat, Auto-Ack!");
            socket.ack(packet.id, options.heartbeatAck);
            if (heartBeatListener != null) {
                heartBeatListener.onBeat();
            }
        } else {
            if (req != null) {
                try {
                    LogUtil.log(TAG, "trigger req callback");
                    req.onReqArrive(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtil.log(TAG, "call onReqArrive cause exception: " + e.getMessage());
                }
            } else {
                LogUtil.log(TAG, "no req callback, ignore!");
            }
        }
    }

    void onConnect() {
        LogUtil.log(TAG, "onConnect");
        //重置重连次数
        autoConnectRetryTimes = 0;
        //启动心跳定时器
        if (pingScheduledFuture == null && options.heartbeatDelay > 0) {
            pingScheduledFuture = Timer.scheduleAtFixedRate(pingRunnable, 0, options.heartbeatDelay, TimeUnit.MILLISECONDS);
        }
        if (connectStateListener != null) {
            try {
                connectStateListener.onConnect();
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.log(TAG, "call onConnect cause exception: " + e.getMessage());
            }
        }
    }

    void onDisconnect() {
        LogUtil.log(TAG, "onDisconnect");
        //启动重连定时器
        if (options.autoConnectDelay > 0) {
            Timer.schedule(connectRunnable, options.autoConnectDelay, TimeUnit.MILLISECONDS);
        }
        if (connectStateListener != null) {
            try {
                connectStateListener.onDisconnect();
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.log(TAG, "call onDisconnect cause exception: " + e.getMessage());
            }
        }
    }
}
