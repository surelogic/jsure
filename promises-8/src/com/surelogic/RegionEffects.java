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
 * Declares the regions that may be read or written during execution of the
 * method or constructor to which this annotation is applied. In general the
 * annotation syntax is
 * <p>
 * <code>&#064;RegionEffects("reads</code> <i>readTarget</i>
 * <code>, ... ; writes</code> <i>writeTarget</i><code>, ... ")</code>
 * <p>
 * The annotation contains <code>reads</code> and <code>writes</code> clauses
 * that each have a list of one or more targets. The <code>reads</code> clause
 * describes the data that may be read by the method/constructor; the
 * <code>writes</code> clause describes the state that may be read or written by
 * the method/constructor. Because writing includes reading, there is no need to
 * list a target in the <code>reads</code> clause if its state is already
 * described in the <code>writes</code> clause.
 * <p>
 * Both the <code>reads</code> and <code>writes</code> clauses are optional: to
 * indicate that there are no effects use
 * <code>&#064;RegionEffects("none")</code>. An unannotated method is assumed to
 * have the annotation <code>&#064;RegionEffects("writes All")</code> which
 * declares that the method could read from or write to any state in the
 * program.
 * <p>
 * A target is an extrinsic syntactic mechanism to name references to regions,
 * and can be one of
 * <dl>
 * <dt>RegionName</dt>
 * <dd>If <b>RegionName</b> is an instance region, then it is the same as
 * <code>this:RegionName</code> (see below). If <b>RegionName</b> is a
 * <code>static</code> region, it must be a region declared in the annotated
 * class or one of its ancestors, and is thus a short hand for
 * <code>pkg.C:RegionName</code> below.
 * <dt>this:RegionName</dt>
 * <dd><b>RegionName</b> is an instance region of the class containing the
 * method.
 * <dt>param:RegionName</dt>
 * <dd><b>param</b> is a parameter of the method that references an object.
 * <b>RegionName</b> is a region of the class of <b>param</b>'s type.
 * <dt>pkg.C.this:RegionName</dt>
 * <dd><code>pkg.C</code> is an "outer class" of the class that contains the
 * annotated method. That is, the method being annotated is in class
 * <code>D</code>, and <code>D</code> is an inner class of <code>C</code>.
 * <b>Region</b> is a region of <code>pkg.C</code>.
 * <dt>any(pkg.C):RegionName</dt>
 * <dd>The any instance target: <code>pkg.C</code> is a class name and
 * <b>RegionName</b> is a region of <code>pkg.C</code>.
 * <dt>pkg.C:RegionName</dt>
 * <dd>The static target: <b>RegionName</b> is a <code>static</code> region of
 * class <code>pkg.C</code>.
 * </dl>
 * 
 * The analysis checks that the actual effects of the method implementation are
 * no greater than its declared effects. There are several fine points to this:
 * <ul>
 * <li>Uses of <code>final</code> fields produce no effects and do not need to
 * be declared.</li>
 * <li>Uses of the class literal expression, e.g., <tt>ArrayList.class</tt>,
 * produce no effects and do not need to be declared.</li>
 * <li>Effects on local variables are not visible outside of a
 * method/constructor and do not need to be declared.</li>
 * <li>Effects on objects created within a method are not visible outside of a
 * method and do not need to be declared.</li>
 * <li>Constructors do not have to declare the effects on the
 * <code>Instance</code> region of the object under construction.</li>
 * <li>Region aggregation (see {@link Unique} and {@link UniqueInRegion}) is
 * taken into account.</li>
 * </ul>
 * 
 * There are three pre-defined regions available for use:
 * 
 * <dl>
 * <dt>All</dt>
 * <dd>A region representing the entire state of the program's heap.</dd>
 * <dt>Instance</dt>
 * <dd>A region representing the instance state of an object.</dd>
 * <dt>Static</dt>
 * <dd>A region representing the static state of a class.</dd>
 * </dl>
 * 
 * Arguments to methods can be referenced by name or by position using the name
 * <tt>arg</tt><i>n</i> where <i>n</i> is an non-negative integer. The parameter
 * count starts at zero. For example, the declaration below
 * 
 * <pre>
 * &#064;RegionEffects(&quot;writes list:Instance&quot;)
 * public void eraseList(List&lt;?&gt; list) {
 *   list.clear();
 * }
 * </pre>
 * 
 * could have used <tt>arg0</tt> instead of <tt>list</tt> as shown below.
 * 
 * <pre>
 * &#064;RegionEffects(&quot;writes arg0:Instance&quot;)
 * public void eraseList(List&lt;?&gt; list) {
 *   list.clear();
 * }
 * </pre>
 * 
 * <p>
 * Naming arguments by position is mostly used when annotating library methods
 * using XML.
 * 
 * <p>
 * A <i>default</i> effects annotation can be set for a type or package using
 * the {@link Promise} annotation. How to do this is shown in the examples
 * below.
 * 
 * <h3>Semantics:</h3>
 * 
 * States the upper-bound effects of an annotated method or constructor on the
 * Java heap. These effects do not include the stack of any thread. An
 * implementation of a method or constructor is consistent with its declared
 * {@link RegionEffects} if all possible executions read and write only to the
 * state specified in the annotation.
 * <p>
 * At runtime, a target&mdash;like a region&mdash;represents a subset of the JVM
 * heap, subdividing one or more objects in the heap. While in most cases a
 * target is equivalent to particular runtime region, the any instance target
 * refers to state that is not possible to name with any single region.
 * 
 * <dl>
 * <dt>this:RegionName</dt>
 * <dd>The target denotes the runtime region <code>RegionName</code> of the
 * object <em>o</em> referenced by the method/constructor's receiver.
 * <dt>param:RegionName</dt>
 * <dd>The target denotes the runtime region <code>RegionName</code> of the
 * object <em>o</em> referenced by the parameter <code>p</code>
 * <em>at the start of the method's execution</em>.
 * <dt>pkg.C.this:RegionName</dt>
 * <dd>The target denotes the runtime region <code>RegionName</code> of the
 * object <em>o</em> referenced by the qualified receiver
 * <code>pkg.C.this</code>.
 * <dt>any(pkg.C):RegionName</dt>
 * <dd>Let <em>X</em> be the set of all heap objects <em>o</em> such that
 * <em>o</em> has type <code>T</code> and <code>T instanceof pkg.C</code>. The
 * target denotes the union of the runtime regions <code>RegionName</code> of
 * each <em>o</em> &isin; <em>X</em>
 * <dt>pkg.C:Region</dt>
 * <dd>The target denotes the runtime region of the <code>static</code> region
 * <code>RegionName</code> declared in class <code>pkg.C</code>.
 * </dl>
 * 
 * <h3>Examples:</h3>
 * 
 * Here is a simple "variable" class with effects annotations.
 * 
 * <pre>
 * &#064;Region(&quot;public ValueRegion&quot;)
 * public class Var {
 * 
 *   &#064;InRegion(&quot;ValueRegion&quot;)
 *   private int value;
 * 
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Var(int v) {
 *     value = v;
 *   }
 * 
 *   &#064;RegionEffects(&quot;reads ValueRegion&quot;)
 *   public int getValue() {
 *     return value;
 *   }
 * 
 *   &#064;RegionEffects(&quot;writes ValueRegion&quot;)
 *   public void setValue(int v) {
 *     value = v;
 *   }
 * }
 * </pre>
 * 
 * Here is an example that uses the pre-defined <tt>Static</tt> region.
 * 
 * <pre>
 * &#064;Utility
 * public class CounterUtility {
 * 
 *   private static long count = 0;
 * 
 *   &#064;RegionEffects(&quot;writes Static&quot;)
 *   public static long incrementAndGet() {
 *     return ++count;
 *   }
 * 
 *   &#064;RegionEffects(&quot;reads Static&quot;)
 *   public static long get() {
 *     return count;
 *   }
 * 
 *   private CounterUtility() {
 *     // no instances
 *   }
 * }
 * </pre>
 * 
 * This is another approach to the above example using a named static region.
 * 
 * <pre>
 * package com.surelogic;
 * 
 * import com.surelogic.*;
 * 
 * &#064;Utility
 * &#064;Region(&quot;static public CounterState&quot;)
 * public class CounterUtility {
 * 
 *   &#064;InRegion(&quot;CounterState&quot;)
 *   private static long count = 0;
 * 
 *   &#064;RegionEffects(&quot;writes CounterState&quot;)
 *   public static long incrementAndGet() {
 *     return ++count;
 *   }
 * 
 *   &#064;RegionEffects(&quot;reads CounterState&quot;)
 *   public static long get() {
 *     return count;
 *   }
 * 
 *   private CounterUtility() {
 *     // no instances
 *   }
 * }
 * 
 * </pre>
 * 
 * Here is some client code of the counter class referencing the static
 * <tt>CounterState</tt> region.
 * 
 * <pre>
 * package com.surelogic;
 * 
 * import com.surelogic.RegionEffects;
 * 
 * public class ClientCode {
 * 
 *   &#064;RegionEffects(&quot;writes com.surelogic.CounterUtility:CounterState&quot;)
 *   public static void main(String[] args) {
 *     long counter = CounterUtility.incrementAndGet();
 *   }
 * 
 * }
 * </pre>
 * 
 * Here is an example of <i>changing the default</i> effects of all constructors
 * in a package to <code>&#064;RegionEffects("none")</code>. In the file
 * <tt>com/stuff/package-info.java</tt>
 * 
 * <pre>
 * &#064;com.surelogic.Promise(&quot;@RegionEffects(none) for new(**)&quot;)
 * package com.stuff;
 * </pre>
 * 
 * Any particular constructor within the package can override this default with
 * a specific annotation. For example the constructor for <code>Flight</code> in
 * the listing below declares <i>reads All</i> as its effects because it perhaps
 * reads several static variables in the codebase.
 * 
 * <pre>
 * package com.stuff;
 * 
 * public class Flight {
 *   ...
 *   &#064;RegionEffects("reads All")
 *   public Flight(int number) { ... }
 * }
 * </pre>
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface RegionEffects {
  /**
   * This attribute is either <code>"none"</code> to indicate that the
   * method/constructor has no visible effects or divided into separate (and
   * optional) reads and writes clauses. Each clause is a list of
   * comma-separated targets describing the regions that may be read/written.
   * The clauses are separated by a semicolon.
   * 
   * <h3>Examples:</h3>
   * 
   * <pre>
   * &#064;RegionEffects(&quot;reads this:Instance; writes other:Instance&quot;)
   * &#064;RegionEffects(&quot;writes C:StaticRegion, any(D):Instance; reads this:Instance&quot;)
   * &#064;RegionEffects(&quot;reads this:Instance&quot;)
   * &#064;RegionEffects(&quot;writes Instance&quot;)
   * &#064;RegionEffects(&quot;writes Static&quot;)
   * &#064;RegionEffects(&quot;none&quot;)
   * </pre>
   * 
   * The value of this attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value =
   *   none /  ; No effects
   *   readsEffect [&quot;;&quot; writesEffect] /
   *   writesEffect [&quot;;&quot; readsEffect]
   * 
   * readsEffect = &quot;reads&quot; effectsSpecification
   * 
   * writesEffect = &quot;writes&quot; effectsSpecification
   * 
   * effectsSpecification = &quot;nothing&quot; / effectSpecification *(&quot;,&quot; effectSpecification)
   *   
   * effectSpecification =
   *   simpleEffectExpression &quot;:&quot; simpleRegionSpecification /
   *   IDENTIFIER   ; instance region of &quot;this&quot; or a static region declared in the current class or one of its ancestors 
   * 
   * simpleEffectExpression =
   *   &quot;any&quot; &quot;(&quot; namedType &quot;)&quot; /    ; any instance
   *   namedType &quot;.&quot; &quot;this&quot; /       ; qualified this expression
   *   namedType /                  ; class name 
   *   simpleExpression             ; parameter name
   * 
   * namedType = IDENTIFIER *(&quot;.&quot; IDENTIFIER)
   * 
   * simpleExpression = &quot;this&quot; / IDENTIFIER
   * 
   * simpleRegionSpecification = IDENTIFIER  ; Region of the class being annotated
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
