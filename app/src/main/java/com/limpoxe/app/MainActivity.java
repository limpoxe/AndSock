package com.limpoxe.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.limpoxe.andsock.LogUtil;
import com.limpoxe.andsock.Packet;
import com.limpoxe.andsock.Socket;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static Socket client = null;
    private static Socket server = null;
    private static Socket scaner = null;
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
                    System.out.println("Server-Side: onConnect");
                }
                @Override
                public void onDisconnect() {
                    System.out.println("Server-Side: onDisconnect");
                }
            });
            server.registerReqListener(new Socket.Req() {
                @Override
                public void onReqArrive(Packet req) {
                    System.out.println("on Req: " + new String(req.data));
                    String str = "我很好！";
                    System.out.println("Ack: " + str);
                    server.ack(req.id, str.getBytes());
                }
            });
            server.connect();
        }
        if (client == null) {
            Socket.Options clientOptions = new Socket.Options();
            clientOptions.mode = Socket.Options.MODE_CLIENT;
            clientOptions.ip = "127.0.0.1";
            clientOptions.remotePort = 9527;
            clientOptions.heartbeatDelay = 5000;
            client = new Socket(clientOptions);
            client.registerConnectStateChange(new Socket.ConnectStateListener() {
                @Override
                public void onConnect() {
                    System.out.println("Client-Side: onConnect");
                }
                @Override
                public void onDisconnect() {
                    System.out.println("Client-Side: onDisconnect");
                }
            });
            client.connect();
        }
        if (scaner == null) {
            Socket.Options options = new Socket.Options();
            options.ip = "239.192.168.1";//239.0.0.0 ~ 239.255.255.255
            options.localPort = 5279;
            options.remotePort = 5279;
            options.protocol = Socket.Options.PROTOCOL_UDP_MULTICAST;
            options.heartbeatDelay = 0;
            options.udpBufferSize = 512;
            scaner = new Socket(options);
            scaner.registerReqListener(new Socket.Req() {
                @Override
                public void onReqArrive(Packet req) {
                    try {
                        String data = new String(req.data);
                        JSONObject jsonObject = new JSONObject(data);
                        String cmd = jsonObject.optString("cmd");
                        if ("who".equals(cmd)) {
                            JSONObject response = new JSONObject();
                            response.put("cmd", "who_reply");
                            response.put("name", "Cindy");
                            scaner.send(response.toString().getBytes(), null);
                        } else if ("who_reply".equals(cmd)) {
                            LogUtil.log("who_reply", "name=" + jsonObject.optString("name"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            scaner.connect();
        }
        if (unicast == null) {
            Socket.Options options = new Socket.Options();
            options.ip = "127.0.0.1";
            options.localPort = 2795;
            options.remotePort = 2795;
            options.protocol = Socket.Options.PROTOCOL_UDP_UNICAST;
            options.heartbeatDelay = 0;
            options.udpBufferSize = 256;
            unicast = new Socket(options);
            unicast.registerReqListener(new Socket.Req() {
                @Override
                public void onReqArrive(Packet req) {
                    try {
                        String data = new String(req.data);
                        LogUtil.log("onReqArrive", "data=" + data);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                String str = "unicast message";
                                System.out.println("send Req: " + str);
                                unicast.send(str.getBytes(), null);
                            }
                        }, 5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            unicast.connect();
        }
    }

    private void test1() {
        String str = "你好？";
        System.out.println("Req: " + str);
        client.send(str.getBytes(), new Socket.Ack() {
            @Override
            public void onAckArrive(Packet ack) {
                System.out.println("on Ack：" + new String(ack.data));
            }
            @Override
            public void onTimeout(Packet req) {
                System.out.println("发包超时");
            }
        });
    }

    private void test2() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "who");
            scaner.send(jsonObject.toString().getBytes(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test3() {
        String str = "unicast message";
        System.out.println("send Req: " + str);
        unicast.send(str.getBytes(), null);
    }
}
