package com.github.dullgiulio.ringjo.verticles.bus;

import com.github.dullgiulio.ringjo.ring.Line;
import com.github.dullgiulio.ringjo.ring.Reader;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class RingRequest {
	private RingAddress address;
	private Vertx vertx;

	public static final String OPEN_ADDRESS = "ringjo.open";
	public static final String CLOSE_ADDRESS = "ringjo.close";
	public static final String STAT_ADDRESS = "ringjo.stat";

	public RingRequest(Vertx vertx, String name) {
		this.vertx = vertx;
		this.address = new RingAddress(name);
	}

	private Future<Buffer> requestAddress(String addr) {
		Future<Buffer> fut = Future.future();
		vertx.eventBus().send(addr, address.getName(), ar -> {
			// TODO: Return some object that includes both the status code and error message
			// 		 Currently the status gets lost here and it always returned as 500.
			Buffer buffer = Buffer.buffer();
			if (!ar.succeeded()) {
				fut.fail(ar.cause().getMessage());
				return;
			}
			buffer.appendString("OK");
			fut.complete(buffer);
		});
		return fut;
	}

	public String getRingName() {
		return address.getName();
	}

	public Future<Buffer> requestOpen() {
		return requestAddress(OPEN_ADDRESS);
	}

	public Future<Buffer> requestClose() {
		return requestAddress(CLOSE_ADDRESS);
	}

	public Future<Buffer> requestStat() {
		return requestAddress(STAT_ADDRESS);
	}

	public Future<Buffer> requestRead(Reader reader) {
		Future<Buffer> fut = Future.future();
		vertx.eventBus().send(address.getReadAddress(), reader, ar -> {
			Buffer buffer = Buffer.buffer();
			if (!ar.succeeded()) {
				fut.fail("could not read lines from ring");
				return;
			}
			Reader r = (Reader) ar.result().body();
			List<Line> lines = r.getBuffer();
			for (Line l : lines) {
				buffer.appendString(String.format("%s: %s\n", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(l.getDate()), l.toString()));
			}
			fut.complete(buffer);
		});
		return fut;
	}

	public Future<Buffer> requestWrite(Buffer body) {
		Future<Buffer> fut = Future.future();
		vertx.eventBus().send(address.getWriteAddress(), body, ar -> {
			Buffer buffer = Buffer.buffer();
			if (!ar.succeeded()) {
				buffer.appendString("could not add lines to ring");
			} else {
				buffer.appendString(String.format("Added %d messages", (Integer) ar.result().body()));
			}
			fut.complete(buffer);
		});
		return fut;
	}
}
