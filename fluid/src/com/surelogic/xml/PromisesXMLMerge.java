package com.surelogic.xml;

import java.io.File;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

public class PromisesXMLMerge implements TestXMLParserConstants {

	/**
	 * Merges a local diff XML promises file, which must exist in the user's
	 * workspace, into a JSure release promises XML file, which may or may not
	 * exist. If the local file doesn't exist this method does nothing.
	 * <p>
	 * <i>Warning: This method only works if the tool user is running from
	 * source code in a meta-Eclipse.</i>
	 * 
	 * @param local
	 *            a local promises XML file in the user's workspace.
	 * @param jsure
	 *            a release promises XML file which may or may not exist.
	 */
	public static void mergeLocalXMLIntoJSureXML(File local, File jsure) {
		SLLogger.getLogger().log(
				Level.INFO,
				"mergeLocalXMLIntoJSureXML(local-> " + local + ", jsure->"
						+ jsure);

		boolean precondition = local.isFile() && local.exists();
		if (!precondition)
			return; // nothing to do

		try {
			final PackageElement localPE = PromisesXMLReader.loadRaw(local);
			if (localPE == null) {
				SLLogger.getLogger().log(Level.SEVERE, I18N.err(244, local),
						new Exception());
				return;
			}

			PackageElement jsurePE;

			/*
			 * We need to handle the startup case where the file may not exist
			 * in JSure yet.
			 */
			if (jsure.exists()) {
				// A release file exists, do a merge
				jsurePE = PromisesXMLReader.loadRaw(jsure);
				if (jsurePE == null) {
					SLLogger.getLogger().log(Level.SEVERE,
							I18N.err(244, jsure), new Exception());
					return;
				}

				jsurePE.mergeDeep(localPE, MergeType.LOCAL_TO_JSURE);
			} else {
				// no release file exists, create one
				jsurePE = localPE;
				jsure.getParentFile().mkdirs();
			}

			// Need to clear modified bits
			jsurePE.visit(new Cleaner());

			// increment the release version
			jsurePE.incrementReleaseVersion();

			PromisesXMLWriter w = new PromisesXMLWriter(jsure);
			w.write(jsurePE);

			/*
			 * Now delete the local file we no longer need it.
			 */
			local.delete();
		} catch (Exception e) {
			SLLogger.getLogger().log(Level.SEVERE, I18N.err(237, local, jsure),
					e);
		}
	}

	/**
	 * Rewrites a JSure release promises XML file optionally incrementing the
	 * release version. This method may be used to update the file format after
	 * a structural change. If the passed file doesn't exist this method does
	 * nothing.
	 * <p>
	 * <i>Warning: This method only works if the tool user is running from
	 * source code in a meta-Eclipse.</i>
	 * 
	 * @param jsure
	 *            a release promises XML file.
	 * @param incrementVersion
	 *            <tt>true</tt> if the release version should be incremented,
	 *            <tt>false</tt> if it should not be changed.
	 */
	public static void rewriteJSureXML(File jsure, boolean incrementVersion) {
		SLLogger.getLogger().log(Level.INFO, "rewriteJSureXML(jsure->" + jsure);

		boolean precondition = jsure.isFile() && jsure.exists();
		if (!precondition)
			return; // nothing to do

		try {
			final PackageElement jsurePE = PromisesXMLReader.loadRaw(jsure);
			if (jsurePE == null) {
				SLLogger.getLogger().log(Level.SEVERE, I18N.err(244, jsure),
						new Exception());
				return;
			}

			// Need to clear modified bits (really should not be any)
			jsurePE.visit(new Cleaner());

			if (incrementVersion) {
				// increment the release version
				jsurePE.incrementReleaseVersion();
			}

			PromisesXMLWriter w = new PromisesXMLWriter(jsure);
			w.write(jsurePE);

		} catch (Exception e) {
			SLLogger.getLogger().log(Level.SEVERE, I18N.err(243, jsure), e);
		}
	}

	/**
	 * Merges the passed release promises XML file into the passed local diff
	 * promises XML file. If the passed release file does not exist this method
	 * does nothing.
	 * 
	 * @param local
	 *            a local promises XML file in the user's workspace.
	 * @param jsure
	 *            a release promises XML file.
	 */
	public static void mergeJSureXMLIntoLocalXML(File local, File jsure) {
		SLLogger.getLogger().log(
				Level.INFO,
				"mergeJSureXMLIntoLocalXML(local-> " + local + ", jsure->"
						+ jsure);

		boolean precondition = jsure.isFile() && jsure.exists()
				&& local.isFile() && local.exists();
		if (!precondition)
			return; // nothing to do

		try {
			final PackageElement localPE = PromisesXMLReader.loadRaw(local);
			if (localPE == null) {
				SLLogger.getLogger().log(Level.SEVERE, I18N.err(244, local),
						new Exception());
				return;
			}
			final PackageElement jsurePE = PromisesXMLReader.loadRaw(jsure);
			if (jsurePE == null) {
				SLLogger.getLogger().log(Level.SEVERE, I18N.err(244, jsure),
						new Exception());
				return;
			}

			/*
			 * We only need to process the local file if the release file has a
			 * higher version number.
			 */
			if (jsurePE.getReleaseVersion() > localPE.getReleaseVersion()) {
				localPE.mergeDeep(jsurePE, MergeType.JSURE_TO_LOCAL);
				final PackageElement merged = diff(localPE);

				// Set the diff version to the same as the release file
				merged.setReleaseVersion(jsurePE.getReleaseVersion());

				PromisesXMLWriter w = new PromisesXMLWriter(local);
				w.write(merged);
			}
		} catch (Exception e) {
			SLLogger.getLogger().log(Level.SEVERE, I18N.err(238, jsure, local),
					e);
		}
	}

	/*
	 * TODO Ask about this "diff" seems odd???
	 */
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
	 * 
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
	 * Bump revision and mark as annos as clean/unmodified.
	 * <p>
	 * Only called on a merge to Fluid.
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

		@Override
		public Void visit(AnnotationElement a) {
			if (a.isToBeDeleted()) {
				a.removeFromParent();
				return defaultValue;
			}
			a.markAsUnmodified();
			return defaultValue;
		}

		@Override
		protected Void visitFunc(AbstractFunctionElement f) {
			super.visitFunc(f);

			// Remove unannotated parameters
			int i = 0;
			for (FunctionParameterElement p : f.getParameters()) {
				if (p != null && p.getPromises().isEmpty()) {
					f.removeParameter(i);
				}
				i++;
			}
			return defaultValue;
		}

		@Override
		public Void visit(ClassElement c) {
			super.visit(c);

			// These checks work because we've removed unannotated decls above
			for (MethodElement m : c.getMethods()) {
				if (m.getPromises().isEmpty() && m.getChildren().length == 0) {
					c.removeMethod(m);
				}
			}
			for (ConstructorElement e : c.getConstructors()) {
				if (e.getPromises().isEmpty() && e.getChildren().length == 0) {
					c.removeConstructor(e);
				}
			}
			for (NestedClassElement n : c.getNestedClasses()) {
				if (n.getPromises().isEmpty() && n.getChildren().length == 0) {
					c.removeClass(n);
				}
			}
			return defaultValue;
		}
	}
}
