public class conflicttest {
  //@  region A, B;
  //@ static region C, D;

  int f1, f2;
  static int s1, s2;

  {
    //@ inRegion( this.f1, A );
    //@ inRegion( this.f2, B );
  }

  static
  {
    //@ inRegion( conflicttest.s1, C );
    //@ inRegion( conflicttest.s2, D );
  }

  public conflicttest() { f1 = f2 = 0; }

  public static void m( int a1, conflicttest a2 ) {
    int l1;
    conflicttest l2;
    int[] array = new int[5];

    l1 = 1;
    a1 = 2;

    s1 = l1 + a1;
    s2 = a1 - l1;

    array[0] = a2.f1;
    array[1] = a2.f2;

    a2 = new conflicttest();
    l2 = new conflicttest();
    l2.f2 = a1;
    l2.f2 = s2;

    l2 = a2;
    a2.f1 = 1;
    l2.f1 = 1;
  }
}
