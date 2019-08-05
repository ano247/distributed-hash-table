package dht;

import socket.Messenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static socket.Server.CLOSE;

public class Closer
        implements Messenger<Boolean> {

    @Override
    public Boolean message(ObjectInputStream in, ObjectOutputStream out)
            throws IOException {
        out.writeInt(CLOSE);
        out.flush();

        boolean result = in.readBoolean();

        in.close();
        return result;
    }
}
