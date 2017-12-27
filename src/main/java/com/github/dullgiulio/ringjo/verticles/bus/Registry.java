package com.github.dullgiulio.ringjo.verticles.bus;

import com.github.dullgiulio.ringjo.verticles.exceptions.RegistryException;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Registry {
	private ConcurrentMap<String, String> names;

	public Registry() {
		this.names = new ConcurrentHashMap<>();
	}

	public Registry(ConcurrentMap<String, String> names) {
		this.names = names;
	}

	public void stat(String name) throws RegistryException {
		if (!names.containsKey(name)) {
			throw new RegistryException(HttpResponseStatus.NOT_FOUND.code(),
					String.format("A ring named %s does not exists", name));
		}
	}

	public void hold(String name) throws RegistryException {
		if (names.containsKey(name)) {
			throw new RegistryException(HttpResponseStatus.CONFLICT.code(),
					String.format("A ring named %s already exists", name));
		}
		names.put(name, "");
	}

	public void set(String name, String val) {
		names.put(name, val);
	}

	public void remove(String name) {
		names.remove(name);
	}

	public String pop(String name) throws RegistryException {
		if (!names.containsKey(name)) {
			throw new RegistryException(HttpResponseStatus.CONFLICT.code(),
					String.format("A ring named %s does not exist", name));
		}
		String val = names.get(name);
		if (val.equals("")) {
			throw new RegistryException(HttpResponseStatus.NOT_FOUND.code(),
					String.format("A ring named %s was not yet started", name));
		}
		names.remove(name);
		return val;
	}
}
