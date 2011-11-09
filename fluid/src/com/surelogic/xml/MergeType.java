package com.surelogic.xml;

public enum MergeType {
	/**
	 * Update only Java elements, not comments or annotations
	 */
	JAVA,
	/**
	 * Merge dirty elements to fluid 
	 */
	MERGE, 
	/**
	 * Update changes to client
	 */
	UPDATE;	
	
	public static MergeType get(boolean toClient) {
		return toClient ? UPDATE : MERGE;
	}
}
