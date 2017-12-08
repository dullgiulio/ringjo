package ring;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RingWriterTest extends Ring {
	public RingWriterTest() {
		super(4);
	}

	@Test
	public void writeWrapsAround() {
		write(new Message("Test 1"));
		write(new Message("Test 2"));
		write(new Message("Test 3"));
		write(new Message("Test 4"));
		write(new Message("Test 5"));
		assertEquals("Test 5", new String(buffer[0].getContent()));
		assertEquals("Test 2", new String(buffer[1].getContent()));
		assertEquals("Test 3", new String(buffer[2].getContent()));
		assertEquals("Test 4", new String(buffer[3].getContent()));
	}
}