package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.entity.ContentType;
import top.codings.websiphon.light.requester.IRequest;

import java.net.Proxy;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public abstract class BaseRequest<T> implements IRequest<T> {
    protected volatile IRequest.Status status;
    @Getter
    @Setter
    protected Object userData;
    @Setter
    protected volatile RequestResult requestResult;
    @Setter
    protected URI uri;
    @Getter
    @Setter
    protected ContentType contentType;
    @Getter
    @Setter
    protected Proxy proxy;
    @Setter
    protected CompletableFuture future;
    private ReentrantLock lock = new ReentrantLock();

    @Override
    public final void lock() {
        lock.lock();
    }

    @Override
    public final boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public final void unlock() {
        lock.unlock();
    }

    @Override
    public boolean setStatus(Status status) {
        this.status = status;
        return true;
    }

    @Override
    public void release() {
        userData = null;
        requestResult.setThrowable(null);
        requestResult.setData(null);
    }
}
