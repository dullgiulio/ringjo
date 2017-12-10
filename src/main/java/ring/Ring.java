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
		rd.clear();
		if (pos == 0) {
			rd.trim();
			return;
		}
		long start = rd.getPos();
		// New readers start from the beginning inside the ring if we wrapper around.
		if (start == 0 && pos > size) {
			start = pos - size;
		}
		int nMessages = (int) (pos - start);
		// No new messages to read.
		if (nMessages <= 0) {
			rd.trim();
			return;
		}
		// Read maximum Reader.capacity messages.
		if (nMessages > rd.getCapacity()) {
			nMessages = rd.getCapacity();
		}
		if (start < last) {
			// TODO: signal that there are skipped messages
			start = last;
		}
		for (int i = 0; i < nMessages; i++) {
			int p = (int) ((start + i) % size);
			rd.addMessage(buffer[p]);
		}
		rd.trim();
		rd.setPos(start + nMessages);
	}
}
