package com.surelogic.jsure.core;

import java.io.*;
import java.util.zip.*;

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
			public SLStatus run(SLProgressMonitor monitor) {
				ClassSummarizer summarizer = new ClassSummarizer();
				ZipFile lastZip = null;
				try {
					for(Pair<String,String> key: classes.getMapKeys()) {
						Pair<String,Object> info = classes.getMapping(key);
						// How to distinguish libraries?
						if (info.second() instanceof String) {
							final String jarPath = (String) info.second();
							ZipFile jar = new ZipFile(jarPath);
							summarizer.summarize(jar, key.second());
						}
					}	
				} catch(IOException e) {
					return SLStatus.createErrorStatus(e);
				}
				return null;
			}
		};		
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
        // TODO anything else needed?
        for (File jar : util.getPath()) {
            addToPath(proj, path, jar, true);
        }
	}	
}
