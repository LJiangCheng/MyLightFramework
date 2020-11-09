package com.source.analyze.service;

import com.source.analyze.bean.Article;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    public Article getArticle() {
        return new Article("NONE", "精卫衔微木，将以填沧海。刑天舞干戚，猛志固常在。");
    }

}
