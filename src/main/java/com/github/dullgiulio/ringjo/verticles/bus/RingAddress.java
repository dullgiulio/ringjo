package com.github.dullgiulio.ringjo.verticles.bus;

public class RingAddress {
	private String name;
	private String write;
	private String read;

	public RingAddress(String name) {
		this.name = name;
		this.write = String.format("ringjo.talk.%s.write", name);
		this.read = String.format("ringjo.talk.%s.read", name);
	}

	public String getName() {
		return name;
	}

	public String getWriteAddress() {
		return write;
	}

	public String getReadAddress() {
		return read;
	}
}
