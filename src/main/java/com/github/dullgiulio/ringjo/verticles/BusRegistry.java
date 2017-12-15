package com.github.dullgiulio.ringjo.verticles;

import com.github.dullgiulio.ringjo.verticles.bus.Registry;
import com.github.dullgiulio.ringjo.verticles.bus.RingRequest;
import com.github.dullgiulio.ringjo.verticles.exceptions.RegistryException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BusRegistry extends AbstractVerticle {
	private static final String BUS_EXECUTOR = BusExecutor.class.getCanonicalName();
	private static final Logger LOG = LoggerFactory.getLogger(BusRegistry.class);

	private final Registry registry = new Registry();

	@FunctionalInterface private interface RegistryHandler {
		void apply(String name, Message<String> event) throws RegistryException;
	}

	private Handler<Message<String>> namedRequest(RegistryHandler handler) {
		return ar -> {
			String name = ar.body();
			if (name.equals("")) {
				ar.fail(HttpResponseStatus.BAD_REQUEST.code(),
						String.format("Invalid empty name"));
				return;
			}
			try {
				handler.apply(name, ar);
			} catch (RegistryException e) {
				ar.fail(e.getStatus(), e.getMessage());
			}
		};
	}

	private void handleStat(String name, Message<String> event) throws RegistryException {
		LOG.info(String.format("Received registry request to stat %s", name));
		registry.stat(name);
		event.reply("OK");
	}

	private void handleAfterDeploy(String name, AsyncResult<String> res) throws RegistryException {
		if (!res.succeeded()) {
			registry.remove(name);
			throw new RegistryException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
					String.format("could not start verticle: %s", res.cause().getMessage()));
		}
		String verID = res.result();
		registry.set(name, verID);
		LOG.info(String.format("Started ring %s as verticle %s", name, verID));
	}

	private void handleAfterUndeploy(String name, AsyncResult<Void> res) throws RegistryException {
		if (!res.succeeded()) {
			throw new RegistryException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
					String.format("could not stop ring %s", name));
		}
	}

	@FunctionalInterface private interface HandlerWrapFn {
		void apply() throws RegistryException;
	}

	private void wrapReply(HandlerWrapFn fn, Message<String> event) {
		try {
			fn.apply();
		} catch (RegistryException e) {
			event.fail(e.getStatus(), e.getMessage());
		} finally {
			event.reply("OK");
		}
	}

	private void handleOpen(String name, Message<String> event) throws RegistryException {
		JsonObject config = new JsonObject().put("name", name).put("size", 1024);
		DeploymentOptions options = new DeploymentOptions().setConfig(config);

		LOG.info(String.format("Received registry request to add %s", name));

		registry.hold(name);
		vertx.deployVerticle(BUS_EXECUTOR, options, ar -> wrapReply(() -> handleAfterDeploy(name, ar), event));
	}

	private void handleClose(String name, Message<String> event) throws RegistryException {
		LOG.info(String.format("Received registry request to delete %s", name));

		String deployID = registry.pop(name);
		vertx.undeploy(deployID, ar -> wrapReply(() -> handleAfterUndeploy(name, ar), event));
	}

	@Override
	public void start() {
		vertx.eventBus().consumer(RingRequest.OPEN_ADDRESS, namedRequest(this::handleOpen));
		vertx.eventBus().consumer(RingRequest.CLOSE_ADDRESS, namedRequest(this::handleClose));
		vertx.eventBus().consumer(RingRequest.STAT_ADDRESS, namedRequest(this::handleStat));
	}
}
