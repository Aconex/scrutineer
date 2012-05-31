package com.aconex.scrutineer;

import java.io.IOException;
import java.io.ObjectInputStream;

public enum LongIdAndVersionFactory implements IdAndVersionFactory {

	INSTANCE;

	@Override
	public LongIdAndVersion create(Object id, long version) {
		return new LongIdAndVersion(toLong(id), version);
	}

	private long toLong(Object id) {
		if (id instanceof Number) {
			return ((Number) id).longValue();
		} else {
			return Long.parseLong(id.toString());
		}
	}

	public LongIdAndVersion readFromStream(ObjectInputStream inputStream) throws IOException {
		return new LongIdAndVersion(inputStream.readLong(), inputStream.readLong());
	}
}