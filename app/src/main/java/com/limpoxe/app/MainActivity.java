package com.limpoxe.app;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.limpoxe.andsock.LogUtil;
import com.limpoxe.andsock.Socket;

import org.json.JSONObject;

import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static Socket client = null;
    private static Socket server = null;
    private static Socket multicast = null;
    private static Socket unicast = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LogUtil.setLogger(msg -> System.out.println(msg));

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init();
                test1();
                test2();
                test3();
            }
        });
    }

    private void init() {
        if (server == null) {
            Socket.Options serverOptions = new Socket.Options();
            serverOptions.mode = Socket.Options.MODE_SERVER;
            serverOptions.ip = "127.0.0.1";
            serverOptions.localPort = 9527;
            serverOptions.heartbeatDelay = 0;
            server = new Socket(serverOptions);
            server.registerConnectStateChange(new Socket.ConnectStateListener() {
                @Override
                public void onConnect() {
                    LogUtil.log(TAG, "Server-Side: onConnect");
                }
                @Override
                public void onDisconnect() {
                    LogUtil.log(TAG, "Server-Side: onDisconnect");
                }
            });
            server.registerReqListener(new Socket.Req() {
                @Override
                public void onReqArrive(int packetId, byte[] data, InetAddress sourceAddress) {
                    LogUtil.log(TAG, "on Req: " + new String(data));
                    String str = "tcp ack message";
                    LogUtil.log(TAG, "Ack: " + str);
                    server.ack(packetId, str.getBytes());
                }
            });
            server.connect();
        }
        if (client == null) {
            Socket.Options clientOptions = new Socket.Options();
            clientOptions.mode = Socket.Options.MODE_CLIENT;
            clientOptions.ip = "127.0.0.1";
            clientOptions.remotePort = 9527;
            clientOptions.heartbeatDelay = 20000;
            client = new Socket(clientOptions);
            client.registerConnectStateChange(new Socket.ConnectStateListener() {
                @Override
                public void onConnect() {
                    LogUtil.log(TAG, "Client-Side: onConnect");
                }
                @Override
                public void onDisconnect() {
                    LogUtil.log(TAG, "Client-Side: onDisconnect");
                }
            });
            client.connect();
        }
        if (multicast == null) {
            Socket.Options options = new Socket.Options();
            options.ip = "239.192.168.1";//239.0.0.0 ~ 239.255.255.255
            options.localPort = 5279;
            options.remotePort = 5279;
            options.protocol = Socket.Options.PROTOCOL_UDP_MULTICAST;
            options.udpBufferSize = 512;
            multicast = new Socket(options);
            multicast.registerReqListener(new Socket.Req() {
                @Override
                public void onReqArrive(int packetId, byte[] data, InetAddress sourceAddress) {
                    try {
                        JSONObject jsonObject = new JSONObject(new String(data));
                        String cmd = jsonObject.optString("cmd");
                        if ("who".equals(cmd)) {
                            JSONObject response = new JSONObject();
                            response.put("cmd", "who_reply");
                            response.put("name", "Cindy");
                            //组播不支持回调，无ack参数
                            multicast.send(response.toString().getBytes(), null);
                        } else if ("who_reply".equals(cmd)) {
                            LogUtil.log("who_reply", "name=" + jsonObject.optString("name"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            multicast.connect();
        }
        if (unicast == null) {
            Socket.Options options = new Socket.Options();
            options.ip = "127.0.0.1";
            options.localPort = 2795;
            options.remotePort = 2795;
            options.protocol = Socket.Options.PROTOCOL_UDP_UNICAST;
            options.udpBufferSize = 256;
            unicast = new Socket(options);
            unicast.registerReqListener(new Socket.Req() {
                @Override
                public void onReqArrive(int packetId, byte[] data, InetAddress sourceAddress) {
                    try {
                        LogUtil.log("onReqArrive", "data=" + new String(data));
                        String str = "Ack unicast message";
                        LogUtil.log(TAG, "send Ack: " + str);
                        unicast.ack(sourceAddress.getHostAddress(), packetId, str.getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            unicast.connect();
        }
    }

    private void test1() {
        String str = "tcp req message";
        LogUtil.log(TAG, "Req: " + str);
        client.send(str.getBytes(), new Socket.Ack() {
            @Override
            public void onAckArrive(byte[] data) {
                LogUtil.log(TAG, "on Ack：" + new String(data));
            }
            @Override
            public void onTimeout(byte[] data) {
                LogUtil.log(TAG, "发包超时");
            }
        });
    }

    private void test2() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "who");
            //组播不支持回调，无ack参数
            multicast.send(jsonObject.toString().getBytes(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test3() {
        String str = "Req unicast message";
        LogUtil.log(TAG, "send Req: " + str);
        unicast.send("127.0.0.1", str.getBytes(), new Socket.Ack() {
            @Override
            public void onAckArrive(byte[] data) {
                LogUtil.log("onAckArrive", "data=" + new String(data));
            }
            @Override
            public void onTimeout(byte[] data) {
                LogUtil.log("onTimeout", "data=" + new String(data));
            }
        });
    }
}
