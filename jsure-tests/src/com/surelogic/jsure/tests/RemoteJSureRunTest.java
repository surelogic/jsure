package com.surelogic.jsure.tests;

import java.io.File;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.javac.Javac;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.test.xml.JUnitXMLOutput;

public class RemoteJSureRunTest extends RemoteJSureRun {
	public static void main(String[] args) {
		RemoteJSureRunTest job = new RemoteJSureRunTest();
		job.run();
	}
	
	@Override
	protected void init() {
		out.println("Setting up XML output");
		Javac.getDefault().addTestOutputFactory(JUnitXMLOutput.factory);
		//System.out.println("Added JUnitXMLOutput.factory");
		super.init();
	}
	
	@Override
	protected SLJob finishInit(final File runDir) throws Throwable {
		final SLJob job = super.finishInit(runDir);
		return new AbstractSLJob(job.getName()) {
			@Override
			public SLStatus run(SLProgressMonitor monitor) {
				try {
					return job.run(monitor);
				} finally {
					cleanup();
				}
			}				
		};
	}
	
	@Override
	protected void cleanup() {
		out.println("Closing AnnotationRules log");
		AnnotationRules.XML_LOG.close();
	}
}
