package com.surelogic.dropsea.irfree;

import java.io.*;

import com.surelogic.common.IViewable;

public interface ISeaDiff {
	boolean isEmpty();
	IViewable[] getCategories();
	void write(File file) throws IOException;
}
