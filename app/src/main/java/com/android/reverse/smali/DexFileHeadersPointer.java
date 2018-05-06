package com.android.reverse.smali;

public class DexFileHeadersPointer {

    private long baseAddr;
    private long pStringIds;
    private long pTypeIds;
    private long pFieldIds;
    private long pMethodIds;
    private long pProtoIds;
    private long pClassDefs;
    private long classCount;

    public long getClassCount() {
        return classCount;
    }

    public void setClassCount(long classCount) {
        this.classCount = classCount;
    }

    public void setBaseAddr(long baseAddr) {
        this.baseAddr = baseAddr;
    }

    public void setpStringIds(long pStringIds) {
        this.pStringIds = pStringIds;
    }

    public void setpTypeIds(long pTypeIds) {
        this.pTypeIds = pTypeIds;
    }

    public void setpFieldIds(long pFieldIds) {
        this.pFieldIds = pFieldIds;
    }

    public void setpMethodIds(long pMethodIds) {
        this.pMethodIds = pMethodIds;
    }

    public void setpProtoIds(long pProtoIds) {
        this.pProtoIds = pProtoIds;
    }

    public void setpClassDefs(long pClassDefs) {
        this.pClassDefs = pClassDefs;
    }

    public long getBaseAddr() {
        return baseAddr;
    }

    public long getpStringIds() {
        return pStringIds;
    }

    public long getpTypeIds() {
        return pTypeIds;
    }

    public long getpFieldIds() {
        return pFieldIds;
    }

    public long getpMethodIds() {
        return pMethodIds;
    }

    public long getpProtoIds() {
        return pProtoIds;
    }

    public long getpClassDefs() {
        return pClassDefs;
    }

    public String toString() {
        return "baseAddr:" + baseAddr + ";pStringIds:" + pStringIds + ";pTypeIds:" + pTypeIds + ";pFieldIds:" + pFieldIds + ";pMethodIds:" + pMethodIds + ";pProtoIds:" + pProtoIds + ";pClassDefs:" + pClassDefs;
    }

}
