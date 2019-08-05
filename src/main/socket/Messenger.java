package socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface Messenger<T> {

    T message(ObjectInputStream in, ObjectOutputStream out)
            throws IOException;
}
