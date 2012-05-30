package com.aconex.scrutineer.javautil;

import java.util.Comparator;

import org.apache.commons.lang.builder.CompareToBuilder;

import com.aconex.scrutineer.IdAndVersion;

public class NumericIdAndVersionComparator implements Comparator<IdAndVersion> {
	@Override
	public int compare(IdAndVersion o1, IdAndVersion o2) {
		return new CompareToBuilder()
			.append(Long.parseLong(o1.getId()), Long.parseLong(o2.getId()))
			.append(o1.getVersion(), o2.getVersion())
			.toComparison();
	}
}