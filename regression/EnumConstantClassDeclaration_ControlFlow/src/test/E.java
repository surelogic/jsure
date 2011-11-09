package test;


public enum E {
  /* No control flow to speak of, but should fix the graph here */
  A,
  
  /* Control flow should pass through the argument list of the constant
   * declaration, visiting the method call "m(1000)".
   */
  B(m(1000)),
  
  /* (1) Control flow should pass through the argument list of the constant
   * declaration, visiting the method call "m(1001)".
   *
   * (2) Control flow should pass through the initialization block of the
   * constant class expression, but not through the definition of z().
   */
  C(m(1001)) {
    {
      m(1002);
    }
    
    private Object q = m(1003);
    
    {
      m(1004);
    }
    
    public void z() {
    	m(1005); 
    }
  };
  
  private E() {
    // empty
  }
  
  private E(final Object o) {
    m(2000);
  }
  
  
  
  private static Object m(final int p) { return null; }
}
