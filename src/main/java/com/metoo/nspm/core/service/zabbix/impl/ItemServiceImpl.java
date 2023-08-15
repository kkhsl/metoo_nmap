package com.metoo.nspm.core.service.zabbix.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.metoo.nspm.container.juc.callable.MyCallable;
import com.metoo.nspm.core.config.redis.util.MyRedisManager;
import com.metoo.nspm.core.manager.admin.tools.DateTools;
import com.metoo.nspm.core.manager.admin.tools.MacUtil;
import com.metoo.nspm.core.manager.myzabbix.utils.ItemUtil;
import com.metoo.nspm.core.mapper.nspm.zabbix.MacMapper;
import com.metoo.nspm.core.mapper.zabbix.ItemMapper;
import com.metoo.nspm.core.mapper.zabbix.ItemTagMapper;
import com.metoo.nspm.core.service.api.zabbix.ZabbixItemService;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.service.topo.ITopoNodeService;
import com.metoo.nspm.core.service.zabbix.ItemService;
import com.metoo.nspm.core.utils.MyStringUtils;
import com.metoo.nspm.core.utils.network.IpUtil;
import com.metoo.nspm.core.utils.network.IpV4Util;
import com.metoo.nspm.entity.nspm.*;
import com.metoo.nspm.entity.zabbix.Interface;
import com.metoo.nspm.entity.zabbix.Item;
import com.metoo.nspm.entity.zabbix.ItemTag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
//@Transactional(rollbackFor = Exception.class)
public class ItemServiceImpl implements ItemService {

    Logger log = LoggerFactory.getLogger(ItemServiceImpl.class);

    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private ItemTagMapper itemTagMapper;
    @Autowired
    private ITopoNodeService topoNodeService;
    @Autowired
    private IpDetailService ipDetailService;
    @Autowired
    private ZabbixItemService zabbixItemService;
    @Autowired
    private ItemUtil itemUtil;
    @Autowired
    private IArpService arpService;
    @Autowired
    private IArpTempService arpTempService;
    @Autowired
    private MacMapper macMapper;
    @Autowired
    private IMacTempService macTempService;
    @Autowired
    private IRoutTempService routTempService;
    @Autowired
    private IIPAddressTempService ipAddressTempService;
    @Autowired
    private ITopologyService topologyService;
    @Autowired
    private MacUtil macUtil;
    @Autowired
    private ISpanningTreeProtocolTempService spanningTreeProtocolTempService;
    @Autowired
    private IMacService macService;
    @Autowired
    private IpAlterationService ipAlterationService;
    @Autowired
    private INetworkElementService networkElementService;
    @Autowired
    private IDeviceTypeService deviceTypeService;
    @Autowired
    private InterfaceServiceImpl interfaceService;

    @Autowired
    private IHostSnmpService hostSnmpService;

    @Autowired
    private static MyRedisManager redisManager = new MyRedisManager("arp");

    @Autowired
    private static MyRedisManager redisMacChange = new MyRedisManager("change");

    @Autowired
    private RedisTemplate redisTemplate;

    public static void main(String[] args) throws Exception {
        List<String> strList = Arrays.asList("a", "b", "c", "d", "e");
        List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5);

        // 线程数默认和CPU个数一致
        strList.parallelStream().forEach((str) -> {
//            try {
//                System.out.println(Thread.currentThread().getName() + "-" + str);
//            } catch (InterruptedException e) {
//            }

            System.out.println(Thread.currentThread().getName() + "-" + str);
        });

