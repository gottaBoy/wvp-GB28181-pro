package com.genersoft.iot.vmp.factory.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.factory.bean.FactoryGroup;
import com.genersoft.iot.vmp.factory.bean.FactoryGroupProxy;
import com.genersoft.iot.vmp.factory.dao.FactoryGroupMapper;
import com.genersoft.iot.vmp.factory.dao.FactoryGroupProxyMapper;
import com.genersoft.iot.vmp.factory.service.IFactoryGroupService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 厂区分组服务实现
 * 
 * @author system
 */
@Slf4j
@Service
public class FactoryGroupServiceImpl implements IFactoryGroupService {

    @Autowired
    private FactoryGroupMapper factoryGroupMapper;

    @Autowired
    private FactoryGroupProxyMapper factoryGroupProxyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FactoryGroup add(FactoryGroup factoryGroup) {
        // 参数校验
        validateGroupForAdd(factoryGroup);

        // 检查是否已存在同名分组
        FactoryGroup existing = factoryGroupMapper.selectByAppAndName(
            factoryGroup.getApp(), 
            factoryGroup.getName()
        );
        if (existing != null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), 
                String.format("该厂区下已存在同名分组: %s", factoryGroup.getName()));
        }

        // 设置默认值
        String now = DateUtil.getNow();
        factoryGroup.setCreateTime(now);
        factoryGroup.setUpdateTime(now);
        if (factoryGroup.getSortOrder() == null) {
            factoryGroup.setSortOrder(0);
        }

        // 执行插入
        int result = factoryGroupMapper.add(factoryGroup);
        if (result <= 0) {
            log.error("添加厂区分组失败: app={}, name={}", factoryGroup.getApp(), factoryGroup.getName());
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "添加分组失败");
        }

        log.info("添加厂区分组成功: app={}, name={}, id={}", 
            factoryGroup.getApp(), factoryGroup.getName(), factoryGroup.getId());
        return factoryGroup;
    }

    /**
     * 校验分组添加参数
     */
    private void validateGroupForAdd(FactoryGroup factoryGroup) {
        if (factoryGroup == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "分组信息不能为空");
        }
        if (!StringUtils.hasText(factoryGroup.getApp())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "厂区应用名不能为空");
        }
        if (!StringUtils.hasText(factoryGroup.getName())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "分组名称不能为空");
        }
        // 名称长度校验
        if (factoryGroup.getName().length() > 255) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "分组名称长度不能超过255个字符");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FactoryGroup update(FactoryGroup factoryGroup) {
        if (factoryGroup == null || factoryGroup.getId() == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "分组ID不能为空");
        }

        // 查询现有分组
        FactoryGroup existing = factoryGroupMapper.select(factoryGroup.getId());
        if (existing == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), 
                String.format("分组不存在: %d", factoryGroup.getId()));
        }

        // 如果修改了名称，检查新名称是否已存在
        if (StringUtils.hasText(factoryGroup.getName()) 
            && !factoryGroup.getName().equals(existing.getName())) {
            FactoryGroup nameExists = factoryGroupMapper.selectByAppAndName(
                existing.getApp(), 
                factoryGroup.getName()
            );
            if (nameExists != null && !nameExists.getId().equals(factoryGroup.getId())) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(), 
                    String.format("该厂区下已存在同名分组: %s", factoryGroup.getName()));
            }
        }

        // 保留原有字段（如果未提供）
        if (!StringUtils.hasText(factoryGroup.getApp())) {
            factoryGroup.setApp(existing.getApp());
        }
        if (factoryGroup.getSortOrder() == null) {
            factoryGroup.setSortOrder(existing.getSortOrder());
        }

        factoryGroup.setUpdateTime(DateUtil.getNow());
        int result = factoryGroupMapper.update(factoryGroup);
        if (result <= 0) {
            log.error("更新厂区分组失败: id={}", factoryGroup.getId());
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "更新分组失败");
        }

        log.info("更新厂区分组成功: id={}, name={}", factoryGroup.getId(), factoryGroup.getName());
        return factoryGroup;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        if (id == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "分组ID不能为空");
        }

        FactoryGroup group = factoryGroupMapper.select(id);
        if (group == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), 
                String.format("分组不存在: %d", id));
        }

        // 删除分组下的所有关联（级联删除，这里显式删除是为了记录日志）
        int proxyCount = factoryGroupProxyMapper.countByGroupId(id);
        if (proxyCount > 0) {
            factoryGroupProxyMapper.deleteByGroupId(id);
            log.debug("删除分组关联: groupId={}, count={}", id, proxyCount);
        }

        // 删除分组
        int result = factoryGroupMapper.delete(id);
        if (result <= 0) {
            log.error("删除厂区分组失败: id={}", id);
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "删除分组失败");
        }

        log.info("删除厂区分组成功: id={}, name={}, 关联数量={}", id, group.getName(), proxyCount);
    }

    @Override
    public FactoryGroup getById(Integer id) {
        return factoryGroupMapper.select(id);
    }

    @Override
    public List<FactoryGroup> getByApp(String app) {
        if (!StringUtils.hasText(app)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "厂区应用名不能为空");
        }

        List<FactoryGroup> groups = factoryGroupMapper.selectByApp(app);
        
        // 查询每个分组的摄像头数量
        // 注意：由于MyBatis注解限制，这里使用循环查询
        // 如果分组数量较多，可以考虑使用XML映射文件实现批量查询优化
        if (!groups.isEmpty()) {
            groups.forEach(group -> {
                int count = factoryGroupProxyMapper.countByGroupId(group.getId());
                group.setCameraCount(count);
            });
        }
        
        return groups;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProxyToGroup(Integer groupId, String app, String stream) {
        // 参数校验
        if (groupId == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "分组ID不能为空");
        }
        if (!StringUtils.hasText(app)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "应用名不能为空");
        }
        if (!StringUtils.hasText(stream)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "流ID不能为空");
        }

        // 检查分组是否存在
        FactoryGroup group = factoryGroupMapper.select(groupId);
        if (group == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), 
                String.format("分组不存在: %d", groupId));
        }

        // 检查是否已存在关联
        FactoryGroupProxy existing = factoryGroupProxyMapper.selectByAppAndStream(app, stream);
        if (existing != null) {
            if (existing.getGroupId().equals(groupId)) {
                log.debug("拉流代理已在分组中: app={}, stream={}, groupId={}", app, stream, groupId);
                return; // 已存在，无需重复添加
            } else {
                // 更新到新分组（先删除旧关联）
                factoryGroupProxyMapper.deleteByProxy(app, stream);
                log.debug("更新拉流代理分组: app={}, stream={}, 从分组{}移动到分组{}", 
                    app, stream, existing.getGroupId(), groupId);
            }
        }

        // 添加关联
        FactoryGroupProxy groupProxy = new FactoryGroupProxy();
        groupProxy.setGroupId(groupId);
        groupProxy.setApp(app);
        groupProxy.setStream(stream);
        groupProxy.setCreateTime(DateUtil.getNow());
        
        int result = factoryGroupProxyMapper.add(groupProxy);
        if (result <= 0) {
            log.error("添加拉流代理到分组失败: app={}, stream={}, groupId={}", app, stream, groupId);
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "添加拉流代理到分组失败");
        }
        
        log.info("添加拉流代理到分组成功: app={}, stream={}, groupId={}", app, stream, groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeProxyFromGroup(Integer groupId, String app, String stream) {
        if (groupId == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "分组ID不能为空");
        }
        if (!StringUtils.hasText(app) || !StringUtils.hasText(stream)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "应用名和流ID不能为空");
        }

        int result = factoryGroupProxyMapper.deleteByGroupAndProxy(groupId, app, stream);
        if (result > 0) {
            log.info("从分组中移除拉流代理成功: app={}, stream={}, groupId={}", app, stream, groupId);
        } else {
            log.warn("从分组中移除拉流代理失败，关联不存在: app={}, stream={}, groupId={}", app, stream, groupId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeProxyFromAllGroups(String app, String stream) {
        if (!StringUtils.hasText(app) || !StringUtils.hasText(stream)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "应用名和流ID不能为空");
        }

        int result = factoryGroupProxyMapper.deleteByProxy(app, stream);
        if (result > 0) {
            log.info("移除拉流代理的所有分组关联成功: app={}, stream={}, count={}", app, stream, result);
        } else {
            log.debug("移除拉流代理的所有分组关联，关联不存在: app={}, stream={}", app, stream);
        }
    }

    @Override
    public List<String[]> getProxiesByGroupId(Integer groupId) {
        List<FactoryGroupProxy> proxies = factoryGroupProxyMapper.selectByGroupId(groupId);
        return proxies.stream()
            .map(proxy -> new String[]{proxy.getApp(), proxy.getStream()})
            .collect(Collectors.toList());
    }

    @Override
    public Integer getGroupIdByProxy(String app, String stream) {
        FactoryGroupProxy proxy = factoryGroupProxyMapper.selectByAppAndStream(app, stream);
        return proxy != null ? proxy.getGroupId() : null;
    }
}
