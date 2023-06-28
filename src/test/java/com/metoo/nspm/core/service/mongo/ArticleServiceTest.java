package com.metoo.nspm.core.service.mongo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ArticleServiceTest {

    // 测试Mongo begin

    @Autowired
    private ArticleService articleService;

    @Test
    public void test(){
        this.articleService.findAll().stream().forEach(System.out::println);
    }
}
