package ring;

public class Ring {
	protected int size;
	protected Message[] buffer;
	protected Reader last;
	protected long pos = 0;

	public Ring(int size) {
		this.size = size;
		this.buffer = new Message[size];
		this.last = new Reader("internal", 0);
	}

	public void write(Message msg) {
		int lastDistance = (int) (pos - last.getPos());
		// If the slowest reader is in the way, it will skip some messages.
		// Advance the slowest reader to the current writing position.
		if (pos > 0 && lastDistance >= size) {
			last.setPos(pos - size + 1);
		}
		int p = (int) (pos % size);
		buffer[p] = msg;
		this.pos++;
	}

	public void read(Reader rd) {
		// New readers start from the beginning inside the ring
		if (!rd.hasStarted()) {
			rd.setPos(last.getPos());
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
		if (start < last.getPos()) {
			// TODO: signal that there are skipped messages
			start = last.getPos();
			rd.setPos(start);
		}
		for (int i = 0; i < nMessages; i++) {
			int p = (int) ((start + i) % size);
			rd.addMessage(buffer[p]);
		}
	}
}
