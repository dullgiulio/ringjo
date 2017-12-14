package com.github.dullgiulio.ringjo.verticles.exceptions;

public class RegistryException extends Exception {
	int status;

	public RegistryException(int status, String message) {
		super(message);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
