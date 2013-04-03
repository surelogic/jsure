package com.surelogic.jsure.core;

import java.io.*;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;

import com.surelogic.common.*;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.*;
import com.surelogic.common.jobs.remote.*;

public class RemoteJSecureJob extends RemoteScanJob<JavaProjectSet<JavaProject>,JavaProject> {
	protected RemoteJSecureJob() {
		super(IJavaFactory.prototype);
	}
	
	protected SLJob finishInit(final File runDir, final JavaProjectSet<JavaProject> projects) throws Throwable {
		final JavaClassPath<JavaProjectSet<JavaProject>> classes = new JavaClassPath<JavaProjectSet<JavaProject>>(projects, true);
		return new AbstractSLJob("Running JSecure on "+projects.getLabel()) {
			@Override
			public SLStatus run(final SLProgressMonitor monitor) {
				final ClassSummarizer summarizer = new ClassSummarizer();
				//ZipFile lastZip = null;
				try {
					for(final Pair<String,String> key: classes.getMapKeys()) {
						final IJavaFile info = classes.getMapping(key);
						if (info.getType() == IJavaFile.Type.CLASS_FOR_SRC) {
							summarizer.summarize(info.getStream());
						}
					}	
				} catch(IOException e) {
					return SLStatus.createErrorStatus(e);
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
        // TODO anything else needed?
        for (File jar : util.getPath()) {
            addToPath(proj, path, jar, true);
        }
	}	
}
