package ring;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExecutorTest {
	Executor executor;
	Thread executorThread;
	Future<Void> done = Future.future();

	@Before
	public void setUp() {
		executor = new Executor(10, 1024);
		executorThread = new Thread(executor);
	}

	private void checkFirstRead(AsyncResult<Reader> ar) {
		assertEquals(true, ar.succeeded());
		Reader r = ar.result();
		List<Line> buffer = r.getBuffer();
		assertEquals(2, buffer.size());
		Line msg = buffer.get(0);
		assertEquals("test 1", new String(msg.getContent()));
		msg = buffer.get(1);
		assertEquals("test 2", new String(msg.getContent()));
	}

	private void checkSecondRead(AsyncResult<Reader> ar) {
		assertEquals(true, ar.succeeded());
		Reader r = ar.result();
		List<Line> buffer = r.getBuffer();
		assertEquals(0, buffer.size());
	}

	private void checkThreadJoinable(AsyncResult<Void> ar) {
		assertEquals(true, ar.succeeded());
		done.complete();
	}

	@Test
	public void executorOnSeparateThread() throws InterruptedException {
		executorThread.start();
		Reader r = new Reader("test reader", 2);
		executor.write(new Line("test 1"));
		executor.write(new Line("test 2"));
		Future<Reader> fr = executor.read(r);
		fr.setHandler(this::checkFirstRead);
		fr = executor.read(r);
		fr.setHandler(this::checkSecondRead);
		Future<Void> fb = executor.stop();
		fb.setHandler(this::checkThreadJoinable);
		done.result();
		try {
			executorThread.join();
		} catch(InterruptedException e) {
			System.out.printf("Interrupted\n");
		}
	}
}