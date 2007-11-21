# This file contains common utility procedures that are
# used throughout the test suite.

# constant_expression: Check an unlimited number of boolean
# expressions at compile time. If each expression returns
# true, the compile result will be PASS. If any of the
# expressions evaluates to false, FAIL will be returned.
#
# Example(s):
# constant_expression example {1+2 == 3} -> PASS
# constant_expression example {7+3 == 10} {10*10 == 110} -> FAIL

proc constant_expression { class args } {
    set class_data "
class $class \{
    $class ()\{\}
    void foo(int i) \{
        switch (i) \{
            case 0:
"

    set total_cases 0
    foreach expression $args {
        incr total_cases
        puts $expression
        append class_data [
          format "%scase ((%s) ? %d : 0):\n" "            " $expression $total_cases]
    }

    
    append class_data "        \}\n    \}\n\}\n"

    compile [saveas $class.java $class_data]
}


# switch_labels: Compile a switch statement
# with the given parameter type and
# labels supplied by the user.
#
# Example(s):
# switch_labels example int -> PASS (switch with no labels)
# switch_labels example int {case 0:} {case 1: break} -> PASS

proc switch_labels { class paramtype args } {
    set class_data "
class $class \{
    $class ()\{\}
    void foo($paramtype i) \{
        switch (i) \{
"

    foreach arg $args {
        append class_data "            $arg\n"
    }

    append class_data "        \}\n    \}\n\}\n"

    compile [saveas $class.java $class_data]
}

# is_assignable_to: Check that the given expression is
# assignable to the given type.
#
# Example(s):
# is_assignable_to example1 int {1 * 2L} -> FAIL
# is_assignable_to example2 long {1 * 2L} long {3 * 4L} -> PASS

proc is_assignable_to { class args } {
    if {[llength $args] < 2 || ([llength $args] % 2 != 0)} {
        error "usage: is_assignable_to class type expression ... type expression ..."
    }

    set class_data "
class $class \{
    $class ()\{\}
    void foo() \{
"

    set total_expressions 0
    foreach {type expression} $args {
        incr total_expressions
        append class_data [
          format "%s%s n%d = %s\;\n" "        " $type $total_expressions $expression]
    }
    append class_data "    \}\n\}\n"
    compile [saveas $class.java $class_data]
}

# empty_class: Wraps the data inside an empty class
# and then compiles it.
#
# Example(s):
# empty_class example1 {int foo() {}} -> FAIL
# empty_class example2 {int foo() {return 0;}} -> PASS
proc empty_class { class data } {
    set class_data "
class $class \{
    $data
\}\n"
    compile [saveas $class.java $class_data]
}

proc pedantic_empty_class { class data } {
    set class_data "
class $class \{
    $data
\}\n"
    compile -pedantic [saveas $class.java $class_data]
}

# empty_main: Wraps the data inside an empty main method
# and then compiles it.
#
# Example(s):
# empty_main example1 {example1() {}} -> FAIL
# empty_main example2 {new example2() {}} -> PASS

proc empty_main { class data } {
    set class_data "
public class $class \{
    $class ()\{\}
    public static void main(String\[\] args) \{
        $data
    \}
\}\n"

    compile [saveas $class.java $class_data]
}

proc pedantic_empty_main { class data } {
    set class_data "
public class $class \{
    $class ()\{\}
    public static void main(String\[\] args) \{
        $data
    \}
\}\n"

    compile -pedantic [saveas $class.java $class_data]
}

