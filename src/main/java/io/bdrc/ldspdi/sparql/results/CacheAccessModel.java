package io.bdrc.ldspdi.sparql.results;

import java.text.SimpleDateFormat;

import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;

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
    
    
}