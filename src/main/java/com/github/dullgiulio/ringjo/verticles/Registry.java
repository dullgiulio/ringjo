package com.github.dullgiulio.ringjo.verticles;

import com.github.dullgiulio.ringjo.codecs.ReaderCodec;
import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.verticles.bus.RingRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Registry extends AbstractVerticle {
	private static final String BUS_EXECUTOR = BusExecutor.class.getCanonicalName();
	private static final Logger LOG = LoggerFactory.getLogger(Registry.class);

	private Map<String, String> names = new HashMap<>();

	private class HandleStat implements Handler<Message<String>> {
		@Override
		public void handle(Message<String> event) {
			String name = event.body();
			LOG.info(String.format("Received registry request to stat %s", name));

			synchronized (names) {
				if (!names.containsKey(name)) {
					event.fail(HttpResponseStatus.NOT_FOUND.code(),
							String.format("A ring named %s does not exists", name));
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
					event.fail(HttpResponseStatus.CONFLICT.code(),
							String.format("A ring named %s already exists", name));
					return;
				}
				names.put(name, ""); // placeholder
			}
			JsonObject config = new JsonObject().put("name", name).put("size", 1024);
			DeploymentOptions options = new DeploymentOptions().setConfig(config);
			vertx.deployVerticle(BUS_EXECUTOR, options, res -> {
				if (!res.succeeded()) {
					event.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
							String.format("could not start verticle: %s", res.cause().getMessage()));
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
					event.fail(HttpResponseStatus.CONFLICT.code(),
							String.format("A ring named %s does not exist", name));
					return;
				}
				deployID = names.get(name);
				if (deployID.equals("")) {
					event.fail(HttpResponseStatus.NOT_FOUND.code(),
							String.format("A ring named %s was not yet started", name));
					return;
				}
				names.remove(name);
			}
			vertx.undeploy(deployID, res -> {
				if (!res.succeeded()) {
					event.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
							String.format("could not stop ring %s", name));
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
		vertx.eventBus().consumer(RingRequest.OPEN_ADDRESS, new HandleOpen());
		vertx.eventBus().consumer(RingRequest.CLOSE_ADDRESS, new HandleClose());
		vertx.eventBus().consumer(RingRequest.STAT_ADDRESS, new HandleStat());
	}
}
