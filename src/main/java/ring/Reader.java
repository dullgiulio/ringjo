package ring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Reader implements Serializable {
	private String name;
	private long pos;
	private int capacity;
	private ArrayList<Message> buffer;

	public Reader(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
		this.buffer = new ArrayList<>(capacity);
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

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public ArrayList<Message> getBuffer() {
		return buffer;
	}

	public void clear() {
		buffer.clear();
	}

	public void addMessage(Message msg) {
		buffer.add(msg);
		pos++;
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
