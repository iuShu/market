package org.iushu.market.component.notify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("windowsNotifier")
@ConditionalOnProperty(name = "trade.notify.windows")
public class WindowsNotifier implements Notifier {

    @Override
    public void notify(String title, String content) {

    }

}
