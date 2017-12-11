package ring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public class Line implements Serializable {
	private LocalDateTime date;
	private ByteArrayOutputStream content;

	public Line(String content) {
		this(content.getBytes(StandardCharsets.UTF_8));
	}

	public Line(byte[] bytes) {
		this.date = LocalDateTime.now();
		this.content = new ByteArrayOutputStream(bytes.length);
		this.content.write(bytes, 0, bytes.length);
	}

	public void set(Line msg) throws IOException {
		date = msg.getDate();
		content.reset();
		msg.writeTo(this);
	}

	public void writeTo(Line msg) throws IOException {
		content.writeTo(msg.getBuffer());
	}

	public OutputStream getBuffer() {
		return content;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public byte[] getContent() {
		return content.toByteArray();
	}

	@Override
	public int hashCode() {
		return Objects.hash(date, content);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Line)) {
			return false;
		}
		return Objects.equals(((Line) o).getDate(), getDate()) &&
				Arrays.equals(((Line) o).getContent(), getContent());
	}
}
