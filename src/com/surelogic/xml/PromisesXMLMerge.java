package com.surelogic.xml;

public class PromisesXMLMerge implements TestXMLParserConstants {
	/**
	 * Merge changes into the "original
	 */
	public static PackageElement merge(PackageElement orig, PackageElement changed) {
		return orig.merge(changed);
	}
}
