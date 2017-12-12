package com.github.dullgiulio.ringjo.verticles;

import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.verticles.bus.RingRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRunner extends AbstractVerticle {
	private int port;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);
	private final Pattern onlyAlphanum = Pattern.compile("[a-zA-Z0-9_\\-]+");
	private final String defaultReaderName = "_unnamed";
	private final int defaultReaderCapacity = 100;

	private class DefaultWebHandler implements Handler<AsyncResult<Buffer>> {
		HttpServerResponse resp;

		public DefaultWebHandler(HttpServerResponse resp) {
			this.resp = resp;
		}

		@Override
		public void handle(AsyncResult<Buffer> ar) {
			if (!ar.succeeded()) {
				HttpRunner.responseError(resp, HttpResponseStatus.INTERNAL_SERVER_ERROR, ar.cause());
				return;
			}
			resp.putHeader("Content-Type", "text/plain");
			resp.end(ar.result());
		}
	}

	public HttpRunner setPort(int port) {
		this.port = port;
		return this;
	}

	private boolean validName(String name) {
		Matcher m = onlyAlphanum.matcher(name);
		return m.matches();
	}

	protected static void responseError(HttpServerResponse resp, HttpResponseStatus status, String message) {
		resp.setStatusCode(status.code());
		resp.end(message);
	}

	protected static void responseError(HttpServerResponse resp, HttpResponseStatus status, Throwable t) {
		responseError(resp, status, t.getMessage());
	}

	private Handler<RoutingContext> namedRequest(Function<RingRequest, Future<Buffer>> handle) {
		return rc -> {
			HttpServerRequest req = rc.request();
			HttpServerResponse resp = req.response();
			String name = req.getParam("name");
			if (!validName(name)) {
				String err = String.format("Invalid name '%s' requested", name);
				LOG.info(String.format("invalid request received: %s", err));
				responseError(resp, HttpResponseStatus.BAD_REQUEST, err);
				return;
			}
			RingRequest rr = new RingRequest(vertx, name);
			Future<Buffer> fut = handle.apply(rr);
			fut.setHandler(new DefaultWebHandler(resp));
		};
	}

	private Function<RingRequest, Future<Buffer>> handleGet() {
		return rr -> {
			LOG.info(String.format("Read request for ring %s", rr.getRingName()));
			Reader reader = new Reader(defaultReaderName, defaultReaderCapacity);
			return rr.requestRead(reader);
		};
	}

	private Function<RingRequest, Future<Buffer>> handlePost() {
		return rr -> {
			// TODO: we don't have RoutingContext here; refactor further.
			//Buffer body = rc.getBody(); // TODO: test with curl
			Buffer body = Buffer.buffer("Hello\nWorld\nTest again\nAnd again\n"); // TODO: remove.

			LOG.info(String.format("Post request for ring %s with body of size %d bytes", rr.getRingName(), body.length()));
			return rr.requestWrite(body);
		};
	}

	private Function<RingRequest, Future<Buffer>> handleStat() {
		return rr -> {
			LOG.info(String.format("Stat request for ring %s", rr.getRingName()));
			return rr.requestStat();
		};
	}

	private Function<RingRequest, Future<Buffer>> handleOpen() {
		return rr -> {
			LOG.info(String.format("Open request for ring %s", rr.getRingName()));
			return rr.requestOpen();
		};
	}

	private Function<RingRequest, Future<Buffer>> handleClose() {
		return rr -> {
			LOG.info(String.format("Close request for ring %s", rr.getRingName()));
			return rr.requestClose();
		};
	}

	@Override
	public void start() {
		HttpServerOptions options = new HttpServerOptions().setLogActivity(true);

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.get("/stat/:name").handler(namedRequest(handleStat()));
		router.get("/open/:name").handler(namedRequest(handleOpen()));
		router.get("/close/:name").handler(namedRequest(handleClose()));
		router.get("/read/:name").handler(namedRequest(handleGet()));
		router.post("/write/:name").handler(namedRequest(handlePost()));
		router.get("/write/:name").handler(namedRequest(handlePost())); // TODO: For testing without actually posting data; remove

		HttpServer server = vertx.createHttpServer(options).requestHandler(router::accept);
		server.listen(port, event -> {
			if (!event.succeeded()) {
				LOG.error("cannot start HTTP server", event.cause());
				return;
			}
			LOG.info(String.format("HTTP server running on port %d", event.result().actualPort()));
		});
	}
}
