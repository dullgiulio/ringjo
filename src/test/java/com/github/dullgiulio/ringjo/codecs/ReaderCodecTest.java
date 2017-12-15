package com.github.dullgiulio.ringjo.codecs;

import com.github.dullgiulio.ringjo.ring.Reader;
import io.vertx.core.buffer.Buffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReaderCodecTest {
	@Test
	public void testEncodeDecode() {
		Buffer buf = Buffer.buffer();
		ReaderCodec codec = new ReaderCodec();

		Reader r1 = new Reader("name", 100);
		codec.encodeToWire(buf, r1);

		Reader r2 = codec.decodeFromWire(0, buf);
		assertEquals(r1, r2);
	}
}