package com.page.isomerism.feign;

import java.util.Objects;

/**
 * 模块定义
 *
 * @author page.xee
 * @date 2018/8/24
 */
public class ModuleDefinition {
    /**
     * 模块定义在元数据中的字段名称
     */
    public final static String FIELD_NODE_MODULE = "node.module";
    public final static String FIELD_NODE_FLAG = "node.flag";

    /**
     * 注册的模块名称
     */
    private String module;
    /**
     * 服务所属的flag
     */
    private String flag;
    /**
     * 服务的调用uri
     */
    private String uri;
    /**
     * 是否为相同模块服务
     */
    private boolean sameModule = false;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getPrjKey() {
        return flag;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isSameModule() {
        return sameModule;
    }

    public void setSameModule(boolean sameModule) {
        this.sameModule = sameModule;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModuleDefinition that = (ModuleDefinition) o;
        return Objects.equals(module, that.module) &&
                Objects.equals(flag, that.flag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, flag);
    }

}
