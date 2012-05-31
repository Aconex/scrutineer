package com.aconex.scrutineer;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class StringIdAndVersion extends AbstractIdAndVersion {

	public static final IdAndVersionFactory FACTORY = StringIdAndVersionFactory.INSTANCE;

	private final String id;

	public StringIdAndVersion(String id, long version) {
		super(version);
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	protected HashCodeBuilder appendId(HashCodeBuilder appender) {
		return appender.append(id);
	}

	@Override
	protected CompareToBuilder appendId(CompareToBuilder appender, IdAndVersion other) {
		return appender.append(id, ((StringIdAndVersion) other).id);
	}

	@Override
	protected void writeId(ObjectOutputStream objectOutputStream) throws IOException {
		objectOutputStream.writeUTF(getId());
	}

}
