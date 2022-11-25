package org.iushu.market.trade.okx.config;


import org.iushu.market.Constants;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Profile(Constants.EXChANGE_OKX)
public @interface OkxComponent {

}
