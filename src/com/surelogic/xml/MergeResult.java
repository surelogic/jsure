package com.surelogic.xml;

class MergeResult<T extends IJavaElement> {
	final T element;
	final boolean isModified;
	
	MergeResult(T e, boolean mod) {
		element = e;
		isModified = mod;
	}
	
	@SuppressWarnings("unchecked")
	private static final MergeResult nullResult = new MergeResult(null, false);
	
	@SuppressWarnings("unchecked")
	static <T extends IJavaElement> MergeResult<T> nullResult() {
		return nullResult;
	}
}
