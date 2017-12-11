package verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import ring.Line;
import ring.Reader;
import ring.Ring;

public class BusExecutor extends AbstractVerticle {
	private Ring ring;
	private int ringSize;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);

	public BusExecutor setRingSize(int ringSize) {
		this.ringSize = ringSize;
		return this;
	}

	private class HandleWrite implements Handler<Message<Line>> {
		@Override
		public void handle(Message<Line> event) {
			Line line = event.body();
			LOG.info(String.format("Received write '%s'", line.getContent()));
			ring.write(line);
		}
	}

	private class HandleRead implements Handler<Message<Reader>> {
		@Override
		public void handle(Message<Reader> event) {
			Reader r = event.body();
			LOG.info(String.format("Received read at position '%d'", r.getPos()));
			ring.read(r);
			event.reply(r);
		}
	}

	@Override
	public void start() {
		ring = new Ring(ringSize);
		vertx.eventBus().consumer("ringjo.read", new HandleRead());
		vertx.eventBus().consumer("ringjo.write", new HandleWrite());
	}
}
