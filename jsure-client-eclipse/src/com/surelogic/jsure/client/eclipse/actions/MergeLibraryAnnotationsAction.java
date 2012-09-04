package com.surelogic.jsure.client.eclipse.actions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.surelogic.common.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class MergeLibraryAnnotationsAction extends AbstractMainAction {
	//private static final FileUtility.TempFileFilter filter = 
	//	new FileUtility.TempFileFilter("textArchiver", "destDir");
	
	@Override
	public void run(IAction action) {
		// choose a text file
		FileDialog d = new FileDialog(EclipseUIUtility.getShell(), SWT.OPEN);
		String path = d.open();
		if (path != null) {
			File srcArchive = new File(path);
			if (srcArchive.exists()) {
				System.out.println("Length of "+srcArchive+" : "+srcArchive.length());
			} else {
				System.out.println("Can't find "+srcArchive);
			}
			// Make a temp directory
			try {				
				File destDir = JSurePreferencesUtility.getJSureXMLDirectory();//filter.createTempFolder();
				List<String> warnings = TextArchiver.unarchive(srcArchive, destDir);
				System.out.println("Unarchived to "+destDir);
				if (!warnings.isEmpty()) {		
					StringBuilder sb = new StringBuilder();
					for(String s : warnings) {
						sb.append(s).append('\n');
					}
					MessageDialog.openError(EclipseUIUtility.getShell(), "Warnings from merging library annotations", 
							sb.toString());
				}
			} catch (IOException e) {
				MessageDialog.openError(EclipseUIUtility.getShell(), "Error while merging library annotations", 
						e.getMessage());
				SLLogger.getLogger().log(Level.WARNING, "Error while merging library annotations", e);
			}

		}
	}
}
