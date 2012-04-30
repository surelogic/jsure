package com.surelogic.xml;

public enum MergeType {
	/**
	 * Update only Java elements, not comments or annotations
	 */
	JAVA,
	/**
	 * Merge dirty elements to fluid
	 */
	LOCAL_TO_JSURE,
	/**
	 * Update changes to client
	 */
	JSURE_TO_LOCAL;
}
