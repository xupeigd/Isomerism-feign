package com.page.isomerism.feign;

/**
 * 节点的类型
 *
 * @author page.xee
 * @date 2018/8/24
 */
public enum NodeType {

    /**
     * 节点:工作节点
     */
    NODE("NODE", "工作节点"),
    /**
     * 节点：网关节点
     */
    GATEWAY("GATEWAY", "网关节点");


    private String flag;

    private String name;

    NodeType(String flag, String name) {
        this.flag = flag;
        this.name = name;
    }

    public String getFlag() {
        return flag;
    }

    public String getName() {
        return name;
    }
}