package com.metoo.nspm.entity.nspm;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.DoubleSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.metoo.nspm.core.config.annotation.excel.ExcelExport;
import com.metoo.nspm.core.config.annotation.excel.ExcelImport;
import com.metoo.nspm.core.domain.IdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@ApiModel("拓扑终端")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Terminal extends IdEntity {

    private String name;
    @ApiModelProperty("设备Ip")
    private String deviceIp;
    @ApiModelProperty("设备名称")
    private String deviceName;
    @ApiModelProperty("设备类型")
    @Deprecated
    private String deviceType;
    @ApiModelProperty("接口名称")
    private String interfaceName;
    @ApiModelProperty("Mac地址")
    private String mac;
    @ApiModelProperty("Mac标记")
    private String tag;
    @ApiModelProperty("Mac索引")
    private String index;
    @ApiModelProperty("索引")
    private String uuid;
    @ApiModelProperty("类型 0: 普通终端 1：资产终端")
    private Integer type;
    @ApiModelProperty("接口索引")
    private String interfaceIndex;

    @ApiModelProperty("端口状态")
    private Integer interfaceStatus;
    @ApiModelProperty("终端ip")
    private String ip;
    @ApiModelProperty("接口ip")
    private String ipAddress;
    @ApiModelProperty("对端设备名称")
    private String remoteDevice;
    @ApiModelProperty("对端接口名称")
    private String remoteInterface;
    @ApiModelProperty("对端设备Ip")
    private String remoteDeviceIp;
    @ApiModelProperty("对端设备类型")
    private String remoteDeviceType;
    private String remoteUuid;
    private String vendor;
    private String vlan;
    @ApiModelProperty("是否在线")
    private Boolean online;
    private Long terminalTypeId;
    private String terminalTypeName;

    @ApiModelProperty("部门Id")
    private Long departmentId;
    @ApiModelProperty("部门名称")
    private String departmentName;
    @ApiModelProperty("责任人")
    private String duty;
    @ApiModelProperty("设备位置：摄像头等")
    private String location;


    // 终端类型改用设备类型
    @ApiModelProperty("设备类型")
    private Long deviceTypeId;
    @ApiModelProperty("设备类型名称")
    private String deviceTypeName;
    @ApiModelProperty("设备类型Uuid")
    private String deviceTypeUuid;

    // 增加属性
    @ApiModelProperty("主机名")
    private String host_name;

    @ApiModelProperty("采购时间")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date purchase_time;

    @ApiModelProperty("过保时间")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date warranty_time;

    @JSONField(name="price",serializeUsing = DoubleSerializer.class)
    @ApiModelProperty("价格")
    private Double price;

    @ApiModelProperty("序列号")
    private String serial_number;

    @ApiModelProperty("资产编号")
    private String asset_number;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty(value = "变更原因")
    private String changeReasons;

    @ApiModelProperty("来源 0：采集 1：手动录入 2:  3:HUB-terminal（已手动修改终端，采集不在更新设备）")
    private Integer from;

    @ApiModelProperty("项目Id")
    private Long projectId;
    @ApiModelProperty("项目名")
    private String projectName;
    @ApiModelProperty("厂商ID")
    private Long vendorId;
    @ApiModelProperty("厂商名称")
    private String vendorName;
    @ApiModelProperty("型号")
    private String model;


}
