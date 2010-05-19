package edu.cmu.cs.fluid.sea.drops;

import com.surelogic.analysis.IIRProject;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.xml.*;

public class ProjectDrop extends Drop {
  final String projectName;
  final IIRProject project;
  
  private ProjectDrop(String name, IIRProject p) {
	  projectName = name;
	  project = p;
  }

  public static ProjectDrop ensureDrop(String project, IIRProject p) {
	  for(ProjectDrop pd : Sea.getDefault().getDropsOfExactType(ProjectDrop.class)) {
		  if (project.equals(pd.getName())) {
			  return pd;
		  }
	  }
	  return new ProjectDrop(project, p);
  }
  
  @Override
  public String getEntityName() {
	  return "project-drop";
  }	
  
  @Override
  public void snapshotAttrs(AbstractSeaXmlCreator s) {
	  super.snapshotAttrs(s);
	  s.addAttribute("name", projectName);
  }

  public String getName() {
	  return projectName;
  }
  
  public IIRProject getIIRProject() {
	  return project;
  }
  
  public static ProjectDrop getDrop() {
	  ProjectDrop p = null;
	  for (ProjectDrop pd : Sea.getDefault().getDropsOfExactType(
			  ProjectDrop.class)) {
		  if (p == null) {
			  p = pd;
		  } else {
			  LOG.info("Multiple projects analyzed for JSure: "
					  + pd.getName());
		  }
	  }
	  return p;
  }
  
  public static String getProject() {
	  ProjectDrop pd = getDrop();
	  if (pd == null) {
		  return null;
	  }
	  return pd.getName();
  }
}