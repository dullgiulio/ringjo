package verticles;

import codecs.ReaderCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
import ring.Line;
import ring.Reader;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HttpRunner extends AbstractVerticle {
	private int port;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);

	public HttpRunner setPort(int port) {
		this.port = port;
		return this;
	}

	private void handleGet(RoutingContext rc) {
		HttpServerRequest req = rc.request();
		HttpServerResponse resp = req.response();
		LOG.info(String.format("URI: %s; path = %s", req.uri(), req.path()));
		Buffer buffer = Buffer.buffer();
		Future<Void> fut = Future.future();
		Reader reader = new Reader("_unnamed", 100);
		vertx.eventBus().send("ringjo.read", reader, ar -> {
			if (!ar.succeeded()) {
				resp.setStatusCode(500);
				buffer.appendString("could not read lines from ring");
			} else {
				resp.putHeader("Content-Type", "text/plain");
				Reader r = (Reader) ar.result().body();
				List<Line> lines = r.getBuffer();
				for (Line l : lines) {
					buffer.appendString(String.format("%s: %s\n", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(l.getDate()), l.toString()));
				}
			}
			fut.complete();
		});
		fut.setHandler(ar -> {
			resp.end(buffer);
		});
	}

	private void handlePost(RoutingContext rc) {
		HttpServerRequest req = rc.request();
		HttpServerResponse resp = req.response();
		//Buffer body = rc.getBody(); // TODO: test with curl
		Buffer body = Buffer.buffer("Hello\nWorld\nTest again\nAnd again\n"); // TODO: remove.
		LOG.info(String.format("POST request with body of size %d bytes", body.length()));
		Buffer buffer = Buffer.buffer();
		Future<Void> fut = Future.future();
		vertx.eventBus().send("ringjo.write", body, ar -> {
			if (!ar.succeeded()) {
				resp.setStatusCode(500);
				buffer.appendString("could not add lines to ring");
			} else {
				buffer.appendString(String.format("Added %d messages", (Integer) ar.result().body()));
			}
			fut.complete();
		});
		fut.setHandler(ar -> {
			resp.end(buffer);
		});
	}

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		vertx.eventBus().registerDefaultCodec(Reader.class, new ReaderCodec());
	}

	@Override
	public void start() {
		HttpServerOptions options = new HttpServerOptions().setLogActivity(true);

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		// TODO: put path variable before actions and select different BusExecutors based on that.
		router.get("/read").handler(this::handleGet);
		router.post("/write").handler(this::handlePost);
		router.get("/write").handler(this::handlePost); // TODO: For testing

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
