package test_requiresLock_for_parameters;

/**
 * Simple class whose methods all have lock preconditions.  Used by
 * class Test to test that method preconditions can require locks to be 
 * held on parameters.
 * 
 * @RegionLock L is this protects Instance
 */
public class C {
  private int x;
  private int y;
  
  /**
   * @SingleThreaded
   * @Borrowed this
   */
  public C() {
    this.x = 0;
    this.y = 0;
  }
  
  /**
   * @RequiresLock L
   */
  public void setX(int newX) { this.x = newX; }
  
  /**
   * @RequiresLock L
   */
  public void setY(int newY) { this.y = newY; }
  
  /**
   * @RequiresLock L
   */
  public double getMagnitude() {
    return Math.sqrt(x*x + y*y);
  }
}
