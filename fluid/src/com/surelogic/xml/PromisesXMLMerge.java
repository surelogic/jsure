package com.surelogic.xml;

import java.io.File;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

public class PromisesXMLMerge implements TestXMLParserConstants {

	public static void merge(MergeType type, File local, File jsure) {
		if (type == MergeType.LOCAL_TO_JSURE) {
			if (local.isFile() && local.exists()) {
				mergeLocalXMLIntoJSureXML(local, jsure);
			} else {
				throw new IllegalArgumentException(local.toString()
						+ " must be an existing file (it is not) for a " + type
						+ " merge");
			}
		} else if (type == MergeType.JSURE_TO_LOCAL) {
			mergeJSureXMLIntoLocalXML(local, jsure);
		} else {
			throw new UnsupportedOperationException(type.toString()
					+ " merge is not supported");
		}
	}

	private static void mergeLocalXMLIntoJSureXML(File local, File jsure) {
		try {
			SLLogger.getLogger().log(
					Level.INFO,
					"mergeLocalXMLIntoJSureXML(local-> " + local + ", jsure->"
							+ jsure);
			PackageElement source = PromisesXMLReader.loadRaw(local);
			PackageElement merged;

			/*
			 * We need to handle the startup case where the file may not exist
			 * in JSure yet.
			 */
			if (jsure.exists()) {
				PackageElement target = PromisesXMLReader.loadRaw(jsure);

				merged = merge_private(false, target, source);
			} else {
				merged = source;
				jsure.getParentFile().mkdirs();
			}

			// Need to clear modified bits
			merged.visit(new Cleaner());

			PromisesXMLWriter w = new PromisesXMLWriter(jsure);
			w.write(merged);

			/*
			 * Now delete the local file we no longer need it.
			 */
			local.delete();
		} catch (Exception e) {
			SLLogger.getLogger().log(Level.SEVERE, I18N.err(237, local, jsure),
					e);
		}
	}

	private static void mergeJSureXMLIntoLocalXML(File local, File jsure) {
		SLLogger.getLogger().log(
				Level.INFO,
				"mergeJSureXMLIntoLocalXML(local-> " + local + ", jsure->"
						+ jsure);

		if (!jsure.exists())
			return; // nothing to do

		try {
			PackageElement target = PromisesXMLReader.loadRaw(local);
			PackageElement source = PromisesXMLReader.loadRaw(jsure);

			if (!source.needsToUpdate(target)) {
				return;
			}
			PackageElement all = merge_private(true, target, source);
			PackageElement merged = diff(all);

			PromisesXMLWriter w = new PromisesXMLWriter(local);
			w.write(merged);

		} catch (Exception e) {
			SLLogger.getLogger().log(Level.SEVERE, I18N.err(238, jsure, local),
					e);
		}
	}

	/**
	 * Merge changes into the "original
	 * 
	 * @param toClient
	 *            update if true; merge to fluid otherwise
	 */
	private static PackageElement merge_private(boolean toClient,
			PackageElement orig, PackageElement changed) {
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
