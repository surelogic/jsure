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
					for(final Pair<String,String> key: classes.getMapKeys()) {
						final IJavaFile info = classes.getMapping(key);
						if (info.getType() == IJavaFile.Type.CLASS_FOR_SRC) {
							summarizer.summarize(info.getStream());
						}
					}
					summarizer.dump();
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
	
	@Override
	public SLStatus run(final SLProgressMonitor topMonitor) {
		final SLStatus s = super.run(topMonitor);
		final File runDir = new File(config.getRunDirectory());
		final ClassSummarizer summarizer = new ClassSummarizer(runDir);
		//summarizer.dump();
		// TODO query for ()V methods
		//OK "start n=node(*) where has(n."+NODE_NAME+") return n, n."+NODE_NAME 
		summarizer.query("start caller=node(*) MATCH caller-[r?:"+RelTypes.CALLS+"]->callee return caller, callee" );
		
		summarizer.close();
		return s;
	}
}
