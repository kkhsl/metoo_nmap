package com.metoo.nspm.core.utils.quartz;

import com.metoo.nspm.core.manager.admin.tools.DateTools;
import com.metoo.nspm.core.service.api.zabbix.ZabbixService;
import com.metoo.nspm.core.service.zabbix.IGatherService;
import com.metoo.nspm.core.service.zabbix.IProblemService;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.entity.nspm.IpAddress;
import com.metoo.nspm.entity.nspm.Route;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @EnableScheduling：Spring系列框架中SpringFramwork自带的定时任务（org.springframework.scheduling.annotation.*）
 *
 * 注意事项：
 *  1：@Scheduled 多个任务使用了该方法，也是一个执行完执行下一个，并非并行执行；原因是scheduled默认线程数为1；
 *  2：每个定时任务才能执行下一次，禁止异步执行（@EnableAsync）
 *  3：并发执行，注意第“1”项里说明的，任务未执行完毕，另一个线程又来执行
 *
 */

@Configuration // 用于标记配置类，兼备Component
public class ScheduleGatherZabbixTask {

    Logger log = LoggerFactory.getLogger(ScheduleGatherZabbixTask.class);

    @Value("${task.switch.is-open}")
    private boolean flag;
    @Autowired
    private ZabbixService zabbixService;
    @Autowired
    private IProblemService problemService;
    @Autowired
    private IArpHistoryService arpHistoryService;
    @Autowired
    private IMacHistoryService macHistoryService;
    @Autowired
    private IRoutHistoryService routHistoryService;
    @Autowired
    private IIPAddressHistoryService iipAddressHistoryService;
    @Autowired
    private IGatherService gatherService;

    static DefaultWebSecurityManager manager = new DefaultWebSecurityManager();


