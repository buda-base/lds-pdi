<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<style>
table {
    border-collapse: collapse;
    border-spacing: 0;
    margin-left: 20px;    
    border: 1px solid #ddd;
}

td {
    text-align: left;
    vertical-align:top;
    padding: 16px;
}

th {
    padding-top: 12px;
    padding-bottom: 12px;
    text-align: center;
    background-color: #4e7F50;
    color: white;
}

tr:nth-child(even) {
    background-color: #f2f2f2
}
input[type=text], select {
    width: 100%;
    padding: 12px 20px;
    margin: 8px 0;
    display: inline-block;
    border: 1px solid #ccc;
    border-radius: 4px;
    box-sizing: border-box;
}

input[type=submit] {
    width: 80%;
    background-color: #4CAF50;
    color: white;
    padding: 14px 20px;
    margin: 8px 0;
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

input[type=submit]:hover {
    background-color: #45a049;
}

</style>
<title>Cache and memory monitoring</title>
</head>
<body>
<h2>Cache and memory monitoring</h2>
<h3>CACHE INFO/STATS</h3>
<table style="width: 60%;">
<tr><th colspan="2">Cache status and statistics</th></tr>
<tr><td><b>Cache status</b></td><td>${model.getStatus()}</td><td></td></tr>
<tr><td><b>Created Time</b></td><td>${model.getLastAccessTime()}</td><td>Cache region creation Time</td></tr>
<tr><td><b>Last Accessed Time</b></td><td>${model.getCreateTime()}</td><td>last time the cache was used</td></tr>
<tr><td><b>Objects</b></td><td>${model.getSize()}</td><td>The total number of objects held by the cache</td></tr>
<tr><td><b>Miss expired</b></td><td>${model.getMissCountExpired()}</td><td>Number of times a requested element was found but was expired</td></tr>
<tr><td><b>Miss not found</b></td><td>${model.getMissCountNotFound()}</td><td>Number of times a requested element was not found</td></tr>
<tr><td><b>Found aux</b></td><td>${model.getHitCountAux()}</td><td>Number of times a requested item was found in an auxiliary cache</td></tr>
<tr><td><b>Found ram</b></td><td>${model.getHitCountRam()}</td><td>Number of times a requested item was found in the memory cache</td></tr>
</table>
<br>
<h3>MEMORY WATCH</h3>
<table style="width: 40%;">
<tr><th colspan="2">Heap memory usage</th><th colspan="2">Non-Heap memory usage</th></tr>
<tr><td style="text-align: right;">Committed</td><td style="text-align: right;">${model.getHeapCommitted()}</td>
<td style="text-align: right;">Committed</td><td style="text-align: right;">${model.getNonHeapCommitted()}</td></tr>
<tr><td style="text-align: right;">Init. requested</td><td style="text-align: right;">${model.getHeapInit()}</td>
<td style="text-align: right;">Init. requested</td><td style="text-align: right;">${model.getNonHeapInit()}</td></tr>
<tr><td style="text-align: right;">Max</td><td style="text-align: right;">${model.getHeapMax()}</td>
<td style="text-align: right;">Max</td><td style="text-align: right;">${model.getNonHeapMax()}</td></tr>
<tr><td style="text-align: right;">Used</td><td style="text-align: right;">${model.getHeapUsed()}</td>
<td style="text-align: right;">Used</td><td style="text-align: right;">${model.getNonHeapUsed()}</td></tr>
</table>
<br>
<h3>MEMORY POOL DETAILS</h3>
<table style="width: 60%;">
<tr><th>Memory Pool</th><th>Init</th><th>Committed</th><th>Max</th><th>Used</th></tr>
<tr>
<td>Code Cache</td>
<td>${model.format(model.getCodeMemoryUsage().getInit())}</td>
<td>${model.format(model.getCodeMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getCodeMemoryUsage().getMax())}</td>
<td>${model.format(model.getCodeMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>Meta Space</td>
<td>${model.format(model.getMetaMemoryUsage().getInit())}</td>
<td>${model.format(model.getMetaMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getMetaMemoryUsage().getMax())}</td>
<td>${model.format(model.getMetaMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>Compressed Class Space</td>
<td>${model.format(model.getCompressedMemoryUsage().getInit())}</td>
<td>${model.format(model.getCompressedMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getCompressedMemoryUsage().getMax())}</td>
<td>${model.format(model.getCompressedMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>PS Eden Space</td>
<td>${model.format(model.getEdenMemoryUsage().getInit())}</td>
<td>${model.format(model.getEdenMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getEdenMemoryUsage().getMax())}</td>
<td>${model.format(model.getEdenMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>PS Survivor Space</td>
<td>${model.format(model.getSurvivorMemoryUsage().getInit())}</td>
<td>${model.format(model.getSurvivorMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getSurvivorMemoryUsage().getMax())}</td>
<td>${model.format(model.getSurvivorMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>PS Old Gen</td>
<td>${model.format(model.getOldMemoryUsage().getInit())}</td>
<td>${model.format(model.getOldMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getOldMemoryUsage().getMax())}</td>
<td>${model.format(model.getOldMemoryUsage().getUsed())}</td>
</tr>
</table>

<br>
<h3>CACHE</h3>
<table style="width: 60%;">
<tr><th colspan="2">Cache configuration parameters</th></tr>
<tr><td><b>CacheName</b></td><td>${model.getCacheName()}</td><td>The name of the cache</td></tr>
<tr><td><b>MemoryCacheName</b></td><td>${model.getMemoryCacheName()}</td><td>The memoryCacheName attribute of the ICompositeCacheAttributes object</td></tr>
<tr><td><b>UseMemoryShrinker</b></td><td>${model.isUseMemoryShrinker()}</td><td>Whether the memory cache should perform background memory shrinkage.</td></tr>
<tr><td><b>UseRemote</b></td><td>${model.isUseRemote()}</td><td>Whether the cache is remote enabled</td></tr>
<tr><td><b>UseDisk</b></td><td>${model.isUseDisk()}</td><td>The useDisk attribute of the ICompositeCacheAttributes object</td></tr>
<tr><td><b>UseLateral</b></td><td>${model.isUseLateral()}</td><td>The useLateral attribute of the ICompositeCacheAttributes object</td></tr>
<tr><td><b>MaxMemoryIdleTimeSeconds</b></td><td>${model.getMaxMemoryIdleTimeSeconds()}</td><td>This is only used if you are using the memory shrinker. If this value is set above -1, then if an item has not been accessed in this number of seconds, it will be spooled to disk if the disk is available. You can register an event handler on this event.</td></tr>
<tr><td><b>MaxLife</b></td><td>${model.getMaxLife()}</td><td>The Max life duration of an object, in seconds</td></tr>
<tr><td><b>ShrinkerIntervalSeconds</b></td><td>${model.getShrinkerIntervalSeconds()}</td><td>If UseMemoryShrinker is true the memory cache should auto-expire elements to reclaim space. This gets the shrinker interval.</td></tr>
<tr><td><b>MaxObjects</b></td><td>${model.getMaxObjects()}</td><td>The maximum number of items allowed in memory. Eviction of elements in excess of this number is determined by the memory cache. By default JCS uses the LRU memory cache.</td></tr>
<tr><td><b>DiskUsagePatternName</b></td><td>${model.getDiskUsagePattern()}</td><td>SWAP is the default. Under the swap pattern, data is only put to disk when the max memory size is reached. Since items puled from disk are put into memory, if the memory cache is full and you get an item off disk, the lest recently used item will be spooled to disk. If you have a low memory hit ration, you end up thrashing. The UPDATE usage pattern allows items to go to disk on an update. It disables the swap. This allows you to persist all items to disk. If you are using the JDBC disk cache for instance, you can put all the items on disk while using the memory cache for performance, and not worry about losing data from a system crash or improper shutdown. Also, since all items are on disk, there is no need to swap to disk. This prevents the possibility of thrashing.</td></tr>
<tr><td><b>MaxSpoolPerRun</b></td><td>${model.getMaxSpoolPerRun()}</td><td>If UseMemoryShrinker is true the memory cache should auto-expire elements to reclaim space. This is the maximum number of items to spool per run.</td></tr>
</table>

</body>
</html>