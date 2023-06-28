package com.metoo.nspm.entity.mongo;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.redis.core.index.Indexed;

import javax.persistence.Id;
import java.io.Serializable;

@Data
// 复合索引
@CompoundIndex(def = "{'name':-1,'sequence:1'}")
@Document(collection = "list")// 可以省略，如果省略，则使用类名小写映射集合
public class Article implements Serializable {

    @Id
    private String id;

    @Field("name")
    private String name;

    // 单字段索引
    @Indexed
    private Integer sequence;

}
