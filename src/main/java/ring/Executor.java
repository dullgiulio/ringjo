package ring;

import ring.futures.ReaderFuture;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class Executor implements Runnable {
	private Ring ring;
	private BlockingQueue<Callable<Boolean>> queue;
	private Logger logger = Logger.getLogger("RING");

	public Executor(int ringSize, int queueSize) {
		queue = new ArrayBlockingQueue<>(queueSize);
		ring = new Ring(ringSize);
	}

	public Future<Reader> read(Reader r) throws InterruptedException {
		ReaderFuture readerFuture = new ReaderFuture();
		submit(() -> {
			ring.read(r);
			readerFuture.put(r);
			return true;
		});
		return readerFuture;
	}

	public Future<Boolean> write(Message msg) throws InterruptedException {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		submit(() -> {
			ring.write(msg);
			future.complete(true);
			return true;
		});
		return future; // TODO: Actual return should be just an empty future.
	}

	public Future<Boolean> writeBatch(Iterable<Message> msgs) throws InterruptedException {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		submit(() -> {
			// TODO: fail if there are more messages than space available!
			for (Message msg : msgs) {
				ring.write(msg);
			}
			future.complete(true);
			return true;
		});
		return future; // TODO: Actual return should be just an empty future.
	}

	public Future<Boolean> stop() throws InterruptedException {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		submit(() -> {
			future.complete(true);
			return false; // Stop execution.
		});
		return future; // TODO: Actual return should be just an empty future.
	}

	private void submit(Callable<Boolean> c) throws InterruptedException {
		queue.put(c);
	}

	@Override
	public void run() {
		while (true) {
			Callable<Boolean> c;
			try {
				c = queue.take();
			} catch (InterruptedException e) {
				logger.info("A task was interrupted, exiting...");
				return;
			}
			try {
				Boolean ok = c.call();
				if (!ok) {
					logger.info("Loop shutting down, exiting...");
					return;
				}
			} catch (Exception e) {
				logger.info(String.format("Skipped action: could not execute callable: %s", e.getMessage()));
			}
		}
	}
}
