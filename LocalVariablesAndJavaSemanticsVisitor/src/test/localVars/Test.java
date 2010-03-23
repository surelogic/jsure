package test.localVars;

public class Test extends Foo {
  /* Local variables: s1 */
  static {
    Object s1 = null;
  }

  private Object field = new Object() {
    {
      Object v1 = null;

      new Object() {
        private Object field2 = new Object() {
          {
            Object v2 = null;
          }
        };

        {
          Object v3 = null;
        }
      };
    }
  };

  /* Local variables: flag, zzz, v1, v2, v3, v12, v13 */
  protected Test(boolean flag) {
    super(new Object() { { Object zzz = null; } });
  }

  /* Local variables: v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13.
   *
   * The local variable list must never include "neverIncludeMe"
   */
  protected Test() {
    Object v4 = null;

    Object v5 = new Foo(new Object() { { Object v6 = null; } }) {
      {
        Object v7 = null;

        new Object() {
          {
            Object v8 = null;
          }
        };
      }

      private Object field3 = new Object() {
        {
          Object v9 = null;
        }
        {
          Object v10 = null;
        }

        void m() {
          Object neverIncludeMe = null;
        }
      };
    };

    Object v11 = null;
  }

  {
    new Object() {
      {
        Object v12 = null;
      }
    };

    Object v13 = null;
  }
}

class Foo extends Object {
  public Foo() {
    super();
  }

  public Foo(Object o) {
    super();
  }
}
