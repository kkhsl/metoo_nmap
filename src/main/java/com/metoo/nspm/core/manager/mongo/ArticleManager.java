package com.metoo.nspm.core.manager.mongo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.metoo.nspm.core.service.mongo.ArticleService;
import com.metoo.nspm.entity.mongo.Article;
import org.apache.ibatis.annotations.Delete;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequestMapping("/admin/mongo/article")
@RestController
public class ArticleManager {

    // 测试Mongo begin

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public List<Article> test(){
        List<Article> articleList = this.articleService.findAll();
        System.out.println(JSONObject.toJSON(articleList));
        return articleList;
    }

    @GetMapping("/{id}")
    public Article findById(@PathVariable String id){
        Optional<Article> article = this.articleService.findArticleById(id);
        if(article.isPresent()){
            return article.get();
        }
        return null;
    }

    @GetMapping("/findAllByPage")
    public List findAllByPage(int pageNum, int pageSize, Integer sequence){
        Map params = new HashMap();
        params.put("sequence", sequence);
//        List articles = this.articleService.findAllByPageable(pageNum, pageSize);
        List articles = this.articleService.findAllByPageable(pageNum, pageSize, params);
        return articles;
    }

    @PostMapping
    public void save(@RequestBody Article article){
        this.articleService.saveArticle(article);
    }

    @PostMapping("/batch/save")
    public void batch_save(@RequestBody List<Article> articleList){
        this.articleService.batchSaveArticle(articleList);
    }

    @PutMapping
    public void update(@RequestBody Article article){
        this.articleService.updateArticle(article);
    }

    @PutMapping("/inc/{id}")
    public void updateIncrement(@PathVariable String id){
        this.articleService.updateArticle_mongoTemplate(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id){
        this.articleService.deleteById(id);
    }

}
