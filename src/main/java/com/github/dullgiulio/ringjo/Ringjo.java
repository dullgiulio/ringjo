package com.github.dullgiulio.ringjo;

import com.github.dullgiulio.ringjo.codecs.ReaderCodec;
import com.github.dullgiulio.ringjo.ring.Reader;
import com.github.dullgiulio.ringjo.verticles.BusRegistry;
import com.github.dullgiulio.ringjo.verticles.HttpRunner;
import com.github.dullgiulio.ringjo.verticles.bus.Registry;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.cli.*;

import java.io.FileNotFoundException;

public class Ringjo {
	private static final Logger LOG = LoggerFactory.getLogger(Ringjo.class);

	private Option portOpt = new Option("port", true,"Port to listen to for HTTP interface");
	private Option hazelcastOpt = new Option("hazelcast", true, "Hazelcast configuration or dash for none");
	private	CommandLine cmd = null;

	private void parseOpts(String[] args) {
		Options opts = new Options();
		CommandLineParser parser = new DefaultParser();
		try {
			cmd = parser.parse(opts, args);
		} catch (ParseException e) {
			LOG.fatal(String.format("Cannot parse command line options: %s", e.getMessage()));
		}
	}

	private int getPort(int port) {
		if (!cmd.hasOption("port")) {
			return port;
		}
		try {
			port = Integer.parseInt(portOpt.getValue());
		} catch (NumberFormatException e) {
			LOG.fatal(String.format("Cannot parse -port parameter: must be a valid number: %s", e.getMessage()));
		}
		return port;
	}

	private Config getHazelcastConfig() {
		Config cfg = null;
		if (!cmd.hasOption("hazelcast")) {
			return new Config();
		}
		String hazelcastConfFile = hazelcastOpt.getValue("-");
		if (hazelcastConfFile.equals("-")) {
			return new Config();
		}
		try {
			cfg = new XmlConfigBuilder(hazelcastConfFile).build();
		} catch(FileNotFoundException e) {
			LOG.fatal(String.format("Cannot load Hazelcast configuration file: %s", e.getMessage()));
		}
		return cfg;
	}

	private Vertx initVertx() {
		Vertx vertx = Vertx.vertx();
		vertx.eventBus().registerDefaultCodec(Reader.class, new ReaderCodec());
		return vertx;
	}

	private Registry initRegistry() {
		Registry registry = null;
		if (cmd.hasOption("hazelcast")) {
			Config cfg = getHazelcastConfig();
			HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
			registry = new Registry(hazelcastInstance.getMap("ringjo.registry"));
		}
		if (registry == null) {
			registry = new Registry();
		}
		return registry;
	}

	private void run(String[] args) {
		parseOpts(args);

		Vertx vertx = initVertx();
		HttpRunner http = new HttpRunner().setPort(getPort(8080));

		Registry registry = initRegistry();
		BusRegistry reg = new BusRegistry(registry);
		vertx.deployVerticle(http);
		vertx.deployVerticle(reg);
	}

	public static void main(String[] args) {
		Ringjo ringjo = new Ringjo();
		ringjo.run(args);
	}
}
