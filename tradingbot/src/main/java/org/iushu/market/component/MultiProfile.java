package org.iushu.market.component;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(MultiProfileCondition.class)
public @interface MultiProfile {

    String[] value();

}
