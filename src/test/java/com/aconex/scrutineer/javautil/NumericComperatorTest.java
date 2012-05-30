package com.aconex.scrutineer.javautil;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.aconex.scrutineer.IdAndVersion;

public class NumericComperatorTest {

	@Test
	public void test() {
		NumericIdAndVersionComparator c = new NumericIdAndVersionComparator();
		assertTrue(c.compare(obj(1, 1), obj(1, 1)) == 0);
		assertTrue(c.compare(obj(1, 1), obj(1, 2)) < 0);
		assertTrue(c.compare(obj(1, 1), obj(2, 2)) < 0);
		assertTrue(c.compare(obj(2, 2), obj(1, 2)) > 0);
	}

	private IdAndVersion obj(long id, long version) {
		return new IdAndVersion(Long.toString(id), version);
	}
}
