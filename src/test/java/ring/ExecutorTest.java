package ring;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class ExecutorTest {
	@Test
	public void executorOnSeparateThread() throws InterruptedException, ExecutionException {
		Executor executor = new Executor(1024);
		Thread t = new Thread(executor);
		t.start();
		Reader r = new Reader("test reader", 2);
		executor.write(new Message("test 1"));
		executor.write(new Message("test 2"));
		Future<Reader> fr = executor.read(r);
		r = fr.get();
		ArrayList<Message> buffer = r.getBuffer();
		assertEquals(2, buffer.size());
		Message msg = buffer.get(0);
		assertEquals("test 1", new String(msg.getContent()));
		msg = buffer.get(1);
		assertEquals("test 2", new String(msg.getContent()));
		fr = executor.read(r);
		r = fr.get(); // Ignore, but should use for subsequent reads.
		buffer = r.getBuffer();
		assertEquals(0, buffer.size());
		Future<Boolean> fb = executor.stop();
		fb.get();
		t.join();
	}
}