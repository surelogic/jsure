/*
 * Created on Sep 13, 2004
 *
 */
package edu.cmu.cs.fluid.java.annotation;


/**
 * @author Edwin
 *
 */
public interface IPromiseHandler {
  /**
   * @return The unique identifier for the kind of promise this handler
   *         is for
   */
  String getIdentifier();
}
