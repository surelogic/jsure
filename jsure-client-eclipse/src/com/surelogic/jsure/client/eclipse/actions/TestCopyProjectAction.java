package com.surelogic.jsure.client.eclipse.actions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.*;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.SourceZip;
import com.surelogic.common.ui.actions.AbstractSingleProjectAction;

public class TestCopyProjectAction extends AbstractSingleProjectAction {
	public void run(IAction action) {
		final long start = System.currentTimeMillis();
		final SourceZip srcZip = new SourceZip(project);
		File zipFile = null;
		File targetDir = null;
		try {
			zipFile = File.createTempFile("testCopyProject", ".zip");
			srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
			
			targetDir = new File(zipFile.getParentFile(), zipFile.getName()+".dir");
			targetDir.mkdir();
			ZipFile zf = new ZipFile(zipFile);
			Enumeration<? extends ZipEntry> e = zf.entries();
			while (e.hasMoreElements()) {
				ZipEntry ze = e.nextElement();
				File f = new File(targetDir, ze.getName());
				f.getParentFile().mkdirs();
				FileUtility.copy(ze.getName(), zf.getInputStream(ze), f);
			}
			zf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		final long end = System.currentTimeMillis();
		System.out.println("Copy took "+(end-start)+" ms");
		if (zipFile != null) {
			zipFile.delete();
		}
		if (targetDir != null) {
			FileUtility.recursiveDelete(targetDir);
		}
	}
}
