package com.github.dullgiulio.ringjo.ring;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RingWriterTest extends Ring {
	public RingWriterTest() {
		super(4);
	}

	@Test
	public void writeWrapsAround() {
		write(new Line("Test 1"));
		write(new Line("Test 2"));
		write(new Line("Test 3"));
		write(new Line("Test 4"));
		write(new Line("Test 5"));
		assertEquals("Test 5", new String(buffer[0].getContent()));
		assertEquals("Test 2", new String(buffer[1].getContent()));
		assertEquals("Test 3", new String(buffer[2].getContent()));
		assertEquals("Test 4", new String(buffer[3].getContent()));
	}
}