package edu.cmu.cs.fluid.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.surelogic.common.SLUtility;

/**
 * @author Aaron Greenhouse
 */
public class AaronsFormatter extends Formatter {

	@Override
	public synchronized String format(final LogRecord record) {
		final StringBuilder sb = new StringBuilder();

		final String logger = record.getLoggerName();
		final String level = record.getLevel().toString();
		final String msg = record.getMessage();
		final Throwable thrown = record.getThrown();

		sb.append('[');
		final int idx = logger.lastIndexOf('.');
		sb.append(logger.substring(idx + 1));
		sb.append(',');
		sb.append(level);
		sb.append("] ");
		sb.append(msg);
		sb.append(SLUtility.PLATFORM_LINE_SEPARATOR);

		if (thrown != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
				// Fuggitabotit
			}
		}
		return sb.toString();
	}
}
