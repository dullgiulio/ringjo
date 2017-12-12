package com.github.dullgiulio.ringjo.codecs;

import com.github.dullgiulio.ringjo.ring.Line;
import com.github.dullgiulio.ringjo.ring.Reader;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReaderCodec implements MessageCodec<Reader, Reader> {
	@Override
	public void encodeToWire(Buffer buffer, Reader reader) {
		JsonObject json = new JsonObject();
		json.put("name", reader.getName());
		json.put("pos", reader.getPos());
		json.put("rpos", reader.getReaderPos());

		List<Line> ls = reader.getBuffer();
		JsonArray lines = new JsonArray();
		for (Line l : ls) {
			lines.add(new JsonObject()
					.put("date", l.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
					.put("content", l.toString()));
		}
		json.put("lines", lines);

		byte[] data = json.encode().getBytes();
		buffer.appendInt(data.length);
		buffer.appendBytes(data);
	}

	@Override
	public Reader decodeFromWire(int pos, Buffer buffer) {
		int size = buffer.getInt(pos);
		String jsonData = buffer.getString(pos+4, pos+4+size);
		JsonObject json = new JsonObject(jsonData);
		String name = json.getString("name");
		JsonArray lines = json.getJsonArray("lines");
		List<Line> ls = new ArrayList<>(lines.size());

		for (Object o : lines) {
			JsonObject l = (JsonObject) o;
			LocalDateTime date = LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(l.getString("date")));
			String content = l.getString("content");
			ls.add(new Line(date, content));
		}

		Reader r = new Reader(name, ls.size());
		r.setBuffer(ls);
		r.setPos(json.getInteger("pos"));
		r.setReaderPos(json.getInteger("rpos"));
		return null;
	}

	@Override
	public Reader transform(Reader reader) {
		return reader;
	}

	@Override
	public String name() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public byte systemCodecID() {
		return -1;
	}
}
