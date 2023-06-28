package com.metoo.nspm.core.service.nspm.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.metoo.nspm.core.manager.admin.tools.RsmsDeviceUtils;
import com.metoo.nspm.core.manager.admin.tools.ShiroUserHolder;
import com.metoo.nspm.core.mapper.nspm.TerminalMapper;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.service.zabbix.ItemService;
import com.metoo.nspm.dto.TerminalDTO;
import com.metoo.nspm.entity.nspm.*;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;
import java.util.stream.Collectors;

@Service
//@Transactional
public class TerminalServiceImpl implements ITerminalService {

    @Autowired
    private TerminalMapper terminalMapper;
    @Autowired
    private IMacService macService;
    @Autowired
    private IMacHistoryService macHistoryService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RsmsDeviceUtils rsmsDeviceUtils;
    @Autowired
    private IDeviceTypeService deviceTypeService;

    @Override
    public Terminal selectObjById(Long id) {
        return this.terminalMapper.selectObjById(id);
    }

    @Override
    public Page<Terminal> selectConditionQuery(TerminalDTO instance) {
        Page<Terminal> page = PageHelper.startPage(instance.getCurrentPage(), instance.getPageSize());
        this.terminalMapper.selectConditionQuery(instance);
        return page;
    }

    @Override
    public List<Terminal> selectObjByMap(Map params) {
        return this.terminalMapper.selectObjByMap(params);
    }

