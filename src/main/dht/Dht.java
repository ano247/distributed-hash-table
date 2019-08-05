package dht;

import socket.Client;
import socket.Messenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static socket.Server.CLOSE;
import static socket.Server.START;

public class Dht
        implements Messenger<Boolean> {
    public static final int ABSENT = Integer.MIN_VALUE;
    static final int PUT = 1, GET = 2;
    private static final int INSERT = 3, NEXT = 4, PREV = 5, TRANSFER = 6;

    private boolean alive;
    private Map<Integer, Integer> values;
    private Node current, previous, next;

    public Dht(Node current, int head)
            throws IOException {
        alive = true;
        values = new HashMap<>();
        this.current = current;

        if(head != current.port) {
             previous = new Client<>(head, new Updater(INSERT, current, true)).request();
        } else {
            previous = current;
        }
    }

    @Override
    public Boolean message(ObjectInputStream in, ObjectOutputStream out)
            throws IOException {
        switch(in.readInt()) {
            case START:
                start();
                break;

            case CLOSE:
                out.writeBoolean(close());
                out.flush();
                break;

            case PUT:
                out.writeBoolean(put(in.readInt(), in.readInt()));
                out.flush();
                break;

            case GET:
                out.writeInt(get(in.readInt()));
                out.flush();
                break;

            case INSERT:
                in.readBoolean();
                out.writeObject(insert(getNode(in)));
                out.flush();
                break;

            case NEXT:
                if(in.readBoolean()) {
                    out.writeObject(next);
                    out.flush();
                }
                setNext(getNode(in));
                break;

            case PREV:
                if(in.readBoolean()) {
                    out.writeObject(previous);
                    out.flush();
                }
                setPrevious(getNode(in));
                break;

            case TRANSFER:
                try {
                    putAll((Map<Integer, Integer>) in.readObject());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }

        in.close();
        return isAlive();
    }

    private boolean isAlive() {
        return alive;
    }

    private void start()
            throws IOException {
        if(previous.id != current.id) {
            next = new Client<>(previous.port, new Updater(NEXT, current, true)).request();
            new Client<>(next.port, new Updater(PREV, current, false)).request();
        } else {
            next = current;
        }
    }

    private boolean close()
            throws IOException {
        alive = false;

        if(next.id != current.id) {
            new Client<>(previous.port, new Updater(NEXT, next, false)).request();
            new Client<>(next.port, new Updater(PREV, previous, false)).request();
            return transfer(k -> true, previous);
        } else {
            return false;
        }
    }

    private int get(int key)
            throws IOException {
        if(contains(key)) {
            return values.getOrDefault(key, ABSENT);
        } else {
            return new Client<>(next.port, new Getter(key)).request();
        }
    }

    private boolean put(int key, int value)
            throws IOException {
        if(contains(key)) {
            if(values.containsKey(key)) {
                return false;
            } else {
                values.put(key, value);
                return true;
            }
        } else {
            return new Client<>(next.port, new Putter(key, value)).request();
        }
    }

    private Node insert(Node node)
            throws IOException {
        if(contains(node.id)) {
            return current;
        } else {
            return new Client<>(next.port, new Updater(INSERT, node, true)).request();
        }
    }

    private void setNext(Node next)
            throws IOException {
        this.next = next;
        transfer(k -> !contains(k), next);
    }

    private void setPrevious(Node previous) {
        this.previous = previous;
    }

    private void putAll(Map<Integer, Integer> transfer) {
        values.putAll(transfer);
    }

    private boolean transfer(Predicate<Integer> keys, Node target)
            throws IOException {
        Map<Integer, Integer> result = new HashMap<>();

        values.forEach((k, v) -> {
            if(keys.test(k)) {
                result.put(k,v);
            }
        });

        result.keySet().forEach(k -> values.remove(k));

        new Client<>(target.port, ((in, out) -> {
            out.writeInt(TRANSFER);
            out.writeObject(result);
            out.flush();

            in.close();
            return null;
        })).request();

        return !result.isEmpty();
    }

    private boolean contains(int key) {
        return next.id == current.id ||
                (key >= current.id && (key < next.id || next.id < current.id));
    }

    private Node getNode(ObjectInputStream in)
            throws IOException {
        try {
            return (Node) in.readObject();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private class Updater implements Messenger<Node> {
        private int flag;
        private Node value;
        private boolean blocking;

        private Updater(int flag, Node value, boolean blocking) {
            this.flag = flag;
            this.value = value;
            this.blocking = blocking;
        }

        @Override
        public Node message(ObjectInputStream in, ObjectOutputStream out)
                throws IOException {
            out.writeInt(flag);
            out.writeBoolean(blocking);
            out.writeObject(value);
            out.flush();

            Node result = blocking ? getNode(in) : null;

            in.close();
            return result;
        }
    }
}
