// MainFile: src/main/java/org/z2six/ezactions/helper/ClientTaskQueue.java
package ezactions.helper;

import org.z2six.ezactions.Constants;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Minimal client-thread task queue: post a Runnable now, execute on the next client tick.
 * Used to run actions AFTER we close the radial screen, so input injection hits gameplay.
 */
public final class ClientTaskQueue {

    private static final Deque<Runnable> QUEUE = new ArrayDeque<>();

    private ClientTaskQueue() {}

    public static void post(Runnable r) {
        if (r == null) return;
        synchronized (QUEUE) {
            QUEUE.addLast(r);
        }
    }

    /** Called from client tick to drain tasks safely. */
    public static void drain() {
        Deque<Runnable> run;
        synchronized (QUEUE) {
            if (QUEUE.isEmpty()) return;
            run = new ArrayDeque<>(QUEUE);
            QUEUE.clear();
        }
        while (!run.isEmpty()) {
            Runnable r = run.removeFirst();
            try {
                r.run();
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] ClientTaskQueue task threw: {}", Constants.MOD_NAME, t.toString());
            }
        }
    }
}
