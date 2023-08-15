//package com.metoo.nspm.core.manager.datagear;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.metoo.nspm.core.utils.ResponseUtil;
//import com.metoo.nspm.dto.zabbix.ParamsDTO;
//import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
//import io.github.hengyunabc.zabbix.api.Request;
//import io.github.hengyunabc.zabbix.api.RequestBuilder;
//import io.github.hengyunabc.zabbix.api.ZabbixApi;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//
//import javax.xml.crypto.Data;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/datagear")
//public class DataGearManagerController {
//
//    DataGearApi dataGearApi = null;
//
//    private DataGearApiUtil dataGearApiUtil;
//
//    @GetMapping("/login")
//    public Object login() {
//        String BASE_URL = "http://127.0.0.1:50401";
//        dataGearApi = new DefaultDataGearApi(BASE_URL);
//        dataGearApi.init();
//        boolean flag = dataGearApi.login("admin", "admin");
//        if(flag){
//            return ResponseUtil.ok();
//        }
//        return ResponseUtil.error();
//    }
//
//    @PostMapping("/chartPlugin/queryData")
//    public Object queryData(@RequestBody String param) {
//
//        GearRequest request = new GearRequest();
//
//        request.setParam(param);
//
//        request.setMethod("/chartPlugin/queryData");
//
//        JSONArray result = dataGearApiUtil.call(request);
//
//        return ResponseUtil.ok(result);
//    }
//
////    @PostMapping("/chartPlugin/queryData")
////    public Object queryData(@RequestBody String param) {
////        String BASE_URL = "http://127.0.0.1:50401";
////
////        GearRequest request = new GearRequest();
////
////        request.setParam(param);
////
////        request.setMethod("/chartPlugin/queryData");
////
////        dataGearApi.init();
////
////        JSONArray result = dataGearApi.call(request);
////
////        return ResponseUtil.ok(result);
////    }
//
//
//}
