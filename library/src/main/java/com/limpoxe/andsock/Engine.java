package com.limpoxe.andsock;

public interface Engine {

    public boolean open();

    public void close();

    public boolean write(byte b[], int off, int len);

    public int read(byte b[], int off, int len);

    public int getReadBufferLen();
}
