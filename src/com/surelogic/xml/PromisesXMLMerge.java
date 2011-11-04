package com.surelogic.xml;

public class PromisesXMLMerge implements TestXMLParserConstants {
	public static final boolean onlyKeepDiffs = false;
	
	/**
	 * Merge changes into the "original
	 * 
	 * @param toClient update if true; merge to fluid otherwise
	 */
	public static PackageElement merge(boolean toClient, PackageElement orig, PackageElement changed) {
		return orig.merge(changed, toClient);
	}
	
	public static PackageElement diff(PackageElement root) {
		// Ok to modify root, since we'll just mark it as clean afterwards
		root.visit(new Marker());
		return root.copyIfDirty();
	}
	
	/**
	 * Mark the annos as dirty, so I can figure out what to keep as a diff
	 * 
	 * @author Edwin
	 */
	private static class Marker extends AbstractJavaElementVisitor<Void> {
		Marker() {
			super(null);
		}

		@Override
		protected Void combine(Void old, Void result) {
			return null;
		}
		
		public Void visit(AnnotationElement a) {
			if (a.isModified()) {
				a.markAsDirty();
			}
			return defaultValue;
		}
	}
}
