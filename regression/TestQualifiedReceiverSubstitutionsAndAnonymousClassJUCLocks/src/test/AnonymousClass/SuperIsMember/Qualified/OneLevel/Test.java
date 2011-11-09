package test.AnonymousClass.SuperIsMember.Qualified.OneLevel;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
//import com.surelogic.RegionEffects;
import com.surelogic.Assume;
import com.surelogic.Assumes;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;
import com.surelogic.Starts;

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class Test {
  public final Lock lockT1 = new ReentrantLock();
  public final Lock lockT2 = new ReentrantLock();
  public int t1;
  public int t2;
  
  
  
  @Region("RegionG extends Instance")
  @RegionLocks({
    @RegionLock("F is lockF protects f"),
    @RegionLock("G is lockG protects RegionG")
  })
  public class Super {
    public final Lock lockF = new ReentrantLock();
    public final Lock lockG = new ReentrantLock();
    public int f;
    
    @RequiresLock("Test.this:T1")
    @Borrowed("this")
//    @RegionEffects("writes Test.this:t1")
    public Super() {
      // do stuff
      Test.this.t1 = 5;
      this.f = 10;
    }
  }

  
  
  @RequiresLock("other:T1, other:T2")
  public Test(final Test other) {
    /* The immediately enclosing instance of s1 is "this" (a Test object)
     * The immediately enclosing instance with respect to Super is other.
     * 
     * Writes other.t1, other.t2
     */
    final Super s1 = other. new Super() { 
      @InRegion("RegionG")
      private int g = 10; // unprotected -- no way to protect it

      {
        lockG.lock();
        try {
          g = 100; // protected
        } finally {
          lockG.unlock();
        }
      }
      
      {
        Test.this.t1 += 1; // protected, outer context holds lock
        Test.this.t2 += 1; // protected, outer context holds lock
        needyMethod(); // protected, outer context holds lock
      }

      @RequiresLock("Test.this:T2")
      private void needyMethod() {
      }

      public void callsNeedyMethod1() {
        Test.this.lockT2.lock();
        try {
          needyMethod();
        } finally {
          Test.this.lockT2.unlock();
        }
      }

      @RequiresLock("Test.this:T2")
      public void callsNeedyMethod2() {
        needyMethod();
      }
    };
  }
  
  @RequiresLock("other:T1, other:T2")
  public void stuff(final Test other) {
    /*
     * The immediately enclosing instance of s2 is "this" (a Test object) The
     * immediately enclosing instance with respect to Super is other.
     * 
     * Writes other.t1, other.t2
     */
    final Super s2 = other.new Super() {
      @InRegion("RegionG")
      private int g = 10; // unprotected

      {
        lockG.lock();
        try {
          g = 100; // protected
        } finally {
          lockG.unlock();
        }
      }

      {
        Test.this.t1 += 1; // protected, outer context holds lock
        Test.this.t2 += 1; // protected, outer context holds lock
        needyMethod(); // protected, outer context holds lock
      }

      @RequiresLock("Test.this:T2")
      @Borrowed("this")
      @Starts("nothing")
//      @RegionEffects("none")
      private void needyMethod() {
      }

      @Borrowed("this")
      @Starts("nothing") 
//      @RegionEffects("none")
      public void callsNeedyMethod1() {
        Test.this.lockT2.lock();
        try {
          needyMethod();
        } finally {
          Test.this.lockT2.unlock();
        }
      }

      @RequiresLock("Test.this:T2")
      @Borrowed("this")
      @Starts("nothing")
//      @RegionEffects("none")
      public void callsNeedyMethod2() {
        needyMethod();
      }
    };
  }
  
  @Assumes({
  	@Assume("@Starts(nothing) for lock() in Lock in java.util.concurrent.locks"),
  	@Assume("@Starts(nothing) for unlock() in Lock in java.util.concurrent.locks") })
  public void stuff2(final Test other) {
    other.lockT1.lock();
    try {
      other.lockT2.lock();
      try {
        /*
         * The immediately enclosing instance of s2 is "this" (a Test object) The
         * immediately enclosing instance with respect to Super is other.
         * 
         * Writes other.t1, other.t2
         */
        final Super s2 = other.new Super() {
          @InRegion("RegionG")
          private int g = 10; // unprotected

          {
            lockG.lock();
            try {
              g = 100; // protected
            } finally {
              lockG.unlock();
            }
          }

          {
            Test.this.t1 += 1; // protected, outer context holds lock
            Test.this.t2 += 1; // protected, outer context holds lock
            needyMethod(); // protected, outer context holds lock
          }

          @RequiresLock("Test.this:T2")
          @Borrowed("this")
          @Starts("nothing")
//          @RegionEffects("none")
          private void needyMethod() {
          }

          @Borrowed("this")
          @Starts("nothing")
//          @RegionEffects("none")
          public void callsNeedyMethod1() {
            Test.this.lockT2.lock();
            try {
              needyMethod();
            } finally {
              Test.this.lockT2.unlock();
            }
          }

          @RequiresLock("Test.this:T2")
          @Borrowed("this")
          @Starts("nothing")
//          @RegionEffects("none")
          public void callsNeedyMethod2() {
            needyMethod();
          }
        };
      } finally {
        other.lockT2.unlock();
      }
    } finally {
      other.lockT1.unlock();
    }
  }

  
  
  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Lock lockM1 = new ReentrantLock();
    public int m1;
    
    @RequiresLock("other:T1, other:T2")
//    @RegionEffects("writes other:t1, other:t2")
    public Middle1(final Test other) {
      /*
       * The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is other.
       * 
       * Writes other.t1, other.t2, this.m1
       */
      lockM1.lock();
      try {
        final Super s3 = other.new Super() {
          @InRegion("RegionG")
          private int g = 10; // unprotected

          {
            lockG.lock();
            try {
              g = 100; // protected
            } finally {
              lockG.unlock();
            }
          }

          {
            Middle1.this.m1 = 10; // protected, outer context holds lock
            Test.this.t1 += 1; // protected, outer context holds lock
            Test.this.t2 += 1; // protected, outer context holds lock
            needyMethod(); // protected, outer context holds lock
          }

          @RequiresLock("Middle1.this:M1, Test.this:T2")
//          @RegionEffects("none")
          private void needyMethod() {
          }

          public void callsNeedyMethod1() {
            Middle1.this.lockM1.lock();
            try {
              Test.this.lockT2.lock();
              try {
                needyMethod();
              } finally {
                Test.this.lockT2.unlock();
              }
            } finally {
              Middle1.this.lockM1.unlock();
            }
          }

          @RequiresLock("Middle1.this:M1, Test.this:T2")
          public void callsNeedyMethod2() {
            needyMethod();
          }
        };
      } finally {
        lockM1.unlock();
      }
    }

    @RequiresLock("other:T1, other:T2, this:M1")
    public void stuff(final Test other) {
      /*
       * The immediately enclosing instance of s4 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is other.
       * 
       * Writes other.t1, other.t2, this.m1
       */
      final Super s4 = other.new Super() {
        @InRegion("RegionG")
        private int g = 10; // unprotected

        {
          lockG.lock();
          try {
            g = 100; // protected
          } finally {
            lockG.unlock();
          }
        }

        {
          Middle1.this.m1 = 10; // protected, outer context holds lock
          Test.this.t1 += 1; // protected, outer context holds lock
          Test.this.t2 += 1; // protected, outer context holds lock
          needyMethod(); // protected, outer context holds lock
        }

        @RequiresLock("Middle1.this:M1, Test.this:T2")
//        @RegionEffects("none")
        private void needyMethod() {
        }

        public void callsNeedyMethod1() {
          Middle1.this.lockM1.lock();
          try {
            Test.this.lockT2.lock();
            try {
              needyMethod();
            } finally {
              Test.this.lockT2.unlock();
            }
          } finally {
            Middle1.this.lockM1.unlock();
          }
        }

        @RequiresLock("Middle1.this:M1, Test.this:T2")
        public void callsNeedyMethod2() {
          needyMethod();
        }
      };
    }

    public void stuff2(final Test other) {
      other.lockT1.lock();
      try {
        other.lockT2.lock();
        try {
          this.lockM1.lock();
          try {
            /*
             * The immediately enclosing instance of s4 is "this" (a Middle1 object)
             * The immediately enclosing instance with respect to Super is other.
             * 
             * Writes other.t1, other.t2, this.m1
             */
            final Super s4 = other.new Super() {
              @InRegion("RegionG")
              private int g = 10; // unprotected

              {
                lockG.lock();
                try {
                  g = 100; // protected
                } finally {
                  lockG.unlock();
                }
              }

              {
                Middle1.this.m1 = 10; // protected, outer context holds lock
                Test.this.t1 += 1; // protected, outer context holds lock
                Test.this.t2 += 1; // protected, outer context holds lock
                needyMethod(); // protected, outer context holds lock
              }

              @RequiresLock("Middle1.this:M1, Test.this:T2")
//              @RegionEffects("none")
              private void needyMethod() {
              }

              public void callsNeedyMethod1() {
                Middle1.this.lockM1.lock();
                try {
                  Test.this.lockT2.lock();
                  try {
                    needyMethod();
                  } finally {
                    Test.this.lockT2.unlock();
                  }
                } finally {
                  Middle1.this.lockM1.unlock();
                }
              }

              @RequiresLock("Middle1.this:M1, Test.this:T2")
              public void callsNeedyMethod2() {
                needyMethod();
              }
            };
          } finally {
            this.lockM1.unlock();
          }
        } finally {
          other.lockT2.unlock();
        }
      } finally {
        other.lockT1.unlock();
      }
    }

  
  
    @RegionLock("M2 is lockM2 protects m2")
    public class Middle2 {
      public final Lock lockM2 = new ReentrantLock();
      public int m2;
      
      @RequiresLock("other:T1, other:T2, Middle1.this:M1")
      public Middle2(final Test other) {
        /*
         * The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is other.
         * 
         * Writes other.t1, other.t2, any(Middle1).m1, this.m2
         */
        lockM2.lock();
        try {
          final Super s5 = other.new Super() {
            @InRegion("RegionG")
            private int g = 10; // unprotected

            {
              lockG.lock();
              try {
                g = 100; // protected
              } finally {
                lockG.unlock();
              }
            }

            {
              Middle2.this.m2 = 20; // protected, outer context holds lock
              Middle1.this.m1 = 10; // M1 not resolvable
              Test.this.t1 += 1; // protected, outer context holds lock
              Test.this.t2 += 1; // protected, outer context holds lock
              needyMethod(); // M1 not resolvable
            }

            @RequiresLock("Middle2.this:M2, Middle1.this:M1, Test.this:T2")
//            @RegionEffects("none")
            private void needyMethod() {
            }

            public void callsNeedyMethod1() {
              Middle2.this.lockM2.lock();
              try {
                Middle1.this.lockM1.lock();
                try {
                  Test.this.lockT2.lock();
                  try {
                    needyMethod();
                  } finally {
                    Test.this.lockT2.unlock();
                  }
                } finally {
                  Middle1.this.lockM1.unlock();
                }
              } finally {
                Middle2.this.lockM2.unlock();
              }
            }

            @RequiresLock("Middle2.this:M2, Middle1.this:M1, Test.this:T2")
            public void callsNeedyMethod2() {
              needyMethod();
            }
          };
        } finally {
          lockM2.unlock();
        }
      }

      @RequiresLock("other:T1, other:T2, Middle1.this:M1, M2")
      public void stuff(final Test other) {
        /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is other.
         * 
         * Writes other.t1, other.t2, any(Middle1).m1, this.m2
         */
        final Super s6 = other.new Super() {
          @InRegion("RegionG")
          private int g = 10; // unprotected

          {
            lockG.lock();
            try {
              g = 100; // protected
            } finally {
              lockG.unlock();
            }
          }

          {
            Middle2.this.m2 = 20; // protected, outer context holds lock
            Middle1.this.m1 = 10; // M1 not resolvable
            Test.this.t1 += 1; // protected, outer context holds lock
            Test.this.t2 += 1; // protected, outer context holds lock
            needyMethod(); // M1 not resolvable
          }

          @RequiresLock("Middle2.this:M2, Middle1.this:M1, Test.this:T2")
//          @RegionEffects("none")
          private void needyMethod() {
          }

          public void callsNeedyMethod1() {
            Middle2.this.lockM2.lock();
            try {
              Middle1.this.lockM1.lock();
              try {
                Test.this.lockT2.lock();
                try {
                  needyMethod();
                } finally {
                  Test.this.lockT2.unlock();
                }
              } finally {
                Middle1.this.lockM1.unlock();
              }
            } finally {
              Middle2.this.lockM2.unlock();
            }
          }

          @RequiresLock("Middle2.this:M2, Middle1.this:M1, Test.this:T2")
          public void callsNeedyMethod2() {
            needyMethod();
          }
        };
      }
      
      public void stuff2(final Test other) {
        other.lockT1.lock();
        try {
          other.lockT2.lock();
          try {
            lockM1.lock();
            try {
              lockM2.lock();
              try {
                /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
                 * The immediately enclosing instance with respect to Super is other.
                 * 
                 * Writes other.t1, other.t2, any(Middle1).m1, this.m2
                 */
                final Super s6 = other.new Super() {
                  @InRegion("RegionG")
                  private int g = 10; // unprotected

                  {
                    lockG.lock();
                    try {
                      g = 100; // protected
                    } finally {
                      lockG.unlock();
                    }
                  }

                  {
                    Middle2.this.m2 = 20; // protected, outer context holds lock
                    Middle1.this.m1 = 10; // M1 not resolvable
                    Test.this.t1 += 1; // protected, outer context holds lock
                    Test.this.t2 += 1; // protected, outer context holds lock
                    needyMethod(); // M1 not resolvable
                  }

                  @RequiresLock("Middle2.this:M2, Middle1.this:M1, Test.this:T2")
//                  @RegionEffects("none")
                  private void needyMethod() {
                  }

                  public void callsNeedyMethod1() {
                    Middle2.this.lockM2.lock();
                    try {
                      Middle1.this.lockM1.lock();
                      try {
                        Test.this.lockT2.lock();
                        try {
                          needyMethod();
                        } finally {
                          Test.this.lockT2.unlock();
                        }
                      } finally {
                        Middle1.this.lockM1.unlock();
                      }
                    } finally {
                      Middle2.this.lockM2.unlock();
                    }
                  }

                  @RequiresLock("Middle2.this:M2, Middle1.this:M1, Test.this:T2")
                  public void callsNeedyMethod2() {
                    needyMethod();
                  }
                };
              } finally {
                lockM2.unlock();
              }
            } finally {
              lockM1.unlock();
            }
          } finally {
            other.lockT2.unlock();
          }
        } finally {
          other.lockT1.unlock();
        }
      }
    }
  }
}
