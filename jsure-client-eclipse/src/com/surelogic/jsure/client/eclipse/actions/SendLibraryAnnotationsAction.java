package com.surelogic.jsure.client.eclipse.actions;

import java.io.*;
import java.util.*;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;

import com.surelogic.common.*;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.serviceability.*;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.xml.TestXMLParserConstants;

public class SendLibraryAnnotationsAction extends AbstractMainAction {
	public void run(IAction action) {
		File temp = null;
		Archiver archive = null;
		try {
			temp = File.createTempFile("textArchive", ".txt");
			archive = new Archiver(temp);
			FileUtility.recursiveIterate(archive, JSurePreferencesUtility.getJSureXMLDirectory());
		} catch(IOException e) {
			if (archive != null) {
				archive.close();
			}
		}
		if (archive != null) {
			if (archive.isEmpty()) {
				MessageDialog.openInformation(EclipseUIUtility.getShell(), "No Library Annotations", 
						"There are no library annotations to send");
				return;
			}
			MessageWithArchive message = new MessageWithArchive(temp);
			message.setSummary("Modified library annotations");
			message.setDescription(archive.getDescription());

			try {
			SendServiceMessageWizard.openTip(null, SLLicenseProduct.JSURE + " "
					+ EclipseUtility.getVersion(Activator.getDefault()),
					CommonImages.IMG_JSURE_LOGO, message);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private static class Archiver extends TextArchiver {
		private final Set<String> packages = new TreeSet<String>();
		
		public Archiver(File target) throws IOException {
			super(target);
		}
		
		@Override
		public boolean accept(File pathname) {
			return TestXMLParserConstants.XML_FILTER.accept(pathname);
		}
		
		@Override
		protected void iterate(String relativePath, File f) {
			super.iterate(relativePath, f);
			
			String pkg;
			if (relativePath.indexOf('/') >= 0 && relativePath.endsWith(f.getName())) {								
				pkg = relativePath.substring(0, relativePath.length() - f.getName().length() - 1).replace('/', '.');
			} else {
				pkg = relativePath;
			}			
			packages.add(pkg);
		}
		
		boolean isEmpty() {
			return packages.isEmpty();
		}
		
		String getDescription() {
			StringBuilder sb = new StringBuilder("Archive of library annotations for the following packages:\n");
			for(String pkg : packages) {
				sb.append(pkg).append('\n');
			}
			return sb.toString();
		}
	}
}
