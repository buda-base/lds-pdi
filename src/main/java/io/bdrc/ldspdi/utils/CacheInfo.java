package io.bdrc.ldspdi.utils;

import org.apache.commons.jcs.admin.CacheElementInfo;
import org.apache.commons.jcs.admin.CacheRegionInfo;
import org.apache.commons.jcs.admin.JCSAdminBean;

import io.bdrc.ldspdi.sparql.results.ResultsCache;

public class CacheInfo {
    
    //public JCSAdminBean admin = new JCSAdminBean();
    
    public static void main(String[] args) throws Exception{
        JCSAdminBean admin = ResultsCache.admin;
        CacheRegionInfo[] infos = admin.buildCacheInfo();
        System.out.println("Cache infos: " + infos.length);
        CacheElementInfo[] eles = admin.buildElementInfo("bdrc");
        System.out.println("Cache eles: " + eles.length);
        for(CacheRegionInfo info: infos) {
            System.out.println("Cache Name: " + info.getCacheName());
            System.out.println("Cache Size: " + info.getCacheSize());
            System.out.println("Cache bytes count: " + info.getByteCount());
            System.out.println("Cache Stats: " + info.getCacheStatistics());
            System.out.println("Cache Status: " + info.getCacheStatus());
            System.out.println("Cache Count Aux: " + info.getHitCountAux());
            System.out.println("Cache Count Ram: " + info.getHitCountRam());
            System.out.println("Cache Count Expired: " + info.getMissCountExpired());
            System.out.println("Cache Count Not found: " + info.getMissCountNotFound());
            System.out.println("Cache to string: " + info.toString());
        }
    }

}
