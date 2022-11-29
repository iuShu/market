package org.iushu.market.component.notify;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component("notifier")
public class NotifierAggregator implements Notifier, ApplicationContextAware {

    private Set<Notifier> notifiers = new HashSet<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Notifier> beans = applicationContext.getBeansOfType(Notifier.class);
        List<Notifier> notifiers = beans.values().stream().filter(n -> n.getClass() != NotifierAggregator.class).collect(Collectors.toList());
        this.notifiers.addAll(notifiers);
    }

    @Override
    public void notify(String title, String content) {
        this.notifiers.forEach(notifier -> notifier.notify(title, content));
    }

}
