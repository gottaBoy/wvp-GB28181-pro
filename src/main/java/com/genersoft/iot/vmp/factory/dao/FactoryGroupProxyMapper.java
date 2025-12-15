package com.genersoft.iot.vmp.factory.dao;

import com.genersoft.iot.vmp.factory.bean.FactoryGroupProxy;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface FactoryGroupProxyMapper {

    @Insert("INSERT INTO wvp_factory_group_proxy (group_id, app, stream, create_time) " +
            "VALUES (#{groupId}, #{app}, #{stream}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(FactoryGroupProxy factoryGroupProxy);

    @Delete("DELETE FROM wvp_factory_group_proxy WHERE group_id=#{groupId} AND app=#{app} AND stream=#{stream}")
    int deleteByGroupAndProxy(@Param("groupId") Integer groupId, @Param("app") String app, @Param("stream") String stream);

    @Delete("DELETE FROM wvp_factory_group_proxy WHERE app=#{app} AND stream=#{stream}")
    int deleteByProxy(@Param("app") String app, @Param("stream") String stream);

    @Delete("DELETE FROM wvp_factory_group_proxy WHERE group_id=#{groupId}")
    int deleteByGroupId(@Param("groupId") Integer groupId);

    @Select("SELECT * FROM wvp_factory_group_proxy WHERE group_id=#{groupId}")
    List<FactoryGroupProxy> selectByGroupId(@Param("groupId") Integer groupId);

    @Select("SELECT * FROM wvp_factory_group_proxy WHERE app=#{app} AND stream=#{stream}")
    FactoryGroupProxy selectByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    @Select("SELECT * FROM wvp_factory_group_proxy WHERE app=#{app}")
    List<FactoryGroupProxy> selectByApp(@Param("app") String app);

    @Select("SELECT COUNT(*) FROM wvp_factory_group_proxy WHERE group_id=#{groupId}")
    int countByGroupId(@Param("groupId") Integer groupId);
}

