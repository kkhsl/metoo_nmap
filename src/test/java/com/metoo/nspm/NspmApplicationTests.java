package com.metoo.nspm;

import com.metoo.nspm.core.jwt.util.JwtUtil;
import com.metoo.nspm.core.service.mongo.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

@SpringBootTest
class NspmApplicationTests {

    @Test
    void contextLoads(){
        Map param = new HashMap();
        param.put("userName", "hkk");
//        JwtUtil.getToken(param);
    }

    public static void main(String[] args) {
        Map param = new HashMap();
        param.put("userName", "hkk");
        JwtUtil.getToken(param);
    }

}
