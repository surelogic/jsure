package com.surelogic.jsure.core.driver;

import java.util.Map;

import com.surelogic.analysis.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Common protocol for all analysis modules. An <I>analysis module</I> is
 * defined as a singleton class that implements {@link IAnalysis}. Normally,
 * analysis modules extend {@link AbstractAnalysisModule}rather than implement
 * this interface directly.
 * <p>
 * The <I>per project</I> protocol an analysis module is invoked with by
 * {@link Majordomo}is:
 * <OL>
 * <LI>{@link #resetForAFullBuild}notifies a rebuild is starting. If this module
 * is not invoked then an incremental build is being done.</LI>
 * <LI>{@link #analyzeBegin}frames the beginning of the build.</LI>
 * <LI>For each changed resource (e.g., file) {@link #analyzeResource}is
 * invoked. If <code>false</code> is returned from {@link #analyzeResource} than
 * {@link Majordomo}will invoke {@link #analyzeCompilationUnit}if (1) the
 * resource is a Java compilation unit, and (2) it exists on the project class
 * path. This is how an analysis module requests detailed Eclipse Java AST/type
 * binding information.</LI>
 * <LI>{@link #analyzeEnd}frames the end of the build. If this module requests
 * to examine resources again (possibly the entire project) then further calls
 * to {@link #analyzeCompilationUnit}and {@link #analyzeResource}are made
 * followed by a subsequent call to {@link #analyzeEnd}({@link #analyzeBegin}is
 * not called).</LI>
 * </OL>
 * All builds are <I>per project</I> so if multiple projects are opened then an
 * analysis module will pass through the full protocol for each project in
 * sequence.
 * 
 * @see AbstractAnalysisModule
 */
public interface IAnalysis {

	/**
	 * Frames the start of an any build on a project. Called before any analyses
	 * have been invoked.
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 */
	void preBuild(IProject project);

	/**
	 * Informs an analysis module that a full project rebuild is starting. In
	 * the special case of a project rebuild within Eclipse this method is
	 * called before {@link #analyzeBegin(IProject)}to allow an analysis module
	 * to reset itself (e.g., empty lists, clean up resources).
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 */
	void resetForAFullBuild(IProject project);

	/**
	 * Frames the start of calls to re-analyze resources within a project.
	 * Called after the previous level has been completed but the current one
	 * has not yet started.
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 */
	void analyzeBegin(IProject project);

	/**
	 * Notifies the analysis module that a project resource (i.e., a file) has
	 * changed.
	 * 
	 * @param resource
	 *            the Eclipse resource to be analyzed
	 * @param kind
	 *            one of {@link org.eclipse.core.resources.IResourceDelta#ADDED}
	 *            , {@link org.eclipse.core.resources.IResourceDelta#REMOVED},
	 *            or {@link org.eclipse.core.resources.IResourceDelta#CHANGED}
	 * @return <code>true</code> if done, <code>false</code> if
	 *         {@link Majordomo}should follow up by invoking
	 *         {@link #analyzeCompilationUnit}if <code>resource</code> is a Java
	 *         compilation unit and is on the project class path.
	 */
	boolean analyzeResource(IResource resource, int kind);

	/**
	 * @return True if analyzeCompilationUnit actually needs the AST to be
	 *         created
	 */
	boolean needsAST();

	/**
	 * As an effect of returning <code>false</code> from
	 * {@link #analyzeResource}this method provides an analysis module with
	 * detailed Eclipse AST/type binding information about a Java compilation
	 * unit.
	 * 
	 * @param file
	 *            the Java compilation unit to be analyzed
	 * @param ast
	 *            the abstract syntax tree (with type bindings) for the Java
	 *            compilation unit
	 * @return true if progress is to be handled automatically (e.g. ++)
	 */
	boolean analyzeCompilationUnit(ICompilationUnit file, CompilationUnit ast, 
			                       IAnalysisMonitor monitor);

	/**
	 * Frames the end of calls to re-analyze resources within a project. Called
	 * after the current level has been completed but before the next one has
	 * started. The return value from this call controls if any further
	 * resources within the project are passed to this analysis module. <BR>
	 * If an analysis module requests to examine resources again (possibly the
	 * entire project) then further calls to {@link #analyzeCompilationUnit}and
	 * {@link #analyzeResource}are made followed by a subsequent call to
	 * {@link #analyzeEnd}({@link #analyzeBegin}is not called).
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 * @return an array of specific {@link IResource}objects that the analysis
	 *         module wants to analyze again, <code>null</code> if the entire
	 *         project needs to be analyzed again, or an empty array if no
	 *         further analysis is required (If {@link AbstractAnalysisModule}
	 *         has been extended then the
	 *         {@link AbstractAnalysisModule#NONE_FURTHER}field may be returned,
	 *         which is what the default method implementation does)
	 */
	IResource[] analyzeEnd(IProject project, IAnalysisMonitor monitor);

	/**
	 * Frames the end of an any build on a project. Called after all analyses
	 * have been invoked.
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 */
	void postBuild(IProject project);

	/**
	 * Sets the defined label for this analysis module defined in the plugin.xml
	 * file. Implemented in {@link AbstractAnalysisModule}and not intended to be
	 * overridden.
	 * 
	 * @param label
	 *            The defined label for this analysis module
	 */
	void setLabel(String label);

	/**
	 * Gets the defined label (set in {@link #setLabel}) for this analysis
	 * module.
	 * 
	 * @return the label for this analysis module
	 */
	String getLabel();

	/**
	 * Called after preBuild.
	 */
	@SuppressWarnings({ "rawtypes" })
	void setArguments(Map args);

	void cancel();
}