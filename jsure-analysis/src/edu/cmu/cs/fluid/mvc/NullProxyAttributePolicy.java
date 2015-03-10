package edu.cmu.cs.fluid.mvc;

import java.util.Set;

/**
 * A proxy attribute policy that never sets an attributes.
 */
public class NullProxyAttributePolicy
implements ProxyAttributePolicy
{
  public static final NullProxyAttributePolicy prototype = 
    new NullProxyAttributePolicy();

  protected static final AVPair[] empty = new AVPair[0];

  protected NullProxyAttributePolicy() { super(); }

  @Override
  public AVPair[] attributesFor( final Model model, final Set skippedNodes )
  {
    return empty;  
  }
}
