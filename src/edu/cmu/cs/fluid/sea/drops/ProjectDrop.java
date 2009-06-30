package edu.cmu.cs.fluid.sea.drops;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

public class ProjectDrop extends Drop {
  final String projectName;
  
  private ProjectDrop(String project, Object p) {
	  projectName = project;
  }

  public static void ensureDrop(String project, Object p) {
	  for(ProjectDrop pd : Sea.getDefault().getDropsOfExactType(ProjectDrop.class)) {
		  if (project.equals(pd.getName())) {
			  return;
		  }
	  }
	  new ProjectDrop(project, p);
  }
  
  @Override
  public String getEntityName() {
	  return "project-drop";
  }	
  
  @Override
  public void snapshotAttrs(SeaSnapshot s) {
	  super.snapshotAttrs(s);
	  s.addAttribute("name", projectName);
  }

  public String getName() {
	  return projectName;
  }
  
  public static String getProject() {
	  String p = null;
	  for (ProjectDrop pd : Sea.getDefault().getDropsOfExactType(
			  ProjectDrop.class)) {
		  if (p == null) {
			  p = pd.getName();
		  } else {
			  LOG.warning("Multiple projects analyzed for JSure: "
					  + pd.getName());
		  }
	  }
	  return p;
  }
}