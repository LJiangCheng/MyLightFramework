package com.source.analyze.events.event;

import com.source.analyze.bean.User;
import org.springframework.context.ApplicationEvent;

public class DemoEvent extends ApplicationEvent {
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public DemoEvent(User source) {
        super(source);
    }
}
