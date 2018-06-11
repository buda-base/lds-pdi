package io.bdrc.ldspdi.results;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;

import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class CacheAccessModel {
    
    public CacheAccessModel() {
        super();
    }

    /**
     * Cache config 
     */
    private ICompositeCacheAttributes getConfig() {
        return ResultsCache.CACHE.getCacheAttributes();
    }
    
    private IElementAttributes getElementConfig() {
        return ResultsCache.CACHE.getDefaultElementAttributes();
    }
    
    public String getCacheName() {
        return getConfig().getCacheName();
    }
    
    public ICompositeCacheAttributes.DiskUsagePattern getDiskUsagePattern(){
        return getConfig().getDiskUsagePattern();
    }
    
    public long getMaxLife() {
        return getElementConfig().getMaxLife();
    }
    
    public long getMaxMemoryIdleTimeSeconds() {
        return getConfig().getMaxMemoryIdleTimeSeconds();
    }
    
    public int getMaxObjects() {
        return getConfig().getMaxObjects();
    }
    
    public int getMaxSpoolPerRun() {
        return getConfig().getMaxSpoolPerRun();
    }
    
    public String getMemoryCacheName() {
        String tmp=getConfig().getMemoryCacheName();
        return tmp.substring(tmp.lastIndexOf('.')+1);
    }
    
    public long getShrinkerIntervalSeconds() {
        return getConfig().getShrinkerIntervalSeconds();
    }
    
    public boolean  isUseDisk() {
        return getConfig().isUseDisk();
    }
    
    public boolean  isUseLateral() {
        return getConfig().isUseLateral();
    }

    public boolean isUseMemoryShrinker() {
        return getConfig().isUseMemoryShrinker();
    }
    
    public boolean isUseRemote() {
        return getConfig().isUseRemote();
    }
    
    
    /**
     * Cache stats 
     */
    private IElementAttributes getRegionConfig() {
        return ResultsCache.CACHE.getDefaultElementAttributes();
    }
    
    private CompositeCache<Integer,Object> getCacheControl() {        
        return ResultsCache.CACHE.getCacheControl();        
    }
    
    public CacheStatus getStatus() {
        return getCacheControl().getStatus();
    }
    
    public String getCreateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");        
        return simpleDateFormat.format(getRegionConfig().getCreateTime());
    }
    
    public String getLastAccessTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");        
        return simpleDateFormat.format(getRegionConfig().getLastAccessTime());
    }
    
    public int getSize() {
        return getCacheControl().getSize();
    }
    
    public int getMissCountExpired() {
        return getCacheControl().getMissCountExpired();
    }
    
    public int getHitCountAux() {
        return getCacheControl().getHitCountAux();
    }
    
    public int getHitCountRam() {
        return getCacheControl().getHitCountRam();
    }
    
    public int getMissCountNotFound() {
        return getCacheControl().getMissCountNotFound();
    }
    
    /**
     * Memory stuff
     */
    
    public MemoryUsage getHeap() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }
    
    public MemoryUsage getNonHeap() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
    }
    
    private static HashMap<ObjectName,MemoryPoolMXBean> getMemoryPoolBean(){
        List<MemoryPoolMXBean> l = ManagementFactory.getMemoryPoolMXBeans();        
        HashMap<ObjectName,MemoryPoolMXBean> map=new HashMap<>();
        for(MemoryPoolMXBean mx:l) {
            map.put(mx.getObjectName(), mx);            
        }
        return map;
    }
    
    public MemoryUsage getCodeMemoryUsage() throws RestException {
        try {
            
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Code Cache")).getUsage();
        }
        catch(MalformedObjectNameException ex) {
            throw new RestException(5001,new LdsError(LdsError.GENERIC_ERR).setContext(" in CacheAccessModel.getCodeMemoryUsage()"+ex.getMessage()));
        }
    }
    
    public MemoryUsage getMetaMemoryUsage() throws RestException {
        try {
            
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Metaspace")).getUsage();
        }
        catch(MalformedObjectNameException ex) {
            throw new RestException(5001,new LdsError(LdsError.GENERIC_ERR).setContext(" in CacheAccessModel.getMetaMemoryUsage()"+ex.getMessage()));
        }
    }
    
    public MemoryUsage getCompressedMemoryUsage() throws RestException {
        try {
            
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Compressed Class Space")).getUsage();
        }
        catch(MalformedObjectNameException ex) {
            throw new RestException(5001,new LdsError(LdsError.GENERIC_ERR).setContext(" in CacheAccessModel.getCompressedMemoryUsage()"+ex.getMessage()));
        }
    }
    
    public MemoryUsage getEdenMemoryUsage() throws RestException {
        try {
            
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space")).getUsage();
        }
        catch(MalformedObjectNameException ex) {
            throw new RestException(5001,new LdsError(LdsError.GENERIC_ERR).setContext(" in CacheAccessModel.getEdenMemoryUsage()"+ex.getMessage()));
        }
    }
    
    public MemoryUsage getSurvivorMemoryUsage() throws RestException {
        try {
            
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Survivor Space")).getUsage();
        }
        catch(MalformedObjectNameException ex) {
            throw new RestException(5001,new LdsError(LdsError.GENERIC_ERR).setContext(" in CacheAccessModel.getSurvivorMemoryUsage()"+ex.getMessage()));
        }
    }
    
    public MemoryUsage getOldMemoryUsage() throws RestException {
        try {
            
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Old Gen")).getUsage();
        }
        catch(MalformedObjectNameException ex) {
            throw new RestException(5001,new LdsError(LdsError.GENERIC_ERR).setContext(" in CacheAccessModel.getOldMemoryUsage()"+ex.getMessage()));
        }
    }
    
    public int getPending() {
        return ManagementFactory.getMemoryMXBean().getObjectPendingFinalizationCount();
    }
    
    public String getHeapCommitted() {
        return format(getHeap().getCommitted());
    }
    
    public String getNonHeapCommitted() {
        return format(getNonHeap().getCommitted());
    }
    
    public String getHeapInit() {
        
        return format(getHeap().getInit());
    }
    
    public String getNonHeapInit() {
        return format(getNonHeap().getInit());
    }
    
    public String getHeapMax() {
        return format(getHeap().getMax());
    }
    
    public String getNonHeapMax() {
        return format(getNonHeap().getMax());
    }
    
    public String getHeapUsed() {
        return format(getHeap().getUsed());
    }
    
    public String getNonHeapUsed() {
        return format(getNonHeap().getUsed());
    }
    /*
     * Utilities
     */
    
    public String format(long l) {
        return new DecimalFormat("#,###,###").format(l);
    }
    
}
