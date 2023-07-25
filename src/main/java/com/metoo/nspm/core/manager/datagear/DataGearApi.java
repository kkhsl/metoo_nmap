package com.metoo.nspm.core.manager.datagear;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.github.hengyunabc.zabbix.api.DeleteRequest;
import io.github.hengyunabc.zabbix.api.Request;

public interface DataGearApi {

    void init();

    void destroy();

    JSONArray call(GearRequest var1);

    JSONObject loginCall(GearRequest request);

    JSONObject call(DeleteRequest var1);

    boolean login(String var1, String var2);
}
