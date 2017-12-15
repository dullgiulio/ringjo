package com.github.dullgiulio.ringjo.verticles;

import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.verticles.bus.RingAddress;
import com.github.dullgiulio.ringjo.verticles.bus.RingIO;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;

public class BusExecutor extends AbstractVerticle {
	private RingIO rio;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);

	private interface ReaderMessage extends Handler<Message<Reader>> {}
	private interface BufferMessage extends Handler<Message<Buffer>> {}


	public ReaderMessage handleRead() {
		return event -> {
			Reader r = event.body();
			LOG.info(String.format("Received read at position '%d'", r.getPos()));
			rio.runReader(r);
			event.reply(r);
		};
	}

	public BufferMessage handleWrite() {
		return event -> {
			LOG.info("Received full buffer write");
			Buffer body = event.body();
			try {
				event.reply(rio.writeBodyLines(body));
			} catch (IOException e) {
				event.reply(-1);
			}
		};
	}

	public void start(String name, int ringSize) {
		RingAddress address = new RingAddress(name);
		rio = new RingIO(ringSize);
		vertx.eventBus().consumer(address.getReadAddress(), handleRead());
		vertx.eventBus().consumer(address.getWriteAddress(), handleWrite());
	}

	@Override
	public void start(Future<Void> started) {
		JsonObject config = config();
		if (config == null || !config.containsKey("name") || !config.containsKey("size")) {
			started.fail("configuration is compulsory: include name and size parameters");
			return;
		}
		String name = config.getString("name");
		int ringSize = config.getInteger("size");
		start(name, ringSize);
		started.complete();
	}
}
