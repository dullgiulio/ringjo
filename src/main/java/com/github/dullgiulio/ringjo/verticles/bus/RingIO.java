package com.github.dullgiulio.ringjo.verticles.bus;

import com.github.dullgiulio.ringjo.ring.Line;
import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.ring.Ring;
import io.vertx.core.buffer.Buffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class RingIO {
	private Ring ring;

	public List<String> readBuffer(Buffer buffer) throws IOException {
		List<String> lines = new ArrayList<>();
		BufferedReader rdr = new BufferedReader(new StringReader(buffer.toString()));
		for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
			lines.add(line);
		}
		rdr.close();
		return lines;
	}

	public RingIO(int size) {
		ring = new Ring(size);
	}

	public int writeBodyLines(Buffer buf) throws IOException {
		List<String> lines = readBuffer(buf);
		writeBodyLines(lines);
		return lines.size();
	}

	synchronized private void writeBodyLines(List<String> lines) {
		for (String line : lines) {
			ring.write(new Line(line));
		}
	}

	synchronized public void runReader(Reader r) {
		ring.read(r);
	}
}
