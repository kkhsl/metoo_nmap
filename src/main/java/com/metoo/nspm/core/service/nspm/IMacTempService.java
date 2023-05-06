package com.metoo.nspm.core.service.nspm;

import com.metoo.nspm.entity.nspm.MacTemp;

import java.util.List;
import java.util.Map;

public interface IMacTempService {

    List<MacTemp> selectTagByMap(Map params);

    List<MacTemp> selectByMap(Map params);

    MacTemp getObjByInterfaceName(String interfaceName);

    List<MacTemp> groupByObjByMap(Map params);

    List<MacTemp> groupByObjByMap2(Map params);

    List<MacTemp> getMacUS(Map params);

    List<MacTemp> macJoinArp(Map params);

    MacTemp selectByMac(String mac);

    List<MacTemp> directTerminal(Map params);

    int save(MacTemp instance);

    int update(MacTemp instance);

    int batchInsert(List<MacTemp> macTemps);

    int batchUpdate(List<MacTemp> macTemps);

    void truncateTable();
}
