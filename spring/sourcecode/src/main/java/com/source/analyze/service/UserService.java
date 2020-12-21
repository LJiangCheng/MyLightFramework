package com.source.analyze.service;

import com.source.analyze.bean.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public User getUser() {
        return new User("zhangsan", 11);
    }

}