    @Override
    public int save(Terminal instance) {
        if(instance.getId() == null || instance.getId().equals("")){
            instance.setAddTime(new Date());
            instance.setFrom(1);
            instance.setTag("DT");
            instance.setUuid(UUID.randomUUID().toString());
        }else{
            if(instance.getFrom() != null && Strings.isBlank(instance.getFrom().toString())){
                instance.setFrom(1);
            }
        }
        if(instance.getId() == null || instance.getId().equals("")){
            try {
                return this.terminalMapper.insert(instance);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }else{
            try {
                return this.terminalMapper.update(instance);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    @Override
    public int update(Terminal instance) {
        try {
            int i = this.terminalMapper.update(instance);
            if(i >= 1){
                try {
                    String ip = instance.getIp();
                    if(Strings.isBlank(ip)){
                        Terminal terminal = this.terminalMapper.selectObjById(instance.getId());
                        ip = terminal.getIp();
                    }
                    this.rsmsDeviceUtils.syncUpdateDevice(ip, instance.getName(), instance.getDeviceTypeId(),
                            instance.getMac(), instance.getLocation(), instance.getDuty(), instance.getDepartmentId());
                } catch (Exception e) {
                    e.printStackTrace();
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return 0;
                }
            }
            return i;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int delete(Long id) {
        return this.terminalMapper.delete(id);
    }

    @Override
    public int batchInert(List<Terminal> instances) {
        try {
            return this.terminalMapper.batchInert(instances);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int batchUpdate(List<Terminal> instances) {
        try {
            return this.terminalMapper.batchUpdate(instances);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void syncMacDtToTerminal() {
        Map params = new HashMap();
        params.clear();
        params.put("tags", Arrays.asList("DT", "UDT"));
        List<Mac> macs = this.macService.selectByMap(params);
        if(macs.size() < 0){
            List<Terminal> terminals = this.terminalMapper.selectObjByMap(null);
            terminals = terminals.stream().map(e -> {
                        if(e.getOnline()){
                            e.setOnline(false);
                            if(e.getInterfaceName().equals("PortN") || e.getInterfaceName().contains("Port")){
                                e.setInterfaceStatus(1);
                            }
                            return e;
                        }
                        return null;
                    }
            ).collect(Collectors.toList());
            if(terminals.size() > 0){
                this.terminalMapper.batchUpdate(terminals);
            }
        }else{
            List<Long> ids = new ArrayList<>();
            macs.stream().forEach(e -> {
                // 更新端口状态
                params.clear();
                params.put("interfaceName", e.getInterfaceName());
                params.put("ip", e.getDeviceIp());
                Integer ifup = this.itemService.selectInterfaceStatus(params);
                params.clear();
                params.put("mac", e.getMac());
                List<Terminal> terminals = this.terminalMapper.selectObjByMap(params);
                if(terminals.size() > 0){
                    terminals.stream().forEach(t -> {
                        if(!t.getOnline()){
                            t.setOnline(true);
                        }
                        if(t.getInterfaceStatus() != ifup
                                && t.getInterfaceName() != null
                                && !t.getInterfaceName().equals("PortN")
                                && !t.getInterfaceName().contains("Port")){
                            t.setInterfaceStatus(ifup);
                        }
                        String[] IGNORE_ISOLATOR_PROPERTIES = new String[]{};
                        if(t.getFrom() == 3){
                            IGNORE_ISOLATOR_PROPERTIES = new String[]{
                                    "id",
                                    "online",
                                    "interfaceStatus",
                                    "deviceName",
                                    "interfaceName",
                                    "uuid"};
                        }else{
                            IGNORE_ISOLATOR_PROPERTIES = new String[]{"id", "online", "interfaceStatus", "from"};
                        }
                        BeanUtils.copyProperties(e, t, IGNORE_ISOLATOR_PROPERTIES);
                        this.terminalMapper.update(t);
                        ids.add(t.getId());
                    });
                }else{
                    Terminal terminal = new Terminal();
                    BeanUtils.copyProperties(e, terminal);
                    terminal.setOnline(true);
                    params.clear();
                    params.put("type", 14);
                    List<DeviceType> deviceTypes = this.deviceTypeService.selectObjByMap(params);
                    if(deviceTypes != null && deviceTypes.size() > 0){
                        terminal.setDeviceTypeId(deviceTypes.get(0).getId());
                        terminal.setName(deviceTypes.get(0).getName());
                    }
                    if(e.getInterfaceName() == null || e.getInterfaceName().equals("PortN") || e.getInterfaceName().contains("Port")){
                        terminal.setInterfaceStatus(1);
                        terminal.setFrom(3);
                    }else{
                        terminal.setInterfaceStatus(ifup);
                    }

                    this.terminalMapper.insert(terminal);
                    ids.add(terminal.getId());
                }
            });
            params.clear();
            params.put("notIds", ids);
            List<Terminal> terminals = this.terminalMapper.selectObjByMap(params);
            terminals = terminals.stream().map(e -> {
                        if(e.getOnline()){
                            e.setOnline(false);
                            return e;
                        }
                          return null;
                    }
            ).filter(Objects::nonNull).collect(Collectors.toList());
            if(terminals.size() > 0){
                this.terminalMapper.batchUpdate(terminals);
            }
        }
    }

    @Override
    public void syncHistoryMac(Date time) {
        Map params = new HashMap();
        params.clear();
        params.put("tags", Arrays.asList("DT", "UDT"));
        params.put("time", time);
        List<Mac> macs = this.macHistoryService.selectByMap(params);
        if(macs.size() > 0){
            macs.stream().forEach(e -> {
                params.clear();
                params.put("mac", e.getMac());
                List<Terminal> terminals = this.terminalMapper.selectObjByMap(params);
                if(terminals.size() > 0){
                    Terminal terminal = terminals.get(0);
                    if(terminal.getDeviceTypeId() != null && !terminal.getDeviceTypeId().equals("")){
                        DeviceType deviceType = this.deviceTypeService.selectObjById(terminal.getDeviceTypeId());
                        e.setDeviceTypeName(deviceType.getName());
                        e.setOnline(terminal.getOnline());
                        e.setInterfaceStatus(terminal.getInterfaceStatus());
                        this.macHistoryService.update(e);
                    }
                }
            });
        }
    }

    @Override
    public int editTerminalType(Long[] ids) {
        try {
            for (Long id : ids) {
                Terminal terminal = this.terminalMapper.selectObjById(id);
                if(terminal != null){
                    terminal.setType(1);
                    this.terminalMapper.update(terminal);
                }
            }
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            throw new ArithmeticException("算数异常");
            return 0;
        }
    }
}
