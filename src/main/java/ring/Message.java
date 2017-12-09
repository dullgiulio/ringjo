package ring;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public class Message implements Serializable {
	private LocalDateTime date;
	private byte[] content;

	public Message(String content) {
		this.date = LocalDateTime.now();
		this.content = content.getBytes(StandardCharsets.UTF_8);
	}

	public Message(byte[] content) {
		this.date = LocalDateTime.now();
		this.content = Arrays.copyOf(content, content.length);
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = Arrays.copyOf(content, content.length);
	}

	@Override
	public int hashCode() {
		return Objects.hash(date, content);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Message)) {
			return false;
		}
		return Objects.equals(((Message) o).getDate(), this.date) &&
				Arrays.equals(((Message) o).getContent(), this.content);
	}
}
