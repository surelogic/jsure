package com.surelogic.jsecure.client.eclipse;

import java.io.*;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;

import com.surelogic.common.*;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.*;
import com.surelogic.common.jobs.remote.*;

import static com.surelogic.jsecure.client.eclipse.ClassSummarizer.*;

public class RemoteJSecureJob extends RemoteScanJob<JavaProjectSet<JavaProject>,JavaProject> {
	protected RemoteJSecureJob() {
		super(IJavaFactory.prototype);
	}
	
	protected SLJob finishInit(final File runDir, final JavaProjectSet<JavaProject> projects) throws Throwable {
		final JavaClassPath<JavaProjectSet<JavaProject>> classes = new JavaClassPath<JavaProjectSet<JavaProject>>(projects, true);		
		return new AbstractSLJob("Running JSecure on "+projects.getLabel()) {
			@Override
			public SLStatus run(final SLProgressMonitor monitor) {
				final ClassSummarizer summarizer = new ClassSummarizer(runDir);
				//ZipFile lastZip = null;
				try {
					int fromJars = 0;
					monitor.begin(classes.getMapKeys().size());					
					for(final Pair<String,String> key: classes.getMapKeys()) {
						System.out.println("Got key: "+key);
						monitor.worked(1);
						
						final IJavaFile info = classes.getMapping(key);
						if (info.getType() == IJavaFile.Type.CLASS_FOR_SRC) {
							// TODO what about the jars?
							summarizer.summarize(info.getStream());
						}
						else if (info.getType() != IJavaFile.Type.SOURCE) {
							if (key.first().startsWith(Config.JRE_NAME)) {
								// Skip classes only referenced from the JRE
								continue;
							}
							fromJars++;
							summarizer.summarize(info.getStream());
							// TODO eliminate duplicates between projects?
						}
					}
					summarizer.dump();
					System.out.println("Summarized from jars: "+fromJars);
				} catch(IOException e) {
					return SLStatus.createErrorStatus(e);
				} finally {
					summarizer.close();
				}
				return SLStatus.OK_STATUS;
			}
		};		
	}
	
	public static void main(String[] args) {
		RemoteJSecureJob job = new RemoteJSecureJob();
		job.run();
	}
}

class LocalJSecureJob extends AbstractLocalSLJob<ILocalConfig> {
	LocalJSecureJob(String name, int work, ILocalConfig config) {
		super(name, work, config);
	}

	@Override
	protected Class<? extends AbstractRemoteSLJob> getRemoteClass() {
		return RemoteJSecureJob.class;
	}

	@Override
	protected void setupClassPath(final ConfigHelper util, CommandlineJava cmdj,
			Project proj, Path path) {
        util.addPluginAndJarsToPath(COMMON_PLUGIN_ID, "lib/runtime");
        util.addPluginToPath("com.surelogic.jsure.core");
        util.addPluginAndJarsToPath("com.surelogic.jsecure.client.eclipse", "lib");
        // TODO anything else needed?
        for (File jar : util.getPath()) {
            addToPath(proj, path, jar, true);
        }
	}	
	
	/*
	@Override
	public SLStatus run(final SLProgressMonitor topMonitor) {
		final SLStatus s = super.run(topMonitor);
		final File runDir = new File(config.getRunDirectory());
		final ClassSummarizer summarizer = new ClassSummarizer(runDir);
		//summarizer.dump();
		//OK "start n=node(*) where has(n."+NODE_NAME+") return n, n."+NODE_NAME 
		//"START caller=node(*) MATCH caller-[r?:"+RelTypes.CALLS+"]->callee WHERE callee."+NODE_NAME+"! =~ '.*()V' return caller, callee" );
		summarizer.query("g.V.outE('"+RelTypes.CALLS+"')");		
		summarizer.close();
		return s;
	}
	*/
}
