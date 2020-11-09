package com.source.analyze.service;

import com.source.analyze.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final ArticleService articleService;

    @Autowired
    public UserService(ArticleService articleService) {
        this.articleService = articleService;
    }

    public User getUser() {
        System.out.println(articleService.getArticle());
        return new User("zhangsan", 11);
    }
}
