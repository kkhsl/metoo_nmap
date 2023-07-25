package com.metoo.nspm.core.manager.admin.action;

import com.github.pagehelper.Page;
import com.metoo.nspm.core.config.redis.util.MyRedisManager;
import com.metoo.nspm.core.manager.admin.tools.Md5Crypt;
import com.metoo.nspm.core.service.nspm.IRackService;
import com.metoo.nspm.core.service.nspm.TestInitService;
import com.metoo.nspm.core.service.zabbix.IProblemService;
import com.metoo.nspm.core.utils.query.PageInfo;
import com.metoo.nspm.dto.NspmProblemDTO;
import com.metoo.nspm.entity.nspm.Rack;
import com.metoo.nspm.entity.nspm.User;
import com.metoo.nspm.entity.zabbix.Problem;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequestMapping("/test")
@RestController
public class TestManagerController {

    @Autowired
    private TestInitService testInitService;
    @Autowired
    private IRackService rackService;
    @Autowired
    private IProblemService problemService;

    private static Long domainId = 0L;

    @Value("asd")
    private String client;

    private String b;

    public void init(){
        this.b="456";
    }

    public static void main(String[] args) {
//        System.out.println(System.getenv());

        try {
            testThreadPool();
//            testThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/admin/testInit")
    public String test(){
        System.out.println(client);
        System.out.println(b);
        return this.testInitService.getA();
    }

    @RequestMapping("testadd")
    public void testAdd(){
        Map params = new HashMap();
        params.put("orderBy", "addTime");
        params.put("orderType","desc");
        List<Rack> racks = this.rackService.selectObjByMap(params);
        if(racks.size() > 0){
            Rack rack = racks.get(0);
            if(rack.getId() != domainId && rack.getId() > domainId){
                domainId = rack.getId();
                System.out.println(domainId);
                // 更新路由数据
            }
        }
    }

    public static int testThread() throws InterruptedException {
        int n = 10;
        for (int i = 1; i <= n; i++ ){

            System.out.println(Thread.currentThread().getName());
            Thread.sleep(1000);
        }
        return n;
    }

    public static int testThreadPool() throws InterruptedException {
        Long begin = System.currentTimeMillis();
        int n = 1000;
        ExecutorService exe = Executors.newFixedThreadPool(1000);;
        for (int i = 1; i <= n; i++ ){
            int finalI = i;
            exe.execute(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                        System.out.println(Thread.currentThread().getName());
                        System.out.println("pool:" + finalI);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }}));

        }
        if(exe != null){
            exe.shutdown();
        }
        while (true) {
            if (exe == null || exe.isTerminated()) {
                Long end = System.currentTimeMillis();
                System.out.println(end - begin);
                return n;

            }
        }
    }


    @Autowired
    private static MyRedisManager redisWss = new MyRedisManager("ws");

    /**
     * 测试MD5
     *  toString() | JSONObject.toJSONString()
     * @param dto
     */
    @PostMapping("/comparison")
    public void comparison(@RequestBody NspmProblemDTO dto){

        Map map1 = new HashMap();
        Map map2 = new HashMap();

        List list1 = new ArrayList();
        List list2 = new ArrayList();

        User user1 = new User();
        User user2 = new User();

        Page<Problem> page1 = this.problemService.selectConditionQuery(dto);
        Object o1 = new PageInfo<Problem>(page1);


//        Page<Problem> page2 = this.problemService.selectConditionQuery(dto);
//        Object o2 = new PageInfoMMMM<Problem>(page2);

        Object o2 = redisWss.get("cb9d11b5-7fa4-4626-a221-9ff8bc010678:8:0");

        boolean flag = Md5Crypt.getDiffrent(o1, o2);
        System.out.println(flag);

        boolean flag1 = Md5Crypt.getDiffrentStr(o1, o2);
        System.out.println(flag1);
    }

    // 测试 == equals
    @Test
    public void testEquals(){
        int a = 10;
        int b = 10;
        System.out.println(a == b);

        Integer n = 10;
        Integer m = 10;
        System.out.println(n == m);
        System.out.println(n.equals(m));

        Integer j = new Integer(10);
        Integer g = new Integer(10);
        System.out.println(j);
        System.out.println(g);
        System.out.println(j == g);// 如果是引用类型则比较对象的内存地址
        System.out.println(j.equals(g));


    }
}
