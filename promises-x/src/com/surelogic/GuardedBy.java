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
 * The field or method to which this annotation is applied can only be accessed
 * when holding a particular lock, which may be a built-in (synchronization)
 * lock, or may be an explicit java.util.concurrent.Lock.
 * <p>
 * The argument determines which lock guards the annotated field or method:
 * <ul>
 * <li>
 * <tt>this</tt>: The intrinsic lock of the object in whose class the field is
 * defined. When this annotation is applied to a field, the field must be
 * mutable.</li>
 * <li>
 * <em>class-name</em><tt>.this</tt>: For inner classes, it may be necessary to
 * disambiguate 'this'; the <em>class-name</em><tt>.this</tt> designation allows
 * you to specify which 'this' reference is intended. When this annotation is
 * applied to a field, the field must be mutable.</li>
 * <li>
 * <tt>itself</tt>: For reference fields only (meaningless when applied to a
 * method or a field of a primitive type); the object to which the field refers.
 * The field must be {@code final} and contain a unique reference to the
 * referenced object.</li>
 * <li>
 * <em>field-name</em>: The lock object is referenced by the (instance or
 * static) field specified by <em>field-name</em>. The field that references the
 * lock must be {@code final} and be declared in the same class (or be a visible
 * field declaration in a superclass) as the method or field on which this
 * annotation appears. When this annotation is applied to a field, the field
 * must be mutable. In the case that both a <em>field-name</em> and a
 * <em>lock-name</em> (see below) have the same name, the binding is to the
 * <em>lock-name</em>.</li>
 * <li>
 * <em>class-name</em><tt>.</tt><em>field-name</em>: The lock object is
 * reference by the static field specified by <em>class-name</em><tt>.</tt>
 * <em>field-name</em>. The static field that references the lock must be
 * {@code final} . When this annotation is applied to a field, the field must be
 * mutable.</li>
 * <li>
 * <em>method-name</em><tt>()</tt>: The lock object is returned by calling the
 * named nil-ary method. The method that returns the lock must be declared in
 * the same class (or be a visible method declaration in a superclass) as the
 * method or field on which this annotation appears. Note that
 * <em>method-name</em><tt>()</tt> is trusted to return a consistent object to
 * guard the annotated field or method&mdash;take care with its implementation.
 * When this annotation is applied to a field, the field must be mutable.</li>
 * <li>
 * <em>class-name</em><tt>.class</tt>: The {@link Class} object for the
 * specified class should be used as the lock object. When this annotation is
 * applied to a field, the field must be mutable.</li>
 * <li>
 * <em>lock-name</em>: For methods only (meaningless when applied to a field).
 * The referenced <em>lock-name</em>, which is defined by {@link RegionLock}
 * annotation must be held when invoking the method. In this use,
 * <tt>@GuardedBy("</tt><em>lock-name</em><tt>")</tt> is equivalent to the
 * annotation <tt>@RequiresLock("</tt><em>lock-name</em><tt>")</tt>. In the case
 * that both a <em>field-name</em> and a <em>lock-name</em> have the same name,
 * the binding is to the <em>lock-name</em>.</li>
 * </ul>
 * <p>
 * When this annotation is applied to a method a comma separated list may be
 * provided if more than one lock needs to be held, e.g.
 * <tt>@GuardedBy("this, C.class")</tt>.
 * <p>
 * This annotation interacts with {@link RegionLock} defined locks when applied
 * to methods. As noted above, holding <em>lock-name</em> may be expressed as a
 * prerequisite to invoking the method. Also, non-<em>lock-name</em> lock
 * preconditions expressed by {@link GuardedBy} should be resolved to named
 * locks as applicable. This is best illustrated by example. In the listing
 * below the lock preconditions on <tt>m1()</tt>, <tt>m2()</tt>, and
 * <tt>m3()</tt> are semantically equivalent. In particular, a lock precondition
 * on <tt>this</tt> resolves to both named locks: <tt>l1</tt> and <tt>l2</tt>
 * 
 * <pre>
 * &#064;RegionLocks({ @RegionLock(&quot;l1 is this protects f1&quot;), @RegionLock(&quot;l2 is this protects f2&quot;) })
 * public class C {
 * 
 *   int f1;
 * 
 *   int f2;
 * 
 *   &#064;GuardedBy(&quot;this&quot;)
 *   void m1() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * 
 *   &#064;GuardedBy(&quot;l1, l2&quot;)
 *   void m2() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * 
 *   &#064;RequiresLock(&quot;this&quot;)
 *   void m3() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * 
 *   &#064;RequiresLock(&quot;l1, l2&quot;)
 *   void m4() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * }
 * </pre>
 * <p>
 * {@link GuardedBy} some <em>expression</em> expressed on a method may be
 * thought of as semantically equivalent to a {@link RequiresLock} annotation
 * with the same <em>expression</em>. Which annotation you choose to use is a
 * style preference.
 * 
 * <h3>Semantics:</h3>
 * 
 * <i>Field:</i> The program must be holding the specified lock when the
 * annotated field is read or written.
 * <p>
 * <i>Method:</i> The program must be holding the specified lock when the
 * annotated method is invoked.
 * 
 * <h3>Examples:</h3>
 * 
 * The immutable {@code Point} class below is considered thread-safe.
 * 
 * <pre>
 * &#064;ThreadSafe
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
 * The example below shows how <a href="#note">the generated lock name</a> may
 * be referenced in a <code>RequiresLock</code> annotation:
 * 
 * <pre>
 * public class Var {
 *   &#064;GuardedBy(&quot;this&quot;)
 *   private int value;
 * 
 *   public synchronized void set(final int v) {
 *     value = v;
 *   }
 * 
 *   &#064;RequiresLock(&quot;Guard$_value&quot;)
 *   public int get() {
 *     return value;
 *   }
 * }
 * </pre>
 * 
 * <b>Constructor annotation to support locking policies:</b> To support the
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
 * from <code>&#064;GuardedBy</code> proposed by Brian Goetz and Tim Peierls in
 * the book <i>Java Concurrency in Practice</i> (Addison-Wesley 2006) we have
 * simply adapted it to have semantics as a promise. Further, the annotation in
 * {@code net.jcip.annotations} may be used instead of this one with the same
 * tool behavior.
 * <p>
 * The SureLogic JSure tool supports verification of all the above forms except
 * for <code>itself</code> and <code>method-name()</code>. The other forms are
 * supported by translating this annotation into a {@link RegionLock} annotation
 * on the class that contains the annotated field. A lock name is generated to
 * use with {@link RegionLock} annotation: for a {@link GuardedBy} annotation on
 * a field <code>f</code> we generate the lock name <code>Guard$_f</code>, where
 * <code>$</code> is meant to be pronounced as an <i>S</i>. The supported cases
 * are translated as follows:
 * 
 * <ul>
 * <li><b><code>&#64;GuardedBy("this")</code></b> on field <code>f</code> in
 * class <code>C</code> generates a <code>&#64;RegionLock("Guard$_f is this
 * protects f")</code> on class <code>C</code>.
 * 
 * <li><b><code>&#64;GuardedBy("class-name.this")</code></b> on field
 * <code>f</code> in class <code>C</code> generates a
 * <code>&#64;RegionLock("Guard$_f is class-name.this protects f")</code> on
 * class <code>C</code>.
 * 
 * <li><b><code>&#64;GuardedBy("field-name")</code></b> on field <code>f</code>
 * in class <code>C</code> generates a <code>&#64;RegionLock("Guard$_f is
 * field-name protects f")</code> on class <code>C</code>.
 * 
 * <li><b><code>&#64;GuardedBy("class-name.field-name")</code></b> on field
 * <code>f</code> in class <code>C</code> generates a
 * <code>&#64;RegionLock("Guard$_f is class-name.field-name")</code> on class
 * <code>C</code>.
 * 
 * <li><b><code>&#64;GuardedBy("class-name.class")</code></b> on field
 * <code>f</code> in class <code>C</code> generates a
 * <code>&#64;RegionLock("Guard$_f is class-name.class")</code> on class
 * <code>C</code>.
 * </ul>
 * This implementation approach may be changed in future releases of the JSure
 * tool.
 * 
 * @see RegionLock
 * @see ThreadSafe
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
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
