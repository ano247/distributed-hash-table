package dht;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import socket.Client;
import socket.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DhtTest {

    private List<Thread> servers;

    @BeforeEach
    void setUp() {
        servers = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        servers.forEach(server -> {
            try {
                server.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private Thread startServer(int id, int port, int ring)
            throws IOException {
        Server server = new Server(port, new Dht(new Node(id, port), ring));

        Thread result = new Thread(() -> {
            try {
                server.respond();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        result.start();
        return result;
    }

    @Test
    void node()
            throws Exception {
        int id = 0, port = 5000;

        int key = 0, value = 0;

        servers.add(startServer(id, port, port));

        new Client<>(port, new Putter(key, value)).request();

        assertEquals(value, new Client<>(port, new Getter(key)).request());

        new Client<>(port, new Closer()).request();
    }

    @Test
    void ring()
            throws Exception {
        int id1 = 0, port1 = 5001;
        int id2 = 1, port2 = 5002;

        int key = 0, value = 0;

        servers.add(startServer(id1, port1, port1));
        servers.add(startServer(id2, port2, port1));

        new Client<>(port1, new Putter(key, value)).request();

        assertEquals(value, new Client<>(port1, new Getter(key)).request());

        Closer closer = new Closer();
        new Client<>(port1, closer).request();
        new Client<>(port2, closer).request();
    }

    @Test
    void balance()
            throws Exception {
        int id1 = 0, port1 = 5003;
        int id2 = 1, port2 = 5004;

        int key = 1, value = 0;

        servers.add(startServer(id1, port1, port1));
        servers.add(startServer(id2, port2, port1));

        new Client<>(port1, new Putter(key, value)).request();

        //to start server2 a blocking call is made
        new Client<>(port2, new Getter(key)).request();

        Closer closer = new Closer();

        assertEquals(false, new Client<>(port1, closer).request());

        new Client<>(port2, closer).request();
    }

    @Test
    void persist()
            throws Exception {
        int id1 = 0, port1 = 5005;
        int id2 = 1, port2 = 5006;

        int key = 0, value = 0;

        servers.add(startServer(id1, port1, port1));
        servers.add(startServer(id2, port2, port1));

        new Client<>(port1, new Putter(key, value)).request();

        //to start server2 a blocking call is made
        new Client<>(port2, new Getter(key)).request();

        Closer closer = new Closer();
        new Client<>(port1, closer).request();

        assertEquals(value, new Client<>(port2, new Getter(key)).request());

        new Client<>(port2, closer).request();
    }

    @Test
    void rearrange()
            throws IOException {
        int id1 = 0, port1 = 5007;
        int id2 = 1, port2 = 5008;
        int id3 = 2, port3 = 5009;

        int key = 2, value = 2;

        servers.add(startServer(id1, port1, port1));
        servers.add(startServer(id2, port2, port1));
        servers.add(startServer(id3, port3, port1));

        Getter getter = new Getter(key);

        //to start server3 a blocking call is made
        new Client<>(port3, getter).request();

        Closer closer = new Closer();

        new Client<>(port2, closer).request();

        new Client<>(port1, new Putter(key, value)).request();

        assertEquals(value, new Client<>(port3, getter).request());

        new Client<>(port1, closer).request();
        new Client<>(port3, closer).request();
    }
}