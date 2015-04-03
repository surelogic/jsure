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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This annotation is used to add constraints to the formal type parameters of
 * the annotated type. Essentially, it is a way to add
 * <code>&#064;Containable</code>, <code>&#064;Immutable</code>,
 * <code>&#064;ReferenceObject</code>, <code>&#064;ThreadSafe</code>, and
 * <code>&#064;ValueObject</code> annotations to the formal type parameters.
 * Annotating a type formal this way establishes a bound on the type actual that
 * may be passed to the type when the generic type is instantiated. For example,
 * consider the generic class <code>C</code>
 * 
 * <pre>
 * &#64;AnnotationBounds(containable="T")
 * public class C&lt;T&gt; {
 *   &hellip;
 * }
 * </pre>
 * 
 * <P>
 * The type formal <code>T</code> has the annotation bound
 * <code>&#064;Containable</code>, meaning that it can only be instantiated with
 * a type actual <code><i>A</i></code> such that the implementation of
 * <code><i>A</i></code> is containable.
 * 
 * <p>
 * Annotation bounds are ultimately used to satisfy the assurance of a
 * <code>&#064;Containable</code>, <code>&#064;Immutable</code>, or
 * <code>&#064;ThreadSafe</code> annotation on a type. Consider the class
 * 
 * <pre>
 * &#64;Immutable
 * &#64;AnnotationBounds(immutable="W")
 * public class Wrapper&lt;W&gt; {
 *   private final W wrapped;
 *   
 *   &#64;RegionEffects("none")
 *   &#64;Unique("return")
 *   public Wrapper(final W w) {
 *     wrapped = w;
 *   }
 *   &hellip;
 * }
 * </pre>
 * 
 * <p>
 * For the implementation of <code>Wrapper</code> to be assured to be immutable,
 * the field <code>wrapped</code> must have an immutable type. Thus we must
 * declare the annotation bound <code>&#064;Immutable</code> on the type formal
 * <code>W</code>. When the type <code>Wrapper</code> is instantiated to a
 * parameterized type, the type actual passed to <code>W</code> must have an
 * immutable implementation. The following parameterized types would be
 * acceptable:
 * 
 * <ul>
 * <li><code>Wrapper&lt;String&gt;</code>
 * <li><code>Wrapper&lt;Integer&gt;</code>
 * <li><code>Wrapper&lt;T&gt;</code>, where <code>T</code> is a type formal in
 * scope that has <code>&#064;Immutable</code> as an annotation bound
 * </ul>
 * 
 * <p>
 * The following parameterized types would not be acceptable:
 * 
 * <ul>
 * <li><code>Wrapper&lt;Object&gt;</code>
 * <li><code>Wrapper&lt;int[]&gt;</code>
 * <li><code>Wrapper&lt;T&gt;</code>, where <code>T</code> is a type formal in
 * scope that does not have <code>&#064;Immutable</code> as an annotation bound
 * </ul>
 * 
 * <p>
 * The attributes of the annotation are arrays of strings, where each string is
 * the name of a formal type parameter of the type being annotated. It is
 * illegal to name a type parameter of a type that encloses or is enclosed by
 * the type being annotated. The examples below show legal uses of the
 * annotation:
 * 
 * <pre>
 * // X and Y must have thread safe implementations 
 * &#64;AnnotationBounds(threadSafe={"X", "Y"})
 * public class C&lt;X, Y&gt; { &hellip; }
 * 
 * // A must have an immutable implementation
 * // B must have a containable implementation
 * // C is unconstrained 
 * &#64;AnnotationBounds(immutable="A", containable="B")
 * public class D&lt;A, B, C&gt; { &hellip; }
 * </pre>
 * 
 * <p>
 * If a type formal is named in more than one attribute of this annotation an
 * <i>or</i> semantics is used. That is,
 * 
 * <pre>
 * &#64;AnnotationBounds(threadSafe="A", referenceObject="A")
 * public class Example&lt;A&gt; { &hellip; }
 * </pre>
 * 
 * <p>
 * means that the actual type passed to a <code>A</code> must be a thread safe
 * or reference type. When <code>Example</code> is assured, it must be correct
 * under either assumption. The following class would not assure, for example:
 * 
 * <pre>
 * &#064;ThreadSafe
 * &#064;AnnotationBounds(threadSafe = &quot;A&quot;, referenceObject = &quot;A&quot;)
 * public class Bad&lt;A&gt; {
 *   private final A field;
 * }
 * </pre>
 * 
 * <p>
 * Class <code>Bad</code> does not assure because its own &#64;ThreadSafe
 * annotation only assures when <code>A</code> is assumed to be thread safe; it
 * does not assure when <code>A</code> is assumed to be a reference type.
 * 
 * <p>
 * The thread-safe collection classes in <code>java.util</code> and
 * <code>java.util.concurrent</code> are annotated with this annotation to
 * enforce that the elements placed in the collection are also thread safe.
 * Specifically, thread safe maps such as {@link Hashtable} and
 * {@link ConcurrentHashMap} are annotated such that the formal type parameters
 * <code>K</code> and <code>V</code> for the key and value types are bounded by
 * &#64;ThreadSafe, while thread safe lists such as {@link CopyOnWriteArrayList}
 * and {@link BlockingQueue} are annotated such that the formal type for the
 * element type <code>E</code> is bounded by &#64;ThreadSafe. Consider the
 * example below:
 * 
 * <pre>
 * public class Example {
 *   private List&lt;String&gt; names; // not checked
 *   private List&lt;? extends List&lt;String&gt;&gt; listOfLists; // not checked
 *   
 *   public void initForConcurrent() {
 *     names = new CopyOnWriteArrayList&lt;String&gt;(); // GOOD
 *     listOfLists = new CopyOnWriteArrayList&lt;CopyOnWriteArrayList&lt;String&gt;&gt;(); // GOOD
 *   }
 *   
 *   public void initForSequential() {
 *     names = new ArrayList&lt;String&gt;(); // not checked
 *     listOfLists = new ArrayList&lt;List&lt;String&gt;&gt;(); // not checked
 *   }
 *   
 *   public ConcurrentMap&lt;Location, Player&gt; getGameMap() { &hellip; } // GOOD
 * }
 * 
 * &#64;ThreadSafe
 * public class Location { &hellip; }
 * 
 * &#64;ThreadSafe
 * public class Player { &hellip; }
 * </pre>
 * 
 * <p>
 * The parameterized types in the field declarations do not need to be checked
 * by analysis because the type formal of {@link List} is unbounded. Similarly,
 * the parameterized types in <code>initForSequential()</code> do not need to be
 * checked by analysis because the type formals of {@link List} and
 * {@link ArrayList} are unbounded. But the parameterized types in
 * <code>initForConcurrent</code> do need to be checked because the type formal
 * of {@link CopyOnWriteArrayList} is bounded. Of note is the fact that we must
 * use the parameterized type <code>CopyOnWriteArrayList&lt;String&gt;</code> as
 * the actual type parameter to
 * <code>new CopyOnWriteArrayList&lt;&hellip;&gt;</code> because we must provide
 * a type that is known to be thread safe.
 * 
 * <p>
 * The return type of <code>getGameMap()</code> must also be checked by analysis
 * because {@link ConcurrentMap} has bounded type formals. In this case, the
 * bounds are satisfied because the classes <code>Location</code> and
 * <code>Player</code> are thread safe.
 * 
 * @see Containable
 * @see Immutable
 * @see ReferenceObject
 * @see ThreadSafe
 * @see ValueObject
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AnnotationBounds {
  /**
   * Indicates that the named formal type parameters of the annotated class
   * should be assumed to be <code>&#064;Containable</code>. Specifically, the
   * value of this attribute is an array of formal type parameter names.
   * 
   * @return an array of formal type parameter names.
   */
  public String[] containable() default {};

  /**
   * Indicates that the named formal type parameters of the annotated class
   * should be assumed to be <code>&#064;Immutable</code>. Specifically, the
   * value of this attribute is an array of formal type parameter names.
   * 
   * @return an array of formal type parameter names.
   */
  public String[] immutable() default {};

  /**
   * Indicates that the named formal type parameters of the annotated class
   * should be assumed to be <code>&#064;ReferenceObject</code>. Specifically,
   * the value of this attribute is an array of formal type parameter names.
   * 
   * @return an array of formal type parameter names.
   */
  public String[] referenceObject() default {};

  /**
   * Indicates that the named formal type parameters of the annotated class
   * should be assumed to be <code>&#064;ThreadSafe</code>. Specifically, the
   * value of this attribute is an array of formal type parameter names.
   * 
   * @return an array of formal type parameter names.
   */
  public String[] threadSafe() default {};

  /**
   * Indicates that the named formal type parameters of the annotated class
   * should be assumed to be <code>&#064;ValueObject</code>. Specifically, the
   * value of this attribute is an array of formal type parameter names.
   *
   * @return an array of formal type parameter names.
   */
  public String[] valueObject() default {};
}
