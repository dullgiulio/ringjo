import io.vertx.core.Vertx;
import verticles.BusExecutor;
import verticles.HttpRunner;

public class Ringjo {
	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		HttpRunner http = new HttpRunner().setPort(8080);
		BusExecutor exec = new BusExecutor().setRingSize(1024);
		vertx.deployVerticle(http);
		vertx.deployVerticle(exec);
	}
}
