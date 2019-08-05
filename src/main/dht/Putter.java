package dht;

import socket.Messenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static dht.Dht.PUT;

public class Putter
        implements Messenger<Boolean> {

    private int key, value;

    public Putter(int key, int value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public Boolean message(ObjectInputStream in, ObjectOutputStream out)
            throws IOException {
        out.writeInt(PUT);
        out.writeInt(key);
        out.writeInt(value);
        out.flush();

        boolean result = in.readBoolean();

        in.close();
        return result;
    }
}
