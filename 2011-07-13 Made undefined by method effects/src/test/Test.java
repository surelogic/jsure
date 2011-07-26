package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Test {
  /** Unique reference to an Other object */
  private @Unique Other u;
  
  @RegionEffects("writes this:Instance")
  public void doStuff1(final @Unique Other o) {
    this.u = new Other(o.getValue() + 1);
  }

  @RegionEffects("writes this:Instance")
  public void doStuff2(final @Borrowed Other o) {
    this.u = new Other(o.getValue() + 1);
  }
 
  @RegionEffects("writes this:Instance")
  public void doStuff3(final Other o) {
    this.u = new Other(o.getValue() + 1);
  }
  
  @RegionEffects("writes this:Instance")
  public Other getAndReset() {
    final Other v = this.u;
    this.u = new Other(0);
    return v;
  }
  
  
  public void test1() {
    doStuff1(this.u);
  }

  public void test2() {
    doStuff2(this.u);
  }

  public void test3() {
    doStuff3(this.u);
  }

  public void test1a(final @Unique Other o) {
    doStuff1(o);
  }

  public void test2a(final @Unique Other o) {
    doStuff2(o);
  }

  public void test3a(final @Unique Other o) {
    doStuff3(o);
  }
  
  
  @RegionEffects("none")
  public static void eatTwoParams1(final @Unique Other o, Other p) {
    // do nothing
  }
  
  @RegionEffects("none")
  public static void eatTwoParams2(final @Borrowed Other o, Other p) {
    // do nothing
  }
  
  @RegionEffects("none")
  public static void eatTwoParams3(final Other o, Other p) {
    // do nothing
  }
  
  public void test10() {
    eatTwoParams1(this.u, this.getAndReset());
  }

  public void test11() {
    eatTwoParams2(this.u, this.getAndReset());
  }

  public void test12() {
    eatTwoParams3(this.u, this.getAndReset());
  }
}



class Other {
  private final int v;
  
  @Unique("return")
  @RegionEffects("none")
  public Other(final int x) {
    v = x;
  }
  
  @Borrowed("this")
  @RegionEffects("none")
  public int getValue() {
    return v;
  }
}

