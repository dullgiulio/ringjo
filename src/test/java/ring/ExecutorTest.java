package ring;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class ExecutorTest {
	@Test
	public void executorOnSeparateThread() throws InterruptedException, ExecutionException {
		Executor executor = new Executor(10, 1024);
		Thread t = new Thread(executor);
		t.start();
		Reader r = new Reader("test reader", 2);
		executor.write(new Message("test 1"));
		executor.write(new Message("test 2"));
		Future<Reader> fr = executor.read(r);
		r = fr.get();
		List<Message> buffer = r.getBuffer();
		assertEquals(2, buffer.size());
		Message msg = buffer.get(0);
		assertEquals("test 1", new String(msg.getContent()));
		msg = buffer.get(1);
		assertEquals("test 2", new String(msg.getContent()));
		fr = executor.read(r);
		r = fr.get();
		buffer = r.getBuffer();
		assertEquals(0, buffer.size());
		Future<Boolean> fb = executor.stop();
		fb.get();
		t.join();
	}
}