package com.metoo.nspm.core.service.zabbix.impl;

import com.metoo.nspm.core.manager.admin.tools.DateTools;
import com.metoo.nspm.core.service.api.zabbix.ZabbixItemService;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.service.zabbix.IGatherService;
import com.metoo.nspm.core.service.zabbix.ItemService;
import com.metoo.nspm.core.utils.SystemOutputLogUtils;
import com.metoo.nspm.entity.nspm.ArpTemp;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
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

    @Override
    public void gatherArpItem(Date time) {

        this.itemService.gatherArpItem(time);
        // ?????????
        this.zabbixItemService.arpTag();
        // ?????????arp
        this.arpService.truncateTable();

        this.arpService.copyArpTemp();
        // ????????????Arp
        this.arpHistoryService.copyArpTemp();

        // ???????????????????????????????????????
        this.gatherAlarmService.gatherAlarms();

    }

    @Override
    public void gatherMacItem(Date time) {
        Long startTime = System.currentTimeMillis();
        log.info("Mac???????????????" + DateTools.getCurrentDateByCh(startTime));
        this.itemService.gatherMacItem(time);
        log.info("Mac?????????????????????????????????" + (System.currentTimeMillis() - startTime) / 60 / 1000 + " ??????"
                + (System.currentTimeMillis() - startTime) / 1000 + "??? ");

        Long tagTime = System.currentTimeMillis();
        log.info("Mac-Tag ?????????" + DateTools.getCurrentDateByCh(tagTime));
        this.zabbixItemService.macTag();
        log.info("Mac-Tag ???????????????????????????" + (System.currentTimeMillis() - tagTime) / 60 / 1000 + " ??????"
                + (System.currentTimeMillis() - tagTime) / 1000 + "??? ");

        Long topologyTime = System.currentTimeMillis();
        log.info("Mac-Topology ?????????" + DateTools.getCurrentDateByCh(topologyTime));
        this.itemService.topologySyncToMac();
        log.info("Mac-Topology ???????????????????????????" + (System.currentTimeMillis() - topologyTime) / 60 / 1000 + " ??????"
                + (System.currentTimeMillis() - topologyTime) / 1000 + "??? ");

        Long copyTime = System.currentTimeMillis();
        log.info("Mac-copy ?????????" + DateTools.getCurrentDateByCh(copyTime));
        // ?????????????????????Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();
        // ????????????
        this.macHistoryService.copyMacTemp();
        log.info("Mac-copy ???????????????????????????" + (System.currentTimeMillis() - copyTime) / 60 / 1000 + " ??????"
                + (System.currentTimeMillis() - copyTime) / 1000 + "??? ");

    }

    @Override
    public void gatherMacBatch(Date time)  {
        StopWatch watch = new StopWatch();
        watch.start();
        this.itemService.gatherMacBatch(time);
        watch.stop();
        log.info("Mac???????????????" + watch.getTime(TimeUnit.SECONDS) + "???.");

        watch.reset();
        watch.start();
        this.zabbixItemService.labelTheMac();
        watch.stop();
        log.info("Mac-tag???????????????" + watch.getTime(TimeUnit.SECONDS) + "???.");

        watch.reset();
        watch.start();
        this.itemService.topologySyncToMacBatch(time);
        watch.stop();
        log.info("Mac-topology???????????????" + watch.getTime(TimeUnit.SECONDS) + "???.");

        watch.reset();
        watch.start();
        // ?????????????????????Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();
        // ????????????
        this.macHistoryService.copyMacTemp();
        watch.stop();
        log.info("Mac-copy???????????????" + watch.getTime(TimeUnit.SECONDS) + "???.");
    }

    @Override
    public void gatherMacBatchStream(Date time) {
        StopWatch watch = new StopWatch();
        watch.start();
        this.itemService.gatherMacBatchStream(time);
        watch.stop();
        System.out.println("Mac???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        this.zabbixItemService.labelTheMac();
        watch.stop();
        System.out.println("Mac-tag???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        this.itemService.topologySyncToMacBatch(time);
        watch.stop();
        System.out.println("Mac-topology???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        // ?????????????????????Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();
        // ????????????
        this.macHistoryService.copyMacTemp();
        watch.stop();
        System.out.println("Mac-copy???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");
    }

    @Override
    public void gatherMacThreadPool(Date time) {
        StopWatch watch = new StopWatch();
        watch.start();
        this.itemService.gatherMacThreadPool(time);
        watch.stop();
        System.out.println("Mac??????-?????? ?????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        this.zabbixItemService.labelTheMac();
        watch.stop();
        System.out.println("Mac-tag???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        this.itemService.topologySyncToMacBatch(time);
        watch.stop();
        System.out.println("Mac-topology???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        // ?????????????????????Mac
        this.macService.truncateTable();
        this.macService.copyMacTemp();
        // ????????????
        this.macHistoryService.copyMacTemp();
        watch.stop();
        System.out.println("Mac-copy???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");
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
        System.out.println("Mac??????-?????? ?????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        exe.execute(new Runnable() {
            @Override
            public void run() {
                zabbixItemService.labelTheMac();
            }
        });

        watch.stop();
        System.out.println("Mac-tag???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        exe.execute(new Runnable() {
            @Override
            public void run() {
                itemService.topologySyncToMacBatch(time);
            }
        });
        watch.stop();
        System.out.println("Mac-topology???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");

        watch.reset();
        watch.start();
        // ?????????????????????Mac
        exe.execute(new Runnable() {
            @Override
            public void run() {
                macService.truncateTable();
                macService.copyMacTemp();
                // ????????????
                macHistoryService.copyMacTemp();
            }
        });
        watch.stop();
        System.out.println("Mac-copy???????????????" + watch.getTime(TimeUnit.SECONDS) + " ???.");
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
        // ????????????Ipaddress
        this.ipAddressHistoryService.copyIpAddressTemp();
    }

    @Override
    public void gatherProblemItem(Date time) {
        this.itemService.gatherProblemItem(time);
//        this.ipAddressService.truncateTable();
//        this.ipAddressService.copyIpAddressTemp();
//        // ????????????Ipaddress
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

        this.itemService.gatherStp(time);// ?????? mstpport
        this.itemService.gathermstpinstance(time);// ?????? mstpinstance
        this.itemService.gathermstpDR(time);// ?????? mstpDR
        this.itemService.writeStpRemote();

        //
        this.spanningTreeProtocolService.truncateTable();
        this.spanningTreeProtocolService.copyMacTemp();
        this.spanningTreeProtocolHistoryService.truncateTable();
        this.spanningTreeProtocolHistoryService.copyMacTemp();
    }
}
