package org.iushu.market.trade.okx.config;


import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscribeChannel {

    String[] op() default "";

    String[] event() default "";

    String[] channel() default "";

}
