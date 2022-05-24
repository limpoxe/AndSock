package com.limpoxe.andsock;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Socket {
    private static final String TAG = "Socket";

    private Options options;
    private volatile Engine mEngine;
    private volatile int sid = 0;
    private volatile boolean connected;

    private final Map<Integer, Ack> acks = new HashMap<>();
    private final Queue<Packet> sendBuffer = new LinkedList<>();
    private final ExcutorThread ReadThread;
    private final ExcutorThread WriteThread;
    private final ScheduledExecutorService TimerThread;
    private volatile int packetSeqId;
    private volatile int lastReqSeqId;
    private final Manager manager;

    public Socket(Options options) {
        this.options = options;
        this.TimerThread = Executors.newSingleThreadScheduledExecutor(new SoThreadFactory(options.mode + "-Timer"));
        this.ReadThread = new ExcutorThread(options.mode + "-" + options.protocol + "-Read");
        this.WriteThread = new ExcutorThread(options.mode + "-" + options.protocol + "-Write");
        this.manager = new Manager(this, TimerThread, options);
    }

    public Socket connect() {
        WriteThread.exec(new Runnable() {
            @Override
            public void run() {
                LogUtil.log(TAG, "connect");
                if (Socket.this.connected) {
                    LogUtil.log(TAG, "already connected, trigger onConnect callback");
                    manager.onConnect();
                } else {
                    Engine engine = EngineFactory.newEngine(options);
                    if (engine.open()) {
                        sid++;
                        LogUtil.log(TAG, "Engine opened, sid=" + sid);

                        Socket.this.mEngine = engine;
                        Socket.this.connected = true;

                        LogUtil.log(TAG, "trigger onConnect callback");
                        manager.onConnect();

                        read();

                        try {
                            //等read线程起来
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        sendBuffer();
                    } else {
                        sid++;
                        LogUtil.log(TAG, "Engine open failed, sid=" + sid);
                        LogUtil.log(TAG, "trigger onDisconnect callback");
                        manager.onDisconnect();
                    }
                }
            }
        });
        return this;
    }

    private void read() {
        ReadThread.exec(new Runnable() {
            @Override
            public void run() {
                LogUtil.log(TAG, "Packet read thread started");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[mEngine.getReadBufferLen()];
                int dataLen = 0;
                int readLen = 0;
                while ((readLen = mEngine.read(buf, 0, buf.length)) != -1) {
                    try {
                        bos.write(buf, 0, readLen);
                        if (dataLen == 0 && bos.size() >= 4) {
                            dataLen = ByteOrder.byte4ToIntB(bos.toByteArray());
                        }
                        if (dataLen != 0 && bos.size() >= dataLen) {
                            LogUtil.log(TAG, "Packet arrived ");
                            byte[] packetBytes = bos.toByteArray();
                            if (packetBytes.length > dataLen) {
                                byte[] packetBytesTemp = new byte[dataLen];
                                System.arraycopy(packetBytes, 0, packetBytesTemp, 0, dataLen);
                                packetBytes = packetBytesTemp;
                            }
                            Packet packet = Packet.unpack(packetBytes);
                            bos.reset();
                            dataLen = 0;

                            if (packet != null) {
                                if (packet.type == Packet.TYPE_REQ) {
                                    onReq(packet);
                                } else if (packet.type == Packet.TYPE_ACK) {
                                    onAck(packet);
                                }
                            } else {
                                LogUtil.log(TAG, "Packet unpack fail，should not happen！ ");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtil.log(TAG, "ReadThread IOException ");
                    }
                }
                LogUtil.log(TAG, "Packet read thread done");

                LogUtil.log(TAG, "Try ping (to trigger disconnect event)");
                send(options.heartbeatReq, null);
                send(options.heartbeatReq, null);
            }
        });
    }

    private void onReq(Packet packet) {
        LogUtil.log(TAG, "Packet is req, packet=" + packet);
        lastReqSeqId = packet.id;
        manager.onReqArrive(packet);
    }

    private void onAck(Packet packet) {
        LogUtil.log(TAG, "Packet is ack, packet=" + packet);
        Ack ack = acks.remove(packet.id);
        if (ack != null) {
            try {
                LogUtil.log(TAG, "trigger ack callback");
                ack.onAckArrive(packet);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.log(TAG, "call onAckArrive cause exception: " + e.getMessage());
            }
        } else {
            LogUtil.log(TAG, "id=" + packet.id + ", no ack callback, ignore!");
        }
    }

    public Socket send(final byte[] data, final Ack ack) {
        WriteThread.exec(new Runnable() {
            @Override
            public void run() {
                LogUtil.log(TAG, "send req, id=" + packetSeqId);
                Packet packet = new Packet(Packet.TYPE_REQ, packetSeqId, data);
                if (ack != null) {
                    Socket.this.acks.put(packetSeqId, ack);
                }
                if (packetSeqId == Integer.MAX_VALUE) {
                    packetSeqId = 0;
                } else {
                    packetSeqId++;
                }

                if (Socket.this.connected) {
                    write(packet);
                    if (ack != null) {
                        ackTimeout(packet);
                    }
                } else {
                    LogUtil.log(TAG, "add packet to buffer, packet=" + packet);
                    Socket.this.sendBuffer.add(packet);
                    bufferAndAckTimeout(packet);
                }
            }
        });
        return this;
    }

    private void sendBuffer() {
        //如果有缓存的数据包，将缓存的数据包都发出去
        Packet packet = null;
        while ((packet = sendBuffer.poll()) != null) {
            LogUtil.log(TAG, "send buffered packet=" + packet);
            write(packet);
        }
    }

    public Socket ack(int id, byte[] data) {
        WriteThread.exec(new Runnable() {
            @Override
            public void run() {
                LogUtil.log(TAG, "send ack, id=" + id);

                Packet packet = new Packet(Packet.TYPE_ACK, id, data);

                if (Socket.this.connected) {
                    write(packet);
                } else {
                    LogUtil.log(TAG, "add packet to buffer, packet=" + packet);
                    Socket.this.sendBuffer.add(packet);
                    bufferTimeout(packet);
                }
            }
        });
        return this;
    }

    private void ackTimeout(Packet packet) {
        TimerThread.schedule(new Runnable() {
            @Override
            public void run() {
                WriteThread.exec(new Runnable() {
                    @Override
                    public void run() {
                        Ack ack = Socket.this.acks.remove(packet.id);
                        if (ack != null) {
                            LogUtil.log(TAG, "waiting for ack timeout[" + options.packetTimeout + "], packet=" + packet);
                            try {
                                ack.onTimeout(packet);
                            } catch (Exception e) {
                                e.printStackTrace();
                                LogUtil.log(TAG, "call onTimeout cause exception: " + e.getMessage());
                            }
                        }
                    }
                });
            }
        }, options.packetTimeout, TimeUnit.MILLISECONDS);
    }

    private void bufferTimeout(Packet packet) {
        TimerThread.schedule(new Runnable() {
            @Override
            public void run() {
                WriteThread.exec(new Runnable() {
                    @Override
                    public void run() {
                        boolean ret = Socket.this.sendBuffer.remove(packet);
                        if (ret) {
                            LogUtil.log(TAG, "waiting for sent ack timeout[" + options.packetTimeout + "], packet=" + packet);
                        }
                    }
                });
            }
        }, options.packetTimeout, TimeUnit.MILLISECONDS);
    }

    private void bufferAndAckTimeout(Packet packet) {
        TimerThread.schedule(new Runnable() {
            @Override
            public void run() {
                WriteThread.exec(new Runnable() {
                    @Override
                    public void run() {
                        Ack ack = Socket.this.acks.remove(packet.id);
                        boolean ret = Socket.this.sendBuffer.remove(packet);
                        if (ack != null) {
                            LogUtil.log(TAG, "waiting for ack timeout[" + options.packetTimeout + "], packet=" + packet);
                            try {
                                ack.onTimeout(packet);
                            } catch (Exception e) {
                                e.printStackTrace();
                                LogUtil.log(TAG, "call onTimeout cause exception: " + e.getMessage());
                            }
                        } else if (ret) {
                            LogUtil.log(TAG, "send req packet timeout, packet=" + packet);
                        }
                    }
                });
            }
        }, options.packetTimeout, TimeUnit.MILLISECONDS);
    }

    private void write(Packet packet) {
        LogUtil.log(TAG, "write, packet=" + packet);
        byte[] pak = Packet.pack(packet);
        if (pak != null && pak.length > 0) {
            boolean ret = mEngine.write(pak, 0, pak.length);
            if (!ret) {
                LogUtil.log(TAG, "write packet fail");
                LogUtil.log(TAG, "try disconnect");
                doDisconnect();
            } else {
                LogUtil.log(TAG, "write packet success");
            }
        } else {
            LogUtil.log(TAG, "write packet fail");
        }
    }

    public Socket disconnect() {
        WriteThread.exec(new Runnable() {
            @Override
            public void run() {
                doDisconnect();
            }
        });
        return this;
    }

    private void doDisconnect() {
        LogUtil.log(TAG, "disconnect");
        if (Socket.this.connected) {
            LogUtil.log(TAG, "Engine closed, sid=" + sid);
            mEngine.close();
            connected = false;
        } else {
            LogUtil.log(TAG, "already disconnected");
        }

        LogUtil.log(TAG, "trigger onDisconnect callback");
        manager.onDisconnect();
    }

    public boolean connected() {
        return this.connected;
    }

    public int getSid() {
        return this.sid;
    }

    public Socket registerConnectStateChange(ConnectStateListener listener) {
        manager.registerConnectStateChange(listener);
        return this;
    }

    public Socket registerHeartBeatListener(HeartBeatListener listener) {
        manager.registerHeartBeatListener(listener);
        return this;
    }

    public Socket unregisterConnectStateChange(ConnectStateListener listener) {
        manager.unregisterConnectStateChange(listener);
        return this;
    }

    public Socket registerReqListener(Req reqListener) {
        manager.registerReqListener(reqListener);
        return this;
    }

    public Socket unregisterReqListener(Req reqListener) {
        manager.unregisterReqListener(reqListener);
        return this;
    }

    /**
     * 彻底销毁此对象，不再使用
     */
    public void destroy() {
        TimerThread.shutdown();
        ReadThread.shutdown();
        WriteThread.shutdown();

        connected = false;
        mEngine.close();

        acks.clear();
        sendBuffer.clear();
    }

    public Socket fork() {
        return new Socket(options);
    }

    public static interface ConnectStateListener  {
        public void onConnect();
        public void onDisconnect();
    }

    public static interface HeartBeatListener {
        public void onBeat();
        public void onTimeout();
    }

    public static interface Req {
        void onReqArrive(Packet req);
    }

    public static interface Ack {
        public void onAckArrive(Packet ack);
        public void onTimeout(Packet req);
    }

    public static class Options {
        public static final String MODE_SERVER = "Server";
        public static final String MODE_CLIENT = "Client";

        public static final String PROTOCOL_TCP = "TCP";
        public static final String PROTOCOL_UDP_UNICAST = "UDP_UNICAST";
        public static final String PROTOCOL_UDP_MULTICAST = "UDP_MULTICAST";
        public static final String PROTOCOL_UDP_CAST = "UDP_CAST";

        public String ip = null;
        //本地监听端口
        public int localPort = 0;
        //远程监听端口
        public int remotePort = 0;
        //超时时间：发送req包超时，发送ack包超时，等待ack包超时
        public long packetTimeout = 15 * 1000;
        //tcp or udp
        public String protocol = PROTOCOL_TCP;
        public int udpBufferSize = 1500 - 20 - 8;
        //服务端模式还是客户端模式
        public String mode = MODE_SERVER;
        //连接失败时每隔一段时间自动重试, 0表示不自动重连
        public long autoConnectDelay = 5 * 1000;
        //自动重连最大次数，0表示不限
        public int autoConnectRetryMaxTimes = 0;
        //每隔一段时间进行发送1次心跳, 0表示无心跳
        public long heartbeatDelay = 30 * 1000;
        //心跳连续超时次数超过最大次数则断开连接
        public int heartbeatTimeoutMaxTimes = 3;
        public byte[] heartbeatReq;
        public byte[] heartbeatAck;
        public Options() {
        }
    }
}

