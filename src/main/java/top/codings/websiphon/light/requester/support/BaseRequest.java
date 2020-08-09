package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.locks.ReentrantLock;

@Getter
public abstract class BaseRequest<Q, R> implements IRequest<Q, R> {
    protected volatile IRequest.Status status;
    @Setter
    protected Object userData;
    @Setter
    protected RequestResult requestResult;
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
        /*if (tryLock()) {
            try {
                this.status = status;
                return true;
            } finally {
                unlock();
            }
        } else {
            return false;
        }*/
        this.status = status;
        return true;
    }
}
