/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/parse/ParserTest.java,v 1.1 2008/07/09 19:34:47 chance Exp $*/
package edu.cmu.cs.fluid.java.parse;

import java.io.*;
import java.util.*;

import javax.swing.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.parse.ParseException;

public class ParserTest {
    public static void main(String... args) {
    	File dir = new File("C:/work/workspace/common/src/com/surelogic/common");
    	//File dir = new File("C:/work/workspace/fluid/src");
    	List<File> files = new ArrayList<File>();    	
    	addJavaFiles(dir, files);
    	try {
			openFiles(files);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
   }

	private static void addJavaFiles(File dir, List<File> files) {
		System.out.println("Scanning "+dir.getAbsolutePath());
		for(File f : dir.listFiles()) {
			if (f.getName().endsWith(".java")) {
				//System.out.println("Found "+f.getName());
				files.add(f);
			}
			if (f.isDirectory()) {
				addJavaFiles(f, files);
			}
    	}
	}
    
    private static void browseAndOpen() {
    	JFrame frame = new JFrame();
    	JFileChooser jfc = new JFileChooser();    	
    	jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    	if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
    		try {
				openFiles(Collections.singleton(jfc.getSelectedFile()));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
		    	System.exit(0);
			}
    	}
    }

	private static void openFiles(Iterable<File> files) {	
		/*
		ITypeEnvironment tEnv    = null;
		IBinder binder           = new UnversionedJavaBinder(tEnv);
		JavaCanonicalizer jcanon = new JavaCanonicalizer(binder);
		*/
		JavaParser parser = null;
		for(File f : files) {
			try {
				InputStream is = new BufferedInputStream(new FileInputStream(f));
				if (parser == null) {
					parser = new JavaParser(is);
				} else {
					JavaParser.ReInit(is);
				}
				System.out.println("Parsing file " + f.getName());
				IRNode root = JavaParser.Start();
				JJNode.tree.clearParent(root);			
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
