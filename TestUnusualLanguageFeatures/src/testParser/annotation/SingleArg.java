package testParser.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
public @interface SingleArg {
	 String value();
}
