package com.metoo.nspm.core.manager.rsms;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.metoo.nspm.core.service.nspm.IDeviceTypeService;
import com.metoo.nspm.core.utils.Global;
import com.metoo.nspm.core.utils.ResponseUtil;
import com.metoo.nspm.core.utils.file.UploadFileUtil;
import com.metoo.nspm.dto.DeviceTypeDTO;
import com.metoo.nspm.entity.nspm.DeviceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.nutz.lang.random.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api("设备类型")
@RequestMapping("/admin/device/type")
@RestController
public class RsmsDeviceTypeManagerController {

    @Autowired
    private IDeviceTypeService deviceTypeService;

    @GetMapping("")
    public Object getAll(){
        Map params = new HashMap();
        params.put("diff", 0);
        List<DeviceType> deviceTypes = this.deviceTypeService.selectObjByMap(params);
        return ResponseUtil.ok(deviceTypes);
    }

    @GetMapping("/count")
    public Object getCount(){
        List<DeviceType> deviceTypes = this.deviceTypeService.selectDeviceTypeAndNeByJoin();
        return ResponseUtil.ok(deviceTypes);
    }

    @GetMapping("/terminal/count")
    public Object terminalCount(){
        List<DeviceType> deviceTypes = this.deviceTypeService.selectDeviceTypeAndTerminalByJoin();
        return ResponseUtil.ok(deviceTypes);
    }

    @ApiOperation("列表")
    @PostMapping("/list")
    public Object list(@RequestBody(required = true) DeviceTypeDTO dto){
       Page<DeviceType> page = this.deviceTypeService.selectConditionQuery(dto);
       if(page.getResult().size() > 0){
           return ResponseUtil.ok(new PageInfo<>(page));
       }
        return ResponseUtil.ok();
    }


    @Autowired
    private UploadFileUtil uploadFileUtil;

    @ApiOperation("添加")
    @PostMapping("/save")
    public Object save(DeviceType instance,
                       @RequestParam(value = "onlineFile", required = false) MultipartFile onlineFile,
                       @RequestParam(value = "offlineFile", required = false) MultipartFile offlineFile){
        // 检查页面属性是否改变
        if(instance.getId() != null && !instance.getId().equals("")){
            DeviceType deviceType = this.deviceTypeService.selectObjById(instance.getId());
            if(deviceType == null){
                return ResponseUtil.badArgument("设备类型不存在");
            }
        }
        if(StringUtils.isEmpty(instance.getName())){
            return ResponseUtil.badArgument("名称不能为空");
        }else{
            Map params = new HashMap();
            params.put("name", instance.getName());
            params.put("deviceTypeId", instance.getId());
            params.put("diff", instance.getDiff());
            List<DeviceType> deviceTypes = this.deviceTypeService.selectObjByMap(params);
            if(deviceTypes.size() > 0){
                return ResponseUtil.badArgument("名称重复");
            }
        }
        return this.deviceTypeService.saveAndUpload(instance, onlineFile, offlineFile);
//        String uuid = this.deviceTypeService.saveAndUpload(instance, onlineFile, offlineFile);
//        if(uuid != null){
//            try {
//                uploadFileUtil.uploadFile(onlineFile, uuid, Global.WEBTERMINALPATH);
//                try {
//                    uploadFileUtil.uploadFile(offlineFile, uuid+0, Global.WEBTERMINALPATH);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    return ResponseUtil.badArgument("离线图片上传失败");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                return ResponseUtil.badArgument("在线图片上传失败");
//            }
//        }
//        return ResponseUtil.ok("");
    }

    @ApiOperation("添加")
    @PostMapping("/save1")
    public Object save1(@RequestBody DeviceType instance){
        // 检查页面属性是否改变
        if(instance.getId() != null && !instance.getId().equals("")){
            DeviceType deviceType = this.deviceTypeService.selectObjById(instance.getId());
            if(deviceType == null){
                return ResponseUtil.badArgument("设备类型不存在");
            }
        }
        if(StringUtils.isEmpty(instance.getName())){
            return ResponseUtil.badArgument("名称不能为空");
        }else{
            Map params = new HashMap();
            params.put("name", instance.getName());
            params.put("deviceTypeId", instance.getId());
            List<DeviceType> deviceTypes = this.deviceTypeService.selectObjByMap(params);
            if(deviceTypes.size() > 0){
                return ResponseUtil.badArgument("名称重复");
            }
        }
        return this.deviceTypeService.save(instance);
    }

    @ApiOperation("删除")
    @DeleteMapping("/delete")
    public Object del(@RequestParam(value = "id", required = false) Long id, @RequestParam(value = "ids", required = false) Long[] ids){
        if(ids != null && ids.length > 0){
            int i = this.deviceTypeService.batcheDel(ids);
            if(i >= 1){
                return ResponseUtil.ok();
            }else{
                return ResponseUtil.error();
            }
        }else  if(id != null && !id.equals("")){
            DeviceType deviceType = this.deviceTypeService.selectObjById(id);
            if(deviceType != null){
                if(deviceType.getType() != null && deviceType.getType() == 14){
                    return ResponseUtil.badArgument("默认终端类型，不允许删除");
                }
                int i = this.deviceTypeService.delete(id);
                if(i >= 1){
                    return ResponseUtil.ok();
                }else{
                    return ResponseUtil.error();
                }
            }
        }
        return ResponseUtil.badArgument();
    }

}
