package com.metoo.nspm.core.manager.admin.action;

import com.metoo.nspm.core.service.nspm.IDeviceTypeService;
import com.metoo.nspm.core.service.nspm.ITerminalTypeService;
import com.metoo.nspm.core.utils.ResponseUtil;
import com.metoo.nspm.entity.nspm.DeviceType;
import com.metoo.nspm.entity.nspm.TerminalType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/terminal/type")
public class TerminalTypeManagerController {

    @Autowired
    private ITerminalTypeService terminalTypeService;
    @Autowired
    private IDeviceTypeService deviceTypeService;

//    @GetMapping
//    public Object all(){
//        List<TerminalType> list = this.terminalTypeService.selectObjAll();
//        return ResponseUtil.ok(list);
//    }

    @GetMapping
    public Object terminal(){
        Map params = new HashMap();
        params.put("diff", 1);
        params.put("orderBy", "sequence");
        params.put("orderType", "DESC");
        List<DeviceType> deviceTypes = this.deviceTypeService.selectObjByMap(params);
        return ResponseUtil.ok(deviceTypes);
    }

}
