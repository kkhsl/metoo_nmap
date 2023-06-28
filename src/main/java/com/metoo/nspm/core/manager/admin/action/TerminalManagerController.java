package com.metoo.nspm.core.manager.admin.action;

import com.github.pagehelper.Page;
import com.metoo.nspm.core.config.website.Properties;
import com.metoo.nspm.core.manager.admin.tools.*;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.utils.ResponseUtil;
import com.metoo.nspm.core.utils.file.DownLoadFileUtil;
import com.metoo.nspm.core.utils.network.IpUtil;
import com.metoo.nspm.core.utils.query.PageInfo;
import com.metoo.nspm.dto.TerminalDTO;
import com.metoo.nspm.entity.nspm.*;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.function.Supplier;

@RestController
@RequestMapping("/admin/terminal")
public class TerminalManagerController {

    @Autowired
    private ITerminalService terminalService;
    @Autowired
    private GroupTools groupTools;
    @Autowired
    private IGroupService groupService;
    @Autowired
    private IDeviceTypeService deviceTypeService;
    @Autowired
    private IVendorService vendorService;
    @Autowired
    private IProjectService projectService;
    @Autowired
    private INetworkElementService networkElementService;

    public static void main(String[] args) {

//        Supplier<Map<String, Integer>> supplier = () -> new HashMap<String, Integer>();
//        Map<String, Integer> map = supplier.get();
//
//        String vendor = "a";
//        String model = "b";
//        if(model.equals("1") && (vendor.equals("a") || vendor.equals("c")) ){
//            System.out.println(1);
//        }else{
//            System.out.println(2);
//        }
//
//        boolean a = true || false && false;
//        System.out.println(a);
//
//        boolean b = false || true && false;
//        System.out.println(b);
//
//        boolean c = false && false || true;
//        System.out.println(c);
//
//        Map tag = new HashMap();
//        tag.put("model","S1720");
//        tag.put("vendor","HUaAWEI");
//        if((tag.get("model").equals("S5735") || tag.get("model").equals("S1720"))&& tag.get("vendor").equals("HUAWEI")){
//            System.out.println(3);
//        }else{
//            System.out.println(4);
//        }
//
//        for (int i = 0; i < 10; i++) {
//            System.out.print("");
//        }

        String aa = "Port0";
        System.out.println(aa.replace("Port", ""));
    }

//    @GetMapping("/{id}")
//    public Object edit(@PathVariable(value = "id") String id){
//        if(id != null && !id.equals("")){
//            Terminal terminal = this.terminalService.selectObjById(Long.parseLong(id));
//            if(terminal != null){
//                Map map = new HashMap();
//                User user = ShiroUserHolder.currentUser();
//                Group parent = this.groupService.selectObjById(user.getGroupId());
//                List<Group> groupList = new ArrayList<>();
//                if(parent != null){
//                    this.groupTools.genericGroup(parent);
//                    groupList.add(parent);
//                }
//                map.put("department", groupList);
//
//                // 查询设备信息
////                Map deviceInfo = this.rsmsDeviceUtils.getDeviceInfo(terminal.getIp());
////                map.put("deviceInfo", deviceInfo);
//
////                List<TerminalType> terminalTypeList = this.terminalTypeService.selectObjAll();
////                map.put("terminalType", terminalTypeList);
//
//                // 设备类型
//                List<DeviceType> deviceTypeList = this.deviceTypeService.selectObjByMap(null);
//                map.put("deviceTypeList", deviceTypeList);
//                if(terminal.getDeviceTypeId() != null && !terminal.getDeviceTypeId().equals("")){
//                    DeviceType deviceType = this.deviceTypeService.selectObjById(terminal.getDeviceTypeId());
//                    terminal.setDeviceTypeName(deviceType.getName());
//                }
//                map.put("terminal", terminal);
//                return ResponseUtil.ok(map);
//            }
//        }
//        return ResponseUtil.ok();
//    }

