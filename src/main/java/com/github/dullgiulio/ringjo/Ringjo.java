package com.github.dullgiulio.ringjo;

import com.github.dullgiulio.ringjo.verticles.HttpRunner;
import com.github.dullgiulio.ringjo.verticles.Registry;
import io.vertx.core.Vertx;

public class Ringjo {
	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		HttpRunner http = new HttpRunner().setPort(8080);
		Registry reg = new Registry();
		vertx.deployVerticle(http);
		vertx.deployVerticle(reg);
	}
}
