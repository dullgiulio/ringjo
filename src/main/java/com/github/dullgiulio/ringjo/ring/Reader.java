package com.github.dullgiulio.ringjo.ring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Reader implements Serializable {
	final private String name;
	final private int capacity;

	private long pos; // Position in ringbuffer
	private int rpos; // Position in reader buffer
	private ArrayList<Line> buffer;

	public Reader(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
		this.buffer = new ArrayList<>(capacity);
		this.pos = 0;
		this.rpos = 0;
	}

	public void setBuffer(List<Line> buffer) {
		this.buffer = new ArrayList<>(buffer);
	}

	public String getName() {
		return name;
	}

	public long getPos() {
		return pos;
	}

	public long getReaderPos() {
		return rpos;
	}

	public void setReaderPos(int rpos) {
		this.rpos = rpos;
	}

	public void setPos(long pos) {
		this.pos = pos;
	}

	public int getCapacity() {
		return capacity;
	}

	public List<Line> getBuffer() {
		return buffer.subList(0, rpos);
	}

	public void reset() {
		rpos = 0;
	}

	public void addMessage(Line msg) {
		if (rpos >= buffer.size()) {
			buffer.add(msg);
		} else {
			buffer.set(rpos, msg);
		}
		rpos++;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, pos);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Reader)) {
			return false;
		}
		Reader r = (Reader) o;
		return (r.getName().equals(name) && r.getPos() == getPos() && r.getReaderPos() == getReaderPos());
	}
}
