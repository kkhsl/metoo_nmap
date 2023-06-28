package com.metoo.nspm.core.manager.mongo;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.regex.Pattern;

public class MongoUtil {

    private Integer pageSize;
    private Integer currentPage;


    public void start(Integer currentPage, Integer pageSize, Query query) {
        pageSize = pageSize == 0 ? 10 : pageSize;
        query.limit(pageSize);
        query.skip((currentPage - 1) * pageSize);
        this.pageSize = pageSize;
        this.currentPage = currentPage;
    }

//    public Page getPage(long total, List<T> list) {
//        Page<T> page = new Page<>();
//        page.setRecords(list);
//        page.setCurrent(this.currentPage);
//        page.setTotal(total);
//        page.setPages(this.pageSize);
//        return page;
//    }

    /**
     * 用于模糊查询忽略大小写
     * @param string
     * @return
     */
    public Pattern getPattern(String string) {
        return Pattern.compile("^.*" + string + ".*$", Pattern.CASE_INSENSITIVE);
    }
}
