/*
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
 * When used on a class this annotation indicates that that object specifies,
 * using {@link Initialized} annotations, any methods that may be invoked with a
 * partially initialized object. In particular, any method invoked from the
 * constructor. This annotation may be placed on any class but is required on a
 * class, <i>C</i>, if
 * <ul>
 * <li><i>C</i> contains a field annotated to be <code>&#064;NonNull</code>, or</li>
 * <li>A class <i>X</i>, where <i>X</i> <tt>extends</tt> <i>C</i>, is annotated
 * <code>&#064;TrackPartiallyInitialized</code>.</li>
 * </ul>
 * <p>
 * The class <code>java.lang.Object</code> is annotated
 * <code>&#064;TrackPartiallyInitialized</code>.
 * <p>
 * It is a modeling error to place this annotation on any other declaration
 * except a class.
 * <p>
 * It is possible to avoid the requirement to place this annotation on a parent
 * class by setting the {@link #verifyParent()} attribute to {@code false}. This
 * may be useful if the parent class is from a library or from another
 * organization.
 * 
 * <pre>
 * &#064;TrackPartiallyInitialized(verifyParent=false)
 * class MyList&lt;T&gt; extends java.util.AbstractList&lt;T&gt; {
 *   &#064;NonNull SomeType someField;
 *   &hellip;
 * }
 * </pre>
 * 
 * <h3>Semantics:</h3>
 * 
 * This annotations forces explicit specification of any use of a partially
 * initialized object. In particular when the partially initialized object is
 * used as the receiver or a parameter of a method. The {@link Initialized}
 * annotation is used to document such uses.
 * <p>
 * This annotation avoids the entire codebase being subjected to such checks by
 * a verification tool such as JSure. Only those classes that need specification
 * of any use of a partially initialized object to verify another property, such
 * as a {@link NonNull} field, are subjected to complete annotation with
 * {@link Initialized}. This annotation helps eliminate the need for possibly
 * hundreds of poorly motivated {@link Initialized} annotations in a large Java
 * system.
 * 
 * <h3>Examples:</h3>
 * 
 * Note that many of the examples for this annotation reflect poor Java idioms.
 * It is best practice to call as few methods in a constructor as possible, and
 * to <i>never</i> invoke a dispatching method. That said, in real-world systems
 * this ideal may not be possible so it is important to understand how to deal
 * with such code.
 * <p>
 * Both <code>A</code> and <code>B</code> define {@link NonNull} fields and are
 * therefore required to be annotated {@link TrackPartiallyInitialized}. In,
 * addition the class <code>Base</code> is also required to be annotated
 * {@link TrackPartiallyInitialized} because it is the parent class of
 * <code>A</code>. Note that <code>C</code> is <i>not</i> required to be
 * annotated {@link TrackPartiallyInitialized} because it defines no
 * {@link NonNull} fields, and {@link TrackPartiallyInitialized} does not force
 * like annotation on annotated subclasses.
 * 
 * <pre>
 * &#064;TrackPartiallyInitialized
 * class Base {
 *   &hellip; // no non-null fields
 * }
 * 
 * &#064;TrackPartiallyInitialized
 * class A extends Base {
 *   &#064;NonNull
 *   protected String name;
 * 
 *   public A(@NonNull String s) {
 *     this.name = s;
 *     this.m(55);
 *   }
 * 
 *   &#064;Initialized(through=&quot;Object&quot;)
 *   protected void m(int x) { &amp;hellip }
 * }
 * 
 * &#064;TrackPartiallyInitialized
 * class B extends A {
 *   &#064;NonNull
 *   protected String path;
 * 
 *   public B(@NonNull String p, @NonNull String s) {
 *     super(s);
 *     path = p;
 *   }
 * 
 *   &#064;Initialized(through=&quot;Object&quot;)
 *   protected void m(int x) {
 *     &amp;hellip this.path &amp;hellip // Cannot assume that &quot;this.path&quot; is non-null
 *   }
 * }
 * 
 * class C extends A {
 *   Object myVar;
 * 
 *   public void setMyVar(Object value) {
 *     myVar = value;
 *   }
 * 
 *   public Object getMyVar() {
 *     return myVar;
 *   }
 * }
 * </pre>
 * 
 * If it is undesirable to place the {@link TrackPartiallyInitialized}
 * annotation on the class <code>Base</code> (because perhaps it is in a
 * binary). The <code>verifyParent</code> attribute can be set to
 * <code>false</code>. This avoids the annotation but makes verification results
 * contingent upon this "vouch" about the implementation of <code>Base</code>.
 * 
 * <pre>
 * class Base {
 *   &hellip; // no non-null fields
 * }
 * 
 * &#064;TrackPartiallyInitialized(verifyParent=false)
 * class A extends Base { &hellip; }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface TrackPartiallyInitialized {
  /**
   * Indicates whether or not tool verification of the parent class should be
   * attempted. Normally, unless the parent of the annotated class is
   * {@link Object} the parent is required to be annotated with
   * {@link TrackPartiallyInitialized}. Setting this value to {@code false}
   * eliminates this requirement, but flags any verification results to be
   * contingent.
   * 
   * @return {@code true} if the claim should be verified by a tool, such as
   *         SureLogic JSure, {@code false} otherwise. The default value for
   *         this attribute is {@code true}.
   */
  public boolean verifyParent() default true;
}
