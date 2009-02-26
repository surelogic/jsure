package edu.cmu.cs.fluid.dc;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.Path;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.Activator;

/**
 * The save participant class for double-checker.
 * 
 * @see ISaveParticipant
 */
public final class SaveParticipant implements ISaveParticipant {

	private static final Logger LOG = SLLogger.getLogger();

	public void doneSaving(ISaveContext context) {
		Activator activator = Activator.getDefault();
		if (activator != null) {
			// delete the old saved state since it is not necessary anymore
			int previousSaveNumber = context.getPreviousSaveNumber();
			String oldFileName = "save-" + Integer.toString(previousSaveNumber);
			File f = activator.getStateLocation().append(oldFileName).toFile();
			f.delete();
		}
	}

	public void prepareToSave(ISaveContext context) {
		// We don't need to do any preparation
	}

	public void rollback(ISaveContext context) {
		Activator activator = Activator.getDefault();
		/*
		 * Because the save operation has failed, delete the saved state we have
		 * just written
		 */
		int saveNumber = context.getSaveNumber();
		String saveFileName = "save-" + Integer.toString(saveNumber);
		File f = activator.getStateLocation().append(saveFileName).toFile();
		f.delete();
	}

	public void saving(ISaveContext context) {
		context.needDelta(); // request resource delta on next activation
		switch (context.getKind()) {
		case ISaveContext.FULL_SAVE:
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("performing ISaveContext.FULL_SAVE");
			}
			Activator myPluginInstance = Activator.getDefault();
			// save the plug-in state
			int saveNumber = context.getSaveNumber();
			String saveFileName = "save-" + Integer.toString(saveNumber);
			File f = myPluginInstance.getStateLocation().append(saveFileName)
					.toFile();
			/*
			 * if we fail to write, an exception is thrown and we do not update
			 * the path
			 */
			myPluginInstance.getDoubleChecker().writeStateTo(f);
			context.map(new Path("save"), new Path(saveFileName));
			context.needSaveNumber();
			LOG.info("double-checker saved analysis state to XML file "
					+ saveFileName);
			break;
		case ISaveContext.PROJECT_SAVE:
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("performing ISaveContext.PROJECT_SAVE");
			}
			// get the project related to this save operation
			IProject project = context.getProject();
			// save its information, if necessary
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("saving project " + project.getName());
			}
			break;
		case ISaveContext.SNAPSHOT:
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("performing ISaveContext.SNAPSHOT");
			}
			/*
			 * This operation needs to be really fast because snapshots can be
			 * requested frequently by the workspace...currently we don't do
			 * anything so we are really-really fast :-)
			 */
			break;
		}
	}
}
