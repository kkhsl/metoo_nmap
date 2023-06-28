package com.metoo.nspm.core.utils;

public class Global {

    private static Global global = new Global();

    public final static  String BOARDCPU = "boardcpu";
    public static final String BOARDMEM = "boardmem";
    public static final String BOARDTEMP = "boardtemp";

    public static final String TOPOLOGYFILEPATH = "/opt/nmap/resource";// 拓扑自定义分区文件存储路径

    public static final String DBPATH = "/opt/nmap/resource/db";
    public static final String DBNAME = "nmap";
    public static final String DBPATHLOCAL = "C:\\Users\\Administrator\\Desktop\\backup\\dblocal";


//    public static final String WEBTERMINALPATH = "C:\\Users\\Administrator\\Desktop\\backup\\dblocal";
    public static final String WEBTERMINALPATH = "/opt/nmap/service/www/deviceImg/dt";
//    public static final String DBPATH = "C:\\Users\\Administrator\\Desktop\\backup\\db";


//    public static final String TOPOLOGYFILEPATH = "C:\\Users\\Administrator\\Desktop\\新建文件夹 (3)";

    public Global() {
    }

    public static Global getInstance() {
        return global;
    }
}
