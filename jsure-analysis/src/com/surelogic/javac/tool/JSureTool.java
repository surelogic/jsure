package com.surelogic.javac.tool;

/*
import java.io.File;
import java.net.URI;

import com.surelogic.common.jobs.*;
import com.surelogic.fluid.javac.Util;
import com.surelogic.fluid.javac.Config;

import com.surelogic.sierra.tool.*;
import com.surelogic.sierra.tool.analyzer.ILazyArtifactGenerator;
import com.surelogic.sierra.tool.targets.FileTarget;
import com.surelogic.sierra.tool.targets.IToolTarget;
*/

public class JSureTool /*extends AbstractToolInstance*/ {
/*
	public JSureTool(JSureFactory f, com.surelogic.sierra.tool.message.Config config, 
			         ILazyArtifactGenerator generator, boolean close) {
		super(f, config, generator, close);
	}

	@Override
	protected void execute(SLProgressMonitor monitor) throws Exception {      
		//monitor.beginTask("JSure setup", SLProgressMonitor.UNKNOWN);
		Config c = initTargets();
		Util.openFiles(c, monitor);
	}

	private Config initTargets() {
		final Config c = new Config(config.getProject(), false);
		for(IToolTarget t : getSrcTargets()) {
			final File path = new File(t.getLocation()); 
			switch (t.getKind()) {
			case DIRECTORY:
				Util.addJavaFiles(path, c);        
				break;
			case JAR:
			case FILE:
				if (path.getName().endsWith(".java")) {
					// System.out.println("Ignored: "+path);
					FileTarget ft = (FileTarget) t;
					URI root = ft.getRoot();
					//System.out.println(path+" : "+root);
					String rootPath = new File(root).getAbsolutePath();
					String filePath = path.getParent();
					if (filePath.startsWith(rootPath)) {
						String rest = filePath.substring(rootPath.length()+1, filePath.length());
						String pkg  = rest.replace(File.separatorChar, '.');
						c.addPackage(pkg);
					} else {
						throw new IllegalArgumentException("Root "+rootPath+" doesn't match file: "+filePath);
					}
				} else {
					System.out.println("Ignoring non-Java src target "+t.getLocation()); 
				}
				break;
			default:
				System.out.println("Ignoring src target "+t.getLocation());            
			}
		}

		for(IToolTarget t : getAuxTargets()) {
			final File   file = new File(t.getLocation());
			final String path = file.getAbsolutePath(); 
			switch (t.getKind()) {
			case DIRECTORY:
				// TODO is this right?
				Util.addJavaFiles(file, c, "UNKNOWN"); 
				break;
			case JAR:
				c.addJar(path);
				break;
			case FILE:
				System.out.println("JSure ignored AUX file: "+path);
				break;
			default:
				System.out.println("Ignoring aux target "+t.getLocation());              
			}
		}
		return c;
	}
*/
}
