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
 * The class to which this annotation is applied is declared to be containable.
 * That is, instances of the class can be safely encapsulated via region
 * aggregation into other objects because methods of this class do not leak
 * references to themselves or any of the other objects that they reference,
 * transitively. This implies that
 * <ul id="fieldrules">
 * <li>each non-<code>static</code> method must be annotated
 * <code>&#064;Borrowed("this")</code>;
 * <li>each constructor must be annotated <code>&#064;Unique("return")</code>;
 * and
 * <li>each non-primitively typed non-static field must be
 * <ul>
 * <li>of a type annotated <code>&#064;Containable</code>, and
 * <li>annotated <code>&#064;Unique</code> or <code>&#064;UniqueInRegion</code>.
 * </ul>
 * </ul>
 * <p>
 * A field may have an array type if the array is 1-dimensional and the base
 * type of the array is a primitive type.
 * <p>
 * Only the non-static fields of the class are affected by this annotation.
 * Containability is only interesting when the state of a referenced object is
 * being aggregated into a region of the referencing object. Aggregation only
 * applies to non-static fields, thus containability is not interesting for
 * static fields.
 * <p>
 * Typically, subtypes of the annotated type must be explicitly annotated
 * <code>&#064;Containable</code> as well. It is a modeling error if they are
 * not. This annotation has two attributes, <tt>implementationOnly</tt> and
 * <tt>verify</tt>, that control how subtypes of an {@link Containable} type
 * must be annotated. The <tt>implementationOnly</tt> attribute indicates that
 * the implementation of the annotated class should be assured without making a
 * general statement about the visible behavior of the type or its subtypes.
 * There are several rules with regards to the <tt>implementationOnly</tt>
 * attribute on {@link Containable} types:
 * <ol>
 * <li>The <tt>implementationOnly</tt> attribute must be {@code false} when
 * {@link Containable} appears on an interface, because interfaces do not have
 * an implementation.</li>
 * <li>The subinterfaces of an interface annotated with {@link Containable} must
 * be annotated with {@link Containable}; classes that implement an interface
 * annotated with {@link Containable} must be annotated with
 * <tt>&#064;Containable(implementationOnly=false)</tt>.</li>
 * <li>The superclass of a class annotated with
 * <tt>&#064;Containable(implementationOnly=true)</tt> must be annotated with
 * <tt>&#064;Containable(implementationOnly=true)</tt>; there are no constraints
 * on the subclasses.</li>
 * <li>The superclass of a class annotated with
 * <tt>&#064;Containable(implementationOnly=false)</tt> must be annotated with
 * either <tt>&#064;Containable(implementationOnly=false)</tt> or
 * <tt>&#064;Containable(implementationOnly=true)</tt>; the subclasses must be
 * annotated with <tt>&#064;Containable(implementationOnly=false)</tt>.</li>
 * </ol>
 * Finally, it may be possible to implement a class that satisfies the semantics
 * of {@link Containable}, but that is not verifiable using the syntactic and
 * annotation constraints described above. For this case, we provide the
 * "escape hatch" of turning off tool verification for the annotation with the
 * <tt>verify</tt> attribute. For example,
 * <tt>&#064;Containable(verify=false)</tt> would skip tool verification
 * entirely.
 * 
 * <p>
 * A type may not be annotated with both <code>&#064;Containable</code> and
 * <code>&#064;NotContainable</code>.
 * <p>
 * An annotation type declaration may <b>not</b> be annotated with
 * <code>&#064;Containable</code>.
 * 
 * <p>
 * When the annotated type has formal type parameters, the parameters are all
 * given the {@link AnnotationBounds annotation bound}
 * <code>&#064;ThreadSafe</code>. If the attribute
 * {@link #allowReferenceObject() allowReferenceObject} is <code>true</code>
 * then the bound <code>&#064;ThreadSafe</code> or
 * <code>&#064;ReferenceObject</code> is used instead. Unlike bounds introduced
 * with <tt>&#064;AnnotationBounds</tt>, these bounds are only checked when the
 * type is used in a context that requires the type to be containable. In
 * contexts where the containability is not interesting, the bounds are ignored.
 * This overall behavior is designed to accommodate the non&ndash;thread-safe
 * Java collection classes such as <code>java.util.ArrayList</code> and
 * <code>java.util.HashMap</code>. This behavior is called
 * "conditional containability".
 * 
 * <p>
 * When collections are used to type method parameters or return values, for
 * example, they can be parameterized with any type formal. But when they are
 * used to type a field of a containable or thread-safe class, they must be
 * parameterized with type formals that enable the collection to be containable.
 * Broadly speaking, the collection classes are written in such a way that they
 * only use the <code>hashCode()</code>, <code>equals()</code>, and
 * <code>toString()</code> methods of the objects in the collection. They do not
 * otherwise utilize the state of the object, that is, the fields of the objects
 * in the collection are not interesting to the collection, and thus can be
 * excluded from containability constraints. What does need to prevented,
 * however, is the state of the objects changing in such a way that might
 * corrupt the state of the collection. This can be done by requiring the
 * objects in the collection to be thread-safe, or in many cases by requiring
 * the above mentioned methods from using the state of the object, that is,
 * requiring that the object is a reference object.
 * 
 * <p>
 * The non&ndash;thread-safe collection classes in <code>java.util</code> and
 * <code>java.util.concurrent</code> such as <code>ArrayList</code>,
 * <code>LinkedList</code>, <code>HashSet</code>, and <code>HashMap</code> have
 * been annotated as <code>&#064;Containable</code>. Because they are all
 * generic classes, they use conditional containability.
 * 
 * <h3>Semantics:</h3>
 * 
 * For each object transitively referenced by instances of the annotated type
 * (including the instance itself), no reference to that object is returned by
 * any method/constructor of the class nor is that object referenced by a field
 * of any other object that is not also transitively referenced by the instance
 * (including the instance itself).
 * 
 * <h3>Examples:</h3>
 * 
 * The class <code>Point</code> is a containable class that uses only
 * primitively typed fields. Instances of it are in turn contained by the
 * <code>Rectangle</code> class.
 * 
 * <pre>
 * &#064;Containable
 * public class Point {
 *   private int x;
 * 
 *   private int y;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Point(int x, int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   &#064;Borrowed(&quot;this&quot;)
 *   &#064;RegionEffects(&quot;writes Instance&quot;)
 *   public void translate(int dx, int dy) {
 *     x += dx;
 *     y += dy;
 *   }
 * }
 * 
 * &#064;Containable
 * public class Rectangle {
 *   &#064;Unique
 *   private final Point topLeft;
 * 
 *   &#064;Unique
 *   private final Point bottomRight;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   public Rectangle(int x1, int y1, int x2, int y2) {
 *     topLeft = new Point(x1, y1);
 *     bottomRight = new Point(x2, y2);
 *   }
 * 
 *   &#064;Borrowed(&quot;this&quot;)
 *   &#064;RegionEffects(&quot;writes Instance&quot;)
 *   public void translate(int dx, int dy) {
 *     topLeft.translate(dx, dy);
 *     bottomRight.translate(dx, dy);
 *   }
 * }
 * </pre>
 * 
 * <p>
 * The class below shows the use of annotation bounds and conditional
 * containability:
 * 
 * <pre>
 * &#064;Containable
 * class Example&lt;E&gt; {
 *   &#064;Unique
 *   private final List&lt;String&gt; stringList = new ArrayList&lt;String&gt;();
 * 
 *   &#064;Unique
 *   private final Set&lt;Object&gt; objSet = new HashSet&lt;Object&gt;();
 * 
 *   &#064;Unique
 *   private final Map&lt;String, E&gt; map = new HashMap&lt;String, E&gt;();
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   public Example() {
 *     // ...
 *   }
 * 
 *   // ...
 * }
 * </pre>
 * 
 * <p>
 * The type formal <tt>E</tt> has the annotation bound
 * <code>&#064;ThreadSafe</code>. The field <tt>stringList</tt> is containable
 * because the the immutable type <code>String</code> satisfies the thread-safe
 * bound of <code>ArrayList</code> allowing the type to be considered
 * <code>&#064;Containable</code>. Similarly, <code>String</code> and
 * <code>E</code> satisfy the bounds on the type formals of <code>HashMap</code>
 * allowing the type to be <code>&#064;Containable</code>, and thus the allowing
 * the field <code>map</code> to be containable. The field <code>objSet</code>
 * is not containable, however, because the type
 * <code>HashSet&lt;Object&gt;</code> is not <code>&#064;Containable</code>, due
 * to <code>Object</code> not being <code>&#064;ThreadSafe</code>.
 * 
 * <p>
 * This example also demonstrates how the <a href="#fieldrules">above rules</a>
 * for fields of a <code>&#064;Containable</code> class may be made less
 * conservative. If the field is <code>final</code> the declared type of field
 * does not have to be <code>&#064;Containable</code> (indeed,
 * <code>java.util.List</code>, <code>java.util.Set</code>, and
 * <code>java.util.Map</code> are not annotated to be
 * <code>&#064;Containable</code>), as long as the initializer of the field
 * creates a new instance of a <code>&#064;Containable</code> class.
 * 
 * @see AnnotationBounds
 * @see ThreadSafe
 * @see NotContainable
 * @see Vouch
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Containable {
  /**
   * Indicates that the implementation of the annotated class should be assured
   * without making a general statement about the visible behavior of the type
   * or its subtypes.
   * 
   * @return {@code true} if only the annotated class should be assured without
   *         making a general statement about the visible behavior of the type
   *         or its subtypes, {@code false} otherwise. The default value for
   *         this attribute is {@code false}.
   */
  public boolean implementationOnly() default false;

  /**
   * Indicates whether or not tool verification should be attempted.
   * 
   * @return {@code true} if the claim should be verified by a tool, such as
   *         SureLogic JSure, {@code false} otherwise. The default value for
   *         this attribute is {@code true}.
   */
  public boolean verify() default true;

  public boolean allowReferenceObject() default false;
}
