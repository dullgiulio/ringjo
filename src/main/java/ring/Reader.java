package ring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Reader implements Serializable {
	private String name;
	private long pos; // Position in ringbuffer
	private int rpos; // Position in reader buffer
	private int capacity;
	private boolean started;
	private ArrayList<Message> buffer;

	public Reader(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
		this.started = false;
		this.buffer = new ArrayList<>(capacity);
		this.pos = 0;
		this.rpos = 0;
	}

	public String getName() {
		return name;
	}

	public long getPos() {
		return pos;
	}

	public void setPos(long pos) {
		this.pos = pos;
	}

	public int getCapacity() {
		return capacity;
	}

	public List<Message> getBuffer() {
		return buffer.subList(0, rpos);
	}

	public void reset() {
		rpos = 0;
	}

	public void addMessage(Message msg) {
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
		return (((Reader) o).getName().equals(name) && ((Reader) o).getPos() != getPos());
	}
}
