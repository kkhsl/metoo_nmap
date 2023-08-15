package com.metoo.nspm.core.service.zabbix.impl;

import com.metoo.nspm.core.config.shiro.cache.RedisCache;
import com.metoo.nspm.core.manager.admin.tools.DateTools;
import com.metoo.nspm.core.service.api.zabbix.ZabbixItemService;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.service.zabbix.IGatherService;
import com.metoo.nspm.core.service.zabbix.ItemService;
import com.metoo.nspm.core.utils.SystemOutputLogUtils;
import com.metoo.nspm.entity.nspm.Arp;
import com.metoo.nspm.entity.nspm.ArpTemp;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
//@Transactional
public class GatherServiceImpl implements IGatherService {

    Logger log = LoggerFactory.getLogger(GatherServiceImpl.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private ZabbixItemService zabbixItemService;
    @Autowired
    private IArpService arpService;
    @Autowired
    private IArpHistoryService arpHistoryService;
    @Autowired
    private IMacService macService;
    @Autowired
    private IMacHistoryService macHistoryService;
    @Autowired
    private IRoutService routService;
    @Autowired
    private IRoutHistoryService routHistoryService;
    @Autowired
    private IArpTempService arpTempService;
    @Autowired
    private IIPAddressService ipAddressService;
    @Autowired
    private IIPAddressHistoryService ipAddressHistoryService;
    @Autowired
    private ISpanningTreeProtocolService spanningTreeProtocolService;
    @Autowired
    private ISpanningTreeProtocolHistoryService spanningTreeProtocolHistoryService;
    @Autowired
    private IGatherAlarmService gatherAlarmService;
    @Autowired
    private ITerminalService terminalService;
    @Autowired
    private ITerminalHistoryService terminalHistoryService;

    @Override
    public void gatherArpItem(Date time) {

        this.itemService.gatherArpItem(time);
        // 打标签
        this.zabbixItemService.arpTag();
        // 同步到arp
        this.arpService.truncateTable();

        this.arpService.copyArpTemp();
        // 记录历史Arp
        this.arpHistoryService.copyArpTemp();

        // 记录采集网关欺骗、主机欺骗
        this.gatherAlarmService.gatherAlarms();

    }

    @Override
    public void gatherMacItem(Date time) {
        Long startTime = System.currentTimeMillis();
        log.info("Mac采集开始：" + DateTools.getCurrentDateByCh(startTime));
        this.itemService.gatherMacItem(time);
        log.info("Mac采集结束，采集时间为：" + (System.currentTimeMillis() - startTime) / 60 / 1000 + " 分钟"
                + (System.currentTimeMillis() - startTime) / 1000 + "秒 ");

        Long tagTime = System.currentTimeMillis();
        log.info("Mac-Tag 开始：" + DateTools.getCurrentDateByCh(tagTime));
        this.zabbixItemService.macTag();
        log.info("Mac-Tag 结束，采集时间为：" + (System.currentTimeMillis() - tagTime) / 60 / 1000 + " 分钟"
                + (System.currentTimeMillis() - tagTime) / 1000 + "秒 ");

        Long topologyTime = System.currentTimeMillis();
        log.info("Mac-Topology 开始：" + DateTools.getCurrentDateByCh(topologyTime));
        this.itemService.topologySyncToMac();
        log.info("Mac-Topology 结束，采集时间为：" + (System.currentTimeMillis() - topologyTime) / 60 / 1000 + " 分钟"
                + (System.currentTimeMillis() - topologyTime) / 1000 + "秒 ");

        Long copyTime = System.currentTimeMillis();
        log.info("Mac-copy 开始：" + DateTools.getCurrentDateByCh(copyTime));
        // 同步网元数据到Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();
        // 记录历史
        this.macHistoryService.copyMacTemp();
        log.info("Mac-copy 结束，采集时间为：" + (System.currentTimeMillis() - copyTime) / 60 / 1000 + " 分钟"
                + (System.currentTimeMillis() - copyTime) / 1000 + "秒 ");

    }

    @Override
    public void gatherMacBatch(Date time) {
        StopWatch watch = new StopWatch();
        watch.start();
        this.itemService.gatherMacBatch(time);
        watch.stop();
        log.info("Mac采集耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");

        watch.reset();
        watch.start();
        this.zabbixItemService.labelTheMac(time);
        watch.stop();
        log.info("Mac-tag采集耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");

        watch.reset();
        watch.start();
        this.itemService.topologySyncToMacBatch(time);
        watch.stop();
        log.info("Mac-topology采集耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");

        watch.reset();
        watch.start();
        // 同步网元数据到Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();
        // 记录历史
        this.macHistoryService.copyMacTemp();
        watch.stop();
        log.info("Mac-copy采集耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");
    }

    @Override
    public void gatherMacBatchStream(Date time) {
        StopWatch watch = new StopWatch();
        watch.start();
        this.itemService.gatherMacBatchStream(time);
        watch.stop();
        System.out.println("Mac采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        this.zabbixItemService.labelTheMac(time);
        watch.stop();
        System.out.println("Mac-tag采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        this.itemService.topologySyncToMacBatch(time);
        watch.stop();
        System.out.println("Mac-topology采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        // 同步网元数据到Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();
        // 记录历史
        this.macHistoryService.copyMacTemp();
        watch.stop();
        System.out.println("Mac-copy采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");
    }

    @Override
    public void gatherMacThreadPool(Date time) {
        StopWatch watch = new StopWatch();
        watch.start();
        this.itemService.gatherMacThreadPool(time);
        watch.stop();
        System.out.println("Mac采集-写入 耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        this.itemService.topologySyncToMacBatch(time);
        watch.stop();
        System.out.println("Mac-topology采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");


        watch.reset();
        watch.start();
        this.zabbixItemService.labelTheMac(time);
        watch.stop();
        System.out.println("Mac-tag采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        // 同步网元数据到Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();

        this.terminalService.syncHistoryMac(time);

        // 记录历史
        this.macHistoryService.copyMacTemp();
        watch.stop();


        this.terminalService.syncMacDtToTerminal();


        System.out.println("Mac-copy采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");


    }

    @Override
    public void gatherMacThreadPool2(Date time) throws InterruptedException {
        ExecutorService exe = Executors.newSingleThreadExecutor();
        StopWatch watch = new StopWatch();
        watch.start();
        exe.execute(new Runnable() {
            @Override
            public void run() {
                itemService.gatherMacThreadPool(time);
            }
        });
        watch.stop();
        System.out.println("Mac采集-写入 耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        exe.execute(new Runnable() {
            @Override
            public void run() {
                zabbixItemService.labelTheMac(time);
            }
        });

        watch.stop();
        System.out.println("Mac-tag采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        exe.execute(new Runnable() {
            @Override
            public void run() {
                itemService.topologySyncToMacBatch(time);
            }
        });
        watch.stop();
        System.out.println("Mac-topology采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");

        watch.reset();
        watch.start();
        // 同步网元数据到Mac
        exe.execute(new Runnable() {
            @Override
            public void run() {
                macService.truncateTable();
                macService.copyMacTemp();
                // 记录历史
                macHistoryService.copyMacTemp();
            }
        });
        watch.stop();
        System.out.println("Mac-copy采集耗时：" + watch.getTime(TimeUnit.SECONDS) + " 秒.");
    }

    @Override
    public void gatherMacThreadPool3(Date time) {
        this.itemService.testGatherMacThreadPool(time);
    }

    @Override
    public void gatherMacThreadPool4(Date time) {

        StopWatch watch = new StopWatch();
        watch.start();
        this.itemService.gatherMacCallable(time);
        watch.stop();

        log.info("Mac采集-写入 耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");

        watch.reset();
        watch.start();
        this.itemService.topologySyncToMacBatch(time);
        watch.stop();

        log.info("Mac-topology采集耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");

        watch.reset();
        watch.start();
        this.zabbixItemService.labelTheMac(time);
        watch.stop();

        log.info("Mac-tag采集耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");

        watch.reset();
        watch.start();

        // 同步网元数据到Mac

        this.macService.truncateTable();

        this.macService.copyMacTemp();

        this.terminalService.syncHistoryMac(time);

        this.macHistoryService.copyMacTemp();
        watch.stop();

        this.terminalService.syncMacDtToTerminal();
        // 记录终端历史
//        this.terminalHistoryService.syncTerminalToHistory(time);

        log.info("Mac-tag采集耗时：" + watch.getTime(TimeUnit.SECONDS) + "秒.");
    }

    @Override
    public void gatherRouteItem(Date time) {
        this.itemService.gatherRouteItem(time);
        this.routService.truncateTable();
        this.routService.copyRoutTemp();
        this.routHistoryService.copyRoutTemp();

    }

    @Override
    public void gatherIpaddressItem(Date time) {
        this.itemService.gatherIpaddressItem(time);
        this.ipAddressService.truncateTable();
        this.ipAddressService.copyIpAddressTemp();
        // 记录历史Ipaddress
        this.ipAddressHistoryService.copyIpAddressTemp();
    }

    @Override
    public void gatherProblemItem(Date time) {
        this.itemService.gatherProblemItem(time);
//        this.ipAddressService.truncateTable();
//        this.ipAddressService.copyIpAddressTemp();
//        // 记录历史Ipaddress
//        this.ipAddressHistoryService.copyIpAddressTemp();
    }

    @Override
    public void testTransactional() {
        ArpTemp arpTemp = new ArpTemp();
        arpTemp.setDeviceName("a");
        this.arpTempService.save(arpTemp);
        this.itemService.testTransactional();
    }

    @Override
    public void gatherSpanningTreeProtocol(Date time) {

        this.itemService.gatherStp(time);// 采集 mstpport
        this.itemService.gathermstpinstance(time);// 采集 mstpinstance
        this.itemService.gathermstpDR(time);// 采集 mstpDR
        this.itemService.writeStpRemote();

        //
        this.spanningTreeProtocolService.truncateTable();
        this.spanningTreeProtocolService.copyMacTemp();
        this.spanningTreeProtocolHistoryService.truncateTable();
        this.spanningTreeProtocolHistoryService.copyMacTemp();
    }

    @Override
    public void gatherHostSnmp(Date time) {
        this.itemService.gatherSnmp(time);
    }


}
