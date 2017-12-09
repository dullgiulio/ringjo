package ring;

public class Ring {
	protected int size;
	protected Message[] buffer;
	protected long last = 0;
	protected long pos = 0;

	public Ring(int size) {
		this.size = size;
		this.buffer = new Message[size];
	}

	public void write(Message msg) {
		int lastDistance = (int) (pos - last);
		// If the slowest reader is in the way, it will skip some messages.
		// Advance the slowest reader to the current writing position.
		if (pos > 0 && lastDistance >= size) {
			last = pos - size;
		}
		int p = (int) (pos % size);
		buffer[p] = msg;
		this.pos++;
	}

	public void read(Reader rd) {
		if (pos == 0) {
			return;
		}
		// New readers start from the beginning inside the ring
		if (!rd.hasStarted()) {
			if (pos < size) {
				rd.setPos(0);
			} else {
				rd.setPos(pos - size);
			}
			rd.setStarted();
		}
		rd.clear();
		int nMessages = (int) (pos - rd.getPos());
		// No new messages to read.
		if (nMessages <= 0) {
			return;
		}
		// Read maximum Reader.capacity messages.
		if (nMessages > rd.getCapacity()) {
			nMessages = rd.getCapacity();
		}
		long start = rd.getPos();
		if (start < last) {
			// TODO: signal that there are skipped messages
			start = last;
			rd.setPos(start);
		}
		for (int i = 0; i < nMessages; i++) {
			int p = (int) ((start + i) % size);
			rd.addMessage(buffer[p]);
		}
	}
}
