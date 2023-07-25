package com.metoo.nspm.core.manager.datagear;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class GearRequest {

    private String url;
    private String method;

    private Map<String, Object> params = new HashMap();

    private String param;


}
