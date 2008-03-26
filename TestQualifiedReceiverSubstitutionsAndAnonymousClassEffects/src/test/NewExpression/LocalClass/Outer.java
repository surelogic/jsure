package test.NewExpression.LocalClass;

import com.surelogic.RegionEffects;

public class Outer {
  public int f = 10;

  @RegionEffects("writes this:f")
  public void outerMethod() {
    class LocalClass {
      public int g;
      
      @RegionEffects("writes Outer.this:f")
      public LocalClass() {
        Outer.this.f = 10;
        this.g = 10;
      }
    }
    
    /*
     * The immediately enclosing instance of lc1 is Outer.this == this.
     * 
     * "Outer" is the innermost lexically enclosing class of "LocalClass"
     * "Outer" is the 0th lexically enclosing class of the class in which the
     * instance creation expression appears.
     * 
     * Writes this.f
     */
    final LocalClass lc1 = new LocalClass();
    
    
    class LC2 {
      @RegionEffects("writes Outer.this:f")
      public LocalClass doStuff() {
        /* The immediately enclosing instance is Outer.this
         * 
         * "Outer" is the innermost lexically enclosing class of "LocalClass"
         * "Outer" is the 1st lexically enclosing class of the class in which the
         * instance creation expression appears.
         * 
         * Writes Outer.this.f
         */
        return new LocalClass();
      }
    }

    class LC3 {
      class LC4 {
        @RegionEffects("writes Outer.this:f")
        public LocalClass doStuff() {
          /* The immediately enclosing instance is Outer.this
           * 
           * "Outer" is the innermost lexically enclosing class of "LocalClass"
           * "Outer" is the 2nd lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * Writes Outer.this.f
           */
          return new LocalClass();
        }
      }
    }
  }

  @RegionEffects("writes any(Outer):f")
  public void outerMethod2() {
    class LocalClass_om2 {
      public int g;
      
      @RegionEffects("writes Outer.this:f")
      public LocalClass_om2() {
        Outer.this.f = 10;
        this.g = 10;
      }
      
      @RegionEffects("writes this:g, any(Outer):f")
      public void doStuff() {
        class MoreLocalClass_om2 {
          public int h;
          
          @RegionEffects("writes LocalClass_om2.this:g, Outer.this:f")
          public MoreLocalClass_om2() {
            this.h = 10;
            LocalClass_om2.this.g = 10;
            Outer.this.f = 10;
          }
        }
        
        /* The immediately enclosing instance is LocalClass_om2.this == this
         * 
         * "LocalClass_om2" is the innermost lexically enclosing class of "MoreLocalClass_om2"
         * "LocalClass_om2" is the 0th lexically enclosing class of the class in which the
         * instance creation expression appears.
         * 
         * Writes this:g, any(Outer):f
         */
        final MoreLocalClass_om2 m = new MoreLocalClass_om2();
        
        class LC20 {
          @RegionEffects("writes Outer.this:f")
          public LocalClass_om2 doStuff() {
            /* The immediately enclosing instance is LocalClass_om2.this
             * 
             * "LocalClass_om2" is the innermost lexically enclosing class of "MoreLocalClass_om2"
             * "LocalClass_om2" is the 1st lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * Writes Outer.this.f
             */
            return new LocalClass_om2();
          }
        }

        class LC30 {
          class LC40 {
            @RegionEffects("writes Outer.this:f")
            public LocalClass_om2 doStuff() {
              /* The immediately enclosing instance is LocalClass_om2.this
               * 
               * "LocalClass_om2" is the innermost lexically enclosing class of "MoreLocalClass_om2"
               * "LocalClass_om2" is the 2nd lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * Writes Outer.this.f
               */
              return new LocalClass_om2();
            }
          }
        }
      }
    }
    
    /*
     * The immediately enclosing instance of lc1 is Other.this == this.
     * 
     * "Other" is the innermost lexically enclosing class of "LocalClass_om2"
     * "Other" is the 0th lexically enclosing class of the class in which the
     * instance creation expression appears.
     * 
     * Writes this.f
     */
    final LocalClass_om2 lc1 = new LocalClass_om2();
    // Writes lc1.g, any(Outer).f
    lc1.doStuff();
  }

  
  
  
  public class Middle {
    public int ff = 100;
    
