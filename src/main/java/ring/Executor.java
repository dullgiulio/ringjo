package ring;

import io.vertx.core.Future;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Executor implements Runnable {
	private Ring ring;
	private BlockingQueue<Callable<Future<Void>>> queue;
	private Logger logger = Logger.getLogger("RING");

	public Executor(int ringSize, int queueSize) {
		queue = new ArrayBlockingQueue<>(queueSize);
		ring = new Ring(ringSize);
	}

	public Future<Reader> read(Reader r) {
		Future<Reader> readerFuture = Future.future();
		submit(() -> {
			ring.read(r);
			readerFuture.complete(r);
			return null;
		});
		return readerFuture;
	}

	public Future<Void> write(Message msg) {
		Future<Void> future = Future.future();
		submit(() -> {
			ring.write(msg);
			future.complete();
			return null;
		});
		return future;
	}

	public Future<Void> writeBatch(Iterable<Message> msgs) {
		Future<Void> future = Future.future();
		submit(() -> {
			for (Message msg : msgs) {
				ring.write(msg);
			}
			future.complete();
			return null;
		});
		return future;
	}

	public Future<Void> stop() {
		Future<Void> stopped = Future.future();
		submit(() -> {
			return stopped; // Stop execution and signal back.
		});
		return stopped;
	}

	private void submit(Callable<Future<Void>> c) {
		try {
			queue.put(c);
		} catch (InterruptedException e) {}
	}

	@Override
	public void run() {
		while (true) {
			Callable<Future<Void>> c;
			try {
				c = queue.take();
			} catch (InterruptedException e) {
				logger.info("A task was interrupted, exiting...");
				return;
			}
			try {
				Future<Void> done = c.call();
				if (done != null) {
					logger.info("Loop shutting down, exiting...");
					// ... could do any cleanup here.
					done.complete();
					return;
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception", e);
			}
		}
	}
}
