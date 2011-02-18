package com.surelogic.jsure.tests;

import java.io.BufferedReader;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.fluid.javac.Javac;
import com.surelogic.fluid.javac.jobs.RemoteJSureRun;
import com.surelogic.test.xml.JUnitXMLOutput;

public class RemoteJSureRunTest extends RemoteJSureRun {
	public static void main(String[] args) {
		RemoteJSureRunTest job = new RemoteJSureRunTest();
		job.run();
	}
	
	@Override
	protected SLJob init(BufferedReader br, Monitor mon) throws Throwable {
		out.println("Setting up XML output");
		Javac.getDefault().addTestOutputFactory(JUnitXMLOutput.factory);
		//System.out.println("Added JUnitXMLOutput.factory");

		final SLJob job = super.init(br, mon);
		return new AbstractSLJob(job.getName()) {
			@Override
			public SLStatus run(SLProgressMonitor monitor) {
				try {
					return job.run(monitor);
				} finally {
					out.println("Closing AnnotationRules log");
					AnnotationRules.XML_LOG.close();
				}
			}				
		};
	}
}
