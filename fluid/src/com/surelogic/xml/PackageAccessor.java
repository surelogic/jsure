/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/PackageAccessor.java,v 1.5 2007/09/17 19:00:06 chance Exp $*/
package com.surelogic.xml;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xml.sax.InputSource;

import com.surelogic.common.FileUtility;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.util.*;

public class PackageAccessor implements TestXMLParserConstants {

	public static final String XML_EXT = "xml";
	public static final String ZIP_EXT = "zx";

	public static String packagePath(String pkgName) {
		return packagePath(pkgName, true);
	}

	public static String packagePath(String pkgName, boolean systemSpecific) {
		/** Convert the package name into a directory path */
		String[] path = pkgName.split("\\.");
		StringBuffer dirName = new StringBuffer();

		for (int i = 0; i < path.length; i++) {
			dirName.append(path[i]);
			dirName.append(systemSpecific ? File.separator : '/');
		}
		return dirName.toString();
	}

	public static String promiseFileName(String classPath) {
		String[] path = classPath.split("\\.");

		// TODO this only works for .promise.xml
		return path[path.length - 3] + "." + path[path.length - 2] + "."
				+ path[path.length - 1];
	}

	public static String packageClassName(File f) throws FileNotFoundException {
		if (f == null)
			throw new FileNotFoundException();
		return f.getName().split("\\.")[0];
	}

	public static String packageExtension(File f) {
		String[] fname = f.getName().split("\\.");

		/**
		 * Promise files are expected to be in the format: classname.promise.xml
		 * or classname.promise.zx for zipped xml
		 */
		if (fname.length < 3)
			return null;

		if (fname[fname.length - 2].equalsIgnoreCase("promises")) {
			if (fname[fname.length - 1].equalsIgnoreCase(XML_EXT))
				return XML_EXT;
			else if (fname[fname.length - 1].equalsIgnoreCase(ZIP_EXT))
				return ZIP_EXT;
		}
		return null;
	}

	public static InputSource readFile(File f) throws IOException {
		if (f == null) {
			throw new FileNotFoundException("File doesn't exist");
		}
		String ext = packageExtension(f);

		if (ext == null) {
			throw new FileNotFoundException("Extension not understood");
		} else if (ext.equalsIgnoreCase(XML_EXT)) {
			return new InputSource(new FileReader(f));
		} else if (ext.equalsIgnoreCase(ZIP_EXT)) {
			// ZipFileLocator z = new ZipFileLocator(f,ZipFileLocator.READ);
			// return new InputSource(z.openFileRead(packageClassName(f)));
			System.out.println("Trying to open zip file " + f.getPath());
			ZipFile z = new ZipFile(f, ZipFile.OPEN_READ);
			ZipEntry ze = z.getEntry(packageClassName(f));
			return new InputSource(
					new BufferedInputStream(z.getInputStream(ze)));
			// return new InputSource(new ZipInputStream(new
			// FileInputStream(f)));
		}

		throw new FileNotFoundException();
	}

	public static OutputStreamWriter writePackage(String pkgName,
			String className) throws IOException {
		String dirName = packagePath(pkgName);

		DirectoryFileLocator dir = new DirectoryFileLocator();
		/**
		 * NOTE: this creates the directory structure in the working directory.
		 */
		dir.setAndCreateDirPath(dirName);
		return new OutputStreamWriter(dir.openFileWrite(className
				+ PROMISES_XML));
	}

	/**
	 * @param className
	 *            needs to have the .promises.xml extension
	 */
	public static InputSource readPackage(String pkgName, String className)
			throws IOException {
		final File root = PromisesXMLParser.getFluidXMLDir();

		final String dirName = packagePath(pkgName, false);

		final InputStream is = new FileInputStream(new File(root, dirName
				+ className));

		return new InputSource(is);
	}

	public static OutputStreamWriter writeZipPackage(String pkgName,
			String className) throws IOException {
		String dirName = packagePath(pkgName);

		DirectoryFileLocator dir = new DirectoryFileLocator(DIR_PREFIX
				+ dirName);
		dir.setAndCreateDirPath(dirName);
		File file = dir.locateFile(className + ".promise.zx", false);
		if (file == null) {
			throw new FileNotFoundException(className + " doesn't exist");
		}

		FileLocator z = new ZipFileLocator(file, ZipFileLocator.WRITE);

		return new OutputStreamWriter(z.openFileWrite(className));
	}

	public static InputSource readZipPackage(String pkgName, String className)
			throws IOException {
		String dirName = packagePath(pkgName);

		DirectoryFileLocator dir = new DirectoryFileLocator(DIR_PREFIX
				+ dirName);
		File file = dir.locateFile(className, true);
		if (file == null) {
			throw new FileNotFoundException(className + " doesn't exist");
		}
		ZipFileLocator z = new ZipFileLocator(file, ZipFileLocator.READ);

		return new InputSource(z.openFileRead(className));
	}

	public static final String PROMISES_XML = TestXMLParserConstants.SUFFIX;

	public static Iterable<String> findPromiseXMLs() {
		try {
			final String localPath = IDE.getInstance().getStringPreference(
					IDEPreferences.JSURE_XML_DIFF_DIRECTORY);
			final File local = new File(localPath);

			final URI uri = IDE.getInstance().getResourceRoot().toURI();
			final File root = new File(uri);
			if (root.exists() && root.isDirectory()) {
				final boolean localExists = local.isDirectory();
				XmlCollector qnames = findPromiseXMLsInDir(new File(root,
						FileUtility.JSURE_XML_DIFF_PATH_FRAGMENT), localExists);
				if (localExists) {
					findPromiseXMLsInDir(qnames, local);
				}
				return qnames.results;
			} else if (local.isDirectory()) {
				return findPromiseXMLsInDir(local, false).results;
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new EmptyIterator<String>();
	}

	private static XmlCollector findPromiseXMLsInDir(File dir,
			boolean makeUnique) {
		XmlCollector c = new XmlCollector(makeUnique);
		findPromiseXMLsInDir(c, dir);
		return c;
	}

	public static void findPromiseXMLsInDir(IXmlProcessor qnames, File dir) {
		findPromiseXMLsInDir(qnames, dir, "");
	}

	private static void findPromiseXMLsInDir(IXmlProcessor qnames, File dir,
			String path) {
		if (dir == null || !dir.isDirectory()) {
			return;
		}
		for (File xml : dir.listFiles(XML_FILTER)) {
			findPromiseXMLs(qnames, xml, path);
		}
	}

	private static void findPromiseXMLs(IXmlProcessor qnames, File f,
			String path) {
		if (!f.exists()) {
			return;
		}
		if (f.isDirectory()) {
			findPromiseXMLsInDir(qnames, f, computeName(path, f.getName()));
		} else if (f.getName().endsWith(PROMISES_XML) && f.length() > 0) {
			if (PACKAGE_PROMISES.equals(f.getName())) {
				qnames.addPackage(path);
			} else {
				String name = f.getName().substring(0,
						f.getName().length() - PROMISES_XML.length());
				qnames.addType(path, name);
			}
		}
	}

	static String computeName(String path, String name) {
		if (path.length() == 0) {
			return name;
		} else {
			return path + '.' + name;
		}
	}

	static class XmlCollector implements IXmlProcessor {
		final Collection<String> results;

		XmlCollector(boolean makeUnique) {
			results = makeUnique ? new HashSet<String>()
					: new ArrayList<String>();
		}

		@Override
    public void addPackage(String qname) {
			results.add(qname);
		}

		@Override
    public void addType(String pkg, String name) {
			results.add(computeName(pkg, name));
		}
	}
}