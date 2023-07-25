package com.metoo.nspm.core.service.nspm.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.util.StringUtil;
import com.metoo.nspm.core.manager.admin.tools.ShiroUserHolder;
import com.metoo.nspm.core.mapper.nspm.TopologyHistoryMapper;
import com.metoo.nspm.core.mapper.nspm.TopologyMapper;
import com.metoo.nspm.core.mapper.zabbix.ItemMapper;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.service.zabbix.InterfaceService;
import com.metoo.nspm.core.utils.collections.ListSortUtil;
import com.metoo.nspm.core.utils.network.IpUtil;
import com.metoo.nspm.dto.TopologyDTO;
import com.metoo.nspm.entity.nspm.*;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.metoo.nspm.entity.zabbix.Interface;
import com.metoo.nspm.entity.zabbix.Item;
import com.metoo.nspm.entity.zabbix.ItemTag;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
@Lazy
@Service
@Transactional
public class TopologyServiceImpl implements ITopologyService {

    @Autowired
    private TopologyMapper topologyMapper;
    @Autowired
    private TopologyHistoryMapper topologyHistoryMapper;
    @Autowired
    private ILinkService linkService;
    @Autowired
    private IDeviceTypeService deviceTypeService;
    @Autowired
    private INetworkElementService networkElementService;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private InterfaceService interfaceService;
//    @Autowired
//    private ITerminalService terminalService;

    @Override
    public Topology selectObjById(Long id) {
        return this.topologyMapper.selectObjById(id);
    }

    @Override
    public Topology selectObjBySuffix(String name) {
        return this.topologyMapper.selectObjBySuffix(name);
    }

    @Override
    public Page<Topology> selectConditionQuery(TopologyDTO instance) {
        if(instance == null){
            instance = new TopologyDTO();
        }
        Page<Topology> page = PageHelper.startPage(instance.getCurrentPage(), instance.getPageSize());

        List<Topology> topologies = this.topologyMapper.selectConditionQuery(instance);
        return page;
    }

    @Override
    public List<Topology> selectObjByMap(Map params) {
        return this.topologyMapper.selectObjByMap(params);
    }

    @Override
    public List<Topology> selectTopologyByMap(Map params) {
        return this.topologyMapper.selectTopologyByMap(params);
    }

