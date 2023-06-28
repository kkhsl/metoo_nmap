package com.metoo.nspm.core.mapper.mongo;

import com.metoo.nspm.entity.mongo.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArticleDao extends MongoRepository<Article, String> {
}