    @PostMapping("/list")
    public Object list(@RequestBody TerminalDTO dto){
        if(dto.getStart_purchase_time() != null && dto.getEnd_purchase_time() != null){
            if(dto.getStart_purchase_time().after(dto.getEnd_purchase_time())){
                return ResponseUtil.badArgument("起始时间需要小于结束时间");
            }
        }
        if(dto.getStart_warranty_time() != null && dto.getEnd_warranty_time() != null){
            if(dto.getStart_warranty_time().after(dto.getEnd_warranty_time())){
                return ResponseUtil.badArgument("起始时间需要小于结束时间");
            }
        }

        if(dto.getDepartmentId() != null){
            Group group = this.groupService.selectObjById(dto.getDepartmentId());
            if(group != null){
                Set<Long> ids = this.groupTools.genericGroupId(group.getId());
                dto.setDepartmentIds(ids);
            }
        }
        dto.setOrderBy("online");
        dto.setOrderType("DESC");
        Page<Terminal> page = this.terminalService.selectConditionQuery(dto);
        if(page.size() > 0){
            page.getResult().stream().forEach(e ->{
                if(e.getDeviceTypeId() != null && !e.getDeviceTypeId().equals("")){
                    DeviceType deviceType = this.deviceTypeService.selectObjById(e.getDeviceTypeId());
                    if(deviceType != null){
                        e.setDeviceTypeName(deviceType.getName());
                    }
                }
                if(e.getDepartmentId() != null && !e.getDepartmentId().equals("")){
                    Group group = this.groupService.selectObjById(e.getDepartmentId());
                    if(group != null){
                        e.setDepartmentName(group.getBranchName());
                    }
                }

                if(e.getVendorId() != null && !e.getVendorId().equals("")){
                    Vendor vendor = this.vendorService.selectObjById(e.getVendorId());
                    if(vendor != null){
                        e.setVendorName(vendor.getName());
                    }
                }

                if(e.getProjectId() != null && !e.getProjectId().equals("")){
                    Project project = this.projectService.selectObjById(e.getProjectId());
                    if(project != null){
                        e.setProjectName(project.getName());
                    }
                }
//                if(e.getDeviceTypeId() != null){
//                    DeviceType deviceType = this.deviceTypeService.selectObjById(e.getDeviceTypeId());
//                    if(deviceType != null){
//                        e.setDeviceName(deviceType.getName());
//
//                    }
//                }
            });
        }
        Map map = new HashMap();
        // 设备类型
        Map parmas = new HashMap();
        parmas.put("diff", 1);
        parmas.put("orderBy", "sequence");
        parmas.put("orderType", "DESC");
        List<DeviceType> deviceTypeList = this.deviceTypeService.selectObjByMap(parmas);
        map.put("deviceTypeList", deviceTypeList);
        // 分组
        User user = ShiroUserHolder.currentUser();
        Group parent = this.groupService.selectObjById(user.getGroupId());
        List<Group> groupList = new ArrayList<>();
        if(parent != null){
            this.groupTools.genericGroup(parent);
            groupList.add(parent);
        }
        map.put("group", groupList);
        // 品牌
        List<Vendor> vendors = this.vendorService.selectConditionQuery(null);
        map.put("vendor", vendors);
        // 项目
        Map params = new HashMap();
        params.put("userId", user.getId());
        List<Project> projectList = this.projectService.selectObjByMap(params);
        map.put("project", projectList);
        return ResponseUtil.ok(new PageInfo<Terminal>(page, map));
    }

    @GetMapping("/add")
    public Object add(){
        // 设备类型
        Map map = new HashMap();
        User user = ShiroUserHolder.currentUser();
        Group parent = this.groupService.selectObjById(user.getGroupId());
        List<Group> groupList = new ArrayList<>();
        if(parent != null){
            this.groupTools.genericGroup(parent);
            groupList.add(parent);
        }
        map.put("department", groupList);

        Map parmas = new HashMap();
        parmas.put("diff", 1);
        parmas.put("orderBy", "sequence");
        parmas.put("orderType", "DESC");
        List<DeviceType> deviceTypeList = this.deviceTypeService.selectObjByMap(parmas);
        map.put("deviceTypeList", deviceTypeList);
        // 厂商
        List<Vendor> vendors = this.vendorService.selectConditionQuery(null);
        map.put("vendor", vendors);
        // 项目
        Map params = new HashMap();
        params.put("userId", user.getId());
        List<Project> projectList = this.projectService.selectObjByMap(params);
        map.put("project", projectList);

        return ResponseUtil.ok(map);
    }

