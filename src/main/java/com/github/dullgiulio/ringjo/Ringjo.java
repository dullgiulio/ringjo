package com.github.dullgiulio.ringjo;

import com.github.dullgiulio.ringjo.codecs.ReaderCodec;
import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.verticles.BusRegistry;
import com.github.dullgiulio.ringjo.verticles.HttpRunner;
import io.vertx.core.Vertx;

public class Ringjo {
	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		vertx.eventBus().registerDefaultCodec(Reader.class, new ReaderCodec());
		HttpRunner http = new HttpRunner().setPort(8080);
		BusRegistry reg = new BusRegistry();
		vertx.deployVerticle(http);
		vertx.deployVerticle(reg);
	}
}
