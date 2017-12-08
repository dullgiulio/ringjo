package ring.futures;

import ring.Reader;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ReaderFuture implements Future<Reader> {
	private Reader value;
	private BaseLatchFuture future;

	public ReaderFuture() {
		future = new BaseLatchFuture();
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public Reader get() throws InterruptedException {
		future.awaitCompletion();
		return value;
	}

	@Override
	public Reader get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		try {
			future.awaitTimeout(timeout, unit);
		} finally {
			return value;
		}
	}

	public void put(Reader r) {
		value = r;
		future.done();
	}
}
