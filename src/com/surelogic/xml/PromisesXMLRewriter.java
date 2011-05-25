package com.surelogic.xml;

import java.io.*;

public class PromisesXMLRewriter {
	public static void main(String[] args) {
		final String path = "c:/work/workspace-3.5.1/fluid/lib/promises";
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
