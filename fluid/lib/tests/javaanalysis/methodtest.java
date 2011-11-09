public class methodtest {
  int x;

  static void m( methodtest a ) {
    methodtest b = a;
    a = new methodtest();
    b.x = 5;
  }

  public static void main( String[] args ) {
    methodtest x = new methodtest();
    m( x );
  }
}