        intList.parallelStream().forEach((number) -> {
//            try {
//                System.out.println(Thread.currentThread().getName() + " - " + number);
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//            }
            System.out.println(Thread.currentThread().getName() + " - " + number);
        });
        System.out.println("---");
    }

    @Override
    public void gatherArpItem(Date time) {
        Map params = new HashMap();
        List<Map> ipList = this.topoNodeService.queryNetworkElement();
        List<String> ips = new ArrayList();// 初始化Ip集合（采集IP,记录IP在线时长以及使用率）
        if (ipList != null && ipList.size() > 0) {
            this.arpTempService.truncateTable();
            for (Map map : ipList) {
                // 采集Arp(Zabbix)
                String deviveName = map.get("deviceName").toString();
                String deviceType = map.get("deviceType").toString();
                String ip = map.get("ip").toString();
                String uuid = map.get("uuid").toString();

                params.put("ip", ip);
                params.put("tag", "arphillstone");
                params.put("index", "ifindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> itemTagArpList = itemMapper.selectItemTagByMap(params);
                if(itemTagArpList.size() > 0){
                    for (Item item : itemTagArpList) {
                        List<ItemTag> tags = item.getItemTags();
                        ArpTemp arpTemp = new ArpTemp();
                        arpTemp.setDeviceName(deviveName);
                        arpTemp.setDeviceType(deviceType);
                        arpTemp.setUuid(uuid);
                        arpTemp.setDeviceIp(ip);
                        arpTemp.setTag("S");
                        // ip使用率
                        IpDetail ipDetail = new IpDetail();
                        if (tags != null && tags.size() > 0) {
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if (tag.getTag().equals("ip")) {
                                    arpTemp.setIp(IpUtil.ipConvertDec(value));
                                    ipDetail.setIp(IpUtil.ipConvertDec(value));
                                    ips.add(IpUtil.ipConvertDec(value));
                                }
                                if (tag.getTag().equals("mac")) {

                                    String mac = this.macUtil.supplement(value);
                                    arpTemp.setMac(mac);

//                                    arpTemp.setMac(value);
//                                    ipDetail.setMac(value);
                                    ipDetail.setMac(mac);

                                }if (tag.getTag().equals("type")) {
                                    switch (value){
                                        case "4":
                                            value = "static";
                                            break;
                                        case "3":
                                            value = "dynamic";
                                            break;
                                        case "1":
                                            value = "other";
                                            break;
                                        default:
                                            value = null;
                                            break;
                                    }
                                    arpTemp.setType(value);
                                }
                                if (tag.getTag().equals("ifindex")) {
                                    // 1, 获取ifbasic最小minIndex, minIndex + (ifindex - 1)
                                    //2, 根据新获取的index获取ifbasic的端口名
                                    ItemTag itemTag = this.itemTagMapper.selectItemTagMinIfIndex(ip);
                                    String ifindex = itemTag.getValue();
                                    int index = 0;
                                    if(ifindex != null){
                                        index = Integer.parseInt(ifindex);
                                        index = index + (Integer.parseInt(value) - 1);
                                        params.clear();
                                        params.put("ip", ip);
                                        params.put("ifindex", index);
                                        ItemTag itemTag2 = this.itemTagMapper.selectItemTagIfNameByIndex(params);
                                        arpTemp.setIndex(String.valueOf(index));
                                        arpTemp.setInterfaceName(itemTag2.getValue());
                                    }
                                }
                            }
                            // 保存Arp条目
                            if (arpTemp.getIp() != null && !arpTemp.getIp().equals("")) {

                                if(arpTemp.getMac() != null && !arpTemp.getMac().equals("")){
                                    ipAlterationService.RecordChange(arpTemp.getIp(), arpTemp.getMac());
                                }
//                                try {
//                                    if(arpTemp.getMac() != null && !arpTemp.getMac().equals("")){
//                                        Object mac = redisManager.get(arpTemp.getIp());
//                                        if(mac == null || mac.equals("")){
//                                            redisManager.put(arpTemp.getIp(), arpTemp.getMac());
//                                            redisMacChange.put(arpTemp.getIp(), 0);
//                                        }else if(!mac.toString().equals(arpTemp.getMac())){
//                                            // 更新mac 地址；变化次数加一
//                                            Object v = redisManager.get(arpTemp.getIp());
//                                            if(v == null){
//                                                redisMacChange.put(arpTemp.getIp(), 0);
//                                            }else{
//                                                redisManager.put(arpTemp.getIp(), arpTemp.getMac());
//                                                redisTemplate.opsForHash().increment(redisMacChange.getCacheName(), arpTemp.getIp(), 1);
//                                            }
//                                        }
//                                    }
//                                } catch (CacheException e) {
//                                    e.printStackTrace();
//                                }

                                params.clear();
                                params.put("ip", arpTemp.getIp());
                                params.put("deviceName", arpTemp.getDeviceName());
                                params.put("interfaceName", arpTemp.getInterfaceName());
                                List<ArpTemp> localArps = arpTempService.selectObjByMap(params);
                                if (localArps.size() == 0) {
                                    arpTemp.setAddTime(time);
                                    arpTempService.save(arpTemp);
                                }
                                // 记录ip使用率
                                IpDetail existingIp = ipDetailService.selectObjByIp(arpTemp.getIp());
                                if (existingIp == null) {
                                    ipDetail.setDeviceName(deviveName);
                                    ipDetailService.save(ipDetail);
                                }
                            }
                        }
                    }
                }
                if(itemTagArpList.size() <= 0){
                    params.put("ip", ip);
                    params.put("tag", "arp");
                    params.put("index", "ifindex");
                    params.put("tag_relevance", "ifbasic");
                    params.put("index_relevance", "ifindex");
                    params.put("name_relevance", "ifname");
                    List<Item> itemTagList = itemMapper.gatherItemByTag(params);
                    if (itemTagList.size() > 0) {
                        for (Item item : itemTagList) {
                            List<ItemTag> tags = item.getItemTags();
                            ArpTemp arpTemp = new ArpTemp();
                            arpTemp.setDeviceName(deviveName);
                            arpTemp.setDeviceType(deviceType);
                            arpTemp.setUuid(uuid);
                            arpTemp.setDeviceIp(ip);
                            arpTemp.setTag("S");
                            // ip使用率
                            IpDetail ipDetail = new IpDetail();
                            if (tags != null && tags.size() > 0) {
                                for (ItemTag tag : tags) {
                                    String value = tag.getValue();
                                    if (tag.getTag().equals("ip")) {
                                        arpTemp.setIp(IpUtil.ipConvertDec(value));
                                        ipDetail.setIp(IpUtil.ipConvertDec(value));
                                        ips.add(IpUtil.ipConvertDec(value));
                                    }
                                    if (tag.getTag().equals("mac")) {

                                        String mac = this.macUtil.supplement(value);
                                        arpTemp.setMac(mac);
                                        ipDetail.setMac(mac);
//                                        arpTemp.setMac(value);
//                                        ipDetail.setMac(value);
                                    }
                                    if (tag.getTag().equals("type")) {
                                        switch (value){
                                            case "4":
                                                value = "static";
                                                break;
                                            case "3":
                                                value = "dynamic";
                                                break;
                                            case "1":
                                                value = "other";
                                                break;
                                            default:
                                                value = null;
                                                break;
                                        }
                                        arpTemp.setType(value);
                                    }
                                    if (tag.getTag().equals("ifindex")) {
                                        if(value != null){
                                            arpTemp.setInterfaceName(tag.getName());
                                            arpTemp.setIndex(value);
                                        }else{
                                            //
                                            switch (value){
                                                case "1":
                                            }
                                        }
                                    }
                                }
                                // 保存Arp条目
                                if (arpTemp.getIp() != null && !arpTemp.getIp().equals("")) {
                                    if(arpTemp.getMac() != null && !arpTemp.getMac().equals("")){
                                        ipAlterationService.RecordChange(arpTemp.getIp(), arpTemp.getMac());
                                    }
//                                    try {
//                                        if(arpTemp.getMac() != null && !arpTemp.getMac().equals("")){
//                                            Object mac = redisManager.get(arpTemp.getIp());
//                                            if(mac == null || mac.equals("")){
//                                                redisManager.put(arpTemp.getIp(), arpTemp.getMac());
//                                                redisMacChange.put(arpTemp.getIp(), 0);
//                                            }else if(!mac.toString().equals(arpTemp.getMac())){
//                                                // 更新mac 地址；变化次数加一
//                                                Object v = redisManager.get(arpTemp.getIp());
//                                                if(v == null){
//                                                    redisMacChange.put(arpTemp.getIp(), 0);
//                                                }else{
//                                                    redisManager.put(arpTemp.getIp(), arpTemp.getMac());
//                                                    redisTemplate.opsForHash().increment(redisMacChange.getCacheName(), arpTemp.getIp(), 1);
//                                                }
//                                            }
//                                        }
//                                    } catch (CacheException e) {
//                                        e.printStackTrace();
//                                    }
                                    params.clear();
                                    params.put("ip", arpTemp.getIp());
                                    params.put("deviceName", arpTemp.getDeviceName());
                                    params.put("interfaceName", arpTemp.getInterfaceName());
                                    List<ArpTemp> localArps = arpTempService.selectObjByMap(params);
                                    if (localArps.size() == 0) {
                                        arpTemp.setAddTime(time);
                                        arpTempService.save(arpTemp);
                                    }
                                    // 记录ip使用率
                                    IpDetail existingIp = ipDetailService.selectObjByIp(arpTemp.getIp());
                                    if (existingIp == null) {
                                        ipDetail.setDeviceName(deviveName);
                                        ipDetailService.save(ipDetail);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // 补全(默认标记为L)
        for (Map map : ipList) {
            // 采集Arp(Zabbix)
            String deviveName = map.get("deviceName").toString();
            String deviceType = map.get("deviceType").toString();
            String ip = map.get("ip").toString();
            String uuid = map.get("uuid").toString();
            params.put("ip", ip);
            params.put("tag", "ifipaddr");
            params.put("tag_relevance", "ifbasic");
            params.put("index", "ifindex");
            params.put("index_relevance", "ifindex");
            params.put("name_relevance", "ifname");
            params.put("mac_relevance", "ifmac");
            List<Item> ipaddressItem = itemMapper.gatherItemByTagAndIndex(params);
            if (ipaddressItem.size() > 0) {
                for (Item item : ipaddressItem) {
                    List<ItemTag> tags = item.getItemTags();
                    ArpTemp arpTemp = new ArpTemp();
                    arpTemp.setDeviceName(deviveName);
                    arpTemp.setDeviceType(deviceType);
                    arpTemp.setUuid(uuid);
                    arpTemp.setDeviceIp(ip);
                    arpTemp.setType("static");
                    if (tags != null && tags.size() > 0) {
                        for (ItemTag tag : tags) {
                            String value = tag.getValue();
                            if (tag.getTag().equals("ipaddr")) {
                                arpTemp.setIp(IpUtil.ipConvertDec(value));
                            }
                            if (tag.getTag().equals("mask")) {
                                arpTemp.setMask(value);
                            }
//                                if (tag.getTag().equals("ifindex")) {
//                                    if(tag.getName() != null && !tag.getName().equals("")){
//                                        String[] values = tag.getName().split("%");
//                                        if(values.length == 2){
//                                            arpTemp.setInterfaceName(values[0]);
//                                            arpTemp.setMac(values[1]);
//                                        }
//                                    }
//                                }
                            if (tag.getTag().equals("ifindex")) {
                                arpTemp.setInterfaceName(tag.getName());

                                String mac = this.macUtil.supplement(value);
                                arpTemp.setMac(mac);
//                                    arpTemp.setMac(tag.getMac());

                                arpTemp.setIndex(value);
                            }
                        }
                        // 查询arp
                        if(arpTemp.getIp() != null && !arpTemp.getIp().equals("")){
                            params.clear();
                            params.put("ip", arpTemp.getIp());
                            params.put("deviceName", arpTemp.getDeviceName());
                            params.put("interfaceName", arpTemp.getInterfaceName());
                            List<ArpTemp> localArps = arpTempService.selectObjByMap(params);
                            if (localArps.size() == 0
                                    && arpTemp.getInterfaceName() != null
                                    && !arpTemp.getInterfaceName().equals("")) {
                                arpTemp.setTag("L");
                                arpTemp.setAddTime(time);
                                arpTempService.save(arpTemp);
                            } else if (localArps.size() > 0) {
                                ArpTemp local = localArps.get(0);
                                local.setTag("L");
                                local.setMask(arpTemp.getMask());
                                local.setType(arpTemp.getType());
                                arpTempService.update(local);
                            }
                        }

                    }
                }
            }
        }
        // 计算ip使用率
        params.clear();
        if (ips.size() > 0) {
            Set set = new HashSet();
            set.addAll(ips);
            ips.clear();
            ips.addAll(set);
            IpDetail init = ipDetailService.selectObjByIp("0.0.0.0");
            init.setTime(init.getTime() + 1);
            ipDetailService.update(init);
            // 总时长
            params.put("ipId", init.getId());
            params.put("arpIpList", ips);
            List<IpDetail> IpDetails = ipDetailService.selectObjByMap(params);
            IpDetails.forEach((item) -> {
                item.setOnline(true);
                item.setTime(item.getTime() + 1);
                int initTime = init.getTime() + 1;
                float num = (float) item.getTime() / initTime;
                int usage = Math.round(num * 100);
                item.setUsage(usage);
                ipDetailService.update(item);
            });
            params.clear();
            params.put("notIpList", ips);
            List<IpDetail> ipDetails = ipDetailService.selectObjByMap(params);
            ipDetails.forEach((item) -> {
                item.setOnline(false);
                ipDetailService.update(item);
            });
        }

    }


    public String mac16ConvertMac10(String param){
        String[] strs = param.split("\\.");
        StringBuffer mac = new StringBuffer();
        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            String hex = this.toHex(Integer.parseInt(str));
            if(i+1 <strs.length){
                mac.append(hex).append(":");
            }else if(i+1 == strs.length){
                mac.append(hex);
            }
        }
        return mac.toString();
    }

    public String toHex(Integer str){
        String hex = Integer.toHexString(str);
        return hex;
    }


    public Map<String, String> macVlan( List<Item> vlanMacList){
        Map map = new HashMap();
        if(vlanMacList.size() > 0) {
            for (Item item : vlanMacList) {
                List<ItemTag> tags = item.getItemTags();
                if (tags != null && tags.size() > 0) {
                    for (ItemTag tag : tags) {
                        String vlanMac = tag.getValue();
                        if (tag.getTag().equals("vlan")) {
//                            String vlanMac = "0.80.121.102.104.29";
                            int i = vlanMac.indexOf(".");
                            if(i != -1){
                                String vlan = vlanMac.substring(0, i);
                                String mac16 = vlanMac.substring(i + 1);
                                String mac = mac16ConvertMac10(mac16);
                                map.put(mac, vlan);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }


    @Override
    public void gatherMacItem(Date time){
        List<Map> ipList = this.topoNodeService.queryNetworkElement();
        if (ipList != null && ipList.size() > 0) {
            Map params = new HashMap();
            this.macTempService.truncateTable();
            for (Map map : ipList) {
                String deviceName = map.get("deviceName").toString();
                String deviceType = map.get("deviceType").toString();
                String ip = map.get("ip").toString();
                String uuid = map.get("uuid").toString();

                params.clear();
                params.put("ip", ip);
                params.put("tag", "macvlan");
                List<Item> vlanMacList = itemMapper.gatherItemByTag(params);
                Map<String, String> macMap = null;
                if(vlanMacList.size() > 0){
                    macMap = this.macVlan(vlanMacList);
                }


                // 采集 interface Mac
                params.clear();
                params.put("ip", ip);
                params.put("tag", "ifbasic");
                params.put("tag_relevance", "ifbasic");
                params.put("index", "ifindex");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> itemIfBasicTagList = itemMapper.gatherItemByTag(params);
                if (itemIfBasicTagList.size() > 0) {
                    for (Item item : itemIfBasicTagList) {
                        List<ItemTag> tags = item.getItemTags();
                        MacTemp macTemp = new MacTemp();
                        macTemp.setDeviceName(deviceName);
                        macTemp.setDeviceType(deviceType);
                        macTemp.setUuid(uuid);
                        macTemp.setDeviceIp(ip);
                        if (tags != null && tags.size() > 0) {
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if (tag.getTag().equals("ifmac")) {
                                    // 格式化mac
                                    if(!value.contains(":")){
                                        value = value.trim().replaceAll(" ", ":");
                                    }
                                    String mac = this.macUtil.supplement(value);
                                    macTemp.setMac(mac);
                                    if(macMap != null && !macMap.isEmpty()){
                                        String vlan = macMap.get(value);
                                        macTemp.setVlan(vlan);
                                    }
//                                        params.clear();
//                                        params.put("mac", value);
//                                        List<Arp> arps = arpService.selectObjByMap(params);
//                                        if (arps.size() > 0) {
//                                            Arp arp = arps.get(0);
//                                            macTemp.setIp(arp.getIp());
//                                            macTemp.setIpAddress(arp.getIpAddress());
//                                        }
                                    if(StringUtils.isNotEmpty(value)){
                                        macTemp.setType("local");
                                    }
                                }
                                if (tag.getTag().equals("ifname")) {
                                    macTemp.setInterfaceName(value);
                                }
                                if (tag.getTag().equals("ifindex")) {
                                    macTemp.setIndex(value);
                                }
                            }
                            // 保存Mac条目
                            if (macTemp.getInterfaceName() != null && !macTemp.getInterfaceName().equals("")
                                    && macTemp.getMac() != null && !macTemp.getMac().equals("{#MAC}")
                                    && !macTemp.getMac().equals("{#IFMAC}")) {
                                params.clear();
                                params.put("deviceName", macTemp.getDeviceName());
                                params.put("interfaceName", macTemp.getInterfaceName());
                                params.put("mac", macTemp.getMac());
                                List<MacTemp> macs = macTempService.selectByMap(params);
                                if (macs.size() == 0) {
                                    macTemp.setTag("L");
                                    macTemp.setAddTime(time);
                                    macTempService.save(macTemp);
                                }
                            }
                        }
                    }
                }



                // 采集Mac(Zabbix)(obj：mac)
                params.clear();
                params.put("ip", ip);
                params.put("tag", "mac");
                params.put("index", "portindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> itemTagList = itemMapper.gatherItemByTag(params);
                // Begin(item)
                if (itemTagList.size() > 0) {
                    for (Item item : itemTagList) {
                        List<ItemTag> tags = item.getItemTags();
                        MacTemp macTemp = new MacTemp();
                        macTemp.setDeviceName(deviceName);
                        macTemp.setDeviceType(deviceType);
                        macTemp.setUuid(uuid);
                        macTemp.setDeviceIp(ip);
                        if (tags != null && tags.size() > 0) {
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if (tag.getTag().equals("mac")) {
//                                    格式化mac
                                    if(!value.contains(":")){
                                        value = value.trim().replaceAll(" ", ":");
                                    }
                                    String mac = this.macUtil.supplement(value);
                                    macTemp.setMac(mac);
                                    if(macMap != null && !macMap.isEmpty()){
                                        String vlan = macMap.get(value);
                                        macTemp.setVlan(vlan);
                                    }
                                }
                                if (tag.getTag().equals("portindex")) {
                                    macTemp.setInterfaceName(tag.getName());
                                    macTemp.setIndex(value);
                                }
                                if (tag.getTag().equals("attr")) {
                                    switch (value){
                                        case "5":
                                            value = "static";
                                            break;
                                        case "4":
                                            value = "local";
                                            break;
                                        case "3":
                                            value = "dynamic";
                                            break;
                                        case "2":
                                            value = "invalid";
                                            break;
                                        case "1":
                                            value = "other";
                                            break;
                                        default:
                                            value = null;
                                            break;
                                    }
                                    macTemp.setType(value);
                                }
                            }
                            // 保存Mac条目
                            if(macTemp.getInterfaceName() != null && !macTemp.getInterfaceName().equals("")
                                    && macTemp.getMac() != null && !macTemp.getMac().equals("{#MAC}")
                                    && !macTemp.getMac().equals("{#IFMAC}")){
                                params.clear();
                                params.put("deviceName", macTemp.getDeviceName());
                                params.put("interfaceName", macTemp.getInterfaceName());
                                params.put("mac", macTemp.getMac());
                                List<MacTemp> macs = macTempService.selectByMap(params);
                                if(macs.size() == 0){
                                    if(macTemp.getTag() == null || "".equals(macTemp.getTag())){
                                        if(macTemp.getType() != null && "local".equals(macTemp.getType())
                                                && macTemp.getMac().contains("00:00:5e")){
                                            macTemp.setTag("LV");
                                        }else if(macTemp.getMac().contains("00:00:5e")){
                                            macTemp.setTag("V");
                                        }
                                    }
                                    macTemp.setAddTime(time);
                                    macTempService.save(macTemp);
                                }else{
                                    if(macs.size() > 0){
                                        MacTemp mac = macs.get(0);
                                        mac.setType(macTemp.getType());
                                        this.macTempService.update(mac);
                                    }
                                }
                            }
                        }
                    }
                }else{
                    params.put("ip", ip);
                    params.put("tag", "arp");
                    params.put("index", "ifindex");
                    params.put("tag_relevance", "ifbasic");
                    params.put("index_relevance", "ifindex");
                    params.put("name_relevance", "ifname");
                    List<Item> arpList = itemMapper.gatherItemByTag(params);
                    if (arpList.size() > 0) {
                        for (Item item : arpList) {
                            List<ItemTag> tags = item.getItemTags();
                            ArpTemp arpTemp = new ArpTemp();
                            arpTemp.setDeviceName(deviceName);
                            arpTemp.setDeviceType(deviceType);
                            arpTemp.setUuid(uuid);
                            arpTemp.setDeviceIp(ip);
                            arpTemp.setTag("S");
                            if (tags != null && tags.size() > 0) {
                                for (ItemTag tag : tags) {
                                    String value = tag.getValue();
                                    if (tag.getTag().equals("ip")) {
                                        arpTemp.setIp(IpUtil.ipConvertDec(value));
                                    }
                                    if (tag.getTag().equals("mac")) {
                                        String mac = this.macUtil.supplement(value);
                                        arpTemp.setMac(mac);
                                    }
                                    if (tag.getTag().equals("type")) {
                                        switch (value){
                                            case "4":
                                                value = "static";
                                                break;
                                            case "3":
                                                value = "dynamic";
                                                break;
                                            case "1":
                                                value = "other";
                                                break;
                                            default:
                                                value = null;
                                                break;
                                        }
                                        arpTemp.setType(value);
                                    }
                                    if (tag.getTag().equals("ifindex")) {
                                        if(value != null){
                                            arpTemp.setInterfaceName(tag.getName());
                                            arpTemp.setIndex(value);
                                        }else{
                                            //
                                            switch (value){
                                                case "1":
                                            }
                                        }
                                    }
                                }
                                // 获取arp，写入mac
                                if (arpTemp.getIp() != null
                                        && !arpTemp.getIp().equals("")
                                        && "dynamic".equals(arpTemp.getType())) {
                                    MacTemp macTemp = new MacTemp();
                                    macTemp.setMac(arpTemp.getMac());
                                    macTemp.setIndex(arpTemp.getIndex());
                                    macTemp.setType(arpTemp.getType());
                                    macTemp.setDeviceName(deviceName);
                                    macTemp.setDeviceType(deviceType);
                                    macTemp.setInterfaceName(arpTemp.getInterfaceName());
                                    macTemp.setUuid(uuid);
                                    macTemp.setDeviceIp(ip);
                                    params.clear();
                                    params.put("deviceName", macTemp.getDeviceName());
                                    params.put("interfaceName", macTemp.getInterfaceName());
                                    params.put("mac", macTemp.getMac());
                                    List<MacTemp> macs = macTempService.selectByMap(params);
                                    if (macs.size() == 0) {
                                        macTemp.setAddTime(time);
                                        macTempService.save(macTemp);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /***
     *  批量插入 and 批量更新
     * @param time
     */
    @Override
    public void gatherMacBatch(Date time){
        StopWatch watch = StopWatch.createStarted();
        List<MacTemp> batchInsert = new ArrayList<>();
//        List<MacTemp> batchInsert = Collections.synchronizedList(new ArrayList<>());
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {
            Map params = new HashMap();
            this.macTempService.truncateTable();
            devices.stream().forEach(map ->{
                System.out.println(Thread.currentThread().getName());
//            ipList.stream().forEach(map -> {
//            for (Map map : devices) {
//                devices.forEach(map -> {
                String deviceName = String.valueOf(map.get("deviceName"));
                String deviceType = String.valueOf(map.get("deviceType"));
                String ip = String.valueOf(map.get("ip"));
                String uuid = String.valueOf(map.get("uuid"));
                params.clear();
                params.put("ip", ip);
                params.put("tag", "macvlan");
                List<Item> vlanMacList = itemMapper.gatherItemByTagAndRtdata(params);
                Map<String, String> macMap = null;
                if(vlanMacList.size() > 0){
                    macMap = this.macVlan(vlanMacList);
                }
                // 采集：ifbasic
                params.clear();
                params.put("ip", ip);
                params.put("tag", "ifbasic");
                params.put("tag_relevance", "ifbasic");
                params.put("index", "ifindex");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                params.put("clock", DateTools.getMinTime(-1));
//                List<Item> items = itemMapper.gatherItemByTagAndRtdata(params);
                List<Item> items = itemMapper.gatherItemByTag(params);
                if (items.size() > 0) {
                    final  Map<String, String> macVlan = macMap;
                        items.stream().forEach(item -> {
                        List<ItemTag> tags = item.getItemTags();
                        MacTemp macTemp = new MacTemp();
                        macTemp.setAddTime(time);
                        macTemp.setDeviceName(deviceName);
                        macTemp.setDeviceType(deviceType);
                        macTemp.setUuid(uuid);
                        macTemp.setDeviceIp(ip);
                        macTemp.setTag("L");
                        if (tags != null && tags.size() > 0) {
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if (tag.getTag().equals("ifmac")) {
//                                    格式化mac
                                    if(!value.contains(":")){
                                        value = value.trim().replaceAll(" ", ":");
                                    }
                                    String mac = this.macUtil.supplement(value);
                                    macTemp.setMac(mac);
                                    if(macVlan != null && !macVlan.isEmpty()){
                                        String vlan = macVlan.get(value);
                                        macTemp.setVlan(vlan);
                                    }
                                    if(StringUtils.isNotEmpty(value)){
                                        macTemp.setType("local");
                                    }
                                }
                                if (tag.getTag().equals("ifname")) {
                                    macTemp.setInterfaceName(value);
                                }
                                if (tag.getTag().equals("ifindex")) {
                                    macTemp.setIndex(value);
                                }
                            }
                            // 保存Mac条目
                            if (StringUtils.isNotBlank(macTemp.getInterfaceName())
                                    && StringUtils.isNotBlank(macTemp.getMac())
                                    && !macTemp.getMac().equals("{#MAC}")
                                    && !macTemp.getMac().equals("{#IFMAC}")) {
                                macTemp.setTag("L");
                                macTemp.setAddTime(time);
                                batchInsert.add(macTemp);
                            }
                        }
                    });
                }

                // 采集Mac(Zabbix)(obj：mac)
                params.clear();
                params.put("ip", ip);
                params.put("tag", "mac");
                params.put("index", "portindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                params.put("clock", DateTools.getMinTime(-1));
//                List<Item> itemTagList = itemMapper.gatherItemByTagAndRtdata(params);
                List<Item> itemTagList = itemMapper.gatherItemByTag(params);
                // Begin(item)
                if (itemTagList.size() > 0) {
                    final  Map<String, String> macVlan = macMap;
                    itemTagList.stream().forEach(item -> {
                        List<ItemTag> tags = item.getItemTags();
                        MacTemp macTemp = new MacTemp();
                        macTemp.setAddTime(time);
                        macTemp.setDeviceName(deviceName);
                        macTemp.setDeviceType(deviceType);
                        macTemp.setUuid(uuid);
                        macTemp.setDeviceIp(ip);
                        if (tags != null && tags.size() > 0) {
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if (tag.getTag().equals("mac")) {
                                    // 格式化Mac
                                    if(!value.contains(":")){
                                        value = value.trim().replaceAll(" ", ":");
                                    }
                                    String mac = this.macUtil.supplement(value);
                                    macTemp.setMac(mac);
                                    if(macVlan != null && !macVlan.isEmpty()){
                                        String vlan = macVlan.get(value);
                                        macTemp.setVlan(vlan);
                                    }
                                }
                                if (tag.getTag().equals("portindex")) {
                                    macTemp.setInterfaceName(tag.getName());
                                    macTemp.setIndex(value);
                                }
                                if (tag.getTag().equals("attr")) {
                                    switch (value){
                                        case "5":
                                            value = "static";
                                            break;
                                        case "4":
                                            value = "local";
                                            break;
                                        case "3":
                                            value = "dynamic";
                                            break;
                                        case "2":
                                            value = "invalid";
                                            break;
                                        case "1":
                                            value = "other";
                                            break;
                                        default:
                                            value = null;
                                            break;
                                    }
                                    macTemp.setType(value);
                                }
                            }
                            // 保存Mac条目
                            if(StringUtils.isNotBlank(macTemp.getInterfaceName())
                                    && StringUtils.isNotBlank(macTemp.getMac())
                                    && !macTemp.getMac().equals("{#MAC}")
                                    && !macTemp.getMac().equals("{#IFMAC}")){
                                if(macTemp.getTag() == null || "".equals(macTemp.getTag())){
                                    if(macTemp.getType() != null && "local".equals(macTemp.getType())
                                            && macTemp.getMac().contains("00:00:5e")){
                                        macTemp.setTag("LV");
                                    }else if(macTemp.getMac().contains("00:00:5e")){
                                        macTemp.setTag("V");
                                    }
                                }
                                batchInsert.add(macTemp);
                            }
                        }
                    });
                }else{
                    params.put("ip", ip);
                    params.put("tag", "arp");
                    params.put("index", "ifindex");
                    params.put("tag_relevance", "ifbasic");
                    params.put("index_relevance", "ifindex");
                    params.put("name_relevance", "ifname");
                    params.put("clock", DateTools.getMinTime(-1));
//                    List<Item> arpList = itemMapper.gatherItemByTagAndRtdata(params);
                    List<Item> arpList = itemMapper.gatherItemByTag(params);
                    if (arpList.size() > 0) {
                        arpList.stream().forEach(item -> {
                            List<ItemTag> tags = item.getItemTags();
                            ArpTemp arpTemp = new ArpTemp();
                            arpTemp.setDeviceName(deviceName);
                            arpTemp.setDeviceType(deviceType);
                            arpTemp.setUuid(uuid);
                            arpTemp.setDeviceIp(ip);
                            arpTemp.setTag("S");
                            if (tags != null && tags.size() > 0) {
                                for (ItemTag tag : tags) {
                                    String value = tag.getValue();
                                    if (tag.getTag().equals("ip")) {
                                        arpTemp.setIp(IpUtil.ipConvertDec(value));
                                    }
                                    if (tag.getTag().equals("mac")) {
                                        String mac = this.macUtil.supplement(value);
                                        arpTemp.setMac(mac);
                                    }
                                    if (tag.getTag().equals("type")) {
                                        switch (value){
                                            case "4":
                                                value = "static";
                                                break;
                                            case "3":
                                                value = "dynamic";
                                                break;
                                            case "1":
                                                value = "other";
                                                break;
                                            default:
                                                value = null;
                                                break;
                                        }
                                        arpTemp.setType(value);
                                    }
                                    if (tag.getTag().equals("ifindex")) {
                                        if(value != null){
                                            arpTemp.setInterfaceName(tag.getName());
                                            arpTemp.setIndex(value);
                                        }else{
                                            //
                                            switch (value){
                                                case "1":
                                            }
                                        }
                                    }
                                }
                                // 获取arp，写入mac
                                if (StringUtils.isNotBlank(arpTemp.getIp())
                                        && StringUtils.isNotBlank(arpTemp.getInterfaceName())
                                        && StringUtils.isNotBlank(arpTemp.getMac())
                                        && "dynamic".equals(arpTemp.getType())) {
                                    MacTemp macTemp = new MacTemp();
                                    macTemp.setAddTime(time);
                                    macTemp.setMac(arpTemp.getMac());
                                    macTemp.setIndex(arpTemp.getIndex());
                                    macTemp.setType(arpTemp.getType());
                                    macTemp.setDeviceName(deviceName);
                                    macTemp.setDeviceType(deviceType);
                                    macTemp.setInterfaceName(arpTemp.getInterfaceName());
                                    macTemp.setUuid(uuid);
                                    macTemp.setDeviceIp(ip);
                                    batchInsert.add(macTemp);
                                }
                            }
                        });
                    }
                }
                System.out.println("采集中");
        });
            watch.stop();
            log.info("Mac-采集 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");
            if(batchInsert.size() > 0){
                watch.reset();
                watch.start();
                try {
                    int i = 0;
                    for (MacTemp macTemp : batchInsert) {
                        i++;
                        if(macTemp == null){
                            System.out.println(i);
                        }
                    }
                    List<MacTemp> list2 = batchInsert.stream().filter(e -> e != null).collect(
                            Collectors.collectingAndThen(
                                    Collectors.toCollection(() -> new TreeSet<>(Comparator
                                            .comparing(MacTemp::getDeviceName, Comparator.nullsLast(String::compareTo))
                                            .thenComparing(MacTemp::getInterfaceName, Comparator.nullsLast(String::compareTo))
                                            .thenComparing(MacTemp::getMac, Comparator.nullsLast(String::compareTo)))),
                                    ArrayList::new));
                    System.out.println("批量插入");
                    this.macTempService.batchInsert(list2);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("Mac采集异常：" + e.getMessage());
                }
                watch.stop();
                log.info("Mac-批量插入 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");
                // 等待并发Stream处理完成
            }
        }
    }

    @Override
    public void gatherMacBatchStream(Date time){
        List<MacTemp> batchInsert = new ArrayList();
        List<Map> ipList = this.topoNodeService.queryNetworkElement();
        if (ipList != null && ipList.size() > 0) {
            Map params = new HashMap();
            this.macTempService.truncateTable();
            for (Map map : ipList) {
                String deviveName = map.get("deviceName").toString();
                String deviceType = map.get("deviceType").toString();
                String ip = map.get("ip").toString();
                String uuid = map.get("uuid").toString();

                params.clear();
                params.put("ip", ip);
                params.put("tag", "macvlan");
                List<Item> vlanMacList = itemMapper.gatherItemByTag(params);
                Map<String, String> macMap = null;
                if(vlanMacList.size() > 0){
                    macMap = this.macVlan(vlanMacList);
                }

                // 采集：ifbasic
                params.clear();
                params.put("ip", ip);
                params.put("tag", "ifbasic");
                params.put("tag_relevance", "ifbasic");
                params.put("index", "ifindex");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> basicItems = itemMapper.gatherItemByTag(params);
                if (basicItems.size() > 0) {
                    final Map<String, String> macVlan = macMap;
                    basicItems.stream().forEach(item -> {
                        MacTemp macTemp = new MacTemp();
                        macTemp.setAddTime(time);
                        macTemp.setDeviceName(deviveName);
                        macTemp.setDeviceType(deviceType);
                        macTemp.setUuid(uuid);
                        macTemp.setDeviceIp(ip);
                        macTemp.setTag("L");
                        item.getItemTags().stream().forEach(
                                tag -> {
                                    String value = tag.getValue();
                                    if (tag.getTag().equals("ifmac")) {
//                                    格式化mac
                                        if(!value.contains(":")){
                                            value = value.trim().replaceAll(" ", ":");
                                        }
                                        String mac = this.macUtil.supplement(value);
                                        macTemp.setMac(mac);
                                        if(macVlan != null && !macVlan.isEmpty()){
                                            String vlan = macVlan.get(value);
                                            macTemp.setVlan(vlan);
                                        }
                                        if(StringUtils.isNotEmpty(value)){
                                            macTemp.setType("local");
                                        }
                                    }
                                    if (tag.getTag().equals("ifname")) {
                                        macTemp.setInterfaceName(value);
                                    }
                                    if (tag.getTag().equals("ifindex")) {
                                        macTemp.setIndex(value);
                                    }
                                });
                        // 保存Mac条目
                        if (macTemp.getInterfaceName() != null && !macTemp.getInterfaceName().equals("")
                                && macTemp.getMac() != null && !macTemp.getMac().equals("{#MAC}")
                                && !macTemp.getMac().equals("{#IFMAC}")) {
                            batchInsert.add(macTemp);
                        }
                    });
                }

                // 采集Mac(
                params.clear();
                params.put("ip", ip);
                params.put("tag", "mac");
                params.put("index", "portindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> macItems = itemMapper.gatherItemByTag(params);
                if (macItems.size() > 0) {
                    final Map<String, String> macVlan = macMap;
                    macItems.stream().forEach(item -> {
                        MacTemp macTemp = new MacTemp();
                        macTemp.setAddTime(time);
                        macTemp.setDeviceName(deviveName);
                        macTemp.setDeviceType(deviceType);
                        macTemp.setUuid(uuid);
                        macTemp.setDeviceIp(ip);
                        item.getItemTags().stream().forEach(tag -> {
                                    String value = tag.getValue();
                                    if (tag.getTag().equals("mac")) {
//                                    格式化mac
                                        if (!value.contains(":")) {
                                            value = value.trim().replaceAll(" ", ":");
                                        }
                                        String mac = this.macUtil.supplement(value);
                                        macTemp.setMac(mac);
                                        if (macVlan != null && !macVlan.isEmpty()) {
                                            String vlan = macVlan.get(value);
                                            macTemp.setVlan(vlan);
                                        }
                                    }
                                    if (tag.getTag().equals("portindex")) {
                                        macTemp.setInterfaceName(tag.getName());
                                        macTemp.setIndex(value);
                                    }
                                    if (tag.getTag().equals("attr")) {
                                        switch (value) {
                                            case "5":
                                                value = "static";
                                                break;
                                            case "4":
                                                value = "local";
                                                break;
                                            case "3":
                                                value = "dynamic";
                                                break;
                                            case "2":
                                                value = "invalid";
                                                break;
                                            case "1":
                                                value = "other";
                                                break;
                                            default:
                                                value = null;
                                                break;
                                        }
                                        macTemp.setType(value);
                                    }
                                }
                        );
                        // 保存Mac条目
                        if(macTemp.getInterfaceName() != null && !macTemp.getInterfaceName().equals("")
                                && macTemp.getMac() != null && !macTemp.getMac().equals("{#MAC}")
                                && !macTemp.getMac().equals("{#IFMAC}")) {
                            if (macTemp.getTag() == null || "".equals(macTemp.getTag())) {
                                if (macTemp.getType() != null && "local".equals(macTemp.getType())
                                        && macTemp.getMac().contains("00:00:5e")) {
                                    macTemp.setTag("LV");
                                } else if (macTemp.getMac().contains("00:00:5e")) {
                                    macTemp.setTag("V");
                                }
                            }
                            batchInsert.add(macTemp);
                        }
                    });
                }else{
                    params.put("ip", ip);
                    params.put("tag", "arp");
                    params.put("index", "ifindex");
                    params.put("tag_relevance", "ifbasic");
                    params.put("index_relevance", "ifindex");
                    params.put("name_relevance", "ifname");
                    List<Item> arpItems = itemMapper.gatherItemByTag(params);
                    if (arpItems.size() > 0) {
                        macItems.stream().forEach(item -> {
                            ArpTemp arpTemp = new ArpTemp();
                            arpTemp.setDeviceName(deviveName);
                            arpTemp.setDeviceType(deviceType);
                            arpTemp.setUuid(uuid);
                            arpTemp.setDeviceIp(ip);
                            arpTemp.setTag("S");
                            item.getItemTags().stream().forEach(tag -> {
                                String value = tag.getValue();
                                if (tag.getTag().equals("ip")) {
                                    arpTemp.setIp(IpUtil.ipConvertDec(value));
                                }
                                if (tag.getTag().equals("mac")) {
                                    String mac = this.macUtil.supplement(value);
                                    arpTemp.setMac(mac);
                                }
                                if (tag.getTag().equals("type")) {
                                    switch (value){
                                        case "4":
                                            value = "static";
                                            break;
                                        case "3":
                                            value = "dynamic";
                                            break;
                                        case "1":
                                            value = "other";
                                            break;
                                        default:
                                            value = null;
                                            break;
                                    }
                                    arpTemp.setType(value);
                                }
                                if (tag.getTag().equals("ifindex")) {
                                    if(value != null){
                                        arpTemp.setInterfaceName(tag.getName());
                                        arpTemp.setIndex(value);
                                    }else{
                                        //
                                        switch (value){
                                            case "1":
                                        }
                                    }
                                }
                            });
                            // 获取arp，写入mac
                            if (arpTemp.getIp() != null
                                    && !arpTemp.getIp().equals("")
                                    && "dynamic".equals(arpTemp.getType())) {
                                MacTemp macTemp = new MacTemp();
                                macTemp.setAddTime(time);
                                macTemp.setMac(arpTemp.getMac());
                                macTemp.setIndex(arpTemp.getIndex());
                                macTemp.setType(arpTemp.getType());
                                macTemp.setDeviceName(deviveName);
                                macTemp.setDeviceType(deviceType);
                                macTemp.setInterfaceName(arpTemp.getInterfaceName());
                                macTemp.setUuid(uuid);
                                macTemp.setDeviceIp(ip);
                                batchInsert.add(macTemp);
                            }
                        });
                    }
                }
            }
            if(batchInsert.size() > 0){
                // 执行去重
                List<MacTemp> list = batchInsert.stream().collect(
                        Collectors.collectingAndThen(
                                Collectors.toCollection(() -> new TreeSet<>(Comparator
                                        .comparing(MacTemp::getDeviceName, Comparator.nullsLast(String::compareTo))
                                        .thenComparing(MacTemp::getInterfaceName, Comparator.nullsLast(String::compareTo))
                                        .thenComparing(MacTemp::getMac, Comparator.nullsLast(String::compareTo)))),
                                ArrayList::new));

                this.macTempService.batchInsert(list);
            }
        }
    }

    @Override
    public void gatherMacThreadPool(Date time) {
        StopWatch watch = StopWatch.createStarted();
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {
            final CountDownLatch latch = new CountDownLatch(devices.size());
            int POOL_SIZE = Integer.max(Runtime.getRuntime().availableProcessors(), 0);
            ExecutorService exe = Executors.newFixedThreadPool(POOL_SIZE);
            List<MacTemp> batchInsert = new Vector();
            Map params = new HashMap();
            this.macTempService.truncateTable();
            devices.parallelStream().forEach(map ->{
                exe.execute(
                        new Thread(
                                () -> {
//                    synchronized (batchInsert){
                        System.out.println(Thread.currentThread().getName());
                        String deviceName = String.valueOf(map.get("deviceName"));
                        String deviceType = String.valueOf(map.get("deviceType"));
                        String ip = String.valueOf(map.get("ip"));
                        String uuid = String.valueOf(map.get("uuid"));
                        params.clear();
                        params.put("ip", ip);
                        params.put("tag", "macvlan");
                        List<Item> vlanMacList = itemMapper.gatherItemByTagAndRtdata(params);
                        Map<String, String> macMap = null;
                        if(vlanMacList.size() > 0){
                            macMap = macVlan(vlanMacList);
                        }
                        // 采集：ifbasic
                        params.clear();
                        params.put("ip", ip);
                        params.put("tag", "ifbasic");
                        params.put("tag_relevance", "ifbasic");
                        params.put("index", "ifindex");
                        params.put("index_relevance", "ifindex");
                        params.put("name_relevance", "ifname");
                        List<Item> items = itemMapper.gatherItemByTagAndRtdata(params);
                        if (items.size() > 0) {
                            final  Map<String, String> macVlan = macMap;
                            items.stream().forEach(item -> {
                                List<ItemTag> tags = item.getItemTags();
                                MacTemp macTemp = new MacTemp();
                                macTemp.setAddTime(time);
                                macTemp.setDeviceName(deviceName);
                                macTemp.setDeviceType(deviceType);
                                macTemp.setUuid(uuid);
                                macTemp.setDeviceIp(ip);
                                macTemp.setTag("L");
                                if (tags != null && tags.size() > 0) {
                                    for (ItemTag tag : tags) {
                                        String value = tag.getValue();
                                        if (tag.getTag().equals("ifmac")) {
//                                    格式化mac
                                            if(!value.contains(":")){
                                                value = value.trim().replaceAll(" ", ":");
                                            }
                                            String mac = macUtil.supplement(value);
                                            macTemp.setMac(mac);
                                            if(macVlan != null && !macVlan.isEmpty()){
                                                String vlan = macVlan.get(value);
                                                macTemp.setVlan(vlan);
                                            }
                                            if(StringUtils.isNotEmpty(value)){
                                                macTemp.setType("local");
                                            }
                                        }
                                        if (tag.getTag().equals("ifname")) {
                                            macTemp.setInterfaceName(value);
                                        }
                                        if (tag.getTag().equals("ifindex")) {
                                            macTemp.setIndex(value);
                                        }
                                    }
                                    // 保存Mac条目
                                    if (StringUtils.isNotEmpty(macTemp.getInterfaceName())
                                            && StringUtils.isNotEmpty(macTemp.getMac())
                                            && !macTemp.getMac().equals("{#MAC}")
                                            && !macTemp.getMac().equals("{#IFMAC}")) {
                                        macTemp.setTag("L");
                                        macTemp.setAddTime(time);
                                        batchInsert.add(macTemp);
                                    }
                                }
                            });
                        }

                        // 采集Mac(Zabbix)(obj：mac)
                        params.clear();
                        params.put("ip", ip);
                        params.put("tag", "mac");
                        params.put("index", "portindex");
                        params.put("tag_relevance", "ifbasic");
                        params.put("index_relevance", "ifindex");
                        params.put("name_relevance", "ifname");
                        List<Item> itemTagList = itemMapper.gatherItemByTagAndRtdata(params);
                        // Begin(item)
                        if (itemTagList.size() > 0) {
                            final  Map<String, String> macVlan = macMap;
                            itemTagList.stream().forEach(item -> {
                                List<ItemTag> tags = item.getItemTags();
                                MacTemp macTemp = new MacTemp();
                                macTemp.setAddTime(time);
                                macTemp.setDeviceName(deviceName);
                                macTemp.setDeviceType(deviceType);
                                macTemp.setUuid(uuid);
                                macTemp.setDeviceIp(ip);
                                if (tags != null && tags.size() > 0) {
                                    for (ItemTag tag : tags) {
                                        String value = tag.getValue();
                                        if (tag.getTag().equals("mac")) {
                                            // 格式化Mac
                                            if(!value.contains(":")){
                                                value = value.trim().replaceAll(" ", ":");
                                            }
                                            String mac = macUtil.supplement(value);
                                            macTemp.setMac(mac);
                                            if(macVlan != null && !macVlan.isEmpty()){
                                                String vlan = macVlan.get(value);
                                                macTemp.setVlan(vlan);
                                            }
                                        }
                                        if (tag.getTag().equals("portindex")) {
                                            macTemp.setInterfaceName(tag.getName());
                                            macTemp.setIndex(value);
                                        }
                                        if (tag.getTag().equals("attr")) {
                                            switch (value){
                                                case "5":
                                                    value = "static";
                                                    break;
                                                case "4":
                                                    value = "local";
                                                    break;
                                                case "3":
                                                    value = "dynamic";
                                                    break;
                                                case "2":
                                                    value = "invalid";
                                                    break;
                                                case "1":
                                                    value = "other";
                                                    break;
                                                default:
                                                    value = null;
                                                    break;
                                            }
                                            macTemp.setType(value);
                                        }
                                    }
                                    // 保存Mac条目
                                    if(StringUtils.isNotEmpty(macTemp.getInterfaceName())
                                            && StringUtils.isNotEmpty(macTemp.getMac())
                                            && !macTemp.getMac().equals("{#MAC}")
                                            && !macTemp.getMac().equals("{#IFMAC}")){
                                        if(macTemp.getTag() == null || "".equals(macTemp.getTag())){
                                            if(macTemp.getType() != null && "local".equals(macTemp.getType())
                                                    && macTemp.getMac().contains("00:00:5e")){
                                                macTemp.setTag("LV");
                                            }else if(macTemp.getMac().contains("00:00:5e")){
                                                macTemp.setTag("V");
                                            }
                                        }
                                        batchInsert.add(macTemp);
                                    }
                                }
                            });
                        }else{
                            params.put("ip", ip);
                            params.put("tag", "arp");
                            params.put("index", "ifindex");
                            params.put("tag_relevance", "ifbasic");
                            params.put("index_relevance", "ifindex");
                            params.put("name_relevance", "ifname");
                            List<Item> arpList = itemMapper.gatherItemByTagAndRtdata(params);
                            if (arpList.size() > 0) {
                                arpList.stream().forEach(item -> {
                                    List<ItemTag> tags = item.getItemTags();
                                    ArpTemp arpTemp = new ArpTemp();
                                    arpTemp.setDeviceName(deviceName);
                                    arpTemp.setDeviceType(deviceType);
                                    arpTemp.setUuid(uuid);
                                    arpTemp.setDeviceIp(ip);
                                    arpTemp.setTag("S");
                                    if (tags != null && tags.size() > 0) {
                                        for (ItemTag tag : tags) {
                                            String value = tag.getValue();
                                            if (tag.getTag().equals("ip")) {
                                                arpTemp.setIp(IpUtil.ipConvertDec(value));
                                            }
                                            if (tag.getTag().equals("mac")) {
                                                String mac = macUtil.supplement(value);
                                                arpTemp.setMac(mac);
                                            }
                                            if (tag.getTag().equals("type")) {
                                                switch (value){
                                                    case "4":
                                                        value = "static";
                                                        break;
                                                    case "3":
                                                        value = "dynamic";
                                                        break;
                                                    case "1":
                                                        value = "other";
                                                        break;
                                                    default:
                                                        value = null;
                                                        break;
                                                }
                                                arpTemp.setType(value);
                                            }
                                            if (tag.getTag().equals("ifindex")) {
                                                if(value != null){
                                                    arpTemp.setInterfaceName(tag.getName());
                                                    arpTemp.setIndex(value);
                                                }else{
                                                    //
                                                    switch (value){
                                                        case "1":
                                                    }
                                                }
                                            }
                                        }
                                        // 获取arp，写入mac
                                        if (StringUtils.isNotEmpty(arpTemp.getIp())
                                                && StringUtils.isNotEmpty(arpTemp.getInterfaceName())
                                                && StringUtils.isNotEmpty(arpTemp.getMac())
                                                && "dynamic".equals(arpTemp.getType())) {
                                            MacTemp macTemp = new MacTemp();
                                            macTemp.setAddTime(time);
                                            macTemp.setMac(arpTemp.getMac());
                                            macTemp.setIndex(arpTemp.getIndex());
                                            macTemp.setType(arpTemp.getType());
                                            macTemp.setDeviceName(deviceName);
                                            macTemp.setDeviceType(deviceType);
                                            macTemp.setInterfaceName(arpTemp.getInterfaceName());
                                            macTemp.setUuid(uuid);
                                            macTemp.setDeviceIp(ip);
                                            batchInsert.add(macTemp);
                                        }
                                    }
                                });
                            }
                        }
                        latch.countDown();
                        log.info("采集中");
//                    }
                }));
            });
//            if(exe != null){
//                exe.shutdown();
//            }
            try {
                latch.await();
                watch.stop();
                log.info("Mac-采集 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");

                watch.reset();
                watch.start();
                try {
                    List<MacTemp> list2 = batchInsert.parallelStream().collect(
                            Collectors.collectingAndThen(
                                    Collectors.toCollection(() -> new TreeSet<>(
                                            Comparator
                                                    .comparing(MacTemp::getDeviceName)
                                                    .thenComparing(MacTemp::getInterfaceName)
                                                    .thenComparing(MacTemp::getMac))),
                                    ArrayList::new));
                    System.out.println("批量插入");
                    this.macTempService.batchInsert(list2);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("Mac采集异常：" + e.getMessage());
                }
                watch.stop();
                log.info("Mac-批量插入 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

                // 等待并发Stream处理完成

//            while (true) {
//                if (exe.isTerminated()) {
//                    if(batchInsert.size() > 0){
//                        watch.reset();
//                        watch.start();
//                        try {
//                            List<MacTemp> list2 = batchInsert.parallelStream().collect(
//                                    Collectors.collectingAndThen(
//                                            Collectors.toCollection(() -> new TreeSet<>(
//                                                    Comparator
//                                                            .comparing(MacTemp::getDeviceName)
//                                                            .thenComparing(MacTemp::getInterfaceName)
//                                                            .thenComparing(MacTemp::getMac))),
//                                            ArrayList::new));
//                            System.out.println("批量插入");
//                            this.macTempService.batchInsert(list2);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            log.info("Mac采集异常：" + e.getMessage());
//                        }
//                        watch.stop();
//                        log.info("Mac-批量插入 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");
//                        // 等待并发Stream处理完成
//                    }
//                    break;
//                }
//            }
        }
    }


    // 暂时放外面
    private static int POOL_SIZE = Integer.max(Runtime.getRuntime().availableProcessors(), 0);
    private static ExecutorService staticExe = Executors.newFixedThreadPool(POOL_SIZE);

    @Override
    public void gatherMacCallable(Date time){
        StopWatch watch = StopWatch.createStarted();
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {
            final CountDownLatch latch = new CountDownLatch(devices.size());
//            int POOL_SIZE = Integer.max(Runtime.getRuntime().availableProcessors(), 0);
//            ExecutorService exe = Executors.newFixedThreadPool(POOL_SIZE);
            List<MacTemp> batchInsert = new Vector<>();
            this.macTempService.truncateTable();

            devices.parallelStream().forEach(map ->{
                Map tag = this.interfaceService.getHostTag(map.get("ip").toString());
                Future<List<MacTemp>> future = staticExe.submit(new MyCallable(time, map, tag, itemMapper));
                try {
                    List<MacTemp> macTemps = future.get();
                    batchInsert.addAll(macTemps);
                    latch.countDown();
//                    if(future.isDone()){
//                        List<MacTemp> macTemps = future.get();
//                        batchInsert.addAll(macTemps);
//                        latch.countDown();
//                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });
            try {
                latch.await();// 等待结果线程池线程执行结束

                watch.stop();
                log.info("Mac-采集耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");

                if(batchInsert.size() > 0){
                    watch.reset();
                    watch.start();
                    try {
                        Vector<MacTemp> list2 = batchInsert.parallelStream().collect(
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(() -> new TreeSet<>(
                                                Comparator
                                                        .comparing(MacTemp::getDeviceName)
                                                        .thenComparing(MacTemp::getInterfaceName)
                                                        .thenComparing(MacTemp::getMac))),
                                        Vector::new));
                        this.macTempService.batchInsert(list2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.info("Mac采集异常：" + e.getMessage());
                    }
                    watch.stop();
                    log.info("Mac-批量插入 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
//                exe.shutdown();
            }

        }
    }

    @Override
    public void testGatherMacThreadPool(Date time) {
        StopWatch watch = StopWatch.createStarted();
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {
//            final CountDownLatch latch = new CountDownLatch(devices.size());
//            IListUtils listUtils = new IListUtils();
            int POOL_SIZE = Integer.max(Runtime.getRuntime().availableProcessors(), 0);
            ExecutorService exe = Executors.newFixedThreadPool(POOL_SIZE);
//            ThreadPool threadPool = ThreadPool.getInstance();
            List<MacTemp> batchInsert = new ArrayList<>();
            Map params = new HashMap();
            this.macTempService.truncateTable();
            devices.parallelStream().forEach(map ->{
                exe.execute(new Thread(() -> {
                    synchronized (batchInsert){
                        System.out.println(Thread.currentThread().getName());
                        String deviceName = String.valueOf(map.get("deviceName"));
                        String deviceType = String.valueOf(map.get("deviceType"));
                        String ip = String.valueOf(map.get("ip"));
                        String uuid = String.valueOf(map.get("uuid"));
                        params.clear();
                        params.put("ip", ip);
                        params.put("tag", "macvlan");
                        List<Item> vlanMacList = itemMapper.gatherItemByTagAndRtdata(params);
                        Map<String, String> macMap = null;
                        if(vlanMacList.size() > 0){
                            macMap = macVlan(vlanMacList);
                        }

                        params.clear();
                        params.put("ip", ip);
                        params.put("tag", "mac");
                        params.put("index", "portindex");
                        params.put("tag_relevance", "ifbasic");
                        params.put("index_relevance", "ifindex");
                        params.put("name_relevance", "ifname");
                        List<Item> items = itemMapper.gatherItemByTagAndRtdata(params);
                        if (items.size() > 0) {
                            final  Map<String, String> macVlan = macMap;
                            items.stream().forEach(item -> {
                                List<ItemTag> tags = item.getItemTags();
                                MacTemp macTemp = new MacTemp();
                                macTemp.setAddTime(time);
                                macTemp.setDeviceName(deviceName);
                                macTemp.setDeviceType(deviceType);
                                macTemp.setUuid(uuid);
                                macTemp.setDeviceIp(ip);
                                if (tags != null && tags.size() > 0) {
                                    for (ItemTag tag : tags) {
                                        String value = tag.getValue();
                                        if (tag.getTag().equals("mac")) {
                                            // 格式化Mac
                                            if(!value.contains(":")){
                                                value = value.trim().replaceAll(" ", ":");
                                            }
                                            String mac = macUtil.supplement(value);
                                            macTemp.setMac(mac);
                                            if(macVlan != null && !macVlan.isEmpty()){
                                                String vlan = macVlan.get(value);
                                                macTemp.setVlan(vlan);
                                            }
                                        }
                                        if (tag.getTag().equals("portindex")) {
                                            macTemp.setInterfaceName(tag.getName());
                                            macTemp.setIndex(value);
                                        }
                                        if (tag.getTag().equals("attr")) {
                                            switch (value){
                                                case "5":
                                                    value = "static";
                                                    break;
                                                case "4":
                                                    value = "local";
                                                    break;
                                                case "3":
                                                    value = "dynamic";
                                                    break;
                                                case "2":
                                                    value = "invalid";
                                                    break;
                                                case "1":
                                                    value = "other";
                                                    break;
                                                default:
                                                    value = null;
                                                    break;
                                            }
                                            macTemp.setType(value);
                                        }
                                    }
                                    // 保存Mac条目
                                    if(StringUtils.isNotEmpty(macTemp.getInterfaceName())
                                            && StringUtils.isNotEmpty(macTemp.getMac())
                                            && !macTemp.getMac().equals("{#MAC}")
                                            && !macTemp.getMac().equals("{#IFMAC}")){
                                        if(macTemp.getTag() == null || "".equals(macTemp.getTag())){
                                            if(macTemp.getType() != null && "local".equals(macTemp.getType())
                                                    && macTemp.getMac().contains("00:00:5e")){
                                                macTemp.setTag("LV");
                                            }else if(macTemp.getMac().contains("00:00:5e")){
                                                macTemp.setTag("V");
                                            }
                                        }
                                        batchInsert.add(macTemp);
                                    }
                                }
                            });
                        }
                        System.out.println("采集中");
                    }
                }));
            });
            if(exe != null){
                exe.shutdown();
            }
            watch.stop();
            log.info("Mac-采集 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");
            while (true) {
                if (exe.isTerminated()) {
                    if(batchInsert.size() > 0){
                        watch.reset();
                        watch.start();
                        try {
                            List<MacTemp> list2 = batchInsert.parallelStream().collect(
                                    Collectors.collectingAndThen(
                                            Collectors.toCollection(() -> new TreeSet<>(
                                                    Comparator
                                                            .comparing(MacTemp::getDeviceName)
                                                            .thenComparing(MacTemp::getInterfaceName)
                                                            .thenComparing(MacTemp::getMac))),
                                            ArrayList::new));
                            System.out.println("批量插入");
                            this.macTempService.batchInsert(list2);
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.info("Mac采集异常：" + e.getMessage());
                        }
                        watch.stop();
                        log.info("Mac-批量插入 耗时：" + watch.getTime(TimeUnit.MILLISECONDS) + "毫秒.");
                    }
                    break;
                }
            }
        }
    }

    public boolean filterTag(List<ItemTag> tags, MacTemp macTemp, Map<String, String> macMap){
        macTemp.setTag("L");
        if (tags != null && tags.size() > 0) {
            for (ItemTag tag : tags) {
                String value = tag.getValue();
                if (tag.getTag().equals("ifmac")) {
//                                    格式化mac
                    if(!value.contains(":")){
                        value = value.trim().replaceAll(" ", ":");
                    }
                    String mac = this.macUtil.supplement(value);
                    macTemp.setMac(mac);
                    if(macMap != null && !macMap.isEmpty()){
                        String vlan = macMap.get(value);
                        macTemp.setVlan(vlan);
                    }
                    if(StringUtils.isNotEmpty(value)){
                        macTemp.setType("local");
                    }
                }
                if (tag.getTag().equals("ifname")) {
                    macTemp.setInterfaceName(value);
                }
                if (tag.getTag().equals("ifindex")) {
                    macTemp.setIndex(value);
                }
            }
            // 保存Mac条目
            if (macTemp.getInterfaceName() != null && !macTemp.getInterfaceName().equals("")
                    && macTemp.getMac() != null && !macTemp.getMac().equals("{#MAC}")
                    && !macTemp.getMac().equals("{#IFMAC}")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void gatherRouteItem(Date time) {
        List<Map> ipList = this.topoNodeService.queryNetworkElement();
        if(ipList != null && ipList.size() > 0) {
            this.routTempService.truncateTable();
            for (Map map : ipList) {
                String deviceName = map.get("deviceName").toString();
                String ip = map.get("ip").toString();
                String uuid = map.get("uuid").toString();
                Map params = new HashMap();
                params.clear();
                params.put("ip", ip);
                params.put("tag", "routecisco");
                params.put("index", "routeifindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> itemRouteciscoTagList = this.itemMapper.gatherItemByTag(params);
                if(itemRouteciscoTagList.size() > 0){
                    for (Item item : itemRouteciscoTagList) {
                        List<ItemTag> tags = item.getItemTags();
                        RouteTemp routTemp = new RouteTemp();
                        routTemp.setDeviceName(deviceName);
                        routTemp.setDeviceUuid(uuid);
                        routTemp.setAddTime(time);
                        if (tags != null && tags.size() > 0) {
                            String routedest = "";
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if(tag.getTag().equals("route")){
                                    String[] str = value.split("\\.");
                                    StringBuffer dest = new StringBuffer();
                                    String mask = "";
                                    StringBuffer nexthop = new StringBuffer();
                                    Scanner sc = new Scanner(value).useDelimiter("\\.");
                                    for(int i = 1; i<= str.length; i ++){
                                        if(2 <= i && i <= 4){
                                            dest.append(sc.next()).append(".");
                                        }else if(i == 5){
                                            dest.append(sc.next());
                                        }else if(i == 6){
                                            mask  = sc.next();
                                        }else if(12 <= i && i <= 14){
                                            nexthop.append(sc.next()).append(".");
                                        }else if(i == 15){
                                            nexthop.append(sc.next());
                                        }else{
                                            sc.next();
                                        }
                                    }
                                    routedest = dest.toString();
                                    routTemp.setDestination(IpUtil.ipConvertDec(dest.toString()));
                                    if(com.metoo.nspm.core.utils.MyStringUtils.isInteger(mask)){
                                        routTemp.setMaskBit(Integer.parseInt(mask));
                                        String mk = IpUtil.bitMaskConvertMask(Integer.parseInt(mask));
                                        routTemp.setMask(mk);
                                    }
                                    routTemp.setNextHop(IpUtil.ipConvertDec(nexthop.toString()));
                                }
                                if(tag.getTag().equals("routemetric")){
                                    routTemp.setCost(value);
                                }
                                if(tag.getTag().equals("routeproto")){
                                    String v = "";
                                    switch (value){
                                        case "2":
                                            v = "direct";
                                            break;
                                        case "3":
                                            v = "static";
                                            break;
                                        case "8":
                                            v = "rip";
                                            break;
                                        case "9":
                                            v = "isis";
                                            break;
                                        case "13":
                                            v = "ospf";
                                            break;
                                        case "14":
                                            v = "bgp";
                                            break;
                                    }
                                    routTemp.setProto(v);
                                }
                                if(tag.getTag().equals("routeifindex")){
                                    routTemp.setInterfaceName(tag.getName());
                                }
                            }
                            if(routTemp.getProto().equals("2")){
                                map.put("nextHop", "");
                                map.put("flags", "D");
                            }
                            if(!routTemp.getCost().equals("{#RTMETRIC}")
                                    && !routedest.equals("127.0.0.1")){
                                this.routTempService.save(routTemp);
                            }
                        }
                    }
                }
                if(itemRouteciscoTagList.size() <= 0){
                    params.clear();
                    params.put("ip", ip);
                    params.put("tag", "route");
                    params.put("index", "routeifindex");
                    params.put("tag_relevance", "ifbasic");
                    params.put("index_relevance", "ifindex");
                    params.put("name_relevance", "ifname");
                    List<Item> itemRouteTagList = this.itemMapper.gatherItemByTag(params);
                    if (itemRouteTagList.size() > 0) {
                        for (Item item : itemRouteTagList) {
                            List<ItemTag> tags = item.getItemTags();
                            RouteTemp routTemp = new RouteTemp();
                            routTemp.setDeviceName(deviceName);
                            routTemp.setDeviceUuid(uuid);
                            routTemp.setAddTime(time);
                            String routedest = "";
                            if (tags != null && tags.size() > 0) {
                                for (ItemTag tag : tags) {
                                    String value = tag.getValue();
                                    if(tag.getTag().equals("routedest")){
                                        routedest = value;
                                        routTemp.setDestination(IpUtil.ipConvertDec(value));
                                    }
                                    if(tag.getTag().equals("routemask")){
                                        routTemp.setMask(value);
                                        routTemp.setMaskBit(IpV4Util.getMaskBitByMask(value));
                                    }
                                    if(tag.getTag().equals("routemetric")){
                                        routTemp.setCost(value);
                                    }
                                    if(tag.getTag().equals("routenexthop")){
                                        routTemp.setNextHop(IpUtil.ipConvertDec(value));
                                    }
                                    if(tag.getTag().equals("routeproto")){
                                        String v = "";
                                        switch (value){
                                            case "2":
                                                v = "direct";
                                                break;
                                            case "3":
                                                v = "static";
                                                break;
                                            case "8":
                                                v = "rip";
                                                break;
                                            case "9":
                                                v = "isis";
                                                break;
                                            case "13":
                                                v = "ospf";
                                                break;
                                            case "14":
                                                v = "bgp";
                                                break;
                                        }
                                        routTemp.setProto(v);
                                    }
                                    if(tag.getTag().equals("routeifindex")){
                                        routTemp.setInterfaceName(tag.getName());
                                    }
                                }
                                if(routTemp.getProto().equals("2")){
                                    map.put("nextHop", "");
                                    map.put("flags", "D");
                                }
                                if(!routTemp.getCost().equals("{#RTMETRIC}")
                                        && !routedest.equals("127.0.0.1")){
                                    this.routTempService.save(routTemp);
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    @Override
    public void gatherIpaddressItem(Date time) {
        List<Map> ipList = this.topoNodeService.queryNetworkElement();
        if(ipList != null && ipList.size() > 0) {
            this.ipAddressTempService.truncateTable();
            for (Map map : ipList) {
                String deviceName = map.get("deviceName").toString();
                String ip = map.get("ip").toString();
                String uuid = map.get("uuid").toString();
                Map params = new HashMap();
                params.clear();
                params.put("ip", ip);
                params.put("tag", "ifipaddr");
                params.put("index", "ifindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> itemRouteTagList = this.itemMapper.gatherItemByTag(params);
                if (itemRouteTagList.size() > 0) {
                    for (Item item : itemRouteTagList) {
                        List<ItemTag> tags = item.getItemTags();
                        if (tags != null && tags.size() > 0) {
                            IpAddressTemp ipAddressTemp = new IpAddressTemp();
                            ipAddressTemp.setDeviceName(deviceName);
                            ipAddressTemp.setDeviceUuid(uuid);
                            ipAddressTemp.setAddTime(time);
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if (tag.getTag().equals("ipaddr")) {
                                    ipAddressTemp.setIp(IpUtil.ipConvertDec(value));
                                }
                                if (tag.getTag().equals("mask")) {
                                    ipAddressTemp.setMask(IpUtil.getBitMask(value));
                                }
                                if (tag.getTag().equals("ifindex")) {
                                    ipAddressTemp.setInterfaceName(tag.getName());
                                }
                            }
                            if(ipAddressTemp.getIp() != null && !ipAddressTemp.getIp().equals("127.0.0.1")){
                                params.clear();
//                                params.put("deviceName", ipAddressTemp.getDeviceName());
//                                params.put("interfaceName", ipAddressTemp.getInterfaceName());
                                params.put("ip", ipAddressTemp.getIp());
                                List<IpAddressTemp> ips = this.ipAddressTempService.selectObjByMap(params);
                                if(ips.size() == 0){
                                    ipAddressTemp.setAddTime(time);
                                    params.clear();
                                    params.put("ip", ipAddressTemp.getIp());
                                    List<Arp> arps = this.arpService.selectObjByMap(params);
                                    if(arps.size() > 0){
                                        ipAddressTemp.setMac(arps.get(0).getMac());
                                    }
                                    this.ipAddressTempService.save(ipAddressTemp);
                                }else{
                                    // 比较uuid是否相同，更新uuid
                                    IpAddressTemp ipAddress1 = ips.get(0);
                                    if(!ipAddress1.getDeviceUuid().equals(uuid)){
                                        this.ipAddressTempService.update(ipAddressTemp);
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    private int getVlan(String str) {
        int i = -1;
        Matcher matcher = Pattern.compile("[0-9]").matcher(str);
        if(matcher.find()) {
            i = matcher.start();
        }
        return i;
    }

    @Override
    public Map<Integer, String> gatherIpaddressItem() {
        Map<Integer, String> vlanMap = new HashMap();
        List<Map> ipList = this.topoNodeService.queryNetworkElement();
        if(ipList != null && ipList.size() > 0) {
            this.ipAddressTempService.truncateTable();
            for (Map map : ipList) {
                String deviceName = map.get("deviceName").toString();
                String ip = map.get("ip").toString();
                String uuid = map.get("uuid").toString();
                Map params = new HashMap();
                params.clear();
                params.put("ip", ip);
                params.put("tag", "ifipaddr");
                params.put("index", "ifindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> itemRouteTagList = this.itemMapper.gatherItemByTag(params);
                if (itemRouteTagList.size() > 0) {
                    for (Item item : itemRouteTagList) {
                        List<ItemTag> tags = item.getItemTags();
                        if (tags != null && tags.size() > 0) {
                            String mask = "";
                            String ipaddr = "";
                            String vlan = "";
                            for (ItemTag tag : tags) {
                                String value = tag.getValue();
                                if (tag.getTag().equals("ipaddr")) {
                                    ipaddr = value;
                                }
                                if (tag.getTag().equals("mask")) {
                                    mask = value;
                                }
                                if (tag.getTag().equals("ifindex")) {
                                    String name = tag.getName();
                                    if(name.toLowerCase().contains("vlan")){
                                        int i = this.getVlan(name);
                                        if(i >= 0){
                                            vlan = name.substring(i);
                                        }
                                    }

                                }

                            }
                            if(Strings.isNotBlank(mask) && Strings.isNotBlank(ipaddr) && Strings.isNotBlank(vlan)){
                                Map<String, String> network = IpUtil.getNetworkIp(ipaddr, mask);
                                String networkIp = "";
                                if(!network.isEmpty()){
                                    networkIp = network.get("network");
                                    if(Strings.isNotBlank(networkIp)){
                                        try {
                                            vlanMap.put(Integer.parseInt(vlan), networkIp + "&" + IpUtil.getMaskBitMask(mask));
                                        } catch (NumberFormatException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
        return vlanMap;
    }

    @Override
    public void gatherProblemItem(Date time) {
        List<Map> ipList = this.topoNodeService.queryNetworkElement();
        if(ipList != null && ipList.size() > 0) {
            this.ipAddressTempService.truncateTable();
            for (Map map : ipList) {
                String deviceName = map.get("deviceName").toString();
                String ip = map.get("ip").toString();
                String uuid = map.get("uuid").toString();
                Map params = new HashMap();
                params.clear();
                params.put("ip", ip);
                params.put("tag", "");
                params.put("index", "ifindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> itemRouteTagList = this.itemMapper.gatherItemByTag(params);
                if (itemRouteTagList.size() > 0) {
                    for (Item item : itemRouteTagList) {
                        List<ItemTag> tags = item.getItemTags();
                        if (tags != null && tags.size() > 0) {
                            ProblemTemp problemTemp = new ProblemTemp();
                            problemTemp.setDeviceName(deviceName);
                            problemTemp.setUuid(uuid);
                            problemTemp.setAddTime(time);
                            for (ItemTag tag : tags) {

                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void gatherStp(Date time) {
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {
            this.spanningTreeProtocolTempService.truncateTable();
            ExecutorService exe = Executors.newFixedThreadPool(devices.size());
            List<SpanningTreeProtocol> batchInsert = new Vector<>();
            Map params = new HashMap();
            devices.stream().forEach(item -> {
                String deviceName = String.valueOf(item.get("deviceName"));
                String deviceIp = String.valueOf(item.get("ip"));
                String deviceUuid = String.valueOf(item.get("uuid"));
                params.clear();
                params.put("ip", deviceIp);
                params.put("tag", "mstpport");
                params.put("index", "portindex");
                params.put("tag_relevance", "ifbasic");
                params.put("index_relevance", "ifindex");
                params.put("name_relevance", "ifname");
                List<Item> items = this.itemMapper.gatherItemBySTP(params);
                if(items.size() > 0){
                   exe.execute(new Thread(() -> {
                       synchronized (this){
                           items.stream().forEach(mstpport ->{
                               SpanningTreeProtocol stp = new SpanningTreeProtocol();
                               stp.setAddTime(time);
                               stp.setDeviceName(deviceName);
                               stp.setDeviceUuid(deviceUuid);
                               List<ItemTag> tags = mstpport.getItemTags();
                               if(tags != null && tags.size() > 0){
                                   tags.stream().forEach(tag -> {
                                       String value = tag.getValue();
                                       if(tag.getTag().equals("portindex")){
                                           int i = MyStringUtils.acquireCharacterPositions(value, ".", 1);
                                           if(i != -1){
                                               String instance = value.substring(0, i);
                                               String portindex = value.substring(i + 1);
                                               stp.setInstance(instance);
                                               stp.setPortIndex(portindex);
                                               stp.setPortName(tag.getName());
                                           }
                                       }
                                       if(tag.getTag().equals("portrole")){
                                           stp.setPortRole(value);
                                       }
                                       if(tag.getTag().equals("portstatus")){
                                           stp.setPortStatus(value);
                                       }
                                   });
                               }
                               batchInsert.add(stp);
                           });
                       }
                   }));
                }
            });
            if(exe != null){
                exe.shutdown();
            }
            while (true) {
                if (exe.isTerminated()) {
                    // 写入
                    if(batchInsert.size() > 0){
                        // 批量
                        this.spanningTreeProtocolTempService.batchInsert(batchInsert);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void gathermstpinstance(Date time) {
//        StopWatch watch = StopWatch.createStarted();
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {

            ExecutorService exe = Executors.newFixedThreadPool(devices.size());
//            List<SpanningTreeProtocol> list = new Vector<>();
            Map params = new HashMap();
            devices.stream().forEach(item -> {
                String deviceIp = String.valueOf(item.get("ip"));
                String deviceUuid = String.valueOf(item.get("uuid"));
                params.clear();
                params.put("ip", deviceIp);
                params.put("tag", "mstpinstance");
                List<Item> items = this.itemMapper.selectItemTagByMap(params);
                if (items.size() > 0) {
//                    exe.execute(new Thread(() -> {
//                        synchronized (this) {
                            items.stream().forEach(mstpport -> {
                                List<SpanningTreeProtocol> list = new Vector<>();
                                List<ItemTag> tags = mstpport.getItemTags();
                                if (tags != null && tags.size() > 0) {
                                    SpanningTreeProtocol stp = new SpanningTreeProtocol();
                                    tags.stream().forEach(tag -> {
                                        String value = tag.getValue();
                                        if (tag.getTag().equals("instance")) {
                                            stp.setInstance(value);
                                        }
                                        if (tag.getTag().equals("vlan")) {
                                            stp.setVlan(value);
                                        }
                                    });
//                                    List<SpanningTreeProtocol> instances = this.spanningTreeProtocolTempService.selectObjByInstance(stp.getInstance());
                                    params.clear();
                                    params.put("deviceUuid", deviceUuid);
                                    params.put("instance", stp.getInstance());
                                    List<SpanningTreeProtocol> instances = this.spanningTreeProtocolTempService.selectObjByMap(params);
                                    if(instances.size() > 0){
                                        instances.stream().forEach(e ->{
                                            if(StringUtils.isNotEmpty(e.getVlan())){
                                                e.setVlan(e.getVlan() + "," +stp.getVlan());
                                            }else{
                                                e.setVlan(stp.getVlan());
                                            }
                                        });
                                        list.addAll(instances);
                                        this.spanningTreeProtocolTempService.batchUpdate(list);
                                    }
                                }
                            });
                        }
                    }
                  );
// );
//                }
//            });
//            if(exe != null){
//                exe.shutdown();
//            }
//            while (true) {
//                if (exe.isTerminated()) {
//                    // 写入
//                    if(list.size() > 0){
//                        // 批量
//                        this.spanningTreeProtocolTempService.batchUpdate(list);
//                    }
//                    break;
//                }
//            }
        }
    }

    @Override
    public void gathermstpDR(Date time) {
//        StopWatch watch = StopWatch.createStarted();
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {

            ExecutorService exe = Executors.newFixedThreadPool(devices.size());
            List<SpanningTreeProtocol> list = new Vector<>();
            Map params = new HashMap();
            devices.stream().forEach(item -> {
                String deviceIp = String.valueOf(item.get("ip"));
                String deviceUuid = String.valueOf(item.get("uuid"));
                params.clear();
                params.put("ip", deviceIp);
                params.put("tag", "mstpDR");
                List<Item> items = this.itemMapper.selectItemTagByMap(params);
                if (items.size() > 0) {
                    exe.execute(new Thread(() -> {
                        synchronized (this) {
                            items.stream().forEach(mstpport -> {
                                List<ItemTag> tags = mstpport.getItemTags();
                                if (tags != null && tags.size() > 0) {
                                    SpanningTreeProtocol stp = new SpanningTreeProtocol();
                                    tags.stream().forEach(tag -> {
                                        String value = tag.getValue();
                                        if (tag.getTag().equals("instance")) {
                                            stp.setInstance(value);
                                        }
                                        if (tag.getTag().equals("rootid")) {
                                            if(StringUtils.isNotEmpty(value)){
                                                int n = MyStringUtils.appearNumber(value, " ");
                                                if(n >= 7){
                                                    int i = MyStringUtils.acquireCharacterPositions(value, " ", 2);
                                                    String mac = value.trim().substring(i +1);
                                                    mac = mac.replaceAll(" ", ":");
                                                    stp.setRoot(mac);
                                                }
                                            }
                                        }
                                    });
                                    params.clear();
                                    params.put("deviceUuid", deviceUuid);
                                    params.put("instance", stp.getInstance());
                                    List<SpanningTreeProtocol> instances = this.spanningTreeProtocolTempService.selectObjByMap(params);
                                    if(instances.size() > 0){
                                        instances.stream().forEach(e ->{
                                            String mac = stp.getRoot();
                                            params.clear();
                                            params.put("mac", mac);
                                            params.put("tag", "L");
                                            params.put("uuid", deviceUuid);
                                            List<Mac> macs = this.macMapper.selectByMap(params);
                                            if(macs.size() > 0){
                                                e.setIfRoot("1");
                                            }else{
                                                e.setIfRoot("0");
                                            }
                                            e.setRoot(mac);
                                        });
                                        list.addAll(instances);
                                    }
                                }
                            });
                        }
                    }));
                }
            });
            if(exe != null){
                exe.shutdown();
            }
            while (true) {
                if (exe.isTerminated()) {
                    // 写入
                    if(list.size() > 0){
                        // 批量
                        this.spanningTreeProtocolTempService.batchUpdate(list);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void writeStpRemote() {
        // remote
        Map params = new HashMap();
        List<SpanningTreeProtocol> stps = this.spanningTreeProtocolTempService.selectObjByMap(params);
        List list = new Vector();
        stps.stream().forEach(e -> {
            params.clear();
            params.put("uuid", e.getDeviceUuid());
            params.put("interfaceName", e.getPortName());
            params.put("tag", "DE");
            List<Mac> macs = this.macService.selectByMap(params);
            if(macs.size() > 0){
                Mac mac = macs.get(0);
                e.setRemoteDevice(mac.getRemoteDevice());
                e.setRemoteUuid(mac.getRemoteUuid());
                e.setRemotePort(mac.getRemoteInterface());
                list.add(e);
            }
        });
        if(list.size() > 0){
            this.spanningTreeProtocolTempService.batchUpdate(list);
        }
    }


    @Override
    public List<Item> selectTagByMap(Map params) {
        List<Item> items = this.itemMapper.selectTagByMap(params);
        // 解析items
        return items;
    }

    @Override
    public List<Item> selectItemTagByMap(Map params) {
        return this.itemMapper.selectItemTagByMap(params);
    }

    @Override
    public void testTransactional() throws ArithmeticException{
//        ArpTemp arpTemp = new ArpTemp();
//        arpTemp.setDeviceName("a");
//        this.arpTempService.save(arpTemp);

        try {
            int i = 1 / 0;// 捕获异常后，此处不在回滚
        } catch (Exception e) {
            e.printStackTrace();
        }

        int i = 1 / 0;// 捕获异常后，此处不在回滚

        Arp arp = new Arp();
        arp.setDeviceName("a");
        this.arpService.save(arp);
    }

    @Override
    public List<Item> selectNameObjByIndex(String index) {
        return this.itemMapper.selectNameObjByIndex(index);
    }

    @Override
    public void topologySyncToMac() {
        // 获取拓扑列表
        List<Topology> topologies = this.topologyService.selectObjByMap(null);
        if(topologies.size() > 0){
            for (Topology topology : topologies) {
                if(topology.getContent() != null){
                    Map content = JSONObject.parseObject(topology.getContent().toString(), Map.class);
                    if(content.get("links") != null){
                        JSONArray links = JSONArray.parseArray(content.get("links").toString());
                        if(links.size() > 0) {
                            for (Object object : links) {
                                Map link = JSONObject.parseObject(object.toString(), Map.class);
                                if(link.get("category") == null || !"deviceTeamLink".equals(link.get("category"))){
                                    String fromPort = String.valueOf(link.get("fromPort"));
                                    String from = String.valueOf(link.get("from"));
                                    Map fromNode = JSONObject.parseObject(String.valueOf(link.get("fromNode")), Map.class);
                                    String toPort = String.valueOf(link.get("toPort"));
                                    String to = String.valueOf(link.get("to"));
                                    Map toNode = JSONObject.parseObject(String.valueOf(link.get("toNode")), Map.class);
                                    MacTemp fromMac = new MacTemp();
                                    fromMac.setTag("DE");
                                    fromMac.setMac("00:00:00:00:00:00");
                                    fromMac.setUuid(from);
                                    fromMac.setInterfaceName(fromPort);
                                    if(fromNode != null){
                                        String name = String.valueOf(fromNode.get("name"));
                                        fromMac.setDeviceName(name);
                                    }
                                    fromMac.setRemoteUuid(to);
                                    fromMac.setRemoteInterface(toPort);
                                    if(toNode != null){
                                        String name = String.valueOf(toNode.get("name"));
                                        fromMac.setRemoteDevice(name);
                                    }
                                    MacTemp toMac = new MacTemp();
                                    toMac.setTag("DE");
                                    toMac.setMac("00:00:00:00:00:00");
                                    toMac.setUuid(to);
                                    toMac.setInterfaceName(toPort);
                                    if(toNode != null){
                                        String name = String.valueOf(toNode.get("name"));
                                        toMac.setDeviceName(name);
                                    }
                                    toMac.setRemoteUuid(from);
                                    toMac.setRemoteInterface(fromPort);
                                    if(fromNode != null){
                                        String name = String.valueOf(fromNode.get("name"));
                                        toMac.setRemoteDevice(name);
                                    }
                                    this.macTempService.save(fromMac);
                                    this.macTempService.save(toMac);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void topologySyncToMacBatch(Date time) {
        // 获取拓扑列表
        List<Topology> topologies = this.topologyService.selectObjByMap(null);
        if(topologies.size() > 0){
            List<MacTemp> list = new ArrayList();
            for (Topology topology : topologies) {
                if(topology.getContent() != null){
                    Map content = JSONObject.parseObject(topology.getContent().toString(), Map.class);
                    if(content.get("links") != null){
                        JSONArray links = JSONArray.parseArray(content.get("links").toString());
                        if(links.size() > 0) {
                            for (Object object : links) {
                                Map link = JSONObject.parseObject(object.toString(), Map.class);
                                if(link.get("category") == null || !"deviceTeamLink".equals(link.get("category"))){
                                    String fromPort = String.valueOf(link.get("fromPort"));
                                    String from = String.valueOf(link.get("from"));
                                    Map fromNode = JSONObject.parseObject(String.valueOf(link.get("fromNode")), Map.class);
                                    String toPort = String.valueOf(link.get("toPort"));
                                    String to = String.valueOf(link.get("to"));
                                    Map toNode = JSONObject.parseObject(String.valueOf(link.get("toNode")), Map.class);
                                    MacTemp fromMac = new MacTemp();
                                    fromMac.setAddTime(time);
                                    fromMac.setTag("DE");
                                    fromMac.setMac("00:00:00:00:00:00");
                                    fromMac.setUuid(from);
                                    fromMac.setInterfaceName(fromPort);
                                    if(fromNode != null){
                                        String name = String.valueOf(fromNode.get("name"));
                                        fromMac.setDeviceName(name);
                                    }
                                    fromMac.setRemoteUuid(to);
                                    fromMac.setRemoteInterface(toPort);
                                    if(toNode != null){
                                        String name = String.valueOf(toNode.get("name"));
                                        fromMac.setRemoteDevice(name);
                                    }
                                    NetworkElement toDevice = this.networkElementService.selectObjByUuid(to);
                                    if(toDevice != null){
                                        DeviceType deviceType = this.deviceTypeService.selectObjById(toDevice.getDeviceTypeId());
                                        if(deviceType != null){
                                            fromMac.setRemoteDeviceType(deviceType.getName());
                                        }
                                    }
                                    MacTemp toMac = new MacTemp();
                                    toMac.setAddTime(time);
                                    toMac.setTag("DE");
                                    toMac.setMac("00:00:00:00:00:00");
                                    toMac.setUuid(to);
                                    toMac.setInterfaceName(toPort);
                                    if(toNode != null){
                                        String name = String.valueOf(toNode.get("name"));
                                        toMac.setDeviceName(name);
                                    }
                                    toMac.setRemoteUuid(from);
                                    toMac.setRemoteInterface(fromPort);
                                    if(fromNode != null){
                                        String name = String.valueOf(fromNode.get("name"));
                                        toMac.setRemoteDevice(name);
                                    }

                                    NetworkElement fromDevice = this.networkElementService.selectObjByUuid(from);
                                    if(fromDevice != null){
                                        DeviceType deviceType = this.deviceTypeService.selectObjById(fromDevice.getDeviceTypeId());
                                        if(deviceType != null){
                                            toMac.setRemoteDeviceType(deviceType.getName());
                                        }
                                    }
                                    list.add(fromMac);
                                    list.add(toMac);
                                }
                            }

                        }
                    }
                }
            }
            if(list.size() > 0){
                // 执行去重
                List<MacTemp> batchInsert = list.stream().collect(
                        Collectors.collectingAndThen(
                                Collectors.toCollection(() -> new TreeSet<>(Comparator
                                        .comparing(MacTemp::getUuid, Comparator.nullsLast(String::compareTo))
                                        .thenComparing(MacTemp::getInterfaceName, Comparator.nullsLast(String::compareTo))
                                        .thenComparing(MacTemp::getMac, Comparator.nullsLast(String::compareTo))
                                        .thenComparing(MacTemp::getRemoteUuid, Comparator.nullsLast(String::compareTo))
                                        .thenComparing(MacTemp::getRemoteInterface, Comparator.nullsLast(String::compareTo))
                                )),
                                ArrayList::new));
                this.macTempService.batchInsert(batchInsert);
            }
        }
    }

    @Override
    public Integer selectInterfaceStatus(Map params) {
        String interfaceName = String.valueOf(params.get("interfaceName"));
        if(interfaceName == null || interfaceName.equals("")){
            return -1;
        }
        List<Item> itemTagArpList = itemMapper.selectInterfaceStatus(params);
        if(itemTagArpList.size() > 0) {
//            AtomicInteger status = new AtomicInteger();
            int status = -1;
            String name = "";
            for (Item item : itemTagArpList) {
                List<ItemTag> tags = item.getItemTags();
                if(tags.size() > 0){
                    for (ItemTag tag : tags) {
                        String value = tag.getValue();
                        if (tag.getTag().equals("ifname")) {
                            name = value;
                        }
                        if (tag.getTag().equals("ifup")) {
//                            status.set(Integer.parseInt(value));
                            status = Integer.parseInt(value);
                        }
                    }
                }
                if(name.equals(interfaceName)){
                    break;
                }
            }
            return status;
        }
        return 0;
    }

    @Test
    public void test(){
        Map map = new HashMap();
        if(map.get("ip") != null && map.get("ip").equals("aaa")){
            System.out.println(true);
        }else{
            System.out.println(false);
        }
    }

    @Override
    public void gatherSnmp(Date time) {
        List<Map> devices = this.topoNodeService.queryNetworkElement();
        if (devices != null && devices.size() > 0) {
            for (Map device : devices) {
                if(device.get("ip") != null){
                    Interface obj = this.interfaceService.selectObjByIp(device.get("ip").toString());
                    if(obj != null){
                        String avaliable = obj.getAvailable();
                        HostSnmp hs = new HostSnmp();
                        hs.setAddTime(time);
                        hs.setAvaliable(avaliable);
                        hs.setUuid(device.get("uuid").toString());
                        this.hostSnmpService.save(hs);
                    }
                }
            }
        }
    }

    @Override
    public List gatherSlotNumber(String ip) {
        Map params = new HashMap();
        params.put("ip", ip);
        params.put("obj", "device");
        List<Item> itemTags = this.itemMapper.selectItemTagByIpAndObj(params);
        List devices = new ArrayList();
        if(itemTags.size() > 0){
            itemTags.stream().forEach(item -> {
                List<ItemTag> tags = item.getItemTags();
                Map<String, String> map = new HashMap();
                if (tags != null && tags.size() > 0) {
                    for (ItemTag tag : tags) {
                        String value = tag.getValue();
                        if (tag.getTag().equals("slotid")) {
                            map.put("slotid", value);
                        }
                        if (tag.getTag().equals("board")) {
                            map.put("board", value);
                        }
                        if (tag.getTag().equals("devicestate")) {
                            map.put("devicestate", value);
                        }
                        if (tag.getTag().equals("hardver")) {
                            map.put("hardver", value);
                        }
                        if (tag.getTag().equals("softver")) {
                            map.put("softver", value);
                        }
                        if (tag.getTag().equals("slotnumber")) {
                            map.put("slotnumber", value);
                        }
                    }
//                    if(map.get("slotnumber") == null &&  map.get("slotid") != null){
//                        map.put("slotnumber", map.get("slotid"));
//                    }
//                    if(map.get("slotid") == null){
//                        int num = Integer.parseInt(map.get("slotid"));
//                        StringBuffer sb = new StringBuffer();
//                        for (int i = 0; i < num; i++) {
//                            if(i+1 == num){
//                                sb.append(i);
//                            }else{
//                                sb.append(i).append(",");
//                            }
//                        }
//                        map.put("slotid", sb.toString());
//                    }
                    devices.add(map);
                }
            });
        }
        return devices;
    }

    @Override
    public List gatherPower(String ip) {
        Map params = new HashMap();
        params.put("ip", ip);
        params.put("obj", "power");
        List<Item> itemTags = this.itemMapper.selectItemTagByIpAndObj(params);
        List list = new ArrayList();
        if(itemTags.size() > 0){
            itemTags.stream().forEach(item -> {
                List<ItemTag> tags = item.getItemTags();
                Map<String, String> map = new HashMap();
                if (tags != null && tags.size() > 0) {
                    for (ItemTag tag : tags) {
                        String value = tag.getValue();
                        if (tag.getTag().equals("powerindex")) {
                            map.put("powerindex", value);
                        }
                        if (tag.getTag().equals("powerstate")) {
                            switch (value){
                                case "1":
                                    value = "active";
                                    break;
                                case "2":
                                    value = "deactive";
                                    break;
                                case "3":
                                    value = "not-install";
                                    break;
                                case "4":
                                    value = "unsupport";
                                    break;
                                default:
                                    value = null;
                                    break;
                            }
                            map.put("powerstate", value);
                        }
                    }
                    list.add(map);
                }
            });
        }
        return list;
    }

    @Override
    public List gatherFan(String ip) {
        Map params = new HashMap();
        params.put("ip", ip);
        params.put("obj", "fan");
        List<Item> itemTags = this.itemMapper.selectItemTagByIpAndObj(params);
        List list = new ArrayList();
        if(itemTags.size() > 0){
            itemTags.stream().forEach(item -> {
                List<ItemTag> tags = item.getItemTags();
                Map<String, String> map = new HashMap();
                if (tags != null && tags.size() > 0) {
                    for (ItemTag tag : tags) {
                        String value = tag.getValue();
                        if (tag.getTag().equals("fanindex")) {
                            map.put("fanindex", value);
                        }
                        if (tag.getTag().equals("fanstate")) {
                            switch (value){
                                case "1":
                                    value = "active";
                                    break;
                                case "2":
                                    value = "deactive";
                                    break;
                                case "3":
                                    value = "not-install";
                                    break;
                                case "4":
                                    value = "unsupport";
                                    break;
                                default:
                                    value = null;
                                    break;
                            }
                            map.put("fanstate", value);
                        }
                    }
                    list.add(map);
                }
            });
        }
        return list;
    }


//    @softver
//    public void topologySyncToMac() {
//        Map params = new HashMap();
//        // 获取拓扑列表
//        List<Topology> topologies = this.topologyService.selectObjByMap(null);
//        if(topologies.size() > 0){
//            for (Topology topology : topologies) {
//                if(topology.getContent() != null){
//                    Map content = JSONObject.parseObject(topology.getContent().toString(), Map.class);
//                    if(content.get("links") != null){
//                        JSONArray links = JSONArray.parseArray(content.get("links").toString());
//                        if(links.size() > 0) {
//                            for (Object object : links) {
//                                Map link = JSONObject.parseObject(object.toString(), Map.class);
//                                String fromPort = String.valueOf(link.get("fromPort"));
//                                String from = String.valueOf(link.get("from"));
//                                String toPort = String.valueOf(link.get("toPort"));
//                                String to = String.valueOf(link.get("to"));
//                                params.clear();
//                                params.put("interfaceName", fromPort);
//                                params.put("uuid", from);
//                                params.put("tag", "E");
//                                List<MacTemp> fromMacTemps = this.macTempService.selectByMap(params);
//                                if(fromMacTemps.size() > 0){
//                                    for (MacTemp macTemp : fromMacTemps) {
//                                        try {
//                                            Map toNode = JSONObject.parseObject(link.get("toNode").toString(), Map.class);
//                                            if(toNode != null){
//                                                String name = String.valueOf(toNode.get("name"));
//                                                macTemp.setRemoteDevice(name);
//                                            }
//                                            if(!toPort.equals("")){
//                                                macTemp.setRemoteInterface(toPort);
//                                            }
//                                            macTemp.setRemoteUuid(to);
//                                            macTemp.setTag("DE");
//                                            this.macTempService.update(macTemp);
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                }
//
//                                // 反向连线
//                                params.clear();
//                                params.put("interfaceName", toPort);
//                                params.put("uuid", to);
//                                params.put("tag", "E");
//                                List<MacTemp> toMacTemps = this.macTempService.selectByMap(params);
//                                if(toMacTemps.size() > 0){
//                                    for (MacTemp macTemp : toMacTemps) {
//                                        try {
//                                            Map fromNode = JSONObject.parseObject(link.get("fromNode").toString(), Map.class);
//                                            if(fromNode != null){
//                                                String name = String.valueOf(fromNode.get("name"));
//                                                macTemp.setRemoteDevice(name);
//                                            }
//                                            if(!toPort.equals("")){
//                                                macTemp.setRemoteInterface(fromPort);
//                                            }
//                                            macTemp.setRemoteUuid(from);
//                                            macTemp.setTag("DE");
//                                            this.macTempService.update(macTemp);
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

}
