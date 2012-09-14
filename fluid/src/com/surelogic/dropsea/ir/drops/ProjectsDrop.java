package com.surelogic.dropsea.ir.drops;

import com.surelogic.InRegion;
import com.surelogic.analysis.IIRProjects;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.Sea;


public class ProjectsDrop extends Drop {
  static private ProjectsDrop active = null;

  @InRegion("DropState")
  private IIRProjects f_projects;

  private ProjectsDrop(IIRProjects p) {
    f_projects = p;
    synchronized (ProjectsDrop.class) {
      active = null;
    }
  }

  public static ProjectsDrop ensureDrop(IIRProjects p) {
    ProjectsDrop match = null;
    for (ProjectsDrop pd : Sea.getDefault().getDropsOfExactType(ProjectsDrop.class)) {
      if (pd.f_projects != p) {
        pd.invalidate();
      } else {
        match = pd;
      }
    }
    if (match != null) {
      return match;
    }
    return new ProjectsDrop(p);
  }

  @Override
  public String getXMLElementName() {
    return "projects-drop";
  }

  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(AbstractXMLReader.PROJECTS, f_projects.getLabel());
  }

  public IIRProjects getIIRProjects() {
    synchronized (f_seaLock) {
      return f_projects;
    }
  }

  public void setProjects(IIRProjects p) {
    synchronized (f_seaLock) {
      f_projects = p;
    }
  }

  public static synchronized ProjectsDrop getDrop() {
    if (active != null) {
      if (active.isValid()) {
        return active;
      }
      /*
       * If the drop is not valid null out our reference.
       */
      active = null;
    }
    ProjectsDrop p = null;
    for (ProjectsDrop pd : Sea.getDefault().getDropsOfExactType(ProjectsDrop.class)) {
      if (p == null) {
        p = pd;
      } else {
        LOG.warning("Multiple projects analyzed for JSure: " + pd.getIIRProjects().getLabel());
      }
    }
    active = p;
    return p;
  }

  public static IIRProjects getProjects() {
    ProjectsDrop pd = getDrop();
    if (pd == null)
      return null;
    else
      return pd.getIIRProjects();
  }
}