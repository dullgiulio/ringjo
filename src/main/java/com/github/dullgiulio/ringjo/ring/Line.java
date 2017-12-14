package com.github.dullgiulio.ringjo.ring;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public class Line implements Serializable {
	private LocalDateTime date;
	private byte[] content;

	public Line(String content) {
		this(content.getBytes(StandardCharsets.UTF_8));
	}

	public Line(LocalDateTime date, String str) {
		this.date = date;
		this.content = str.getBytes(StandardCharsets.UTF_8);
	}

	public Line(byte[] bytes) {
		this.date = LocalDateTime.now();
		this.content = bytes.clone();
	}

	public void set(Line msg) throws IOException {
		date = msg.getDate();
		content = msg.getContent();
	}

	public LocalDateTime getDate() {
		return date;
	}

	public byte[] getContent() {
		return content.clone();
	}

	public String toString() {
		return new String(content);
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
