package work.lclpnet.gaco.ds.queue;

public interface QueuePersistence<T> {

    QueueTransfer<T> restore();

    void store(QueueTransfer<T> transfer);
}
