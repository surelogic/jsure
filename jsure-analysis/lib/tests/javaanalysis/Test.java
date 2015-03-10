public class Test {
  public static void testwhile() {
    int u,v,w,x,y,z;
    u = v = w = x = y = z = 0;
  loop1:
    while (w < 1) {
      v = x + 2;
    loop2:
      {
        if (y == 3) {
	  w = z;
	  break loop1;
	} else {
	  w = v + 4;
	  break loop2;
	}
	w = u + 5;
      }
    }
    z = 6 + z;
  }

  public static void testtry() {
    int v,w,x,y,z;
    v = w = x = y = z = 10;
    try {
      v = 11;
      if (w == 12) {
	v = x + 13;
	throw new WeirdException();
      }
      v = y + 14;
    } catch (WeirdException e) {
      v = v + 15;
    } finally {
      v = z + 16;
    }
    v = 17 + w;
  }
  
  public static void testswitch() {
    int v,w,x,y,z;
    v = w = x = y = z = 20;
    switch (v+21) {
    case 22:
      v = 23 + x;
    case 24:
      v = 25 + y;
      break;
      v = 26 + z;
    default:
      v = 27 + w;
    }
    v = 28 + v;
  }
  
  public static int testfor() {
    int v,w,x,y,z;
    v = w = x = y = z = 30;
    try {
      for (v = 31; v < 32 && w < 33; x += 34) {
        y /= 35;
      }
    } catch (ArithmeticException e) {
      z = z + 36;
    }
    return v + 37;
  }
}

class WeirdException extends Throwable {
}