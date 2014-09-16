package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declares that the parameter is the named lock of another parameter. For
 * example,
 * 
 * <pre>
 *   public void method(final C p1, final @IsLock(&quot;p1.Lock&quot;) Object lock) { ... }
 * </pre>
 * 
 * declares that parameter {@code lock} can be used when the lock {@code Lock}
 * for the object referenced by {@code p1} is required. It is an error if the
 * annotated field is not {@code final}. The parameter named in the annotation
 * must also be {@code final}.
 * <p>
 * <em>Analysis does not currently make use of this annotation.</em>
 */
@Documented
@Target(ElementType.PARAMETER)
public @interface IsLock {
  /**
   * The name of the lock represented by the parameter. The value of this
   * attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = simpleLockSpecification
   * 
   * simpleLockSpecification = simpleLockName [&quot;.&quot; (&quot;readLock&quot; / &quot;writeLock&quot;)]
   * 
   * simpleLockName = IDENTIFIER  ; Lock from the receiver (same as &quot;this:IDENTIFIER&quot;)
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
