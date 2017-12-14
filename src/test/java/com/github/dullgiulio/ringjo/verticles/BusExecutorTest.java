package com.github.dullgiulio.ringjo.verticles;

import com.github.dullgiulio.ringjo.codecs.ReaderCodec;
import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.verticles.bus.RingAddress;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class BusExecutorTest {
	Vertx vertx;
	RingAddress address = new RingAddress("ringname");

	@Before
	public void setUp(TestContext context) {
		JsonObject config = new JsonObject().put("name", address.getName()).put("size", 100);
		vertx = Vertx.vertx();
		vertx.eventBus().registerDefaultCodec(Reader.class, new ReaderCodec());
		vertx.deployVerticle(new BusExecutor(), new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());
	}

	@After
	public void tearDown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void testWrite(TestContext context) {
		vertx.eventBus().send(address.getWriteAddress(), Buffer.buffer(""), // empty
				context.asyncAssertSuccess(
						ar -> assertEquals(0, Integer.parseInt(ar.body().toString()))));
		vertx.eventBus().send(address.getWriteAddress(), Buffer.buffer("three\nlines\nbody"),
				context.asyncAssertSuccess(
						ar -> assertEquals(3, Integer.parseInt(ar.body().toString()))));
		vertx.eventBus().send(address.getWriteAddress(), Buffer.buffer("two\nlines\n"),
				context.asyncAssertSuccess(
						ar -> assertEquals(2, Integer.parseInt(ar.body().toString()))));
	}

	@Test
	public void testEmptyRead(TestContext context) {
		vertx.eventBus().send(address.getReadAddress(), new Reader("_", 0),
				context.asyncAssertSuccess(ar -> {
					Reader rr = (Reader) ar.body();
					assertNotEquals(null, rr);
					assertEquals(0, rr.getPos());
					assertEquals(0, rr.getBuffer().size());
				}));
	}

	@Test
	public void testNormalRead(TestContext context) {
		// Make sure there is enough data in the ring; other tests are not guaranteed to finish before this one.
		vertx.eventBus().send(address.getWriteAddress(), Buffer.buffer("three\nlines\nbody"),
				context.asyncAssertSuccess(
						ar -> assertEquals(3, Integer.parseInt(ar.body().toString()))));
		vertx.eventBus().send(address.getReadAddress(), new Reader("_", 3),
				context.asyncAssertSuccess(ar -> {
					Reader rr = (Reader) ar.body();
					assertNotEquals(null, rr);
					assertEquals(3, rr.getPos());
					assertEquals(3, rr.getBuffer().size());
				}));
	}
}