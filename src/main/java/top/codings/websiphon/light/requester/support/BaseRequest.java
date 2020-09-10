package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.entity.ContentType;
import top.codings.websiphon.light.requester.IRequest;

import java.net.Proxy;
import java.net.URI;
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
    private ReentrantLock lock = new ReentrantLock();

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public void unlock() {
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
