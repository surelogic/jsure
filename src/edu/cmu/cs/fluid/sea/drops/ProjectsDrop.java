package edu.cmu.cs.fluid.sea.drops;

import com.surelogic.analysis.*;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.xml.*;

public class ProjectsDrop extends Drop {
  static ProjectsDrop active = null;
  IIRProjects projects;
  
  private ProjectsDrop(IIRProjects p) {
	  projects = p;
	  synchronized (ProjectsDrop.class) {
		  active = null;
	  }
  }

  public static ProjectsDrop ensureDrop(IIRProjects p) {
	  ProjectsDrop match = null;
	  for(ProjectsDrop pd : Sea.getDefault().getDropsOfExactType(ProjectsDrop.class)) {
		  if (pd.projects != p) {
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
  public String getEntityName() {
	  return "projects-drop";
  }	
  
  @Override
  public void snapshotAttrs(AbstractSeaXmlCreator s) {
	  super.snapshotAttrs(s);
  }
  
  public IIRProjects getIIRProjects() {
	  return projects;
  }
  
  public void setProjects(IIRProjects p) {
	  projects = p;
  }
  
  public static synchronized ProjectsDrop getDrop() {
	  if (active != null) {
		  if (active.isValid()) {
			  return active;
		  }
		  active = null;
	  }	  
	  ProjectsDrop p = null;
	  for (ProjectsDrop pd : Sea.getDefault().getDropsOfExactType(
			  ProjectsDrop.class)) {
		  if (p == null) {
			  p = pd;
		  } else {
			  LOG.info("Multiple projects analyzed for JSure: "
					  + pd.getIIRProjects().getLabel());
		  }
	  }
	  active = p; 	  
	  return p;
  }
  
  public static IIRProjects getProjects() {
	  ProjectsDrop pd = getDrop();
	  if (pd == null) {
		  return null;
	  }
	  return pd.getIIRProjects();
  }
}