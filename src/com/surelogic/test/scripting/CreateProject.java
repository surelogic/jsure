package com.surelogic.test.scripting;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;

import edu.cmu.cs.fluid.dc.Nature;

public class CreateProject extends AbstractCommand {
	/**
	 * @param context
	 * @param contents
	 *          arguments: 1 - name of the project 2 - location of source files
	 *          (OPTIONAL)
	 */
	public boolean execute(ICommandContext context, String[] contents)
			throws Exception {
		IProject p = resolveProject(contents[1], true);
		JavaCore.getJavaCore();
		if (!p.exists()) {
			IProjectDescription description = p.getWorkspace().newProjectDescription(
					contents[1]);
			if (contents.length == 3) {
				File source = new File(contents[2]);
				if (source.isDirectory()) {
					description.setLocation(Path.fromOSString(source.getAbsolutePath()));
				} else {
					throw new IllegalArgumentException(source
							+ " is not a valid directory.");
				}
			}
			// Add the Java Nature
			if (!description.hasNature(JavaCore.NATURE_ID)) {
				String[] natures = description.getNatureIds();
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = JavaCore.NATURE_ID;
				description.setNatureIds(newNatures);
			}
			// Add the JSure Nature
			if (!description.hasNature(Nature.DOUBLE_CHECKER_NATURE_ID)) {
				String[] natures = description.getNatureIds();
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = Nature.DOUBLE_CHECKER_NATURE_ID;
				description.setNatureIds(newNatures);
			}
			
			// check for the .settings folder
			IFile settings = resolveFile(p.getName() + "/.settings");

			// If there are no project-specific settings, set some
			if (settings == null || !settings.exists()) {
				// Set the compiler compliance to Java 1.5
				// This sets it for the whole system I believe...need to make this on a
				// per-project basis in case we want multi-project tests
				Hashtable options = JavaCore.getOptions();
				options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
				options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
				options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
						JavaCore.VERSION_1_5);

				JavaCore.setOptions(options);
			}
			
			p.create(description, null);
			p.open(null);

			// now create the Java Project
			IJavaProject javaProject = JavaCore.create(p);
			if (!javaProject.exists()) {
				throw new Exception("Could not create a Java project from " + p);
			}
			setupJRE(javaProject);

			return true;
		}
		return false;
	}

	private void setupJRE(IJavaProject javaProject) throws JavaModelException {
		Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
		entries.addAll(Arrays.asList(javaProject.getRawClasspath()));
		entries.add(JavaRuntime.getDefaultJREContainerEntry());
		javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries
				.size()]), null);
	}
}
