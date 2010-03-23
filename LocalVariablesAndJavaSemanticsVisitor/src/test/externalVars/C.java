package test.externalVars;

public class C {
  Object ff = new Object() { // ACE #0
    public void zz(Object p) {
      p.toString();
    }
  };

  public void m() {
    final Object fromM = null;

    Object o = new Object() { // ACE#1
      final Object fff = new Object() { // ACE#2
        public void nnn() {
          // External variable is fromM
          fromM.toString();
        }
      };

      public void z() {
        // External variable is fromM
        fromM.toString();
      }
    };

    new Object() { // ACE#3
      final Object f = new Object() { // ACE#4
        public void nn() {
          // External variable is fromM
          fromM.toString();
        }
      };

      {
        final Object alsoFromM = null;

        new Object() { // ACE#5
          public void n() {
            // External variables are alsoFromM and fromM
            alsoFromM.toString();
            fromM.toString();
          }
        };
      }
    };
  }
}