    @Override
    public int save(Topology instance) {
        if(instance.getId() == null){
            instance.setAddTime(new Date());
        }else{
            instance.setUpdateTime(new Date());
        }
        if(instance.getContent() != null && !instance.getContent().equals("")){
            // 解析content 并写入uuid
            Object content = this.writerUuid(instance.getContent());
            instance.setContent(content);
            this.syncLinkManager(instance.getContent());
        }
        if(instance.getId() == null){
            try {
               int i = this.topologyMapper.save(instance);
                if(i >= 1){
                    try {
                        Calendar cal = Calendar.getInstance();
                        instance.setAddTime(cal.getTime());
                        this.topologyHistoryMapper.save(instance);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return instance.getId().intValue();
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }else{
            try {
                int i = this.topologyMapper.update(instance);
                if(i >= 1){
                    try {
                        Calendar cal = Calendar.getInstance();
                        instance.setAddTime(cal.getTime());
                        this.topologyHistoryMapper.save(instance);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return i;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    // 为拓扑图连线增加Uuid
    public Object writerUuid(Object param){
        if (param != null) {
            Map content = JSONObject.parseObject(param.toString(), Map.class);
            JSONArray links = this.getLinks(content);
            if(links.size() > 0){
                List list = this.setUuid(links);
                content.put("links", list);
                return JSON.toJSONString(content);
            }
        }
        return param;
    }

    // 同步到链路管理
    public Object syncLinkManager(Object param){
        if (param != null) {
            Map content = JSONObject.parseObject(param.toString(), Map.class);
            JSONArray links = this.getLinks(content);
            if(links.size() > 0){
                List<Link> linkList = new ArrayList();
                for (Object object : links) {
                    Map link = JSONObject.parseObject(object.toString(), Map.class);
                    Map params = new HashMap();
                    if(link.get("fromNode") == null || link.get("toNode") == null){
                        continue;
                    }
                    Map fromNode = JSONObject.parseObject(link.get("fromNode").toString(), Map.class);
                    params.put("startIp", fromNode.get("ip"));
                    Map toNode = JSONObject.parseObject(link.get("toNode").toString(), Map.class);
                    params.put("endIp", toNode.get("ip"));
                    List<Link> linkList1 = this.linkService.selectObjByMap(params);
                    if(linkList1.size() == 0){
                        Link link1 = new Link();
                        link1.setAddTime(new Date());
                        link1.setStartDevice(String.valueOf(fromNode.get("name")));
                        link1.setStartInterface(String.valueOf(link.get("fromPort")));
                        link1.setStartIp(String.valueOf(fromNode.get("ip")));
                        link1.setEndDevice(String.valueOf(toNode.get("name")));
                        link1.setEndInterface(String.valueOf(link.get("toPort")));
                        link1.setEndIp(String.valueOf(toNode.get("ip")));

                        User user = ShiroUserHolder.currentUser();
                        if(link1.getGroupId() == null){
                            link1.setGroupId(user.getGroupId());
                        }
                        linkList.add(link1);
                    }

                }
                if(linkList.size() > 0){
                    try {
                        this.linkService.batchesInsert(linkList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return param;
    }

    public JSONArray getLinks(Map content){
        if (content != null) {
            if (content.get("links") != null) {
                JSONArray links = JSONArray.parseArray(content.get("links").toString());
                if (links.size() > 0) {
                    return links;
                }
            }
        }
        return new JSONArray();
    }

    public List setUuid(JSONArray links){
        List list = new ArrayList();
        for (Object object : links) {
            Map link = JSONObject.parseObject(object.toString(), Map.class);
            if(link.get("uuid") == null || link.get("uuid").equals("")){
                link.put("uuid", UUID.randomUUID());
            }
            list.add(link);
        }
        return list;
    }

    @Override
    public int update(Topology instance) {
        try {
            return this.topologyMapper.update(instance);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int delete(Long id) {
        try {
            return this.topologyMapper.delete(id);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public Long copy(Topology instance) {
        try {
            int i = this.topologyMapper.copy(instance);
            if(i >= 1){
                return instance.getId();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public List<Map<String, Object>> getDevicePortsByUuid(String uuid) {
        Map params = new HashMap();
        params.put("uuid", uuid);
        List<NetworkElement> networkElements = this.networkElementService.selectObjByMap(params);
        String name = "";
        String ip = "";
        Integer type = -1;
        int portIndex = 0;
        if(networkElements.size() > 0){
            NetworkElement networkElement = networkElements.get(0);
            DeviceType deviceType = this.deviceTypeService.selectObjById(networkElement.getDeviceTypeId());
            if(deviceType != null){
                type = deviceType.getType();
            }
            ip = networkElement.getIp();
            name = networkElement.getInterfaceName();
            if(type == 12){
                portIndex = networkElement.getPortIndex() != null ? networkElement.getPortIndex() : 0;
            }
        }
        List<Map<String, Object>> list = new ArrayList();
        if(type != 10 && type != 12 && type != -1){
            params.put("ip", ip);
            params.put("index", "ifindex");
            List<com.metoo.nspm.entity.zabbix.Item> itemTagList = this.itemMapper.interfaceTable(params);
            if(itemTagList.size() > 0){
                // 校验主机vendor
                List<Map<String, String>> h3cList = new ArrayList();
                Interface anInterface = this.interfaceService.selectInfAndTag(ip);
                if(anInterface != null){
//                    String vendor = "";
                    if(anInterface.getItemTags().size() > 0) {
                        if (true) {// vendor.equals("H3C")
                            // 获取默认vlan、端口类型
                            params.clear();
                            params.put("ip", ip);
                            params.put("tag", "ifvlan");
                            List<com.metoo.nspm.entity.zabbix.Item> h3cObj = this.itemMapper.selectItemTagByIpAndObjToPort(params);
                            if(h3cObj.size() > 0){
                                for (com.metoo.nspm.entity.zabbix.Item item : h3cObj) {
                                    Map<String, String> h3cMap = new HashMap();
                                    List<ItemTag> tags = item.getItemTags();
                                    for (ItemTag tag : tags) {
                                        if (tag.getTag().equals("ifname")) {
                                            h3cMap.put("ifname", tag.getValue());
                                        }
                                        if (tag.getTag().equals("iftype")) {
                                            String vlandefault = "unknown";
                                            if(Strings.isNotBlank(tag.getValue())){
                                                switch (tag.getValue()){
                                                    case "1":
                                                        vlandefault = "trunk";
                                                        break;
                                                    case "2":
                                                        vlandefault = "access";
                                                        break;
                                                    default:
                                                        vlandefault = "unknown";
                                                        break;
                                                }
                                            }
                                            h3cMap.put("iftype", vlandefault);
                                        }
                                        if (tag.getTag().equals("defaultvlan")) {
                                            h3cMap.put("defaultvlan", tag.getValue());
                                        }
                                        if (tag.getTag().equals("index")) {
                                            h3cMap.put("name", tag.getName());
                                        }
                                    }
                                    h3cList.add(h3cMap);
                                }
                            }
                        }
                    }
                }

                for (Item item : itemTagList) {
                    List<ItemTag> tags = item.getItemTags();
                    Map map = new HashMap();
                    map.put("description", "");
                    map.put("name", "");
                    map.put("ip", "");
                    map.put("mask", "");
                    map.put("status", "");

                    map.put("iftype", "");
                    map.put("defaultvlan", "");
                    for (ItemTag tag : tags) {
                        if (tag.getTag().equals("description")) {
                            map.put("description", StringUtil.isEmpty(tag.getValue()) ? "" : tag.getValue());
                        }
                        if (tag.getTag().equals("ifname")) {
                            map.put("name", StringUtil.isEmpty(tag.getValue()) ? "" : tag.getValue());
                            if(h3cList.size() > 0){
                                for (Map<String, String> o : h3cList) {
                                    boolean flag = false;
                                    for(Map.Entry<String, String> h3cifvlan : o.entrySet()){
                                        if(h3cifvlan.getKey().equals("name")){
                                            if(h3cifvlan.getValue() != null && h3cifvlan.getValue().equals(tag.getValue())){
                                                flag = true;
                                            }
                                        }
                                    }
                                    if(flag){
                                        for(Map.Entry<String, String> h3cifvlan : o.entrySet()){
                                            map.put(h3cifvlan.getKey(), h3cifvlan.getValue());
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        if (tag.getTag().equals("ifup")) {
                            String status = "";
                            switch (tag.getValue()) {
                                case "1":
                                    status = "up";
                                    break;
                                case "2":
                                    status = "down";
                                    break;
                                default:
                                    status = "unknown";
                            }
                            map.put("status", status);
                        }
                        if (tag.getTag().equals("ifindex")) {
                            map.put("index", tag.getValue());
                            StringBuffer ip_mask = new StringBuffer();
                            if(tag.getIp() != null && !tag.getIp().equals("")){
                                String[] ips = tag.getIp().split("/");
                                String[] masks = tag.getMask().split("/");
                                if(ips.length == 0){
                                    map.put("ip", "");
                                }
                                if(ips.length == 1){
                                    ip_mask.append(tag.getIp());
                                    if(tag.getMask() != null && !tag.getMask().equals("")){
                                        ip_mask.append("/").append(IpUtil.getBitMask(tag.getMask()));
                                    }
                                    map.put("ip", ip_mask);
                                }
                                if(ips.length > 1 && masks.length > 1){
                                    for(int i = 0; i < ips.length; i ++){
                                        ip_mask.append(ips[i]).append("/").append(IpUtil.getBitMask(masks[i]));
                                        if(i + 1 < ips.length){
                                            ip_mask .append("\n");
                                        }
                                    }
                                    map.put("ip", ip_mask);
                                }
                            }
                        }
                    }
                    list.add(map);
                }
            }
        }else{
            if(type == 12 && portIndex > 0){
                for (int i = 0; i < portIndex; i++) {
                    Map portMap = new HashMap();
                    portMap.put("ip", ip);
                    portMap.put("name", name + i);
                    portMap.put("status", "up");
                    list.add(portMap);
                }
            }
        }
        if(list != null && list.size() > 0){
            ListSortUtil.sortStr(list);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getTerminalPortsByUuid(String uuid) {
//        Map params = new HashMap();
//        params.put("uuid", uuid);
//        List<NetworkElement> networkElements = this.networkElementService.selectObjByMap(params);
//        String name = "";
//        String ip = "";
//        Integer type = -1;
//        int portIndex = 0;
//        if(networkElements.size() > 0){
//            NetworkElement networkElement = networkElements.get(0);
//            DeviceType deviceType = this.deviceTypeService.selectObjById(networkElement.getDeviceTypeId());
//            if(deviceType != null){
//                type = deviceType.getType();
//            }
//            ip = networkElement.getIp();
//            name = networkElement.getInterfaceName();
//            if(type == 12){
//                portIndex = networkElement.getPortIndex() != null ? networkElement.getPortIndex() : 0;
//            }
//        }
//        List<Map<String, Object>> list = new ArrayList();
//        if(type != 10 && type != 12 && type != -1){
//            params.put("ip", ip);
//            params.put("index", "ifindex");
//            List<com.metoo.nspm.entity.zabbix.Item> itemTagList = this.itemMapper.interfaceTable(params);
//            if(itemTagList.size() > 0){
//                // 校验主机vendor
//                List<Map<String, String>> h3cList = new ArrayList();
//                Interface anInterface = this.interfaceService.selectInfAndTag(ip);
//                if(anInterface != null){
////                    String vendor = "";
//                    if(anInterface.getItemTags().size() > 0) {
//                        if (true) {// vendor.equals("H3C")
//                            // 获取默认vlan、端口类型
//                            params.clear();
//                            params.put("ip", ip);
//                            params.put("tag", "ifvlan");
//                            List<com.metoo.nspm.entity.zabbix.Item> h3cObj = this.itemMapper.selectItemTagByIpAndObjToPort(params);
//                            if(h3cObj.size() > 0){
//                                for (com.metoo.nspm.entity.zabbix.Item item : h3cObj) {
//                                    Map<String, String> h3cMap = new HashMap();
//                                    List<ItemTag> tags = item.getItemTags();
//                                    for (ItemTag tag : tags) {
//                                        if (tag.getTag().equals("ifname")) {
//                                            h3cMap.put("ifname", tag.getValue());
//                                        }
//                                        if (tag.getTag().equals("iftype")) {
//                                            String vlandefault = "unknown";
//                                            if(Strings.isNotBlank(tag.getValue())){
//                                                switch (tag.getValue()){
//                                                    case "1":
//                                                        vlandefault = "trunk";
//                                                        break;
//                                                    case "2":
//                                                        vlandefault = "access";
//                                                        break;
//                                                    default:
//                                                        vlandefault = "unknown";
//                                                        break;
//                                                }
//                                            }
//                                            h3cMap.put("iftype", vlandefault);
//                                        }
//                                        if (tag.getTag().equals("defaultvlan")) {
//                                            h3cMap.put("defaultvlan", tag.getValue());
//                                        }
//                                        if (tag.getTag().equals("index")) {
//                                            h3cMap.put("name", tag.getName());
//                                        }
//                                    }
//                                    h3cList.add(h3cMap);
//                                }
//                            }
//                        }
//                    }
//                }
//
//                for (Item item : itemTagList) {
//                    List<ItemTag> tags = item.getItemTags();
//                    Map map = new HashMap();
//                    map.put("description", "");
//                    map.put("name", "");
//                    map.put("ip", "");
//                    map.put("mask", "");
//                    map.put("status", "");
//
//                    map.put("iftype", "");
//                    map.put("defaultvlan", "");
//                    for (ItemTag tag : tags) {
//                        if (tag.getTag().equals("description")) {
//                            map.put("description", StringUtil.isEmpty(tag.getValue()) ? "" : tag.getValue());
//                        }
//                        if (tag.getTag().equals("ifname")) {
//                            map.put("name", StringUtil.isEmpty(tag.getValue()) ? "" : tag.getValue());
//                            if(h3cList.size() > 0){
//                                for (Map<String, String> o : h3cList) {
//                                    boolean flag = false;
//                                    for(Map.Entry<String, String> h3cifvlan : o.entrySet()){
//                                        if(h3cifvlan.getKey().equals("name")){
//                                            if(h3cifvlan.getValue() != null && h3cifvlan.getValue().equals(tag.getValue())){
//                                                flag = true;
//                                            }
//                                        }
//                                    }
//                                    if(flag){
//                                        for(Map.Entry<String, String> h3cifvlan : o.entrySet()){
//                                            map.put(h3cifvlan.getKey(), h3cifvlan.getValue());
//                                        }
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                        if (tag.getTag().equals("ifup")) {
//                            String status = "";
//                            switch (tag.getValue()) {
//                                case "1":
//                                    status = "up";
//                                    break;
//                                case "2":
//                                    status = "down";
//                                    break;
//                                default:
//                                    status = "unknown";
//                            }
//                            map.put("status", status);
//                        }
//                        if (tag.getTag().equals("ifindex")) {
//                            map.put("index", tag.getValue());
//                            StringBuffer ip_mask = new StringBuffer();
//                            if(tag.getIp() != null && !tag.getIp().equals("")){
//                                String[] ips = tag.getIp().split("/");
//                                String[] masks = tag.getMask().split("/");
//                                if(ips.length == 0){
//                                    map.put("ip", "");
//                                }
//                                if(ips.length == 1){
//                                    ip_mask.append(tag.getIp());
//                                    if(tag.getMask() != null && !tag.getMask().equals("")){
//                                        ip_mask.append("/").append(IpUtil.getBitMask(tag.getMask()));
//                                    }
//                                    map.put("ip", ip_mask);
//                                }
//                                if(ips.length > 1 && masks.length > 1){
//                                    for(int i = 0; i < ips.length; i ++){
//                                        ip_mask.append(ips[i]).append("/").append(IpUtil.getBitMask(masks[i]));
//                                        if(i + 1 < ips.length){
//                                            ip_mask .append("\n");
//                                        }
//                                    }
//                                    map.put("ip", ip_mask);
//                                }
//                            }
//                        }
//                    }
//                    list.add(map);
//                }
//            }
//        }else{
//            if(type == 12 && portIndex > 0){
//                for (int i = 0; i < portIndex; i++) {
//                    Map portMap = new HashMap();
//                    portMap.put("ip", ip);
//                    portMap.put("name", name + i);
//                    portMap.put("status", "up");
//                    list.add(portMap);
//                }
//            }
//        }
//
//        List list1 = new ArrayList();
//        // 查询设备终端
//        params.clear();
//        params.put("uuid", uuid);
//        List<Terminal> terminals = this.terminalService.selectObjByMap(params);
//        if(terminals.size() > 0){
//            List<String> interfaces = terminals.stream().map(e -> e.getInterfaceName()).collect(Collectors.toList());
//            list1 = list.stream().filter(item -> !interfaces.contains(item.get("name"))).collect(Collectors.toList());
//        }
//        if(list1.size() > 0){
//            ListSortUtil.sortStr(list1);
//            return list;
//        }
//        if(list != null && list.size() > 0){
//            ListSortUtil.sortStr(list);
//        }
//        return list;
        return null;
    }
}
