package com.metoo.nspm.entity.nspm;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.DoubleSerializer;
import com.metoo.nspm.core.config.annotation.excel.ExcelExport;
import com.metoo.nspm.core.config.annotation.excel.ExcelImport;
import com.metoo.nspm.core.domain.IdEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Api("Rsms 设备")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RsmsDevice extends IdEntity {

    @ExcelExport(value = "设备名称", sort = 1)
    @ExcelImport(value = "设备名称", required = true, unique = true)
    @ApiModelProperty("名称")
    private String name;

    @ExcelExport(value = "IP地址", sort = 4)
    @ExcelImport("IP地址")
    @ApiModelProperty("ip(可以为空，不为空校验唯一性)")
    private String ip;

    @ApiModelProperty("设备类型")
    private Long deviceTypeId;

    @ExcelImport("设备类型")
    @ExcelExport(value = "设备类型名称", sort = 5)
    @ApiModelProperty("设备类型名称")
    private String deviceTypeName;


    @ApiModelProperty("厂商ID")
    private Long vendorId;

    @ExcelExport(value = "品牌", sort = 8)
    @ExcelImport("品牌")
    @ApiModelProperty("厂商名称")
    private String vendorName;

    @ApiModelProperty("分组ID")
    private Long groupId;

    @ExcelExport(value = "分组", sort = 6)
    @ExcelImport("分组")
    @ApiModelProperty("分组名称")
    private String groupName;

    @ApiModelProperty("机房")
    private Long plantRoomId;

    @ExcelExport(value = "机房", sort = 10)
    @ExcelImport("机房")
    @ApiModelProperty("机房名称")
    private String plantRoomName;

    @ApiModelProperty("用户")
    private Long userId;

    @ApiModelProperty("机柜")
    private Long rackId;

    @ExcelExport(value= "机柜", sort = 11)
    @ExcelImport("机柜")
    @ApiModelProperty("机柜名称")
    private String rackName;

    @ApiModelProperty("是否显示背面")
    private boolean rear;

    @ExcelExport(value = "开始位置", sort = 12)
    @ExcelImport("开始位置")
    @ApiModelProperty("开始位置")
    private Integer start;

    @ExcelExport(value = "大小", sort = 13)
    @ExcelImport("大小")
    @ApiModelProperty("大小")
    private Integer size;

    @ExcelExport(value = "描述", sort = 20)
    @ApiModelProperty("描述")
    private String description;

    @ExcelExport(value = "资产编号", sort = 18)
    @ExcelImport("资产编号")
    @ApiModelProperty("资产编号")
    private String asset_number;

    @ExcelExport(value = "主机名", sort = 2)
    @ExcelImport("主机名")
    @ApiModelProperty("主机名")
    private String host_name;

    @ExcelExport(value = "状态", sort = 3, kv = "true-在线;false-离线")
    @ExcelImport("状态")
    @ApiModelProperty("状态 0：离线 1：在线")
    private Boolean online;

    @ExcelExport(value = "型号", sort = 9)
    @ExcelImport("型号")
    @ApiModelProperty("型号")
    private String model;

    @ExcelExport(value = "采购时间", sort = 14)
    @ExcelImport("采购时间")
    @ApiModelProperty("采购时间")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date purchase_time;

    @ExcelExport(value = "过保时间", sort = 15)
    @ExcelImport("过保时间")
    @ApiModelProperty("过保时间")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date warranty_time;

    @ExcelExport(value = "价格", sort = 16)
    @JSONField(name="price",serializeUsing = DoubleSerializer.class)
    @ApiModelProperty("价格")
    private Double price;

    @ExcelExport(value = "序列号", sort = 17)
    @ApiModelProperty("序列号")
    private String serial_number;

    @ExcelExport(value = "责任人", sort = 19)
    @ExcelImport("责任人")
    @ApiModelProperty("责任人")
    private String duty;

    @ExcelExport(value = "变更原因", sort = 21)
    @ApiModelProperty(value = "变更原因")
    private String changeReasons;
    private String uuid;

    @ApiModelProperty("项目Id")
    private Long projectId;

    @ExcelExport(value= "项目", sort = 7)
    @ExcelImport("项目")
    @ApiModelProperty("项目名")
    private String projectName;

    private Integer all;// 默认  1：全部

    private List<Long> ids;
    private String excelPath;
    private String excelName;


    private Integer rowNum;
    private String rowData;
    private String rowTips;

    // Mac地址 位置、部门
    @ExcelExport(value = "mac地址", sort = 22)
    @ExcelImport("mac地址")
    @ApiModelProperty("终端Mac地址")
    private String mac;
    @ExcelExport(value = "位置", sort = 23)
    @ExcelImport("位置")
    @ApiModelProperty("设备位置：摄像头等")
    private String location;
    @ApiModelProperty("部门Id")
    private Long departmentId;
    @ExcelExport(value = "部门", sort = 24)
    @ExcelImport("部门")
    @ApiModelProperty("部门名称")
    private String departmentName;

}
