package test_static_lock_semantics.parent_child;

/**
 * Declares a static field used as a lock in a descendent class.
 * Doesn't have any protected state of it's own. 
 */
public class A {
  protected static final Object Lock = new Object();
  
  public int foo = 0;
}
