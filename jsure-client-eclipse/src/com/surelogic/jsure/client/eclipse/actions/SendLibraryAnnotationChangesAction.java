package com.surelogic.jsure.client.eclipse.actions;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;

import com.surelogic.common.*;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.serviceability.*;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import static com.surelogic.xml.TestXMLParserConstants.*;

public class SendLibraryAnnotationChangesAction extends AbstractMainAction {
	public void run(IAction action) {
		File temp = null;
		Archiver archive = null;
		try {
			temp = File.createTempFile("textArchive", ".txt");
			archive = new Archiver(temp);
			FileUtility.recursiveIterate(archive,
					JSurePreferencesUtility.getJSureXMLDirectory());
		} catch (Exception e) {
			SLLogger.getLogger().log(
					Level.WARNING,
					"Failure writing textArchive.txt temp file of annotation changes: "
							+ temp, e);
		} finally {
			if (archive != null) {
				archive.close();
			}
		}
		if (archive != null) {
			if (archive.isEmpty()) {
				MessageDialog
						.openInformation(EclipseUIUtility.getShell(),
								"No Library Annotation Changes",
								"There are no library annotation changes to send to SureLogic");
				return;
			}
			MessageWithArchive message = new MessageWithArchive(temp);
			message.setSummary("Proposed library annotation changes");
			message.setDescription(archive.getDescription());

			SendServiceMessageWizard.openTip(null, SLLicenseProduct.JSURE + " "
					+ EclipseUtility.getVersion(Activator.getDefault()),
					CommonImages.IMG_JSURE_LOGO, message);
		}
	}

	private static class Archiver extends TextArchiver {
		/**
		 * Qualified names of packages or types that changed
		 */
		private final Set<String> changed = new TreeSet<String>();

		public Archiver(File target) throws IOException {
			super(target);
		}

		@Override
		public boolean accept(File pathname) {
			return XML_FILTER.accept(pathname);
		}

		@Override
		protected void iterate(String relativePath, File f) {
			super.iterate(relativePath, f);

			final String pkg;
			if (relativePath.indexOf('/') >= 0
					&& relativePath.endsWith(f.getName())) {
				pkg = relativePath.substring(0,
						relativePath.length() - f.getName().length() - 1)
						.replace('/', '.');
			} else {
				pkg = relativePath;
			}
			if (PACKAGE_PROMISES.equals(f.getName())) {
				changed.add(pkg);
			} else {
				final String qname = pkg+'.'+f.getName().substring(0, f.getName().length() - SUFFIX.length());
				changed.add(qname);
			}
		}

		boolean isEmpty() {
			return changed.isEmpty();
		}

		String getDescription() {
			StringBuilder sb = new StringBuilder(
					"Archive of library annotation changes for the following packages:\n");
			for (String qname : changed) {
				sb.append('\t').append(qname).append('\n');
			}
			return sb.toString();
		}
	}
}
