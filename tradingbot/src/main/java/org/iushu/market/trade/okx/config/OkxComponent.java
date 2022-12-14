package org.iushu.market.trade.okx.config;


import org.iushu.market.Constants;
import org.iushu.market.component.MultiProfile;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@MultiProfile({Constants.EXChANGE_OKX, "test"})
public @interface OkxComponent {

}
