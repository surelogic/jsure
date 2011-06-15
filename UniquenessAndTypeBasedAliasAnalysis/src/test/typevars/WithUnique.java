package test.typevars;

import com.surelogic.Unique;

/*
 * T1 and T2 both extend from Object, and thus objects typed with T1 or T2 
 * may be aliases to those typed with WithUnique.  Similarly for T3: 
 * Parent-typed objects may be aliases to those typed with WithUnique.
 * T4-typed references cannot be aliases to WithUnique-typed referenced because
 * class Unrelated is not a direct ancestor of WithUnique.
 * 
 * 
 * TODO: Not sure how to test Capture types right now using the uniqueness
 * analysis.  Uniqueness uses alias analysis on the formal parameters, and I 
 * don't think a capture type can manifest itself in this context.
 */
public class WithUnique<T1, T2 extends Object, T3 extends Parent, T4 extends Unrelated, T5 extends Parent & Other> extends Parent {
  private @Unique Useless u; // = new Useless();

  
  
  /*
   * Test against type variable T1
   */
  
  public void definitelyAliased_T1_fails1(final T1 other) {
    this.u.twiddleParameter(other);
  }
  
  public void definitelyAliased_T1_assures1(final T1 other) {
    this.u.twiddleReceiver(other);
  }
  
  public void definitelyAliased_T1_fails2(final T1 other) {
    this.u.twiddleBoth(other);
  }
  
  public void definitelyAliased_T1_fails3(final T1 other) {
    Useless.twiddle1(this.u, other); // should be like definitelyAliased_fails1()
  }
  
  public void definitelyAliased_T1_assures2(final T1 other) {
    Useless.twiddle2(this.u, other); // should be like definitelyAliased_assures1()
  }
  
  public void definitelyAliased_T1_fails4(final T1 other) {
    Useless.twiddle3(this.u, other); // should be like definitelyAliased_fails2()
  }

  
  
  /*
   * Test against type variable T2
   */
  
  public void definitelyAliased_T2_fails1(final T2 other) {
    this.u.twiddleParameter(other);
  }
  
  public void definitelyAliased_T2_assures1(final T2 other) {
    this.u.twiddleReceiver(other);
  }
  
  public void definitelyAliased_T2_fails2(final T2 other) {
    this.u.twiddleBoth(other);
  }
  
  public void definitelyAliased_T2_fails3(final T2 other) {
    Useless.twiddle1(this.u, other); // should be like definitelyAliased_fails1()
  }
  
  public void definitelyAliased_T2_assures2(final T2 other) {
    Useless.twiddle2(this.u, other); // should be like definitelyAliased_assures1()
  }
  
  public void definitelyAliased_T2_fails4(final T2 other) {
    Useless.twiddle3(this.u, other); // should be like definitelyAliased_fails2()
  }

  
  
  /*
   * Test against type variable T3
   */
  
  public void definitelyAliased_T3_fails1(final T3 other) {
    this.u.twiddleParameter(other);
  }
  
  public void definitelyAliased_T3_assures1(final T3 other) {
    this.u.twiddleReceiver(other);
  }
  
  public void definitelyAliased_T3_fails2(final T3 other) {
    this.u.twiddleBoth(other);
  }
  
  public void definitelyAliased_T3_fails3(final T3 other) {
    Useless.twiddle1(this.u, other); // should be like definitelyAliased_fails1()
  }
  
  public void definitelyAliased_T3_assures2(final T3 other) {
    Useless.twiddle2(this.u, other); // should be like definitelyAliased_assures1()
  }
  
  public void definitelyAliased_T3_fails4(final T3 other) {
    Useless.twiddle3(this.u, other); // should be like definitelyAliased_fails2()
  }

  
  
  /*
   * Test against type variable T4
   */
  
  public void definitelyNotAliased_T4_assures1(final T4 other) {
    this.u.twiddleParameter(other);
  }
  
  public void definitelyNotAliased_T4_assures2(final T4 other) {
    this.u.twiddleReceiver(other);
  }
  
  public void definitelyNotAliased_T4_assures3(final T4 other) {
    this.u.twiddleBoth(other);
  }
  
  public void definitelyNotAliased_T4_assures4(final T4 other) {
    Useless.twiddle1(this.u, other); 
  }
  
  public void definitelyNotAliased_T4_assures5(final T4 other) {
    Useless.twiddle2(this.u, other); 
  }
  
  public void definitelyNotAliased_T4_assures6(final T4 other) {
    Useless.twiddle3(this.u, other); 
  }



  /*
   * Test against type variable T5
   */
  
  public void definitelyAliased_T5_fails1(final T5 other) {
    this.u.twiddleParameter(other);
  }
  
  public void definitelyNotAliased_T5_assures1(final T5 other) {
    this.u.twiddleReceiver(other);
  }
  
  public void definitelyAliased_T5_fails2(final T5 other) {
    this.u.twiddleBoth(other);
  }
  
  public void definitelyAliased_T5_fails3(final T5 other) {
    Useless.twiddle1(this.u, other); // should be like definitelyAliased_T5_fails1()
  }
  
  public void definitelyNotAliased_T5_assures2(final T5 other) {
    Useless.twiddle2(this.u, other); // should be like definitelyNotAliased_T5_assures1()
  }
  
  public void definitelyAliased_T5_fails4(final T5 other) {
    Useless.twiddle3(this.u, other); // should be like definitelyAliased_T5_fails2()
  }
}
