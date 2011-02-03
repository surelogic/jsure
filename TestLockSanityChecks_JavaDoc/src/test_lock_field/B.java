package test_lock_field;

public class B extends A {
  final Object fieldFromB = new Object();
  Object badFieldFromB = new Object();
}
