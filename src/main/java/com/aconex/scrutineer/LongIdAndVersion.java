package com.aconex.scrutineer;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class LongIdAndVersion extends AbstractIdAndVersion {

	public static final IdAndVersionFactory FACTORY = LongIdAndVersionFactory.INSTANCE;

    private final long id;

    public LongIdAndVersion(long id, long version) {
    	super(version);
        this.id = id;
    }

    public String getId() {
    	// TODO
        return Long.toString(id);
    }

    public long getLongId() {
        return id;
    }

	@Override
	protected HashCodeBuilder appendId(HashCodeBuilder appender) {
		return appender.append(id);
	}

	@Override
	protected CompareToBuilder appendId(CompareToBuilder appender, IdAndVersion other) {
		return appender.append(id, ((LongIdAndVersion)other).id);
	}

	@Override
	protected void writeId(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeLong(id);
	}



}
