package com.metoo.nspm.core.websocket.api;

import com.alibaba.fastjson.JSONObject;
import com.metoo.nspm.core.config.websocket.demo.NoticeWebsocketResp;
import com.metoo.nspm.core.manager.admin.tools.DateTools;
import com.metoo.nspm.core.manager.admin.tools.MacUtil;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.utils.network.IpUtil;
import com.metoo.nspm.entity.nspm.*;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RequestMapping("/websocket/api/zabbix")
@RestController
public class TopologyManagerApi {

    @Autowired
    private IMacHistoryService macHistoryService;
    @Autowired
    private MacUtil macUtil;
    @Autowired
    private ISubnetService subnetService;
    @Autowired
    private IVlanService vlanService;
    @Autowired
    private ITerminalService terminalService;
    @Autowired
    private IDeviceTypeService deviceTypeService;
    @Autowired
    private RedisResponseUtils redisResponseUtils;

    public static void main(String[] args) {
        String time = null;
        System.out.println(Strings.isBlank(time));
    }

    @Test
    public void test(){
        String arg = "";
        Map params = new HashMap();
        params.put("arg", "");
        System.out.println(String.valueOf(params.get("arg")));
    }
    @ApiOperation("设备 Mac (DT))")
    @GetMapping(value = {"/mac/dt"})
    public NoticeWebsocketResp getObjMac(@RequestParam(value = "requestParams", required = false) String requestParams) {
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        if(!String.valueOf(requestParams).equals("")){
            Map params = JSONObject.parseObject(String.valueOf(requestParams), Map.class);
            String sessionId = (String)  params.get("sessionId");
//            Date time = DateTools.parseDate(String.valueOf(params.get("time")), "yyyy-MM-dd HH:mm");
            List<String> list = JSONObject.parseObject(String.valueOf(params.get("params")), List.class);
            Map result = new HashMap();
            Map args = new HashMap();
            if(params.get("time") == null){
                for (String uuid : list) {
                    args.clear();
                    args.put("uuid", uuid);
                    args.put("online", 1);
                    args.put("interfaceStatus", 1);
//                    args.put("tag", "DT");
                    args.put("tags", Arrays.asList("DT", "UDT"));
                    List<Terminal> terminals = this.terminalService.selectObjByMap(args);
                    terminals.stream().forEach(item -> {
                        String terminalIp = item.getIp();
                        if(StringUtils.isNotEmpty(terminalIp) && StringUtils.isEmpty(item.getVlan())){
                            // 获取网络地址
                            String network = IpUtil.getNBIP(terminalIp,"255.255.255.255", 0);
                            Subnet subnet = this.subnetService.selectObjByIp(network);
                            if(subnet != null){
                                if(subnet.getVlanId() != null && !subnet.getVlanId().equals("")){
                                    Vlan vlan = this.vlanService.selectObjById(subnet.getVlanId());
                                    if(vlan != null){
                                        item.setVlan(vlan.getName());
                                    }
                                }
                            }
                        }
                    });
                    this.macUtil.terminalJoint(terminals);
                    terminals.stream().forEach(e -> {
                        if(e.getDeviceTypeId() != null
                                && !e.getDeviceTypeId().equals("")){
//                            TerminalType terminalType = this.terminalTypeService.selectObjById(e.getTerminalTypeId());
//                            e.setTerminalTypeName(terminalType.getName());
                            DeviceType deviceType = this.deviceTypeService.selectObjById(e.getDeviceTypeId());
                            if(deviceType != null){
                                e.setDeviceTypeName(deviceType.getName());
                                e.setDeviceTypeUuid(deviceType.getUuid());
                            }
                        }
                    });
                    result.put(uuid, terminals);
                }
            }else{
                for (String item : list) {
                    args.clear();
                    args.put("uuid", item);
                    args.put("tags", Arrays.asList("DT", "UDT"));
                    args.put("time", params.get("time"));
                    args.put("online", true);
                    args.put("interfaceStatus", 1);
                    List<Mac> macs = this.macHistoryService.selectByMap(args);
                    this.macUtil.macJoint(macs);
                    result.put(item, macs);
                }
            }
            rep.setNoticeType("4");
            rep.setNoticeStatus(1);
            rep.setNoticeInfo(result);
            this.redisResponseUtils.syncStrRedis(sessionId, JSONObject.toJSONString(result), 4);
            return rep;
        }
        rep.setNoticeType("4");
        rep.setNoticeStatus(0);
        return rep;
    }
}
