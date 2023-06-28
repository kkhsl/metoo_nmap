package com.metoo.nspm.core.websocket.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.metoo.nspm.core.config.websocket.demo.NoticeWebsocketResp;
import com.metoo.nspm.core.service.zabbix.IProblemService;
import com.metoo.nspm.core.utils.query.PageInfo;
import com.metoo.nspm.dto.NspmProblemDTO;
import com.metoo.nspm.entity.zabbix.Problem;
import io.swagger.annotations.ApiOperation;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

@RequestMapping("/websocket/api/problem")
@RestController
public class ProblemManagerApi {

    @Autowired
    private IProblemService problemService;@Autowired
    private RedisResponseUtils redisResponseUtils;


    @ApiOperation("告警信息")
    @GetMapping
    public NoticeWebsocketResp problemp(
            @RequestParam(value = "requestParams", required = false) String requestParams){
        Map<String, Object> requestParam = JSONObject.parseObject(requestParams, Map.class);
        String sessionId = (String) requestParam.get("sessionId");
        Map params = new HashMap();
        params.put("limit", requestParam.get("limit"));
//        params.put("orderBy", "clock");
//        params.put("orderType", "desc");
        List<Problem> result = this.problemService.selectObjByMap(params);
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        rep.setNoticeType("6");
        rep.setNoticeInfo(result);
        this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(result), 6);
//        this.redisResponseUtils.syncRedis(sessionId, problemList, 6);
        if(result.size() > 0){
            rep.setNoticeStatus(1);
        }else{
            rep.setNoticeStatus(0);
        }
        return rep;
    }

    @ApiOperation("拓扑端口事件:可改为只是用：uuid，使用uuid查询数据库获取ip")
    @GetMapping("/interface/event")
    public Object interfaceEvent(@RequestParam(value = "requestParams") String requestParams){
        List result = new ArrayList();
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        if(requestParams != null && !requestParams.equals("")){
            Map param = JSONObject.parseObject(String.valueOf(requestParams), Map.class);
            String sessionId = (String) param.get("sessionId");
            List<Object> list = JSONObject.parseObject(String.valueOf(param.get("params")), List.class);
            List<String> events = new ArrayList<>();
            events.add("tempexceed");
            events.add("cpuexceed");
            events.add("memexceed");
            for (Object requestParam : list){
                String[] str = requestParam.toString().split("&");
                // 获取端口事件状态
                Map params = new HashMap();
                params.clear();
                params.put("ip", str[0]);
                params.put("events", events);
                params.put("event", "is not null");
                params.put("objectid", "is not null");
                List<Problem> problemList = this.problemService.selectObjByMap(params);
                if(problemList.size() > 0){
                    Map event = new HashMap();
                    for(Problem problem : problemList){
                        event.put(problem.getEvent(), problem.getStatus());
                        event.put("uuid", str[1]);
                        result.add(event);
                    }
                }
            }
            rep.setNoticeStatus(1);
            rep.setNoticeType("7");
            rep.setNoticeInfo(result);
//            this.redisResponseUtils.syncRedis(sessionId, result, 7);

            this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(result), 7);
            return rep;
        }
        rep.setNoticeType("7");
        rep.setNoticeStatus(0);
        return rep;
    }

    @ApiOperation("告警信息(分页查询)")
    @GetMapping("/all")
    public NoticeWebsocketResp problempAll(
            @RequestParam(value = "requestParams", required = false) String requestParams) throws ClientAbortException {
        Map<String, Object> requestParam = JSONObject.parseObject(requestParams, Map.class);
        String sessionId = (String) requestParam.get("sessionId");
        NspmProblemDTO dto = new NspmProblemDTO();
        Map<String, Object> param = JSONObject.parseObject(String.valueOf(requestParam.get("params")), Map.class);
        dto.setCurrentPage(Integer.parseInt(param.get("currentPage").toString()));
        dto.setPageSize(Integer.parseInt(param.get("pageSize").toString()));
        dto.setAffirm(param.get("affirm") ==  null || param.get("affirm").equals("") ? null : Integer.parseInt(param.get("affirm").toString()));
        Page<Problem> page = this.problemService.selectConditionQuery(dto);
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        rep.setNoticeType("8");
        rep.setNoticeStatus(1);
        rep.setNoticeInfo(new PageInfo<Problem>(page));
//        this.redisResponseUtils.syncRedis(sessionId, new PageInfoMMMM<Problem>(page), 8);
        this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(new PageInfo<Problem>(page)), 7);
//        if(true){// 清除数据，并抛出异常；
//            throw new ClientAbortException("a");
//        }
        return rep;
    }

}
