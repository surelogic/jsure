package test_singleThreaded_constructor;

/**
 * Tests checking singleThreaded constructors with effects and thread effects.
 * 
 * @RegionLock L is this protects Instance
 */
public class TestEffects {
//  @SuppressWarnings("unused")
  private int x;
//  @SuppressWarnings("unused")
  private int y;
  
  /**
   * GOOD: The constructor writes nothing outside of itself and starts
   * no threads.
   * @SingleThreaded
   * @RegionEffects none
   * @Starts nothing
   */
  public TestEffects(int a, int b) {
    // Safe access
    this.x = a;
    // Safe access
    this.y = b;
  }

  /**
   * GOOD: The constructor writes nothing outside of itself (although it reads
   * from other objects), and starts no threads, even though it creates a thread.
   * 
   * @SingleThreaded
   * @RegionEffects reads a:Instance, b:Instance
   * @Starts nothing
   */
  public TestEffects(MyInt a, MyInt b) {
    // Safe access
    this.x = a.intValue();
    // Safe access
    this.y = b.intValue();
//    @SuppressWarnings("unused")
    Thread t = new Thread(new Dumb());
  }

  /**
   * BAD: The constructor writes nothing outside of itself (although it reads
   * from other objects), but it starts a thread.
   * 
   * @SingleThreaded
   * @RegionEffects reads a:Instance
   * @Starts nothing
   */
  public TestEffects(MyInt a) {
    // Safe access
    this.x = a.intValue();
    Thread t = new Thread(new Dumb());
    t.start();
  }
}

class MyInt {
  private int val;
  /**
   * @RegionEffects reads Instance
   * @Starts nothing
   */
  public int intValue() { return val; }
}

class Dumb implements Runnable {
  /**
   * @RegionEffects none
   * @Starts nothing
   */
  public Dumb() {
  }
  
  public void run() {
  }
}