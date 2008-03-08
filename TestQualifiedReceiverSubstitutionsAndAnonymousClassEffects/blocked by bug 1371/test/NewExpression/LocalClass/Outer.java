package test.NewExpression.LocalClass;

import com.surelogic.RegionEffects;

public class Outer {
  public int f = 10;

//  @RegionEffects("writes this:f")
//  public void outerMethod() {
//    class LocalClass {
//      public int g;
//      
//      @RegionEffects("writes Outer.this:f")
//      public LocalClass() {
//        Outer.this.f = 10;
//        this.g = 10;
//      }
//    }
//    
//    /*
//     * The immediately enclosing instance of lc1 is Other.this == this.
//     * 
//     * Writes this.f
//     */
//    final LocalClass lc1 = new LocalClass();
//    
//    
//    class LC2 {
//      public int h;
//      
//      @RegionEffects("writes Outer.this:f")
//      public LocalClass doStuff() {
//        /* The immediately enclosing instance is Outer.this
//         * 
//         * Writes Outer.this.f
//         */
//        return new LocalClass();
//      }
//    }
//  }

  @RegionEffects("none")
  public void outerMethod2() {
    class LocalClass {
      public int g;
      
      @RegionEffects("writes Outer.this:f")
      public LocalClass() {
        Outer.this.f = 10;
        this.g = 10;
      }
      
      @RegionEffects("writes this:g, Outer.this:f")
      public void doStuff() {
        class MoreLocalClass {
          public int h;
          
          @RegionEffects("writes LocalClass.this:g, Outer.this:f")
          public MoreLocalClass() {
            this.h = 10;
            LocalClass.this.g = 10;
            Outer.this.f = 10;
          }
        }
        
        /* The immediately enclosing instance is LocalClass.this == this
         * 
         * Writes this:g, Outer.this:f
         */
        final MoreLocalClass m = new MoreLocalClass();
      }
    }
    
//    /*
//     * The immediately enclosing instance of lc1 is Other.this == this.
//     * 
//     * Writes this.f
//     */
//    final LocalClass lc1 = new LocalClass();
//    // Writes lc1.g, any(Outer).f
//    lc1.doStuff();
  }

  
  
  
//  public class Middle {
//    public int ff = 100;
//    
//    @RegionEffects("none")
//    public void outerMethod() {
//      class LocalClass {
//        public int g;
//        
//        @RegionEffects("writes Outer.this:f, writes Middle.this:ff")
//        public LocalClass() {
//          Outer.this.f = 10;
//          Middle.this.ff = 100;
//          this.g = 10;
//        }
//      }
//      
//      /*
//       * The immediately enclosing instance of lc1 is Middle.this == this.
//       * 
//       * Writes Outer.this.f, this.ff
//       */
//      final LocalClass lc1 = new LocalClass();
//      
//      
//      
//      class LC2 {
//        public int h;
//        
//        @RegionEffects("writes Outer.this:f, writes Middle.this:ff")
//        public LocalClass doStuff() {
//          /* The immediately enclosing instance is Middle.this
//           * 
//           * Writes Outer.this.f, this.ff
//           */
//          return new LocalClass();
//        }
//      }
//    }
//
//    @RegionEffects("none")
//    public void outerMethod2() {
//      class LocalClass {
//        public int g;
//        
//        @RegionEffects("writes Outer.this:f, Middle:this.ff")
//        public LocalClass() {
//          Outer.this.f = 10;
//          Middle.this.ff = 10;
//          this.g = 10;
//        }
//        
//        @RegionEffects("writes this:g, Middle.this:ff, Outer.this:f")
//        public void doStuff() {
//          class MoreLocalClass {
//            public int h;
//            
//            @RegionEffects("Writes LocalClass.this:g, Middle.this:ff, Outer.this:f")
//            public MoreLocalClass() {
//              this.h = 10;
//              LocalClass.this.g = 10;
//              Middle.this.ff = 10;
//              Outer.this.f = 10;
//            }
//          }
//          
//          /* The immediately enclosing instance is LocalClass.this == this
//           * 
//           * Writes this:g, Middle.this:ff, Outer.this:f
//           */
//          final MoreLocalClass m = new MoreLocalClass();
//        }
//      }
//      
//      /*
//       * The immediately enclosing instance of lc1 is Middle.this == this.
//       * 
//       * Writes this.ff, any(Outer).f
//       */
//      final LocalClass lc1 = new LocalClass();
//      // Writes lc1.g, any(Middle).ff, any(Outer).f
//      lc1.doStuff();
//    }
//  }
}
