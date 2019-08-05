package dht;

import socket.Messenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static dht.Dht.ABSENT;
import static dht.Dht.GET;

public class Getter
        implements Messenger<Integer> {

    private int key;

    public Getter(int key) {
        this.key = key;
    }

    @Override
    public Integer message(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        out.writeInt(GET);
        out.writeInt(key);
        out.flush();

        int result = in.readInt();
        in.close();

        return result == ABSENT ? null : result;
    }
}
