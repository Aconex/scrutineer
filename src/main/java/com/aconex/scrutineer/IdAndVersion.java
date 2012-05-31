package com.aconex.scrutineer;

import java.io.IOException;
import java.io.ObjectOutputStream;

public interface IdAndVersion extends Comparable<IdAndVersion> {

	String getId();

	long getVersion();

	void writeToStream(ObjectOutputStream objectOutputStream) throws IOException;

}
