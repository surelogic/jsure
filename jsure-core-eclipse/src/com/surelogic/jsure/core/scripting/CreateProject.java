package com.surelogic.jsure.core.scripting;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Creates a new Java project with the specified source files, 
 * or an empty project if not specified
 * 
 * @author Edwin
 */
public class CreateProject extends AbstractCommand {
	/**
	 * @param context
	 * @param contents
	 *          arguments: 1 - name of the project 
	 *          2 - location of source files (OPTIONAL)
	 */
	@Override
	public boolean execute(ICommandContext context, String... contents)
			throws Exception {
		IProject p = resolveProject(contents[1], true);
		//JavaCore.getJavaCore();
		if (!p.exists()) {
			IProjectDescription description = p.getWorkspace().newProjectDescription(
					contents[1]);
			if (contents.length >= 3) {
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
			/* Needs to be added separately
			 * 
			// Add the JSure Nature
			if (!description.hasNature(Nature.DOUBLE_CHECKER_NATURE_ID)) {
				String[] natures = description.getNatureIds();
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = Nature.DOUBLE_CHECKER_NATURE_ID;
				description.setNatureIds(newNatures);
			}
			*/			
			p.create(description, null);
			p.open(null);

			// now create the Java Project
			IJavaProject javaProject = JavaCore.create(p);
			if (!javaProject.exists()) {
				throw new Exception("Could not create a Java project from " + p);
			}
			setupJRE(javaProject);

			initCompilerSettings(javaProject);
			return true;
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initCompilerSettings(IJavaProject javaProject) {
		// check for the .settings folder
		IFile settings = resolveIFile(javaProject.getElementName() + "/.settings");

		// If there are no project-specific settings, set some
		if (settings == null || !settings.exists()) {							
			// Set the compiler compliance to Java 1.5
			Map options = javaProject.getOptions(true);
			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
					JavaCore.VERSION_1_5);

			javaProject.setOptions(options);
		}
	}

	private void setupJRE(IJavaProject javaProject) throws JavaModelException {
		Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
		entries.addAll(Arrays.asList(javaProject.getRawClasspath()));
		entries.add(JavaRuntime.getDefaultJREContainerEntry());
		javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries
				.size()]), null);
	}
}
