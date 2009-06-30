/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/project/JavaComponent.java,v 1.10 2007/07/10 22:16:34 aarong Exp $
 */
package edu.cmu.cs.fluid.java.project;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.project.Component;
import edu.cmu.cs.fluid.project.ComponentFactory;
import edu.cmu.cs.fluid.project.PlainComponent;
import edu.cmu.cs.fluid.util.UniqueID;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegion;


/**
 * A component with content inside a Java project.
 * It represents either a package or a top-level class.
 * @author boyland
 */
public class JavaComponent extends PlainComponent {
  /**
   * Create a new JavaComponent
   */
  public JavaComponent() {
    super();
  }

  /**
   * Create a new JavaCOmponent in an existing region
   * @param vr region from which all nodes will come
   */
  public JavaComponent(VersionedRegion vr) {
    super(vr);
  }
  
  /**
   * Create an existing JavaCOmponent with the given ID and region.
   * @param id unique id (from file)
   * @param vr region in which nodes are created.
   */
  protected JavaComponent(UniqueID id) {
    super(id);
  }

  protected JavaComponent(JavaComponent from, Version v) {
    super(from,v);
  }
  
  @Override
  protected Component makeShared(Version v) {
    return new JavaComponent(this,v);
  }
  
  public static JavaComponent findComponent(IRNode node) {
    throw new UnsupportedOperationException("Cannot find out what component I'm in!");
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.project.Component#getFactory()
   */
  @Override
  public ComponentFactory getFactory() {
    return Factory.prototype;
  }

  private static final List<Bundle> javaBundles = 
    Arrays.asList(new Bundle[] {JJNode.getBundle(), JavaNode.getBundle(), JavaPromise.getBundle()});
  
  @Override
  public Collection<Bundle> getBundles() {
    return Collections.unmodifiableCollection(javaBundles);
  }
  
  static class Factory extends ComponentFactory {
    public static final Factory prototype = new Factory();
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.project.PlainComponent.Factory#create(edu.cmu.cs.fluid.util.UniqueID, edu.cmu.cs.fluid.version.VersionedRegion, java.io.DataInput)
     */
    @Override
    public Component create(UniqueID id) {
      return new JavaComponent(id);
    }
  }
}
