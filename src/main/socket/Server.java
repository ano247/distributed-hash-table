package socket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int START = 0, CLOSE = -1;

    private ExecutorService executor;
    private ServerSocket server;
    private Messenger<Boolean> messenger;

    public Server(int port, Messenger<Boolean> messenger)
            throws  IOException {
        /*
        if multiple threads are spawned then
        responses may be unordered
        submit of SingleThreadExecutor executes tasks in order
         */
        executor = Executors.newSingleThreadExecutor();
        server = new ServerSocket(port);
        this.messenger = messenger;
    }

    public void respond()
            throws IOException {
        informStarted();

        while(true) {
            Socket client;

            try {
                client = server.accept();
            } catch (SocketException e) {
                break;
            }

            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

            executor.submit(() -> {
                try {
                    boolean alive = messenger.message(in, out);

                    if(!alive) {
                        server.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void informStarted()
            throws IOException {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(message);
        out.writeInt(START);
        out.flush();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(message.toByteArray()));

        messenger.message(in, null);
    }
}
