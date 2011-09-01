package testParser;

import com.surelogic.*;
 
public enum TestEnums {
	  /* Need to make sure that we treat this as a constructor call, and that we
	   * consider the effects of on E().
	   * 
	   * Should have the effect writes(zzz)
	   */
	  A,
	  
	  /* Need to make sure that we visit the argument list.
	   * 
	   * Need to make sure that we treat this as a constructor call, and that we
	   * consider the effects of E(int).
	   * 
	   * Should have the effect writes(zzz) from the constructor call, and the
	   * effect write(secret) from the argument list.
	   */
	  B(incSecret(0)),
	  
	  /* Need to make sure that we visit the argument list. 
	   * 
	   * Need to make sure that we treat this as a constructor call, and that we
	   * consider the effects of E(int).
	   * 
	   * Should have the effect writes(zzz) from the constructor call, the
	   * effect write(secret) from the argument list, and the effect
	   * write(yyy) from the instance initializer.
	   */
	  C(incSecret(1) /* Used to get errors here because the analysis context was not set properly.  This is part of bug 1627 */) {
	    {
	      /* Need to process the enum constant declaration as a class declaration
	       * and visit the class's initializers to reach here.  This is done when
	       * processing the declaration of the enumeration constant itself. 
	       * This is part of bug 1627.
	       */
	      setYYY(10);
	    }
	    
	    @Override
	    @RegionEffects("none")
	    public void foo() {
	      /* Need to visit the contents of the class declaration to reach here.
	       * That is, the top level assurance drivers need to visit the class bodies
	       * of EnumConstantClassDeclaration nodes.  This is part of bug 1627.
	       */
	      // bad, has effect
	      setZZZ(0);
	    }
	  };
	  
	  protected static int secret = 0;

	  protected static int zzz;
	  
	  protected static int yyy;
	  	  
	  @RegionEffects("writes zzz")
	  private TestEnums() {
	    this(-1);
	  }
	  
	  @RegionEffects("writes zzz")
	  private TestEnums(final int v) {
	    /* Need to visit the class body of enumerations to reach here.  This was
	     * already being done, and is not part of bug 1627.
	     */
	    setZZZ(v);
	  }
	  
	  @RegionEffects("writes secret")
	  protected static int incSecret(final int v) {
	    secret += v;
	    return secret;
	  }
	  
	  @RegionEffects("writes zzz")
	  protected static void setZZZ(final int v) {
	    zzz = v;
	  }
	  
	  @RegionEffects("writes yyy")
	  protected static void setYYY(final int v) {
	    yyy = v;
	  }
	  
	  @RegionEffects("none")
	  protected void foo() {
	    // do nothing
	  }	
	/*
	A,

	B(0),

	C(1) {
		@Override
		protected void m() {  }
	};

	E() {
		this(-1);
	}
	
	E(int v) { }

	protected void m() { }
	*/
}

