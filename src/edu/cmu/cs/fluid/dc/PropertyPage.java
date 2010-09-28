package edu.cmu.cs.fluid.dc;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.analysis.JavacDriver;
import com.surelogic.jsure.client.eclipse.analysis.ScriptCommands;
import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

/**
 * Java project property page to enable and set preferences for double-checking.
 * This property page is attached, via the <code>plugin.xml</code> file to
 * <code>IJavaProject</code> and <code>IProject</code> resources. The
 * property page uses the existence of the double-checking nature on a project
 * as a persistent indication to show the state in a check box on the dialog.
 */
public class PropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

  private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

  /**
   * check box button that indicating if double-checking state (i.e., on or off)
   * for a Java project.
   */
  private Button m_onOff;

  /**
   * What was the initial state of the focus button?
   */
  private boolean focusButtonInitialState = false;
  
  
  
  /**
   * Checks if a project currently uses double-checking assurance. This is
   * determined by seeing if the double-checker nature is attached to the
   * project.
   * 
   * @return <code>true</code> if the project uses double-checking,
   *         <code>false</code> otherwise.
   */
  private boolean doesProjectUseDoubleChecking() {
    boolean result = false; // assume double-checking is off
    IAdaptable element = getElement();
    IProject project = null;
    if (element instanceof IJavaProject) {
      IJavaProject javaProject = (IJavaProject) element;
      project = javaProject.getProject();
    } else if (element instanceof IProject) {
      project = (IProject) element;
    } else {
      // we should never get here (our plugin.xml must be messed up)
      throw new IllegalStateException("unable to obtain project information");
    }
    try {
      result = project.hasNature(Nature.DOUBLE_CHECKER_NATURE_ID);
    } catch (CoreException e) {
      LOG.log(Level.SEVERE,
          "failure checking if double-checking nature is on Java project "
              + project.getName(), e);
    }
    return result;
  }

  /**
   * Sets the project to use or not use double-checking assurance based upon the
   * <code>value</code>.
   * 
   * <p>Assumes that it is only called if the state needs to be changed.
   * 
   * @param value
   *          <code>true</code> if the project should be double-checked,
   *          <code>false</code> otherwise.
   */
  private void setProjectDoubleCheckingStatus(final boolean newValue) {
    IAdaptable element = getElement();
    IProject project = null;
    if (element instanceof IJavaProject) {
      IJavaProject javaProject = (IJavaProject) element;
      project = javaProject.getProject();
    } else if (element instanceof IProject) {
      project = (IProject) element;
    } else {
      // we should never get here (our plugin.xml must be wrong)
      throw new IllegalStateException("no project information");
    }
    
    /* If we are setting the focus, we need to remove the focus from
     * all the other projects.
     */
    boolean removedNature = false;
    if (newValue) {
      final IProject[] projects =
        ResourcesPlugin.getWorkspace().getRoot().getProjects();

      for (int i = 0; i < projects.length; i++) {
        final IProject current = projects[i];
        // Can only manipulate the nature of open projects
        if (current.isOpen()) {
          if (project != current && Nature.hasNature(current)) {
            try {
              Nature.removeNatureFromProject(current);
              JavacDriver.getInstance().recordProjectAction(ScriptCommands.REMOVE_NATURE, project);
              removedNature = true;
            } catch (final CoreException e) {
              LOG.log(Level.SEVERE,
                  "failure while removing double-checking nature from Java project "
                  + current.getName(), e);
            }
          }
        }
      }
    }
    
    try {
      if (newValue) {
        // add our Fluid nature to the project
        try {
          Nature.addNatureToProject(project);
          JavacDriver.getInstance().recordProjectAction(ScriptCommands.ADD_NATURE, project);
        } catch (CoreException e) {
          LOG.log(Level.SEVERE, "failure adding double-checking nature to Java project "
              + project.getName(), e);
        }
      } else {
        // remove our nature from the project
        Nature.removeNatureFromProject(project);
        removedNature = true;
      }
    } catch (CoreException e) {
      LOG.log(Level.SEVERE, "failure setting (" + newValue
          + ") or removing double-checking nature to/from Java project "
          + project.getName(), e);
    }
    
    // Report change of state to the system.
    ClearProjectListener.postNatureChangeUtility(removedNature);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    composite.setLayout(gridLayout);
    m_onOff = new Button(composite, SWT.CHECK);
    m_onOff.setText("Focus assurance on this Java project");
    focusButtonInitialState = doesProjectUseDoubleChecking();
    m_onOff.setSelection(focusButtonInitialState);
    return composite;
  }

  /**
   * When the user clicks on the <code>OK</code> button, this callback sets or
   * removes double-checking on the project based upon the check box button in
   * the preference dialog.
   * 
   * @see org.eclipse.jface.preference.IPreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    if (focusButtonInitialState != m_onOff.getSelection()) {
      setProjectDoubleCheckingStatus(m_onOff.getSelection());
    }
    return super.performOk();
  }
}
