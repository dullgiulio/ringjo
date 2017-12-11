package verticles;

import codecs.ReaderCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import ring.Line;
import ring.Reader;
import ring.Ring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class BusExecutor extends AbstractVerticle {
	private Ring ring;
	private int ringSize;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);

	public BusExecutor setRingSize(int ringSize) {
		this.ringSize = ringSize;
		return this;
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

	private class HandleWriteBuffer implements Handler<Message<Buffer>> {
		private List<String> splitLines(Buffer buffer) {
			List<String> lines = new ArrayList<>();
			BufferedReader rdr = new BufferedReader(new StringReader(buffer.toString()));
			try {
				for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
					lines.add(line);
				}
				rdr.close();
			} catch (IOException e) {
				lines = null;
			}
			return lines;
		}

		@Override
		public void handle(Message<Buffer> event) {
			LOG.info("Received full buffer write");
			Buffer body = event.body();
			List<String> lines = splitLines(body);
			if (lines == null) {
				event.reply(-1);
				return;
			}
			for (String line : lines) {
				ring.write(new Line(line));
			}
			event.reply(lines.size());
		}
	}

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		vertx.eventBus().registerDefaultCodec(Reader.class, new ReaderCodec());
	}

	@Override
	public void start() {
		ring = new Ring(ringSize);
		vertx.eventBus().consumer("ringjo.read", new HandleRead());
		vertx.eventBus().consumer("ringjo.write", new HandleWriteBuffer());
	}
}