    @RegionEffects("writes any(Outer):f, this:ff")
    public void outerMethod() {
      class LocalClass_m_om {
        public int g;
        
        @RegionEffects("writes Outer.this:f, Middle.this:ff")
        public LocalClass_m_om() {
          Outer.this.f = 10;
          Middle.this.ff = 100;
          this.g = 10;
        }
      }
      
      /*
       * The immediately enclosing instance of lc1 is Middle.this == this.
       * 
       * "Middle" is the innermost lexically enclosing class of "LocalClass_m_om"
       * "Middle" is the 0th lexically enclosing class of the class in which the
       * instance creation expression appears.
       * 
       * Writes any(Outer).f, this.ff 
       */
      final LocalClass_m_om lc1 = new LocalClass_m_om();
      
      
      
      class LC200 {
        @RegionEffects("writes any(Outer):f, Middle.this:ff")
        public LocalClass_m_om doStuff() {
          /* The immediately enclosing instance is Middle.this
           * 
           * "Middle" is the innermost lexically enclosing class of "LocalClass_m_om"
           * "Middle" is the 1st lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * Writes any(Outer).f, this.ff
           */
          return new LocalClass_m_om();
        }
      }
      
      class LC300 {
        class LC400 {
          @RegionEffects("writes any(Outer):f, Middle.this:ff")
          public LocalClass_m_om doStuff() {
            /* The immediately enclosing instance is Middle.this
             * 
             * "Middle" is the innermost lexically enclosing class of "LocalClass_m_om"
             * "Middle" is the 2nd lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * Writes any(Outer).f, this.ff
             */
            return new LocalClass_m_om();
          }
        }
      }
    }

    @RegionEffects("writes this:ff, any(Outer):f, any(Middle):ff")      // XXX: this:ff and any(Middle):ff are redundant -- remove this:ff because it makes the diffing of results nondeterministic
    public void outerMethod2() {
      class LocalClass_m_om2 {
        public int g;
        
        @RegionEffects("writes Outer.this:f, Middle.this:ff")
        public LocalClass_m_om2() {
          Outer.this.f = 10;
          Middle.this.ff = 10;
          this.g = 10;
        }
        
        @RegionEffects("writes this:g, any(Middle):ff, any(Outer):f")
        public void doStuff() {
          class MoreLocalClass {
            public int h;
            
            @RegionEffects("writes LocalClass_m_om2.this:g, Middle.this:ff, Outer.this:f")
            public MoreLocalClass() {
              this.h = 10;
              LocalClass_m_om2.this.g = 10;
              Middle.this.ff = 10;
              Outer.this.f = 10;
            }
          }
          
          /* The immediately enclosing instance is LocalClass_m_om2.this == this
           * 
           * "LocalClass_m_om2" is the innermost lexically enclosing class of "MoreLocalClass"
           * "LocalClass_m_om2" is the 0th lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * Writes this:g, any(Middle):ff, any(Outer):f
           */
          final MoreLocalClass m = new MoreLocalClass();
          
          class LC2000 {
            @RegionEffects("writes LocalClass_m_om2.this:g, any(Middle):ff, any(Outer):f")
            public MoreLocalClass doStuff() {
              /* The immediately enclosing instance is LocalClass_m_om2.this
               * 
               * "LocalClass_m_om2" is the innermost lexically enclosing class of "MoreLocalClass"
               * "LocalClass_m_om2" is the 1st lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * Writes any(Outer).f, this.ff
               */
              return new MoreLocalClass();
            }
          }
          
          class LC3000 {
            class LC4000 {
              @RegionEffects("writes LocalClass_m_om2.this:g, any(Middle):ff, any(Outer):f")
              public MoreLocalClass doStuff() {
                /* The immediately enclosing instance is LocalClass_m_om2.this
                 * 
                 * "LocalClass_m_om2" is the innermost lexically enclosing class of "MoreLocalClass"
                 * "LocalClass_m_om2" is the 2nd lexically enclosing class of the class in which the
                 * instance creation expression appears.
                 * 
                 * Writes any(Outer).f, this.ff
                 */
                return new MoreLocalClass();
              }
            }
          }
        }
      }
      
      /*
       * The immediately enclosing instance of lc1 is Middle.this == this.
       * 
       * Writes this.ff, any(Outer).f
       */
      final LocalClass_m_om2 lc1 = new LocalClass_m_om2();
      // Writes lc1.g, any(Middle).ff, any(Outer).f
      lc1.doStuff();
    }
  }
}
