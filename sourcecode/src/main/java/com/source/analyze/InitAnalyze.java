package com.source.analyze;

import com.source.analyze.service.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class InitAnalyze {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext("com.source.analyze");
        UserService userService = (UserService) applicationContext.getBean("userService");
        System.out.println(userService.getUser());
    }

}
