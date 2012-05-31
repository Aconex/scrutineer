package com.aconex.scrutineer.elasticsearch;

import com.aconex.scrutineer.IdAndVersion;
import com.aconex.scrutineer.IdAndVersionFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

public class IteratorFactory {

    private final IdAndVersionFactory factory;

	public IteratorFactory(IdAndVersionFactory factory) {
    	this.factory = factory;
    }

	public Iterator<IdAndVersion> forFile(File file) {
        try {
            return new IdAndVersionInputStreamIterator(new IdAndVersionDataReader(factory,
                    new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
