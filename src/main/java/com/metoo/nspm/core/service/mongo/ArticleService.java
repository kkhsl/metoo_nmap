package com.metoo.nspm.core.service.mongo;

import com.metoo.nspm.core.mapper.mongo.ArticleDao;
import com.metoo.nspm.entity.mongo.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArticleService {

    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private MongoTemplate mongoTemplate;

    // 查询所有文章
    public List <Article> findAll(){
        return this.articleDao.findAll();
    }

    // 根据Id查询文章
    public Optional<Article> findArticleById(String id){
        return this.articleDao.findById(id);
    }

    // 分页查询
//    public List<Object> findAllByPageable(int pageNum, int pageSize){
//        //创建查询对象
//        Query query = new Query();
//        //设置起始数
//        query.skip((pageNum - 1) * pageSize);
//        //设置查询条数
//        query.limit(pageSize);
//
//        return mongoTemplate.find(query, Object.class, "list");
//    }

    public List findAllByPageable(int pageNum, int pageSize, Map<String, Object> conditions){
        //创建查询对象
        Query query = new Query();
        //设置起始数
        query.skip((pageNum - 1) * pageSize);
        //设置查询条数
        query.limit(pageSize);

//        if(startTime!=null&&endTime!=null){
//
//            criteria.andOperator(
//
//                    Criteria.where("createTime").gte(startTime),
//
//                    Criteria.where("createTime").lt(endTime)
//
//            );
//
//        }

        if (conditions != null) {
            for (String field: conditions.keySet()) {
                if(conditions.get(field) != null){
                    query.addCriteria(new Criteria(field).is(conditions.get(field)));
                }
            }
        }

        //排序规则 根据分类倒叙，排序升序
        query.with(Sort.by(Sort.Order.desc("sequence")));

        return mongoTemplate.find(query, Object.class, "list");
    }

    // 保存文章
    public void saveArticle(Article article){
        this.articleDao.save(article);
    }

    // 更新文章
    public void updateArticle(Article article){
        this.articleDao.save(article);
    }

    // 更新文章（MongoTemplate）
    // 根据文章id 把 sequence自增1
    public void updateArticle_mongoTemplate(String id){
        // 查询对象
        Query query = Query.query(Criteria.where("_id").is(id));
        // 更新对象
        Update update = new Update();
//         update.inc("sequence", 1);
        update.inc("sequence");

        this.mongoTemplate.updateFirst(query, update, Article.class);
    }

    // 根据Id删除文章
    public void deleteById(String id){
        this.articleDao.deleteById(id);
    }

    // 批量添加
    public void batchSaveArticle(List<Article> articleList){
        this.articleDao.saveAll(articleList);
    }

    // 批量更新
}
