package com.limpoxe.app;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.limpoxe.andsock.LogUtil;
import com.limpoxe.andsock.Packet;
import com.limpoxe.andsock.Socket;

public class MainActivity extends AppCompatActivity {

    private static Socket client = null;
    private static Socket server = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LogUtil.setLogger(msg -> System.out.println(msg));

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (server == null) {
                    Socket.Options serverOptions = new Socket.Options();
                    serverOptions.mode = Socket.Options.MODE_SERVER;
                    serverOptions.ip = "127.0.0.1";
                    serverOptions.port = 9527;
                    serverOptions.heartbeatDelay = 0;
                    server = new Socket(serverOptions);
                    server.connect();
                    server.registerReqListener(new Socket.Req() {
                        @Override
                        public void onReqArrive(Packet req) {
                            System.out.println("on Req: " + new String(req.data));
                            String str = "我很好！";
                            System.out.println("Ack: " + str);
                            server.ack(req.id, str.getBytes());
                        }
                    });
                }
                if (client == null) {
                    Socket.Options clientOptions = new Socket.Options();
                    clientOptions.mode = Socket.Options.MODE_CLIENT;
                    clientOptions.ip = "127.0.0.1";
                    clientOptions.port = 9527;
                    clientOptions.heartbeatDelay = 5000;
                    client = new Socket(clientOptions);
                    client.connect();
                }
                String str = "你好吗？";
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
        });
    }

}
