package com.surelogic.xml;

public class PromisesXMLMerge implements TestXMLParserConstants {
	/**
	 * Merge changes into the "original
	 * 
	 * @param toClient update if true; merge to fluid otherwise
	 */
	public static PackageElement merge(boolean toClient, PackageElement orig, PackageElement changed) {
		return orig.merge(changed, toClient);
	}
}
