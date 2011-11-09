package test;

public class C {
  // ======================================================================
  // === Constructor
  // ======================================================================
  
	public C() {
		super();
	}
	
  public C(final Object o) {
    super();
  }
  

  
  // ======================================================================
  // === Helper methods
  // ======================================================================
  
  private static boolean test() { return true; }
  
  private static Object m(final int v) { return null; }
  
  
  
  // ======================================================================
  // === Normal case: Initializer of the outer class
  // ======================================================================
  
  {
    if (test()) {
      m(1);
    } else {
      m(2);
    }
    
    /* Control flow should pass through the initializer block of the
     * anonymous class expression, but not the methods it declares. 
     */
    new Other(m(3)) {
      {
        if (test()) {
          m(4);
        } else {
          m(5);
        }
      }
      
      public void method() {
        m(6);
      }
    };
  }

  /* Field whose initialization invokes the constructor, for comparison
   * against enum constant declarations.  In particular, we want to see that the
   * control flow passes through the argument list of the allocation expression.
   */
  private static final Object f1 = new C(); 

  private static final Object f2 = new C(m(101));
  
  private static final Object f3 = new C(m(102)) {
    private Object o1 = m(103);
    
    {
      if (test()) {
        m(104);
      } else {
        m(105);
      }
    }
    
    public void method() {
      m(106);
    }
  };
}
