package com.surelogic.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import org.xml.sax.Attributes;

import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.IXmlResultListener;
import com.surelogic.dropsea.irfree.NestedJSureXmlReader;
import com.surelogic.xml.AnnotatedJavaElement.Access;

/**
 * Reads .promises.xml files to create JSure models
 * 
 * @author Edwin
 */
public class PromisesXMLReader extends NestedJSureXmlReader implements
		IXmlResultListener, TestXMLParserConstants {
	private PackageElement pkg;
	private String pkgName;
	private int releaseVersion = 0;
	private ClassElement clazz;
	private final List<AnnotationElement> promises = new ArrayList<AnnotationElement>(
			0);

	@Override
	protected String checkForRoot(String name, Attributes attributes) {
		// System.out.println("Checking for root: "+name);
		if (PACKAGE.equals(name)) {
			if (attributes == null) {
				return "";
			}
			final String pkg = attributes.getValue(NAME_ATTRB);
			String rev = attributes.getValue(RELEASE_VERSION_ATTRB);
			if (rev == null) {
				/*
				 * Try the old "revision" attribute (for backwards
				 * compatibility).
				 * 
				 * Output a warning about this file it is in an obsolete format.
				 */
				rev = attributes.getValue("revision");
				LOG.warning(pkg + " is still using 'revision' attribute not '"
						+ RELEASE_VERSION_ATTRB + "'");
			}
			if (rev != null) {
				try {
					releaseVersion = Integer.parseInt(rev);
				} catch (NumberFormatException e) {
					LOG.warning("Bad release version for package " + pkg + ": "
							+ rev + " (defaulting to 0)");
				}
			}
			return pkg;
		}
		return null;
	}

	@Override
  public void start(String pkg, String na) {
		pkgName = pkg;
	}

	@Override
  public Entity makeEntity(String name, Attributes a) {
		// System.out.println("Making entity for "+name);
		return new Entity(name, a);
	}

	/**
	 * Called for 'top-level' entities within the package
	 */
	@Override
  public void notify(Entity e) {
		final String name = e.getName().toLowerCase();
		if (CLASS.equals(name)) {
			final String id = e.getAttribute(NAME_ATTRB);
			clazz = new ClassElement(false, id, Access.PUBLIC);

			handleNestedElements(clazz, e);
		} else {
			final String uid = e.getAttribute(UID_ATTRB);
			promises.add(new AnnotationElement(null, uid, name, e.getCData(), e
					.getAttributes()));
		}
	}

	private void handleNestedElement(ClassElement c, Entity n) {
		final String name = n.getName();
		final String id = n.getAttribute(NAME_ATTRB);
		// System.out.println("Looking at "+name+" -- "+id);

		if (METHOD.equals(name)) {
			c.addMember(handleNestedElements(new MethodElement(id, n), n));
		} else if (CLASS.equals(name)) {
			c.addMember(handleNestedElements(new NestedClassElement(false, id,
					Access.PUBLIC), n));
		} else if (CONSTRUCTOR.equals(name)) {
			c.addMember(handleNestedElements(new ConstructorElement(n), n));
		} else if (FIELD.endsWith(name)) {
			c.addMember(handleAnnotations(new FieldElement(false, id,
					Access.PUBLIC), n));
		} else if (CLASSINIT.equals(name)) {
			c.addMember(handleAnnotations(new ClassInitElement(), n));
		} else {
			handleAnnotationOnElt(c, n);
			return;
		}
	}

	private IClassMember handleNestedElements(ClassElement cl, Entity c) {
		for (Entity n : c.getReferences()) {
			handleNestedElement(cl, n);
		}

		if (cl instanceof NestedClassElement) {
			return (IClassMember) cl;
		}
		return null;
	}

	private IClassMember handleNestedElements(AbstractFunctionElement func,
			Entity f) {
		for (Entity n : f.getReferences()) {
			if (PARAMETER.equals(n.getName())) {
				final int i = Integer.parseInt(n.getAttribute(INDEX_ATTRB));
				FunctionParameterElement fe = new FunctionParameterElement(
						false, i);
				func.setParameter(handleAnnotations(fe, n));
			} else {
				handleAnnotationOnElt(func, n);
			}
		}
		// func.setLastComments(comments);
		return func;
	}

	private static void handleAnnotationOnElt(AnnotatedJavaElement func,
			Entity n) {
		final String uid = n.getAttribute(UID_ATTRB);
		AnnotationElement a = new AnnotationElement(func, uid, n.getName(),
				n.getCData(), n.getAttributes());
		func.addPromise(a);
	}

	private static <T extends AnnotatedJavaElement> T handleAnnotations(T e,
			Entity n) {
		for (Entity a : n.getReferences()) {
			handleAnnotationOnElt(e, a);
		}
		return e;
	}

	@Override
  public void done() {
		pkg = new PackageElement(true, pkgName, releaseVersion, clazz);
		for (AnnotationElement a : promises) {
			pkg.addPromise(a);
		}
		pkg.markAsClean();
	}

	public final PackageElement getPackage() {
		return pkg;
	}

	public static PackageElement loadRaw(File f) throws Exception {
		if (!f.isFile() || f.length() == 0) {
			return null;
		}
		InputStream is = new FileInputStream(f);
		return loadRaw(is);
	}

	public static PackageElement loadRaw(InputStream is) throws Exception {
		try {
			PromisesXMLReader r = new PromisesXMLReader();
			r.read(is);
			return r.getPackage();
		} finally {
			is.close();
		}
	}

	public interface Listener {
		void refresh(PackageElement e);

		void refreshAll();
	}

	private static final Collection<Listener> listeners = new CopyOnWriteArraySet<Listener>();

	public static void listenForRefresh(Listener l) {
		listeners.add(l);
	}

	public static void stopListening(Listener l) {
		listeners.remove(l);
	}

	public static void refresh(PackageElement e) {
		for (Listener v : listeners) {
			v.refresh(e);
		}
	}

	public static void refreshAll() {
		for (Listener v : listeners) {
			v.refreshAll();
		}
	}

	private static final Map<String, PackageElement> cache = new WeakHashMap<String, PackageElement>();

	public static PackageElement load(String relativePath, File fluid,
			File local) {
		// System.out.println("Getting XML for "+relativePath);
		PackageElement p = cache.get(relativePath);
		if (p == null) {
			PackageElement f = loadOrNull(fluid);
			if (f != null) {
				// Remember appropriate attributes to diff
				f.visit(new DiffHelper());
			}
			PackageElement l = loadOrNull(local);
			if (f == null) {
				p = l;
			} else if (l == null) {
				p = f;
			} else {
				l.mergeDeep(f, MergeType.JSURE_TO_LOCAL);
				p = l;
				p.markAsClean();
			}
			if (p == null) {
				return null;
			}
			// System.out.println("Loaded XML for "+relativePath);
			cache.put(relativePath, p);
		} else {
			// System.out.println("Used cache for "+relativePath);
		}
		return p;
	}

	private static PackageElement loadOrNull(File f) {
		if (f != null && f.isFile() && f.length() > 0) {
			try {
				return loadRaw(f);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static void clear(String relativePath) {
		cache.remove(relativePath);
	}

	public static PackageElement get(String path) {
		return cache.get(path);
	}
	
	// HACK!?!
	public static void cache(String path, PackageElement p) {
		if (p == null) {
			return;
		}
		if (!cache.containsKey(path)) {
			cache.put(path, p);
		}
	}
}
