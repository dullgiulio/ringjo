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

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRunner extends AbstractVerticle {
	private int port;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);
	private final Pattern onlyAlphanum = Pattern.compile("[a-zA-Z0-9_\\-]+");
	private final String defaultReaderName = "_unnamed";
	private final int defaultReaderCapacity = 100;

	private interface RingResponse extends BiFunction<RoutingContext, RingRequest, Future<Buffer>> {}

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

	private Handler<RoutingContext> namedRequest(RingResponse handle) {
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
			Future<Buffer> fut = handle.apply(rc, rr);
			fut.setHandler(new DefaultWebHandler(resp));
		};
	}

	private long convertLong(String s) {
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private RingResponse handleRead() {
		return (rc, rr) -> {
			HttpServerRequest req = rc.request();
			long offset = convertLong(req.getParam("offset"));
			if (offset < 0) {
				LOG.info(String.format("Requested invalid offset '%s', forced zero", req.getParam("offset")));
				offset = 0;
			}
			LOG.info(String.format("Read request for ring %s, offset %d", rr.getRingName(), offset));
			Reader reader = new Reader(defaultReaderName, defaultReaderCapacity);
			reader.setPos(offset);
			return rr.requestRead(reader);
		};
	}

	private Handler<RoutingContext> handleReadWithoutOffset() {
		return rc -> {
			HttpServerRequest req = rc.request();
			HttpServerResponse resp = req.response();
			String name = req.getParam("name");
			LOG.info(String.format("Read request for ring %s without offset, redirected", name));
			resp.putHeader("Location", req.path() + "/0");
			resp.setStatusCode(HttpResponseStatus.PERMANENT_REDIRECT.code());
			resp.end();
		};
	}

	private RingResponse handleDummyPost() {
		return (rc, rr) -> {
			Buffer body = Buffer.buffer("Hello\nWorld\nTest again\nAnd again\n");
			LOG.info(String.format("Post request for ring %s with body of size %d bytes", rr.getRingName(), body.length()));
			return rr.requestWrite(body);
		};
	}

	private RingResponse handleWrite() {
		return (rc, rr) -> {
			Buffer body = rc.getBody();
			LOG.info(String.format("Post request for ring %s with body of size %d bytes", rr.getRingName(), body.length()));
			return rr.requestWrite(body);
		};
	}

	private RingResponse handleStat() {
		return (rc, rr) -> {
			LOG.info(String.format("Stat request for ring %s", rr.getRingName()));
			return rr.requestStat();
		};
	}

	private RingResponse handleOpen() {
		return (rc, rr) -> {
			LOG.info(String.format("Open request for ring %s", rr.getRingName()));
			return rr.requestOpen();
		};
	}

	private RingResponse handleClose() {
		return (rc, rr) -> {
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
		router.get("/read/:name").handler(handleReadWithoutOffset());
		router.get("/read/:name/:offset").handler(namedRequest(handleRead()));
		router.post("/write/:name").handler(namedRequest(handleWrite()));
		// TODO: For testing without actually posting data; remove
		router.get("/write/:name").handler(namedRequest(handleDummyPost()));

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
