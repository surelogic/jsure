package test_requiresLock_for_parameters;

/**
 * Simple class whose methods all have lock preconditions.  Used by
 * class Test to test that method preconditions can require locks to be 
 * held on parameters.
 * 
 * @lock L is this protects Instance
 */
public class C {
  private int x;
  private int y;
  
  /**
   * @singleThreaded
   * @borrowed this
   */
  public C() {
    this.x = 0;
    this.y = 0;
  }
  
  /**
   * @requiresLock L
   */
  public void setX(int newX) { this.x = newX; }
  
  /**
   * @requiresLock L
   */
  public void setY(int newY) { this.y = newY; }
  
  /**
   * @requiresLock L
   */
  public double getMagnitude() {
    return Math.sqrt(x*x + y*y);
  }
}
