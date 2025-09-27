package work.lclpnet.gaco.ds.queue;

public final class VoidQueuePersistence<T> implements QueuePersistence<T> {

    private static final VoidQueuePersistence<?> INSTANCE = new VoidQueuePersistence<>();

    private VoidQueuePersistence() {}

    @Override
    public QueueTransfer<T> restore() {
        return QueueTransfer.empty();
    }

    @Override
    public void store(QueueTransfer<T> transfer) {}

    @SuppressWarnings("unchecked")
    public static <T> VoidQueuePersistence<T> instance() {
        return (VoidQueuePersistence<T>) INSTANCE;
    }
}
