package org.vitoliu.client;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vitoliu.protocol.Request;
import org.vitoliu.protocol.Response;

/**
 *
 * @author yukun.liu
 * @since 30 一月 2019
 */
public class RpcFuture<T> implements Future<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcFuture.class);

	private final Sync sync;

	private Request request;

	private Response<T> response;

	private long startTime;

	private long responeTimeThreshhold = 5000;

	private List<Callback> pendingCallbacks = Lists.newArrayList();

	private Monitor monitor = new Monitor();

	public RpcFuture(Request request) {
		this.sync = new Sync();
		this.request = request;
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDone() {
		return sync.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		sync.acquire(-1);
		if (response != null) {
			return response.getResult();
		}
		return null;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
		if (success) {
			if (response != null) {
				return response.getResult();
			}
			return null;
		}
		else {
			throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
					+ ". Request class name: " + this.request.getClassName()
					+ ". Request method: " + this.request.getMethodName());
		}

	}


	void done(Response response) {
		this.response = response;
		sync.release(1);
		invokeCallBack();
		long responseTime = System.currentTimeMillis() - startTime;
		if (responseTime > responeTimeThreshhold) {
			LOGGER.warn("Service response time is too slow. Request id = " + response.getRequestId() + ". Response Time = " + responseTime + "ms");
		}
	}

	private void invokeCallBack() {
		monitor.enter();
		try {
			for (final Callback callback : pendingCallbacks) {
				execCallBack(callback);
			}
		}
		finally {
			monitor.leave();
		}
	}

	private void execCallBack(final Callback callback) {
		final Response res = response;
		RpcClient.submit(() -> {
			if (!res.isError()) {
				callback.onSuccess(res.getResult());
			}
			else {
				callback.onFailed(new RuntimeException("Response error", new Throwable(res.getError())));
			}
		});
	}

	static class Sync extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 1L;

		private final int done = 1;

		private final int pending = 0;

		@Override
		protected boolean tryAcquire(int arg) {
			return getState() == done;
		}

		@Override
		protected boolean tryRelease(int arg) {
			if (getState() == pending) {
				if (compareAndSetState(pending, done)) {
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return true;
			}
		}

		boolean isDone() {
			return getState() == done;
		}
	}
}
