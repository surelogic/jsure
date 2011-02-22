package test.simple;

import com.surelogic.Unique;

public class WithUnique {
  private @Unique Useless u; // = new Useless();

  
  
  /*
   * Test with classes that are definitely may aliases:
   * Two references of type WithUnique.
   */
  
  /* Should fail with a "Undefined value on stack borrowed" message on the
   * control flow node.  
   */
  public void definitelyAliased_fails1(final WithUnique other) {
    this.u.twiddleParameter(other);
  }
  
  public void definitelyAliased_assures1(final WithUnique other) {
    this.u.twiddleReceiver(other);
  }
  
  /* Should fail with a "read undefined local: 1" message on the control
   * flow node.
   */
  public void definitelyAliased_fails2(final WithUnique other) {
    this.u.twiddleBoth(other);
  }
  
  public void definitelyAliased_fails3(final WithUnique other) {
    Useless.twiddle1(this.u, other); // should be like definitelyAliased_fails1()
  }
  
  public void definitelyAliased_assures2(final WithUnique other) {
    Useless.twiddle2(this.u, other); // should be like definitelyAliased_assures1()
  }
  
  public void definitelyAliased_fails4(final WithUnique other) {
    Useless.twiddle3(this.u, other); // should be like definitelyAliased_fails2()
  }



  /*
   * Test with classes that are definitely not may aliases.
   * A reference to WithUnique (this), and a reference to Unrelated (other).
   * The objects of the types are not assignable to each other.
   */
  
  public void definitelyNotAliased_assures1(final Unrelated other) {
    this.u.twiddleParameter(other);
  }
  
  public void definitelyNotAliased_assures2(final Unrelated other) {
    this.u.twiddleReceiver(other);
  }
  
  public void definitelyNotAliased_assures3(final Unrelated other) {
    this.u.twiddleBoth(other);
  }
  
  public void definitelyNotAliased_assures4(final Unrelated other) {
    Useless.twiddle1(this.u, other); // should be like definitelyNotAliased_assures1()
  }
  
  public void definitelyNotAliased_assures5(final Unrelated other) {
    Useless.twiddle2(this.u, other); // should be like definitelyNotAliased_assures2()
  }
  
  public void definitelyNotAliased_assures6(final Unrelated other) {
    Useless.twiddle3(this.u, other); // should be like definitelyNotAliased_assures3()
  }
}
