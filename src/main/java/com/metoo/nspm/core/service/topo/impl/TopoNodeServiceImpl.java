package com.metoo.nspm.core.service.topo.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.metoo.nspm.core.manager.zabbix.tools.InterfaceUtil;
import com.metoo.nspm.core.service.nspm.INetworkElementService;
import com.metoo.nspm.core.service.nspm.ISysConfigService;
import com.metoo.nspm.core.service.topo.ITopoNodeService;
import com.metoo.nspm.core.utils.NodeUtil;
import com.metoo.nspm.dto.TopoNodeDto;
import com.metoo.nspm.entity.nspm.NetworkElement;
import com.metoo.nspm.entity.nspm.SysConfig;
import com.metoo.nspm.entity.zabbix.Interface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TopoNodeServiceImpl implements ITopoNodeService {

    @Autowired
    private ISysConfigService sysConfigService;
    @Autowired
    private NodeUtil nodeUtil;
    @Autowired
    private INetworkElementService networkElementService;
    @Autowired
    private InterfaceUtil interfaceUtil;

    @Override
    public List<Map> queryAnt() {
        SysConfig sysConfig = this.sysConfigService.select();
        String token = sysConfig.getNspmToken();
        if (token != null) {
            List<Map> ipList = new ArrayList<>();
            String url = "topology/node/queryNode.action";
            TopoNodeDto dto = new TopoNodeDto();
//            User user = ShiroUserHolder.currentUser();
//            if (user.getGroupLevel() == null || user.getGroupLevel().equals("")) {
//                dto.setBranchLevel(user.getGroupLevel());
//            }
            dto.setStart(1);
            dto.setLimit(100000);
            Object result = this.nodeUtil.getBody(dto, url, token);
            JSONObject object = JSONObject.parseObject(result.toString());
            if (object.get("data") != null && object.getInteger("total") > 0) {
                JSONArray arrays = JSONArray.parseArray(object.get("data").toString());
                for (Object array : arrays) {
                    Map map = new HashMap();
                    JSONObject data = JSONObject.parseObject(array.toString());
                    if (object != null && object.get("errorMess") != null) {
                        if (data.get("errorMess") != null && !data.get("errorMess").toString().equals("")) {
                            return null;
                        }
                    }

                    if(data.get("isVsys") == null || !data.getBoolean("isVsys")){
//                            ipList.add(data.getString("ip"));
                        map.put("ip", data.getString("ip"));
                        map.put("deviceName", data.getString("deviceName"));
                        map.put("pluginId", data.getString("pluginId"));
                        map.put("uuid", data.getString("uuid"));
                        ipList.add(map);
                    }
                }
                return ipList;
            }
        }
        return null;
    }

    @Override
    public List<Map> queryNetworkElement() {
        List<NetworkElement> nes = this.networkElementService.selectObjByMap(null);
        List<Map> devices = new ArrayList<>();
        if(nes.size() > 0) {
            for (NetworkElement ne : nes) {
                Interface obj = this.interfaceUtil.getInteface(ne.getIp());
                // 校验主机是否宕机
                if(obj != null){
                    Map map = new HashMap();
                    map.put("ip", ne.getIp());
                    map.put("deviceName", ne.getDeviceName());
                    map.put("uuid", ne.getUuid());
                    map.put("deviceType", ne.getVendorName());
                    map.put("type", obj.getValue());
                    devices.add(map);
                }
            }
        }
        return devices;
    }

    public static void main(String[] args) {
//        Map<String, String> params = new HashMap();
//        params.put("name", null);
//        try {
//            System.out.println("测试空指针 toString()：" + params.get("name").toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("测试空指针 toString()：" + e.getMessage());
//            System.out.println("测试空指针 toString()：" + e.getLocalizedMessage());
//            System.out.println("测试空指针 toString()：" + e.getCause());
//            System.out.println("测试空指针 toString()：" + e.getSuppressed());
//            System.out.println("测试空指针 toString()：" + e.getStackTrace());
//            System.out.println("测试空指针 toString()：" + e.getClass());
//        }
//        System.out.println("测试空指针 String.valueOf()：" + String.valueOf(params.get("name")));
//        System.out.println("测试空指针 String.valueOf()：" + params.get("a"));

//        Map<String, Integer> numM = new HashMap();
//        numM.put("num", -1);
////        int i = numM.get("i");
//
//        Integer ii = numM.get("i");
//
//        int iii = numM.get("num");
//
//        Integer iiii = numM.get("num");


        int i =128;
        byte b = (byte)i;
        System.out.println(i);
        System.out.println(b);

    }

}
