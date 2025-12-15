package com.genersoft.iot.vmp.factory.service;

import com.genersoft.iot.vmp.factory.bean.FactoryGroup;

import java.util.List;

/**
 * 厂区分组服务接口
 */
public interface IFactoryGroupService {

    /**
     * 添加分组
     */
    FactoryGroup add(FactoryGroup factoryGroup);

    /**
     * 更新分组
     */
    FactoryGroup update(FactoryGroup factoryGroup);

    /**
     * 删除分组
     */
    void delete(Integer id);

    /**
     * 根据ID查询分组
     */
    FactoryGroup getById(Integer id);

    /**
     * 根据厂区应用名查询所有分组
     */
    List<FactoryGroup> getByApp(String app);

    /**
     * 将拉流代理添加到分组
     */
    void addProxyToGroup(Integer groupId, String app, String stream);

    /**
     * 从分组中移除拉流代理
     */
    void removeProxyFromGroup(Integer groupId, String app, String stream);

    /**
     * 移除拉流代理的所有分组关联
     */
    void removeProxyFromAllGroups(String app, String stream);

    /**
     * 获取分组下的所有拉流代理（app, stream）
     */
    List<String[]> getProxiesByGroupId(Integer groupId);

    /**
     * 获取拉流代理所属的分组ID
     */
    Integer getGroupIdByProxy(String app, String stream);
}

