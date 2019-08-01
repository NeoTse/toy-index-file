package me.johntse.toy.index.impl;

/**
 * 搜索结果对象。
 *
 * @author John Tse
 */
public final class SearchResult {
    private String certNum;
    private String mob;
    private String name;

    public SearchResult() {

    }

    public SearchResult(String certNum, String mob, String name) {
        this.certNum = certNum;
        this.mob = mob;
        this.name = name;
    }

    public SearchResult(String name) {
        this.name = name;
    }

    public String getCertNum() {
        return certNum;
    }

    public void setCertNum(String certNum) {
        this.certNum = certNum;
    }

    public String getMob() {
        return mob;
    }

    public void setMob(String mob) {
        this.mob = mob;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return certNum + "\t" + mob + "\t" + name;
    }
}
