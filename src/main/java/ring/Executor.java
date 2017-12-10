package ring;

import ring.futures.ReaderFuture;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Executor implements Runnable {
	private Ring ring;
	private BlockingQueue<Callable<CompletableFuture<Boolean>>> queue;
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
			return null;
		});
		return readerFuture;
	}

	public Future<Boolean> write(Message msg) throws InterruptedException {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		submit(() -> {
			ring.write(msg);
			future.complete(true);
			return null;
		});
		return future;
	}

	public Future<Boolean> writeBatch(Iterable<Message> msgs) throws InterruptedException {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		submit(() -> {
			for (Message msg : msgs) {
				ring.write(msg);
			}
			future.complete(true);
			return null;
		});
		return future;
	}

	public CompletableFuture<Boolean> stop() throws InterruptedException {
		CompletableFuture<Boolean> stopped = new CompletableFuture<>();
		submit(() -> {
			return stopped; // Stop execution and signal back.
		});
		return stopped;
	}

	private void submit(Callable<CompletableFuture<Boolean>> c) throws InterruptedException {
		queue.put(c);
	}

	@Override
	public void run() {
		while (true) {
			Callable<CompletableFuture<Boolean>> c;
			try {
				c = queue.take();
			} catch (InterruptedException e) {
				logger.info("A task was interrupted, exiting...");
				return;
			}
			try {
				CompletableFuture<Boolean> done = c.call();
				if (done != null) {
					logger.info("Loop shutting down, exiting...");
					// ... could do any cleanup here.
					done.complete(true);
					return;
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception", e);
			}
		}
	}
}
