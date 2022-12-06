package org.iushu.market.trade.okx.config;

import org.iushu.market.Constants;
import org.iushu.market.component.MultiProfile;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@MultiProfile({Constants.EXChANGE_OKX, "shadow"})
public @interface OkxShadowComponent {

    @AliasFor(annotation = Component.class)
    String value() default "";

}
