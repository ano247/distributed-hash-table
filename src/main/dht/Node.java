package dht;

import java.io.Serializable;

public class Node
        implements Serializable {

    public final int id;
    public final int port;

    public Node(int id, int port) {
        this.id = id;
        this.port = port;
    }
}
