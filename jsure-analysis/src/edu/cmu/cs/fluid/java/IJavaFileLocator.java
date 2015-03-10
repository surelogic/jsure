/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/IJavaFileLocator.java,v 1.21 2008/08/25 15:38:52 chance Exp $*/
package edu.cmu.cs.fluid.java;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.parse.JJNode;
import com.surelogic.common.java.Config.Type;

/**
 * Tracks all the Java files that have been processed by the system
 * at some point
 * 
 * @author Edwin.Chan
 */
public interface IJavaFileLocator<T,P> extends Iterable<IJavaFileStatus<T>> {
  public static final boolean testIRPaging = false;
  public static final boolean useIRPaging = !JJNode.versioningIsOn && (testIRPaging || false);
  
  /**
   * Registers the given resource 
   * 
   * @param project non-null
   * @param id non-null
   * @param label A String to describe the resource
   * @param root The root of the resource (after canonicalization)
   * @return The corresponding status object created for it
   */
  IJavaFileStatus<T> register(P project, T id, String label, 
                           long modTime, IRNode root, Type t);
  
  /**
   * Return true if the resource has been registered (and loaded)
   */
  boolean isLoaded(T id);
  
  /**
   * Gets the status object for the resource
   * 
   * @param id non-null
   * @return possibly null if not previously registered
   */
  IJavaFileStatus<T> getStatus(T id);

  IJavaFileStatus<T> getStatusForAST(IRNode root);
  
  /**
   * Make sure that everything is canonicalized
   */
  void ensureAllCanonical();

  /**
   * Persist all the resources that haven't been 
   * persisted
   * @throws IOException
   */
  void persistNew() throws IOException;
  
  /**
   * Persist everything again
   * @throws IOException
   */
  void persistAll() throws IOException;
  
  List<CodeInfo> loadArchiveIndex() throws IOException;
  
  void printSummary(PrintWriter pw);
  
  /**
   * Removes the resource 
   * 
   * Also unloads it?
   */
  IJavaFileStatus<T> unregister(T id);
  
  /**
   * Returns the corresponding resource if up to date
   */
  IJavaFileStatus<T> isUpToDate(T id, long modTime, Type type);
  
  /**
   * Mark the resource as being reference by the project
   */
  void setProjectReference(P proj, T id);
  
  /**
   * Find the project associated with a resource
   */
  Object findProject(T id);
}
