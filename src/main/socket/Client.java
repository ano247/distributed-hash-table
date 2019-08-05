package socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Client<T> {

    private Messenger<T> messenger;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Client(int hostPort, Messenger<T> messenger)
            throws IOException {
        this.messenger = messenger;

        Socket connection = new Socket(InetAddress.getLocalHost(), hostPort);
        out = new ObjectOutputStream(connection.getOutputStream());
        in = new ObjectInputStream(connection.getInputStream());
    }

    public T request() {
        try {
            return messenger.message(in, out);
        } catch (Exception e) {
            return null;
        }
    }
}
