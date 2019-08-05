package socket;

import dht.Closer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static socket.Server.CLOSE;
import static socket.Server.START;

class SocketTest {

    @Test
    void test()
            throws Exception {
        int message = 1;
        int port = 6000;

        Server server = new Server(port, (in, out) -> {
            int input = in.readInt();

            if(input != START && input != CLOSE) {
                out.writeInt(input);
                out.flush();
            }

            in.close();
            return input != CLOSE;
        });

        new Thread(() -> {
            try {
                server.respond();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Client<Integer> client = new Client<>(port, (in, out) -> {
            out.writeInt(message);
            out.flush();

            int result = in.readInt();

            in.close();
            return result;
        });

        assertEquals(message, client.request());

        new Client<>(port, new Closer()).request();
    }
}