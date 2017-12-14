package com.github.dullgiulio.ringjo.verticles;

import com.github.dullgiulio.ringjo.verticles.bus.RingAddress;
import com.github.dullgiulio.ringjo.verticles.bus.RingRequest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class RegistryTest {
	Vertx vertx;

	@Before
	public void setUp(TestContext context) {
		vertx = Vertx.vertx();
		vertx.deployVerticle(new Registry(), context.asyncAssertSuccess());
	}

	@After
	public void tearDown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void testRegistryAdd(TestContext context) {
		vertx.eventBus().send(RingRequest.OPEN_ADDRESS, "testRegistryAdd0", context.asyncAssertSuccess(ar -> {
			assertEquals("OK", ar.body());
			vertx.eventBus().send(RingRequest.STAT_ADDRESS, "testRegistryAdd0", context.asyncAssertSuccess(
					done -> assertEquals("OK", done.body())
			));
		}));
		vertx.eventBus().send(RingRequest.STAT_ADDRESS, "_nonexisting", context.asyncAssertFailure());
		vertx.eventBus().send(RingRequest.OPEN_ADDRESS, "", context.asyncAssertFailure()); // empty name fails
	}

	@Test
	public void testRegistryRemove(TestContext context) {
		vertx.eventBus().send(RingRequest.OPEN_ADDRESS, "testRegistryAdd1", context.asyncAssertSuccess(ar -> {
			assertEquals("OK", ar.body());
			vertx.eventBus().send(RingRequest.CLOSE_ADDRESS, "testRegistryAdd1", context.asyncAssertSuccess(
					done -> assertEquals("OK", done.body())
			));
		}));
	}

	@Test
	public void testRingReachable(TestContext context) {
		String name = "testRegistryAdd2";
		vertx.eventBus().send(RingRequest.OPEN_ADDRESS, name, context.asyncAssertSuccess(ar -> {
			assertEquals("OK", ar.body());
			RingAddress address = new RingAddress(name);
			vertx.eventBus().send(address.getWriteAddress(), Buffer.buffer("three\nlines\nbody"),
					context.asyncAssertSuccess(
							done -> assertEquals(3, Integer.parseInt(done.body().toString()))));
		}));
	}
}