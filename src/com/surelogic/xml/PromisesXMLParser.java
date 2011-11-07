package com.surelogic.xml;

import java.io.File;
import java.net.URISyntaxException;

import com.surelogic.annotation.parse.AnnotationVisitor;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.util.Pair;

/**
 * Does the same thing as TestXMLParser, but uses the Java XML model to apply the promises
 * 
 * @author Edwin
 */
public final class PromisesXMLParser {
	public static File getFluidXMLDir() {
		File fluidDir = null;
		try {
			fluidDir = new File(IDE.getInstance().getResourceRoot().toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Bad URL: "+IDE.getInstance().getResourceRoot());
		}
		final File fLibDir = new File(fluidDir, TestXMLParserConstants.PROMISES_XML_PATH);
		if (!fLibDir.isDirectory()) {
			throw new IllegalStateException("Couldn't find directory "+fLibDir);
		}
		return fLibDir;
	}
	
	public static File getLocalXMLDir() {
		File local = new File(IDE.getInstance().getStringPreference(IDEPreferences.JSURE_XML_DIRECTORY));
		if (!local.isDirectory()) {
			File data = new File(IDE.getInstance().getStringPreference(IDEPreferences.JSURE_DATA_DIRECTORY));
			local = new File(data, TestXMLParserConstants.LOCAL_XML_PATH);
			if (!local.isDirectory()) {
				return null;
			}
		}
		return local;
	}
	
	/**
	 * @return non-null Pair of files for fluid and local
	 */
	public static Pair<File,File> findPromisesXML(final String path) {
		File fluid = null; 
		File local = null;		
		File localXml = getFluidXMLDir();
		if (localXml != null) {
			File f = new File(localXml, path);
			if (f.isFile()) {
				local = f;
			}
		}
		// Try fluid
		final File xml = getFluidXMLDir();
		if (xml != null) {
			File f = new File(xml, path);
			if (f.isFile()) {
				fluid = f;
			}
		}
		return new Pair<File,File>(fluid, local);	
	}
	
	/**
	 * @param root
	 *            A CompilationUnit
	 * @param xml
	 *            The name of the promises.xml file to parse in
	 * @return The number of annotations added
	 */	
	public static int process(ITypeEnvironment tEnv, IRNode root, String xmlPath) {
		if (xmlPath.startsWith("java/lang/Object")) {
			System.out.println("Looking up "+xmlPath);
		}
		if (root == null) {
			return 0;
		}
		Pair<File,File> f = findPromisesXML(xmlPath);
		PackageElement p = PromisesXMLReader.load(xmlPath, f.first(), f.second());
		if (p == null) {
			return 0;
		}
		AnnotationVisitor v = new AnnotationVisitor(tEnv, "XML Parser for "+xmlPath);
		return p.applyPromises(v, root);
	}
}
