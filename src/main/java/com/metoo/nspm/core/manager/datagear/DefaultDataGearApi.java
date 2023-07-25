package com.metoo.nspm.core.manager.datagear;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.DeleteRequest;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultDataGearApi implements DataGearApi {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDataGearApi.class);

    private CloseableHttpClient httpClient;

    private URI uri;

    private volatile String SESSIONID;

    public String getSESSIONID(){
        return this.SESSIONID;
    }

    public void setSESSIONID(String SESSIONID){
        this.SESSIONID = SESSIONID;
    }

    private BasicCookieStore cookieStore = null;


    public DefaultDataGearApi(String url) {
        try {
            this.uri = new URI(url.trim());
        } catch (URISyntaxException var3) {
            throw new RuntimeException("url invalid", var3);
        }
    }

    public DefaultDataGearApi(URI uri) {
        this.uri = uri;
    }

    public DefaultDataGearApi(String url, CloseableHttpClient httpClient) {
        this(url);
        this.httpClient = httpClient;
    }

    public DefaultDataGearApi(URI uri, CloseableHttpClient httpClient) {
        this(uri);
        this.httpClient = httpClient;
    }

    @Override
    public void init() {
        if (this.httpClient == null) {
            cookieStore = new BasicCookieStore();
            this.httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        }
    }

    @Override
    public void destroy() {
        if (this.httpClient != null) {
            try {
                this.httpClient.close();
            } catch (Exception var2) {
                logger.error("close httpclient error!", var2);
            }
        }
    }

    @Override
    public JSONObject loginCall(GearRequest request) {
            HttpPost httppost = new HttpPost(this.uri + request.getMethod());
            // 创建参数队列
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("name", "admin"));
            formparams.add(new BasicNameValuePair("password", "admin"));
            UrlEncodedFormEntity uefEntity;
            try {
                uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
                httppost.setEntity(uefEntity);
                System.out.println("executing request " + httppost.getURI());

                CloseableHttpResponse response = this.httpClient.execute(httppost);

                HttpEntity entity = response.getEntity();
                List<Cookie> cookies = cookieStore.getCookies();
                Cookie cookie = null;
                cookie = cookies.get(0);
                this.setSESSIONID(cookie.getValue());
                byte[] data = EntityUtils.toByteArray(entity);
                return (JSONObject)JSON.parse(data, new Feature[0]);
            } catch (IOException var6) {
                System.out.println(var6.getMessage());
                throw new RuntimeException("DefaultZabbixApi call exception!", var6);
            }

    }


    @Override
    public JSONArray call(GearRequest request) {
        try {
            HttpUriRequest httpRequest = org.apache.http.client.methods.RequestBuilder.post()
                    .setUri(this.uri + request.getMethod())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("cookie", "SESSIONID" + "=" + this.getSESSIONID())
                    .setEntity(new StringEntity(request.getParam(), ContentType.APPLICATION_JSON)).build();

            CloseableHttpResponse response = this.httpClient.execute(httpRequest);
            HttpEntity entity = response.getEntity();
            byte[] data = EntityUtils.toByteArray(entity);
            return (JSONArray)JSON.parse(data, new Feature[0]);
        } catch (IOException var6) {
            throw new RuntimeException("DefaultZabbixApi call exception!", var6);
        }
    }

    @Override
    public JSONObject call(DeleteRequest var1) {
        return null;
    }

    @Override
    public boolean login(String name, String password) {
        this.SESSIONID = null;
        GearRequest request = new GearRequest();
        Map map = request.getParams();
        request.setMethod("/login/doLogin");
        map.put("name", name);
        map.put("password", password);
        JSONObject response = this.loginCall(request);
        String success = response.getString("success");
        return Boolean.valueOf(success);
    }



}
