/*
 * Copyright (c) 2005 Brian Goetz and Tim Peierls
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 *
 * Any republication or derived work distributed in source code form
 * must include this copyright and license notice.
 * 
 * 
 * Copyright (c) 2015 SureLogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field to which this annotation is applied can only be accessed
 * when holding a particular lock, which may be a built-in (synchronization)
 * lock, or may be an explicit <code>java.util.concurrent</code> lock.
 * <p>
 * The argument determines which lock guards the annotated field:
 * <ul>
 * <li>
 * <tt>this</tt>: The intrinsic lock of the object in whose class the annotated
 * field is defined.  The field must be mutable and non-<code>static</code>.  
 * </li>
 * <li>
 * <em>field-name</em>: The lock object is referenced by the (instance or
 * static) field specified by <em>field-name</em>. The field <em>field-name</em>
 * must be {@code final} and, if non-<code>static</code>, be declared in the same class as,
 * or be a visible
 * field declaration in a superclass of, the class in which the annotated field 
 * is declared. The annotated field must be mutable. 
 * If the annotated field is <code>static</code> the
 * referenced field must also be <code>static</code>.  The referenced field 
 * must have a reference type.</li>
 * <li>
 * <tt>itself</tt>: For reference fields only (meaningless when applied to a
 * field of a primitive type); the object to which the
 * field refers. The field must be {@code final} and contain a unique reference
 * to the referenced object.  The lock on the object referenced by the 
 * annotated field must be held before accessing the state of the referenced
 * object.</li>
 * <li>
 * <em>method-name</em><tt>()</tt>: The lock object is returned by calling the
 * named nil-ary method. If the named method is non-<code>static</code>, it must be declared in
 * the same class (or be a visible method declaration in a superclass) as the
 * field on which this annotation appears. Note that
 * <em>method-name</em><tt>()</tt> is trusted to return a consistent non-<code>null</code> object to
 * guard the annotated field&mdash;so take particular care with its
 * implementation. The annotated field must be
 * mutable.  The named method must return a reference type.  If the annotated
 * field is <code>static</code> the named method must also be <code>static</code>.
 * </li>
 * <li>
 * <em>class-name</em><tt>.class</tt>: The {@link Class} object for the
 * specified class should be used as the lock object. The 
 * annotated field must be mutable.</li>
 * <li>
 * <em>class-name</em><tt>.</tt><em>field-name</em>: The lock object is
 * reference by the static field specified by <em>class-name</em><tt>.</tt>
 * <em>field-name</em>. The static field that references the lock must be
 * {@code final}. The annotated field must be
 * mutable.</li>
 * <li>
 * <em>class-name</em><tt>.this</tt>: For inner classes, it may be necessary to
 * disambiguate 'this'; the <em>class-name</em><tt>.this</tt> designation allows
 * you to specify which 'this' reference is intended. The annotated
 * field must be mutable and non-<code>static</code>.
 * </li>
 * </ul>
 * 
 * <h3>Semantics:</h3>
 * 
 * <i>Field:</i> The program must be holding the specified lock when the
 * annotated field is read or written.
 * 
 * <h3>Examples:</h3>
 * 
 * The {@code Point} class below is considered thread-safe.
 * 
 * <pre>
 * public class ex1 {
 * 
 *   &#064;GuardedBy(&quot;this&quot;)
 *   double xPos = 1.0;
 * 
 *   &#064;GuardedBy(&quot;this&quot;)
 *   double yPos = 1.0;
 * 
 *   &#064;GuardedBy(&quot;itself&quot;)
 *   static final List&lt;ex1&gt; memo = new ArrayList&lt;ex1&gt;();
 * 
 *   public void move(double slope, double distance) {
 *     synchronized (this) {
 *       xPos = xPos + ((1 / slope) * distance);
 *       yPos = yPos + (slope * distance);
 *     }
 *   }
 * 
 *   public static void memo(ex1 value) {
 *     synchronized (memo) {
 *       memo.add(value);
 *     }
 *   }
 * }
 * </pre>
 * 
 * <p>If the named field or method in an annotation has the type 
 * {@link java.util.concurrent.locks.Lock} or {@link java.util.concurrent.locks.ReadWriteLock}
 * then the lock must be acquired according to the rules of those classes.  For
 * example, consider the highly contrived class below:
 * 
 * <pre>
 * public class ex2 {
 *   private final Object lock = new Object();
 *   private final Lock jucLock = new ReentrantLock();
 *   private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
 *   
 *   &#064;GuardedBy(&quot;lock&quot;)
 *   private int x;
 *   
 *   &#064;GuardedBy(&quot;jucLock&quot;)
 *   private int y;
 *   
 *   &#064;GuardedBy(&quot;rwLock&quot;)
 *   private int z;
 *   
 *   &#064;Unique(&quot;return&quot;)
 *   public ex2() {}
 *   
 *   public void set(int a, int b, int c) {
 *     synchronized (lock) { 
 *       x = a;
 *     }
 *     
 *     jucLock.lock(); 
 *     try { 
 *      y = b;
 *     } finally {
 *       jucLock.unlock();
 *     }
 *     
 *     rwLock.writeLock().lock();
 *     try {
 *       z = c;
 *     } finally {
 *       rwLock.writeLock().unlock();
 *     }
 *   }
 * }
 * </pre>
 * 
 * <p><b>Constructor annotation to support locking policies:</b> To support the
 * {@link GuardedBy} annotation, a {@link Unique} or {@link Borrowed} annotation
 * is needed on each constructor to assure that the object being constructed is
 * confined to the thread that invoked {@code new}. A second less common
 * approach, using effects, is described below.
 * <p>
 * Annotating <code>&#64;Unique("return")</code> on a constructor is defined to
 * be equivalent to annotating <code>&#64;Borrowed("this")</code>. Either of
 * these annotations indicate that the object being constructed is not aliased
 * during construction, which implies that the reference "returned" by the
 * {@code new} expression that invokes the constructor is unique. Which
 * annotation is preferred, <code>&#64;Unique("return")</code> or
 * <code>&#64;Borrowed("this")</code>, is a matter of programmer preference.
 * 
 * <pre>
 * public class Example {
 * 
 *   &#064;GuardedBy(&quot;this&quot;) int y;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   public Example(int y) {
 *     this.y = y;
 *   }
 *   ...
 * }
 * </pre>
 * 
 * It is also possible to support the {@link GuardedBy} assertion with effects (
 * {@link Starts} and {@link RegionEffects}) annotations on a constructor
 * instead of using {@link Unique} or {@link Borrowed}. This is useful if the
 * constructor aliases the receiver into a field within the newly constructed
 * object. This situation is uncommon in real-world Java code. In the example
 * below if an explicit lock object is not provided to the constructor then
 * {@code this} is used and, hence, aliased into the field {@code lock}. In this
 * code <code>&#64;Unique("return")</code> cannot be verified so the effects
 * annotations are used on the constructor instead.
 * 
 * <pre>
 * public class Example {
 * 
 *   private final Object lock;
 *   &#064;GuardedBy(&quot;lock&quot;)
 *   private int y;
 * 
 *   &#064;Starts(&quot;nothing&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Example(int y, Object lock) {
 *     this.y = y;
 *     if (lock == null)
 *       this.lock = this;
 *     else
 *       this.lock = lock;
 *   }
 * 
 *   public int getY() {
 *     synchronized (lock) {
 *       return y;
 *     }
 *   }
 * 
 *   public void setY(int value) {
 *     synchronized (lock) {
 *       y = value;
 *     }
 *   }
 * }
 * </pre>
 * 
 * The scoped promise {@link Promise} can be used if the constructor is implicit
 * (i.e., generated by the compiler). It has the ability to place promises on
 * implicit and explicit constructors.
 * 
 * <pre>
 * &#064;Promise(&quot;@Unique(return) for new(**)&quot;)
 * public class Example {
 *   &#064;GuardedBy(&quot;this&quot;) int x = 1;
 *   &#064;GuardedBy(&quot;this&quot;) int y = 1;
 *   ...
 * }
 * </pre>
 * 
 * <a name="note"><i>Implementation note:</i></a> This annotation is derived
 * from the <code>&#064;GuardedBy</code> proposed by Brian Goetz and Tim Peierls in
 * the book <i>Java Concurrency in Practice</i> (Addison-Wesley 2006) we have
 * simply adapted it to have semantics as a promise. Further, the annotation in
 * {@code net.jcip.annotations} may be used instead of this one with the same
 * tool behavior.
 * 
 * @see RegionLock
 * @see RequiresLock
 * @see ThreadSafe
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GuardedBy {
  /**
   * <p>
   * The value of this attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = &quot;this&quot; / &quot;itself&quot; / class / field / method
   * 
   * class = qualifiedName &quot;.&quot; (&quot;class&quot; / &quot;this&quot;)
   * 
   * field = qualfiedName
   * 
   * method = qualifiedName &quot;()&quot;
   * 
   * qualifiedName = IDENTIFIER *(&quot;.&quot; IDENTIFIER) : IDENTIFIER
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
