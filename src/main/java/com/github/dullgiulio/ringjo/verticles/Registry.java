package com.github.dullgiulio.ringjo.verticles;

import com.github.dullgiulio.ringjo.codecs.ReaderCodec;
import com.github.dullgiulio.ringjo.ring.Reader;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Registry extends AbstractVerticle {
	private final String BUS_EXECUTOR = "com.github.dullgiulio.ringjo.verticles.BusExecutor";
	private static final Logger LOG = LoggerFactory.getLogger(Registry.class);

	private Map<String, String> names = new HashMap<>();

	private class HandleStat implements Handler<Message<String>> {
		@Override
		public void handle(Message<String> event) {
			String name = event.body();
			LOG.info(String.format("Received registry request to stat %s", name));

			synchronized (names) {
				if (!names.containsKey(name)) {
					event.fail(404, String.format("A ring named %s does not exists", name));
					return;
				}
				event.reply("OK");
			}
		}
	}

	private class HandleOpen implements Handler<Message<String>> {
		@Override
		public void handle(Message<String> event) {
			String name = event.body();
			LOG.info(String.format("Received registry request to add %s", name));

			synchronized (names) {
				if (names.containsKey(name)) {
					event.fail(409, String.format("A ring named %s already exists", name));
					return;
				}
			}
			JsonObject config = new JsonObject().put("name", name).put("size", 1024);
			DeploymentOptions options = new DeploymentOptions().setConfig(config);
			vertx.deployVerticle(BUS_EXECUTOR, options, res -> {
				if (!res.succeeded()) {
					event.fail(500, String.format("could not start verticle: %s", res.cause().getMessage()));
					return;
				}
				synchronized (names) {
					names.put(name, res.result());
				}
				event.reply("OK");
			});
		}
	}

	private class HandleClose implements Handler<Message<String>> {
		@Override
		public void handle(Message<String> event) {
			String deployID;
			String name = event.body();
			LOG.info(String.format("Received registry request to delete %s", name));

			synchronized (names) {
				if (!names.containsKey(name)) {
					event.fail(409, String.format("A ring named %s does not exist", name));
					return;
				}
				deployID = names.get(name);
				names.remove(name);
			}
			vertx.undeploy(deployID, res -> {
				if (!res.succeeded()) {
					event.fail(500, String.format("could not stop ring %s", name));
					return;
				}
				event.reply("OK");
			});
		}
	}

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		vertx.eventBus().registerDefaultCodec(Reader.class, new ReaderCodec());
	}

	@Override
	public void start() {
		vertx.eventBus().consumer("ringjo.open", new HandleOpen());
		vertx.eventBus().consumer("ringjo.close", new HandleClose());
		vertx.eventBus().consumer("ringjo.stat", new HandleStat());
	}
}
