package verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import ring.Executor;

public class HttpRunner extends AbstractVerticle {
	private Executor executor;
	private Thread executorThread;
	private HttpServer server;
	private int port;

	private static final Logger LOG = LoggerFactory.getLogger(HttpRunner.class);

	public HttpRunner setPort(int port) {
		this.port = port;
		return this;
	}

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		executor = new Executor(10, 1024);
		executorThread = new Thread(executor);
		executorThread.start();
	}

	@Override
	public void start() {
		HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
		server = vertx.createHttpServer(options);
		server.listen(port, event -> {
			if (!event.succeeded()) {
				LOG.error("cannot start HTTP server", event.cause());
				return;
			}
			LOG.info(String.format("HTTP server running on port %d", event.result().actualPort()));
			awaitTermination();
		});
	}

	public void awaitTermination() {
		try {
			executorThread.join();
		} catch (InterruptedException e) {
			// TODO: Anything at all?
		}
	}
}