    @GetMapping("/update/{id}")
    public Object update(@PathVariable Long id){
        if(id == null){
            return  ResponseUtil.badArgument();
        }
        Terminal terminal = this.terminalService.selectObjById(id);
        if(terminal == null){
            return  ResponseUtil.badArgument();
        }else{
            if(terminal.getDepartmentId() != null){
                Group department = this.groupService.selectObjById(terminal.getDepartmentId());
                if(department != null){
                    terminal.setDepartmentName(department.getBranchName());
                }
            }
            // 设备类型
            if(terminal.getDeviceTypeId() != null){
                DeviceType deviceType = this.deviceTypeService.selectObjById(terminal.getDeviceTypeId());
                if(deviceType != null){
                   terminal.setDeviceTypeName(deviceType.getName());
                }
            }
        }
        Map map = new HashMap();
        map.put("terminal", terminal);

        User user = ShiroUserHolder.currentUser();
        Group parent = this.groupService.selectObjById(user.getGroupId());
        List<Group> groupList = new ArrayList<>();
        if(parent != null){
            this.groupTools.genericGroup(parent);
            groupList.add(parent);
        }
        map.put("department", groupList);

        // 设备类型
        Map parmas = new HashMap();
        parmas.put("diff", 1);
        parmas.put("orderBy", "sequence");
        parmas.put("orderType", "DESC");
        List<DeviceType> deviceTypeList = this.deviceTypeService.selectObjByMap(parmas);
        map.put("deviceTypeList", deviceTypeList);

        // 厂商
        List<Vendor> vendors = this.vendorService.selectConditionQuery(null);
        map.put("vendor", vendors);
        // 项目
        Map params = new HashMap();
        params.put("userId", user.getId());
        List<Project> projectList = this.projectService.selectObjByMap(params);
        map.put("project", projectList);

        return ResponseUtil.ok(map);
    }

    @GetMapping("/verify")
    public Object verifyIp(@RequestParam(value = "id", required = false) Long id,
                           @RequestParam(value = "ip", required = true) String ip){
        // 校验Ip
        if(!StringUtils.isEmpty(ip)){
            boolean flag = IpUtil.verifyIp(ip);
            if(flag){
                Map params = new HashMap();
                params.clear();
                params.put("ip", ip);
                params.put("terminalId", id);
                List<Terminal> terminals = this.terminalService.selectObjByMap(params);
                if(terminals.size() > 0){
                    return ResponseUtil.badArgument("Ip已存在");
                }return ResponseUtil.ok();
            }else{
                return ResponseUtil.badArgument("Ip格式错误");
            }
        }
        return ResponseUtil.badArgument("Ip为空");
    }

    @PostMapping("/save")
    public Object save(@RequestBody Terminal instance){
        // 验证名称是否唯一
        Map params = new HashMap();
        if(instance.getId() != null && !instance.getId().equals("")){
            Terminal terminal = this.terminalService.selectObjById(instance.getId());
            if(terminal == null){
                return ResponseUtil.badArgument("终端不存在");
            }
        }
        // 验证Ip唯一性
        if(instance.getIp() == null || instance.getIp().equals("")){
            return ResponseUtil.badArgument("请输入有效IP");
        }else{
            // 验证ip合法性
            boolean flag =  IpUtil.verifyIp(instance.getIp());
            if(!flag){
                return ResponseUtil.badArgument("ip不合法");
            }
            params.clear();
            params.put("ip", instance.getIp());
            params.put("terminalId", instance.getId());
            List<Terminal> terminals = this.terminalService.selectObjByMap(params);
            if(terminals.size() > 0){
                return ResponseUtil.badArgument("IP重复");
            }
        }
//      if(Strings.isBlank(instance.getName())){
//            params.clear();
//            params.put("name", instance.getName());
//            params.put("terminalId", instance.getId());
//            List<Terminal> Terminals = this.terminalService.selectObjByMap(params);
//            if(Terminals.size() > 0){
//                return ResponseUtil.badArgumentRepeatedName();
//            }
//        }
        // 验证资产编号唯一性
        if(instance.getAsset_number() != null && !instance.getAsset_number().isEmpty()){
            params.clear();
            params.put("asset_number", instance.getAsset_number());
            params.put("terminalId", instance.getId());
            List<Terminal> terminals = this.terminalService.selectObjByMap(params);
            if(terminals.size() > 0){
                Terminal terminal = terminals.get(0);
                return ResponseUtil.badArgument("资产编号与(" + terminal.getName() + ")设备重复");
            }
        }
        if(instance.getHost_name() != null && !instance.getHost_name().isEmpty()){
            params.clear();
            params.put("host_name", instance.getHost_name());
            params.put("terminalId", instance.getId());
            List<Terminal> terminals = this.terminalService.selectObjByMap(params);
            if(terminals.size() > 0){
                Terminal terminal = terminals.get(0);
                return ResponseUtil.badArgument("主机名与(" + terminal.getName() + ")设备重复");
            }
        }

        // 验证日期
        if(instance.getWarranty_time() != null && instance.getPurchase_time() != null){
            if(instance.getWarranty_time().before(instance.getPurchase_time())){
                return ResponseUtil.badArgument("过保时间必须大于采购时间");
            }
        }

        // 验证部门
        if(instance.getDepartmentId() != null){
            Group department = this.groupService.selectObjById(instance.getDepartmentId());
            if(department == null){
                return ResponseUtil.badArgument("请输入正确部门信息");
            }
        }

        // 验证厂商
        if(instance.getVendorId() != null && !instance.getVendorId().equals("")){
            Vendor vendor = this.vendorService.selectObjById(instance.getVendorId());
            if(vendor == null){
                return ResponseUtil.badArgument("请输入正确品牌参数");
            }
        }

        // 验证项目
        if(instance.getProjectId() != null && !instance.getProjectId().equals("")){
            Project project = this.projectService.selectObjById(instance.getProjectId());
            if(project == null){
                return ResponseUtil.badArgument("请输入正确项目参数");
            }
        }

        // 设备类型
        if(instance.getDeviceTypeId() != null){
            DeviceType deviceType = this.deviceTypeService.selectObjById(instance.getDeviceTypeId());
            if(deviceType == null){
                return ResponseUtil.badArgument("请选择设备类型");
            }else{
                if(Strings.isBlank(instance.getName())){
                    instance.setName(deviceType.getName());
                }
            }
        }
        if(instance.getUuid() != null && !instance.getUuid().equals("")){
            instance.setFrom(3);
            instance.setInterfaceStatus(1);
        }
        if(instance.getInterfaceName() != null && !instance.getInterfaceName().equals("")){
            instance.setIndex(instance.getInterfaceName().replace("Port", ""));
        }
        int i = this.terminalService.save(instance);
        if(i >= 1){
            return ResponseUtil.ok();
        }
        return ResponseUtil.error();
    }


