package com.surelogic.xml;

import java.io.File;

import com.surelogic.common.FileUtility;

public class PromisesXMLMerge implements TestXMLParserConstants {
	public static final boolean onlyKeepDiffs = true;
	
	/**
	 * @param onlyMerge also copy (to fluid) if false
	 */
	public static void merge(final boolean onlyMerge, File to, File from) {
		if (from.isFile()) {			
			if (!to.exists()) {
				// Check if I should copy 
				if (onlyMerge) {
					//System.out.println("Ignoring "+from);
					return; // No need to do anything
				}
				// Copy 
				System.out.println("Copying "+from+" into "+to);
				to.getParentFile().mkdirs();
				FileUtility.copy(from, to);
			} else {
				try {					
					// Merge
					System.out.println("Merging "+from+" into "+to);
					PackageElement target = PromisesXMLReader.loadRaw(to);
					PackageElement source = PromisesXMLReader.loadRaw(from);
					PackageElement merged;
					if (onlyKeepDiffs) {
						if (onlyMerge) {
							// Updating client
							PackageElement all = merge_private(onlyMerge, target, source);	
							merged = diff(all);
						} else {
							// Merging to fluid 
							/* Simulates what we used to do
							PackageElement all = merge_private(!onlyMerge, source, target);	
							merged = merge_private(onlyMerge, target, all);
							*/
							merged = merge_private(onlyMerge, target, source);	
							// Need to clear modified bits
							merged.visit(new Cleaner());
						}
					} else {
						merged = merge_private(onlyMerge, target, source);	
					}					
					PromisesXMLWriter w = new PromisesXMLWriter(to);
					w.write(merged);
					if (!onlyMerge) {
						// Merging all changes to fluid, so they should both be the same afterward
						// (or the local one should be deleted/empty)
						// TODO what about conflicts?
						if (PromisesXMLMerge.onlyKeepDiffs) {
							from.delete();
						} else {
							w = new PromisesXMLWriter(from);
							w.write(merged);
						}
					}
				} catch (Exception e) {
					System.err.println("While merging "+from+" into "+to);
					e.printStackTrace();
				}
			}
		} 
		else if (from.isDirectory()) {
			for(File f : from.listFiles(TestXMLParserConstants.XML_FILTER)) {
				merge(onlyMerge, new File(to, f.getName()), f);
			}
		}
		// 'from' doesn't exist, so nothing to do
	}
	
	/**
	 * Merge changes into the "original
	 * 
	 * @param toClient update if true; merge to fluid otherwise
	 */
	private static PackageElement merge_private(boolean toClient, PackageElement orig, PackageElement changed) {
		return orig.merge(changed, toClient);
	}
	
	public static PackageElement diff(PackageElement root) {
		// Ok to modify root, since we'll just mark it as clean afterwards
		root.markAsClean();
		root.visit(new Marker());
		PackageElement p = root.copyIfDirty();
		if (p != null) {
			p.visit(new Flusher());
		}
		return p;
	}
	
	/**
	 * Removed cached attributes used for diffing (not meant to be persisted)
	 * @author Edwin
	 */
	private static class Flusher extends AbstractJavaElementVisitor<Void> {
		Flusher() {
			super(null);
		}

		@Override
		protected Void combine(Void old, Void result) {
			return null;
		}
		
		@Override
		public Void visit(AnnotationElement a) {
			a.flushDiffState();
			return defaultValue;
		}
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
		
		@Override
		public Void visit(AnnotationElement a) {
			if (a.isModified()) {
				a.markAsDirty();
			}
			return defaultValue;
		}
	}
	
	/**
	 * Bump revision and mark as annos as clean/unmodified
	 * 
	 * @author Edwin
	 */
	private static class Cleaner extends AbstractJavaElementVisitor<Void> {
		Cleaner() {
			super(null);
		}

		@Override
		protected Void combine(Void old, Void result) {
			return null;
		}
		
		public Void visit(AnnotationElement a) {
			if (a.isModified()) {
				a.incrRevision();
			}
			a.markAsClean();
			return defaultValue;
		}
	}	
}
