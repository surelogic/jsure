package t;

import com.surelogic.Unique;

public class Outer {
  public class Inner1 {
    @Unique
    private Object u = null;
    
    public void method1() {
    	// This fails: the receiver is shared
    	Inner1.this.u = Inner1.this;
    }
    
    public void method2() {
      // This fails: the qualified receiver is shared
    	// (Point is that this used to pass, but it shouldn't have)
      Inner1.this.u = Outer.this;
    }
    
    
    public class Inner2 {
    	@Unique
    	private Object uu = null;
    	
      public void method1() {
      	// Currently this fails, as it should
      	Inner2.this.uu = Inner2.this;
      }
      
      public void method2() {
        // Currently this passes, but it should fail
      	// (Point is that this used to pass, but it shouldn't have)
        Inner2.this.uu = Inner1.this;
      }
      
      public void method3() {
        // Currently this passes, but it should fail
     	  // (Point is that this used to pass, but it shouldn't have)
        Inner2.this.uu = Outer.this;
      }
   }
  }
}