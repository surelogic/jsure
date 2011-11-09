package com.surelogic.javac.coe;

import com.surelogic.common.AbstractJavaZip;

public interface IZipContext<T> {
	NameSorter getNameSorter();
	AbstractJavaZip<T> getZip();
}