    // 终端修改上联设备
    @PutMapping("/update")
    public Object update(@RequestParam String uuid, String id){
        NetworkElement networkElement  = this.networkElementService.selectAccessoryByUuid(uuid);
        if(networkElement != null){
            Terminal terminal = this.terminalService.selectObjById(Long.parseLong(id));
            if(terminal != null){
                terminal.setDeviceName(networkElement.getDeviceName());
                terminal.setInterfaceName(networkElement.getInterfaceName());
                terminal.setUuid(networkElement.getUuid());
                int i = this.terminalService.update(terminal);
                if(i >= 1){
                    return ResponseUtil.ok();
                }
                return ResponseUtil.error();
            }
        }
        return ResponseUtil.badArgument();
    }

    @ApiOperation("批量修改终端为资产终端")
    @PutMapping("/batch/update")
    public Object editTerminal(@RequestParam Long[] ids){
        if(ids.length > 0){
            int i = this.terminalService.editTerminalType(ids);
            if(i >= 1){
                return ResponseUtil.ok();
            }
        }
        return ResponseUtil.badArgument("请选择终端");
    }

    @DeleteMapping(value = {"/{id}","/{ids}"})
    public Object delete(Long id, String ids){
        if(Strings.isNotBlank(ids) && ids.split(",").length > 0){
            String[] idList = ids.split(",");
            for (String s : idList) {
                int i = this.terminalService.delete(Long.parseLong(s));
            }
            return ResponseUtil.ok();
        }else{
            Terminal terminal = this.terminalService.selectObjById(id);
            if(terminal != null){
                int i = this.terminalService.delete(id);
                if(i >= 1){
                    return ResponseUtil.ok();
                }else{
                    return ResponseUtil.error();
                }
            }
        }
        return ResponseUtil.badArgument();
    }

//    @PutMapping
//    public Object update(@RequestBody Terminal instance){
//        if(instance.getDeviceTypeId() != null && !instance.getDeviceTypeId().equals("")){
//            DeviceType deviceType = this.deviceTypeService.selectObjById(instance.getDeviceTypeId());
//            if(deviceType == null){
//                return ResponseUtil.badArgument();
//            }
//        }
//        int i = this.terminalService.update(instance);
//        if(i >= 1){
//            return ResponseUtil.ok();
//        }else{
//            return ResponseUtil.error("保存失败");
//        }
//    }

    @Autowired
    private Properties properties;
    @Value("${batchImportTerminalFileName}")
    private String batchImportDeviceFileName;
    @Value("${batchImportFilePath}")
    private String batchImportFilePath;

    @ApiOperation("下载模板")
    @GetMapping("/downTemp")
    public Object downTemplate(HttpServletResponse response) {
        boolean flag = DownLoadFileUtil.downloadTemplate(this.batchImportFilePath, this.batchImportDeviceFileName, response);
        if(flag){
            return ResponseUtil.ok();
        }else{
            return ResponseUtil.error();
        }
    }

}
