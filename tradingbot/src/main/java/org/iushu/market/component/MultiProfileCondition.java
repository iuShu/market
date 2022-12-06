package org.iushu.market.component;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

public class MultiProfileCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(MultiProfile.class.getName());
        if (attrs != null) {
            for (Object value : attrs.get("value")) {
                for (String profile : (String[]) value)
                    if (!context.getEnvironment().acceptsProfiles(Profiles.of(profile)))
                        return false;
            }
            return true;
        }
        return true;
    }

}
