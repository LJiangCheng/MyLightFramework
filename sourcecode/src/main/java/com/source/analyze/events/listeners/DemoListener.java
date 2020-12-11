package com.source.analyze.events.listeners;

import com.source.analyze.bean.User;
import com.source.analyze.events.event.DemoEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DemoListener implements ApplicationListener<DemoEvent> {

    public void onApplicationEvent(DemoEvent event) {
        User source = (User) event.getSource();
        System.out.println(source);
    }
}
