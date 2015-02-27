package com.surelogic.xml;

import java.io.*;

//import com.surelogic.annotation.rules.AnnotationRules;

/**
 * Only meant to be used initially to make sure we can handle all cases
 * 
 * @author Edwin
 */
@Deprecated
public class PromisesXMLRewriter {
	public static void main(String[] args) {
		final String path = "c:/work/workspace-3.5.1/fluid/"+TestXMLParserConstants.PROMISES_XML_REL_PATH;
		//AnnotationRules.initialize();
		rewrite(new File(path));
	}

	private static void rewrite(File f) {
		if (f.isDirectory()) {
			for(File c : f.listFiles()) {
				rewrite(c);
			}
		}
		else if (f.getName().endsWith(TestXMLParserConstants.SUFFIX)) {
			try {
				InputStream is = new FileInputStream(f);
				PromisesXMLReader r = new PromisesXMLReader();
				r.read(is);
				is.close();
				
				PromisesXMLWriter w = new PromisesXMLWriter(f);
				w.write(r.getPackage());
			} catch (Exception e) {
				System.err.println("While rewriting "+f);
				e.printStackTrace();
			}
		
		}		
	}
}
