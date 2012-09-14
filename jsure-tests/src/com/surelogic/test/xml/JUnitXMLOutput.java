/*$Header: /cvs/fluid/com.surelogic.jsure.tests/src/com/surelogic/test/xml/JUnitXMLOutput.java,v 1.12 2007/11/15 20:40:02 chance Exp $*/
package com.surelogic.test.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import com.surelogic.ant.junit.SLTest;
import com.surelogic.ant.junit.XMLJUnitResultFormatter;
import com.surelogic.test.AbstractTestOutput;
import com.surelogic.test.ITest;
import com.surelogic.test.ITestOutput;
import com.surelogic.test.ITestOutputFactory;

public class JUnitXMLOutput extends AbstractTestOutput {
	private final OutputStream out;
	private final XMLJUnitResultFormatter formatter;
	private final JUnitTest test;
	private final long startTime;
	private int runs, failures, errors;

	public JUnitXMLOutput(String name, OutputStream out) {
		super(name);
		if (name == null || out == null) {
			throw new IllegalArgumentException(name + ", " + out);
		}
		this.out = out;
		test = new JUnitTest(name);
		formatter = new XMLJUnitResultFormatter();
		formatter.setOutput(out);
		formatter.startTestSuite(test);
		test.setProperties(System.getProperties());

		startTime = System.currentTimeMillis();
		System.out.println("Creating JUnitXMLOutput: " + name);
	}

	private static abstract class Test implements SLTest {
		final Throwable t;
		final String msg;
		final String className;

		Test(ITest o, String msg, Throwable t) {
			this.className = o.getClassName();
			this.msg = msg;
			this.t = t;
		}
	    @Override
		public int countTestCases() {
			return 1;
		}
	    @Override
		public void run(TestResult result) {
			if (t == null) {
				return;
			}
			if (t instanceof Error) {
				throw (Error) t;
			}
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			throw new Error(t);
		}
	    @Override
		public String getTestName() {
			return msg;
		}
	    @Override
		public String getClassName() {
			return className; // this.getClass().getSimpleName();
		}
	}

	private static class Success extends Test {
		Success(ITest o, String msg) {
			super(o, msg, null);
		}
	}

	private static class Failure extends Test {
		Failure(ITest o, String msg, AssertionFailedError e) {
			super(o, msg, e);
		}
	}

	private static class Exception extends Test {
		Exception(ITest o, String msg, Throwable e) {
			super(o, msg, e);
		}
	}

	/*
	 * @Override public ITest reportStart(ITest o) {
	 * System.out.println("Started:  "+o); return super.reportStart(o); }
	 */
    @Override
	public void reportSuccess(ITest o, String msg) {
		// System.out.println("Reported success: "+o+" -- "+msg);
		if (report(o, msg)) {
			Test t = new Success(o, msg);
			formatter.startTest(t);
			formatter.endTest(t);
			runs++;
		}
	}
    @Override
	public void reportFailure(ITest o, String msg) {
		// System.out.println("Reported failure: "+o+" -- "+msg);
		if (report(o, msg)) {
			AssertionFailedError err = new AssertionFailedError(msg);
			Test t = new Failure(o, msg, err);
			formatter.startTest(t);
			formatter.addFailure(t, err);
			failures++;
		}
	}
    @Override
	public void reportError(ITest o, Throwable err) {
		// System.out.println("Reported error:   "+o+" -- "+err.getClass().getSimpleName()+" : "+err.getMessage());
		if (report(o, err)) {
			Test t = new Exception(o, err.getMessage(), err);
			formatter.startTest(t);
			formatter.addFailure(t, err);
			errors++;
		}
	}

	@Override
	public void close() {
		super.close();
		runs = runs + failures + errors;

		test.setRunTime(System.currentTimeMillis() - startTime);
		test.setCounts(runs, failures, errors);
		formatter.endTestSuite(test);
		try {
			out.close();
			System.out.println("Closed JUnitXMLOutput " + test.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static final ITestOutputFactory factory = new ITestOutputFactory() {
		@Override
		public ITestOutput create(String name) {
			try {
				/*
				final IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace()
						.getRoot();
				final File wsFile = new File(wsRoot.getLocationURI());
				*/
				final String logFileName = name + ".log.Tests.xml";
				//final File logFile = new File(wsFile, logFileName);
				final File logFile = new File(logFileName);
				return new JUnitXMLOutput(name, new FileOutputStream(logFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		}
	};
}
