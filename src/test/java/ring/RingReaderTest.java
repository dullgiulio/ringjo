package ring;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class RingReaderTest {
	@Test
	public void readEmptyRing() {
		Ring ring = new Ring(4);
		Reader r = new Reader("Test", 2);
		ring.read(r);
		List<Line> buffer = r.getBuffer();
		assertEquals(0, buffer.size());
	}

	@Test
	public void readAfterInsert() {
		Ring ring = new Ring(4);
		Reader r = new Reader("Test", 2);
		Line msg;
		ring.write(new Line("Test 1"));
		ring.write(new Line("Test 2"));
		ring.write(new Line("Test 3"));
		ring.read(r);
		assertEquals(2, r.getPos());
		List<Line> buffer = r.getBuffer();
		assertEquals(2, buffer.size());
		msg = buffer.get(0);
		assertEquals("Test 1", new String(msg.getContent()));
		msg = buffer.get(1);
		assertEquals("Test 2", new String(msg.getContent()));
		ring.read(r);
		buffer = r.getBuffer();
		assertEquals(3, r.getPos());
		assertEquals(1, buffer.size());
		msg = buffer.get(0);
		assertEquals("Test 3", new String(msg.getContent()));
	}

	@Test
	public void multipleReaders() {
		Ring ring = new Ring(4);
		ring.write(new Line("Test 1"));
		ring.write(new Line("Test 2"));
		ring.write(new Line("Test 3"));
		Reader r1 = new Reader("Test", 2);
		ring.read(r1);
		assertEquals(2, r1.getPos());
		List<Line> buffer = r1.getBuffer();
		Line msg = buffer.get(0);
		assertEquals("Test 1", new String(msg.getContent()));
		msg = buffer.get(1);
		assertEquals("Test 2", new String(msg.getContent()));
		Reader r2 = new Reader("Test", 5);
		ring.read(r2);
		assertEquals(3, r2.getPos());
	}

	@Test
	public void readAfterWraparound() {
		Ring ring = new Ring(4);
		ring.write(new Line("Test 1"));
		ring.write(new Line("Test 2"));
		ring.write(new Line("Test 3"));
		ring.write(new Line("Test 4"));
		ring.write(new Line("Test 5")); // wraps around
		Reader r = new Reader("Test", 2);
		Line msg;
		ring.read(r); // reads first two
		List<Line> buffer = r.getBuffer();
		msg = buffer.get(0);
		assertEquals("Test 2", new String(msg.getContent()));
		msg = buffer.get(1);
		assertEquals("Test 3", new String(msg.getContent()));
		assertEquals(3, r.getPos());
	}
}