    /**
     * 修改采集时间[调整子网在线时长]
     *
     * 采集Arp
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void configureTask() throws InterruptedException {
//        ThreadContext.bind(manager);
        //下面正常使用业务代码即可
        if(flag){
            Long time=System.currentTimeMillis();
            System.out.println("Arp开始采集");
            // 采集时间
            Calendar cal = Calendar.getInstance();
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);
            Date date = cal.getTime();
            // 此处开启两个线程
            // 存在先后顺序，先录取arp，在根据arp解析数据

            try {
                this.gatherService.gatherArpItem(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("===Arp采集耗时：" + (System.currentTimeMillis()-time) + "===");
        }
    }

    /**
     * 使用批量插入、Stream并行流优化采集
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void gatherMac() {
        if(flag){
            Long time = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);
            Date date = cal.getTime();
            try {
                StopWatch watch = StopWatch.createStarted();
                this.gatherService.gatherMacThreadPool4(date);
                watch.stop();
                System.out.println("采集总耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info("Task-Mac采集结束，采集时间为：" + (System.currentTimeMillis()-time));
        }
    }

    /**
     * 采集IpAddress
     */
    @Scheduled(cron = "0 */1 * * * ?")
    // 添加定时任务
    public void gatherIpAddress(){
//        ThreadContext.bind(manager);
        //下面正常使用业务代码即可
        if(flag){
            Long time=System.currentTimeMillis();
            System.out.println("IpAddress开始采集");
            // 采集时间
            Calendar cal = Calendar.getInstance();
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);
            Date date = cal.getTime();
            try {
                this.gatherService.gatherIpaddressItem(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("===IpAddress采集耗时：" + (System.currentTimeMillis()-time) + "===");
        }
    }


    @Scheduled(cron = "0 */1 * * * ?")
    public void gatherStp(){
//        ThreadContext.bind(manager);
        //下面正常使用业务代码即可
        if(flag){
            Long time=System.currentTimeMillis();
            System.out.println("Stp开始采集");
            // 采集时间
            Calendar cal = Calendar.getInstance();
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);
            Date date = cal.getTime();
            try {
                this.gatherService.gatherSpanningTreeProtocol(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("===Stp采集耗时：" + (System.currentTimeMillis()-time) + "===");
        }
    }

    /**
     * 采集Problem
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void gatherThreadProblem(){
        //下面正常使用业务代码即可
        if(flag) {
            // 采集时间
            Long begin = System.currentTimeMillis();
            this.zabbixService.gatherProblem();
            Long end = System.currentTimeMillis();
            log.info("执行时间：" + (end - begin));
        }
    }

//    @Scheduled(cron = "0 */1 * * * ?")
//    public void updateProblemStatus(){
//        System.out.println("执行更新problem");
//        this.zabbixService.updateProblemStatus();
//    }

    /**
     * 采集路由
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void updateRout(){
//        ThreadContext.bind(manager);
        if(flag) {// false
            Map params = new HashMap();
            Calendar calendar = Calendar.getInstance();
            params.put("endClock", DateTools.getTimesTamp10(calendar.getTime()));
            calendar.add(Calendar.MINUTE, -1);
            params.put("startClock", DateTools.getTimesTamp10(calendar.getTime()));
            int count = this.problemService.selectCount(params);
//            if(count > 0){
            if(true){
                Long time=System.currentTimeMillis();
                System.out.println("Rout开始采集");
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.clear(Calendar.SECOND);
                    cal.clear(Calendar.MILLISECOND);
//                    this.zabbixService.gatherRout(cal.getTime());
                    this.gatherService.gatherRouteItem(cal.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("===Rout采集耗时：" + (System.currentTimeMillis()-time) + "===");
            }
        }
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void gatherSnmp(){
        if(flag) {
            Long time=System.currentTimeMillis();
            System.out.println("Snmp开始采集");
            Calendar cal = Calendar.getInstance();
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);
            Date date = cal.getTime();
            try {
                this.gatherService.gatherHostSnmp(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("===Snmp采集耗时：" + (System.currentTimeMillis()-time) + "===");
        }
    }


//    @Scheduled(cron = "0 0 0 */1 * ?")
    @Scheduled(cron = "0 0 0 1,15 * ?")
//    @Scheduled(cron = "0 */2 * * * ?")
    public void clearHistory(){
//        ThreadContext.bind(manager);
        if(flag) {
            Long time = System.currentTimeMillis();
            System.out.println("删除历史数据开始");

            Calendar cal = Calendar.getInstance();
            cal.add(cal.DATE,-15);
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            Date beginOfDate = cal.getTime();

            Map params = new HashMap();
            params.put("addTime", beginOfDate);

            // 删除Arp history
            try {
                this.arpHistoryService.deleteObjByMap(params);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                this.macHistoryService.deleteObjByMap(params);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                List<Route> routList = this.routHistoryService.selectObjByMap(params);
                if(routList.size() > 0){
                    this.routHistoryService.deleteObjByMap(params);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                List<IpAddress> ipAddresses = this.iipAddressHistoryService.selectObjByMap(params);
                if(ipAddresses.size() > 0){
                    this.iipAddressHistoryService.deleteObjByMap(params);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


//            try {
//                List<Arp> arps = this.arpHistoryService.selectObjByMap(params);
//                if(arps.size() > 0){
//                    List<Long> ids = arps.stream().map(Arp::getId).collect(Collectors.toList());
//                    this.arpHistoryService.deleteObjByMap(params);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

//            try {
//                List<Mac> macs = this.macHistoryService.selectObjByMap(params);
//                if(macs.size() > 0){
//                    List<Long> ids = macs.stream().map(Mac::getId).collect(Collectors.toList());
//                    this.macHistoryService.batchDelete(ids);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

//            try {
//                List<Route> routList = this.routHistoryService.selectObjByMap(params);
//                if(routList.size() > 0){
//                    this.routHistoryService.batchDelete(routList);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            try {
//                List<IpAddress> ipAddresses = this.iipAddressHistoryService.selectObjByMap(params);
//                if(ipAddresses.size() > 0){
//                    this.iipAddressHistoryService.batchDelete(ipAddresses);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            System.out.println("==删除数据耗时：" + (System.currentTimeMillis()-time) + "===");
        }
    }

    @Test
    public void test(){
        Calendar cal = Calendar.getInstance();
        cal.add(cal.DATE,-15);
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        Date beginOfDate = cal.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println ("===:"+formatter.format(beginOfDate) );
    }

}
