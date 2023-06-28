package com.metoo.nspm.entity.nspm;

import com.metoo.nspm.core.domain.IdEntity;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@ApiModel("设备类型")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceType extends IdEntity {

    private String name;
    private Integer count;
    private Integer onlineCount;
    private Integer online;
    private Integer type;
    private List<NetworkElement> networkElementList = new ArrayList<>();
    private List<Terminal> terminalList = new ArrayList<>();

    private Integer sequence;
    private Integer diff;

    private String uuid;// 可用作自定义图片名称
}
