package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Test {
  private @Unique Other u1;
  private @Unique Other u2;
  
  @RegionEffects("none")
  @Unique("return")
  private Other create() {
    return null;
  }
  
  private void m(int x, @Unique Other p) {
    Other o = null;
    if (x == 1) {
      o = this.u2;
      this.u2 = new Other();
    } else if (x == 2) {
      o = new Other();
    } else if (x == 3) {
      o = create();
    } else {
      o = p;
    } 
    this.u1 = o;
  }
  
  @RegionEffects("reads o:Instance")
  private int makeValue(final @Borrowed Other o) {
    return 10;
  }

  
  
  public void m1() {
    this.u1 = null;
  }

  public void m2() {
    this.u1 = new Other();
  }

  public void m3() {
    this.u1.x = makeValue(this.u2);
  }
  
  public void m4() {
    this.u1.x = makeValue(this.u1);
  }
}

class Other {
  public int x;
  
  @Unique("return")
  @RegionEffects("none")
  public Other() {
    super();
  }
  
  @Unique("this, return")
  private Other m(boolean f, @Unique Other p) {
    return f ? p : this;
  }

}