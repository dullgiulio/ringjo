package com.github.dullgiulio.ringjo.verticles;

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
import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.verticles.bus.RingRequest;

public class HttpRunner extends AbstractVerticle {
	private int port;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);

	private class DefaultWebHandler implements Handler<AsyncResult<Buffer>> {
		HttpServerResponse resp;

		public DefaultWebHandler(HttpServerResponse resp) {
			this.resp = resp;
		}

		@Override
		public void handle(AsyncResult<Buffer> ar) {
			if (!ar.succeeded()) {
				resp.setStatusCode(500);
				resp.end(ar.cause().getMessage());
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

	// TODO: the following looks very repetitive...

	private void handleGet(RoutingContext rc) {
		HttpServerRequest req = rc.request();
		HttpServerResponse resp = req.response();
		String name = req.getParam("name");

		LOG.info(String.format("HTTP read request for ring %s", name));

		Reader reader = new Reader("_unnamed", 100);
		RingRequest rr = new RingRequest(vertx, name);
		Future<Buffer> fut = rr.requestRead(reader);
		fut.setHandler(new DefaultWebHandler(resp));
	}

	private void handlePost(RoutingContext rc) {
		HttpServerRequest req = rc.request();
		HttpServerResponse resp = req.response();
		String name = req.getParam("name");
		//Buffer body = rc.getBody(); // TODO: test with curl
		Buffer body = Buffer.buffer("Hello\nWorld\nTest again\nAnd again\n"); // TODO: remove.

		LOG.info(String.format("POST request with body of size %d bytes", body.length()));

		RingRequest rr = new RingRequest(vertx, name);
		Future<Buffer> fut = rr.requestWrite(body);
		fut.setHandler(new DefaultWebHandler(resp));
	}

	private void handleStat(RoutingContext rc) {
		HttpServerRequest req = rc.request();
		HttpServerResponse resp = req.response();
		String name = req.getParam("name");

		LOG.info(String.format("Stat request for ring %s", name));

		RingRequest rr = new RingRequest(vertx, name);
		Future<Buffer> fut = rr.requestStat();
		fut.setHandler(new DefaultWebHandler(resp));
	}

	private void handleOpen(RoutingContext rc) {
		HttpServerRequest req = rc.request();
		HttpServerResponse resp = req.response();
		String name = req.getParam("name");

		LOG.info(String.format("Open request for ring %s", name));

		RingRequest rr = new RingRequest(vertx, name);
		Future<Buffer> fut = rr.requestOpen();
		fut.setHandler(new DefaultWebHandler(resp));
	}

	private void handleClose(RoutingContext rc) {
		HttpServerRequest req = rc.request();
		HttpServerResponse resp = req.response();
		String name = req.getParam("name");

		LOG.info(String.format("Close request for ring %s", name));

		RingRequest rr = new RingRequest(vertx, name);
		Future<Buffer> fut = rr.requestClose();
		fut.setHandler(new DefaultWebHandler(resp));
	}

	@Override
	public void start() {
		HttpServerOptions options = new HttpServerOptions().setLogActivity(true);

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.get("/stat/:name").handler(this::handleStat);
		router.get("/open/:name").handler(this::handleOpen);
		router.get("/close/:name").handler(this::handleClose);
		router.get("/read/:name").handler(this::handleGet);
		router.post("/write/:name").handler(this::handlePost);
		router.get("/write/:name").handler(this::handlePost); // TODO: For testing without actually posting data; remove

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
