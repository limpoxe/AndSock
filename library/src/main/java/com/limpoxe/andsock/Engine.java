package com.limpoxe.andsock;

import java.net.InetAddress;

public interface Engine {

    public boolean open();

    public void close();

    public boolean write(byte b[], int off, int len, InetAddress address);

    public int read(byte b[], int off, int len, InetAddress[] addressHolder);

    public int getReadBufferLen();
}
