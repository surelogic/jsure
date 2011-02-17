package com.surelogic.jsure.tests;

import java.io.BufferedReader;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.fluid.javac.jobs.RemoteJSureRun;
import com.surelogic.jsure.core.Eclipse;
import com.surelogic.jsure.core.driver.JavacEclipse;
import com.surelogic.test.xml.JUnitXMLOutput;

public class RemoteJSureRunTest extends RemoteJSureRun {
	@Override
	protected SLJob init(BufferedReader br, Monitor mon) throws Throwable {
		try {
			System.out.println("Running "+getClass().getSimpleName());
			Eclipse.getDefault().addTestOutputFactory(JUnitXMLOutput.factory);
			JavacEclipse.getDefault().addTestOutputFactory(
					JUnitXMLOutput.factory);
			System.out.println("Added JUnitXMLOutput.factory");
			return super.init(br, mon);
		} finally {
			AnnotationRules.XML_LOG.close();
		}
	}
}
