package com.metoo.nspm.core.manager.datagear;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.metoo.nspm.core.manager.myzabbix.utils.ZabbixApiUtil;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DataGearApiUtil {

    public static String BASE_URL;

    public static final String USER = "admin";// 默认账号
    public static final String PASSWORD = "admin";// 默认密码

    public static DataGearApi DATAGEAR_API = null;// dataGearApi实例

    @Value("${datagear.url}")
    public void setUrl(String url) {
        DataGearApiUtil.BASE_URL= url;
    }

    @PostConstruct
    public void init() {
        DataGearApi dataGearApi = new DefaultDataGearApi(BASE_URL);
        dataGearApi.init();
        try {
            dataGearApi.login(DataGearApiUtil.USER, DataGearApiUtil.PASSWORD);
            DATAGEAR_API = dataGearApi;
        } catch (Exception e) {
            e.printStackTrace();
            DATAGEAR_API = null;
        }
    }

    public static void verifyDataGear(){// UnknownHostException
        if(DATAGEAR_API == null){
            DataGearApi dataGearApi = new DefaultDataGearApi(BASE_URL);
            dataGearApi.init();
            try {
                dataGearApi.login(DataGearApiUtil.USER, DataGearApiUtil.PASSWORD);
                DATAGEAR_API = dataGearApi;
            } catch (Exception e) {
                e.printStackTrace();
                DATAGEAR_API = null;
            }
        }
    }

    public static JSONArray call(GearRequest request){
        if(DATAGEAR_API != null){
            JSONArray result = null;
            try {
                result = DATAGEAR_API.call(request);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

}
