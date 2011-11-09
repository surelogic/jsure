package test.NewExpression.LocalClass;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("F is lockF protects f")
public class Outer {
  public final Lock lockF = new ReentrantLock();
  public int f;

  @RequiresLock("this:F")
  public void outerMethod() {
    @RegionLock("G is lockG protects g")
    class LocalClass {
      public final Lock lockG = new ReentrantLock();
      public int g;
      
      @RequiresLock("Outer.this:F")
      @Borrowed("this")
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
     * Requires this.F
     */
    final LocalClass lc1 = new LocalClass();
    
    
    class LC2 {
      @RequiresLock("Outer.this:F")
      public LocalClass doStuff1() {
        /* The immediately enclosing instance is Outer.this
         * 
         * "Outer" is the innermost lexically enclosing class of "LocalClass"
         * "Outer" is the 1st lexically enclosing class of the class in which the
         * instance creation expression appears.
         * 
         * Requires Outer.this.F
         */
        return new LocalClass();
      }

      public LocalClass doStuff2() {
        /* The immediately enclosing instance is Outer.this
         * 
         * "Outer" is the innermost lexically enclosing class of "LocalClass"
         * "Outer" is the 1st lexically enclosing class of the class in which the
         * instance creation expression appears.
         * 
         * Requires Outer.this.F
         */
        Outer.this.lockF.lock();
        try {
          return new LocalClass();
        } finally {
          Outer.this.lockF.unlock();
        }
      }
    }

    class LC3 {
      class LC4 {
        @RequiresLock("Outer.this:F")
        public LocalClass doStuff() {
          /* The immediately enclosing instance is Outer.this
           * 
           * "Outer" is the innermost lexically enclosing class of "LocalClass"
           * "Outer" is the 2nd lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * Requires Outer.this.F
           */
          return new LocalClass();
        }

        public LocalClass doStuff2() {
          /* The immediately enclosing instance is Outer.this
           * 
           * "Outer" is the innermost lexically enclosing class of "LocalClass"
           * "Outer" is the 2nd lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * Requires Outer.this.F
           */
          Outer.this.lockF.lock();
          try {
            return new LocalClass();
          } finally {
            Outer.this.lockF.unlock();
          }
        }
      }
    }
  }

  @RequiresLock("this:F")
  public void outerMethod2() {
    @RegionLock("G is lockG protects g")
    class LocalClass_om2 {
      public final Lock lockG = new ReentrantLock();
      public int g;
      
      @RequiresLock("Outer.this:F")
      @Borrowed("this")
      public LocalClass_om2() {
        Outer.this.f = 10;
        this.g = 10;
      }
      
      @RequiresLock("this:G, Outer.this:F")
      public void doStuff() {
        @RegionLock("H is lockH protects h")
        class MoreLocalClass_om2 {
          public final Lock lockH = new ReentrantLock();
          public int h;
          
          @RequiresLock("LocalClass_om2.this:G, Outer.this:F")
          @Borrowed("this")
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
         * Requires this:G, Outer.this:F
         */
        final MoreLocalClass_om2 m = new MoreLocalClass_om2();
        
        class LC20 {
          @RequiresLock("Outer.this:F")
          public LocalClass_om2 doStuff1() {
            /* The immediately enclosing instance is LocalClass_om2.this
             * 
             * "LocalClass_om2" is the innermost lexically enclosing class of "MoreLocalClass_om2"
             * "LocalClass_om2" is the 1st lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * Requires Outer.this.F
             */
            return new LocalClass_om2();
          }

          public LocalClass_om2 doStuff2() {
            /* The immediately enclosing instance is LocalClass_om2.this
             * 
             * "LocalClass_om2" is the innermost lexically enclosing class of "MoreLocalClass_om2"
             * "LocalClass_om2" is the 1st lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * Requires Outer.this.F
             */
            Outer.this.lockF.lock();
            try {
              return new LocalClass_om2(); 
            } finally {
              Outer.this.lockF.unlock();
            }
          }
        }

        class LC30 {
          class LC40 {
            @RequiresLock("Outer.this:F")
            public LocalClass_om2 doStuff1() {
              /* The immediately enclosing instance is LocalClass_om2.this
               * 
               * "LocalClass_om2" is the innermost lexically enclosing class of "MoreLocalClass_om2"
               * "LocalClass_om2" is the 2nd lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * Requires Outer.this.F
               */
              return new LocalClass_om2();
            }

            public LocalClass_om2 doStuff2() {
              /* The immediately enclosing instance is LocalClass_om2.this
               * 
               * "LocalClass_om2" is the innermost lexically enclosing class of "MoreLocalClass_om2"
               * "LocalClass_om2" is the 2nd lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * Requires Outer.this.F
               */
              Outer.this.lockF.lock();
              try {
                return new LocalClass_om2(); 
              } finally {
                Outer.this.lockF.unlock();
              }
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
     * Requires this.F
     */
    final LocalClass_om2 lc1 = new LocalClass_om2();
    lc1.lockG.lock();
    try {
      // Requires lc1.G; F cannot be resolved
      lc1.doStuff();
    } finally {
      lc1.lockG.unlock();
    }
  }

  
  
  
  @RegionLock("FF is lockFF protects ff")
  public class Middle {
    public final Lock lockFF = new ReentrantLock();
    public int ff = 100;
    
    @Borrowed("this")
    public Middle() {
      // do nothing
    }
    
    @RequiresLock("Outer.this:F, this:FF")
    public void outerMethod() {
      @RegionLock("G is lockG protects g")
      class LocalClass_m_om {
        public final Lock lockG = new ReentrantLock();
        public int g;
        
        @RequiresLock("Outer.this:F, Middle.this:FF")
        @Borrowed("this")
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
       * Requires this:FF, Outer.this:F
       */
      final LocalClass_m_om lc1 = new LocalClass_m_om();
      
      
      
      class LC200 {
        @RequiresLock("Outer.this:F, Middle.this:FF")
        public LocalClass_m_om doStuff1() {
          /* The immediately enclosing instance is Middle.this
           * 
           * "Middle" is the innermost lexically enclosing class of "LocalClass_m_om"
           * "Middle" is the 1st lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * Requires Outer.this:F, Middle.this:FF
           */
          return new LocalClass_m_om();
        }

        public LocalClass_m_om doStuff2() {
          /* The immediately enclosing instance is Middle.this
           * 
           * "Middle" is the innermost lexically enclosing class of "LocalClass_m_om"
           * "Middle" is the 1st lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * Requires Outer.this:F, Middle.this:FF
           */
          Outer.this.lockF.lock();
          try {
            Middle.this.lockFF.lock();
            try {
              return new LocalClass_m_om();
            } finally {
              Middle.this.lockFF.unlock();
            }
          } finally {
            Outer.this.lockF.unlock();
          }
        }
      }
      
      class LC300 {
        class LC400 {
          @RequiresLock("Outer.this:F, Middle.this:FF")
          public LocalClass_m_om doStuff1() {
            /* The immediately enclosing instance is Middle.this
             * 
             * "Middle" is the innermost lexically enclosing class of "LocalClass_m_om"
             * "Middle" is the 2nd lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * Requires Outer.this:F, Middle.this:FF
             */
            return new LocalClass_m_om();
          }

          public LocalClass_m_om doStuff2() {
            /* The immediately enclosing instance is Middle.this
             * 
             * "Middle" is the innermost lexically enclosing class of "LocalClass_m_om"
             * "Middle" is the 2nd lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * Requires Outer.this:F, Middle.this:FF
             */
            Outer.this.lockF.lock();
            try {
              Middle.this.lockFF.lock();
              try {
                return new LocalClass_m_om();
              } finally {
                Middle.this.lockFF.unlock();
              }
            } finally {
              Outer.this.lockF.unlock();
            }
          }
        }
      }
    }

    @RequiresLock("this:FF")
    public void outerMethod2() {
      @RegionLock("G is lockG protects g")
      class LocalClass_m_om2 {
        public final Lock lockG = new ReentrantLock();
        public int g;
        
        @RequiresLock("Outer.this:F, Middle.this:FF")
        @Borrowed("this")
        public LocalClass_m_om2() {
          Outer.this.f = 10;
          Middle.this.ff = 10;
          this.g = 10;
        }
        
        @RequiresLock("this:G, Middle.this:FF, Outer.this:F")
        public void doStuff() {
          @RegionLock("H is lockH protects h")
          class MoreLocalClass {
            public final Lock lockH = new ReentrantLock();
            public int h;
            
            @RequiresLock("LocalClass_m_om2.this:G, Middle.this:FF, Outer.this:F")
            @Borrowed("this")
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
           * Requires this:G, Middle.this:FF, Outer.this:F
           */
          final MoreLocalClass m = new MoreLocalClass();
          
          class LC2000 {
            @RequiresLock("LocalClass_m_om2.this:G, Middle.this:FF, Outer.this:F")
            public MoreLocalClass doStuff1() {
              /* The immediately enclosing instance is LocalClass_m_om2.this
               * 
               * "LocalClass_m_om2" is the innermost lexically enclosing class of "MoreLocalClass"
               * "LocalClass_m_om2" is the 1st lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * Requires this:G, Middle.this:FF, Outer.this:F
               */
              return new MoreLocalClass();
            }

            public MoreLocalClass doStuff2() {
              /* The immediately enclosing instance is LocalClass_m_om2.this
               * 
               * "LocalClass_m_om2" is the innermost lexically enclosing class of "MoreLocalClass"
               * "LocalClass_m_om2" is the 1st lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * Requires this:G, Middle.this:FF, Outer.this:F
               */
              LocalClass_m_om2.this.lockG.lock();
              try {
                Middle.this.lockFF.lock();
                try {
                  Outer.this.lockF.lock();
                  try {
                    return new MoreLocalClass();
                  } finally {
                    Outer.this.lockF.unlock();
                  }
                } finally {
                  Middle.this.lockFF.unlock();
                }
              } finally {
                LocalClass_m_om2.this.lockG.unlock();
              }
            }
          }
          
          class LC3000 {
            class LC4000 {
              @RequiresLock("LocalClass_m_om2.this:G, Middle.this:FF, Outer.this:F")
              public MoreLocalClass doStuff1() {
                /* The immediately enclosing instance is LocalClass_m_om2.this
                 * 
                 * "LocalClass_m_om2" is the innermost lexically enclosing class of "MoreLocalClass"
                 * "LocalClass_m_om2" is the 2nd lexically enclosing class of the class in which the
                 * instance creation expression appears.
                 * 
                 * Requires this:G, Middle.this:FF, Outer.this:F
                 */
                return new MoreLocalClass();
              }

              public MoreLocalClass doStuff2() {
                /* The immediately enclosing instance is LocalClass_m_om2.this
                 * 
                 * "LocalClass_m_om2" is the innermost lexically enclosing class of "MoreLocalClass"
                 * "LocalClass_m_om2" is the 2nd lexically enclosing class of the class in which the
                 * instance creation expression appears.
                 * 
                 * Requires this:G, Middle.this:FF, Outer.this:F
                 */
                LocalClass_m_om2.this.lockG.lock();
                try {
                  Middle.this.lockFF.lock();
                  try {
                    Outer.this.lockF.lock();
                    try {
                      return new MoreLocalClass();
                    } finally {
                      Outer.this.lockF.unlock();
                    }
                  } finally {
                    Middle.this.lockFF.unlock();
                  }
                } finally {
                  LocalClass_m_om2.this.lockG.unlock();
                }
              }
            }
          }
        }
      }
      
      /*
       * The immediately enclosing instance of lc1 is Middle.this == this.
       * 
       * Requires FF -- F is not resolvable
       */
      final LocalClass_m_om2 lc1 = new LocalClass_m_om2();
      lc1.lockG.lock();
      try {
        // Requires G --- FF and F are not resolvable
        lc1.doStuff();
      } finally {
        lc1.lockG.unlock();
      }
    }
  }
}
