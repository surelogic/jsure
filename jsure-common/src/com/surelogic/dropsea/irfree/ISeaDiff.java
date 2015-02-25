package com.surelogic.dropsea.irfree;

import java.io.*;

import com.surelogic.common.IViewable;
import com.surelogic.dropsea.ScanDifferences;

public interface ISeaDiff {
	boolean isEmpty();
	IViewable[] getCategories();
	void write(File file) throws IOException;
	ScanDifferences build();
}
