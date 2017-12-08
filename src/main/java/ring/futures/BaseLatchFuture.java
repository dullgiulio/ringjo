package ring.futures;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BaseLatchFuture {
	private final CountDownLatch latch = new CountDownLatch(1);

	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	public boolean isCancelled() {
		return false;
	}

	public boolean isDone() {
		return latch.getCount() == 0;
	}

	protected void awaitCompletion() throws InterruptedException {
		latch.await();
	}

	protected void awaitTimeout(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if (!latch.await(timeout, unit)) {
			throw new TimeoutException();
		}
	}

	protected void done() {
		latch.countDown();
	}
}
