package com.genersoft.iot.vmp.factory.dao;

import com.genersoft.iot.vmp.factory.bean.FactoryGroup;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface FactoryGroupMapper {

    @Insert("INSERT INTO wvp_factory_group (name, description, app, sort_order, create_time, update_time) " +
            "VALUES (#{name}, #{description}, #{app}, #{sortOrder}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(FactoryGroup factoryGroup);

    @Update("UPDATE wvp_factory_group " +
            "SET name=#{name}, description=#{description}, sort_order=#{sortOrder}, update_time=#{updateTime} " +
            "WHERE id=#{id}")
    int update(FactoryGroup factoryGroup);

    @Delete("DELETE FROM wvp_factory_group WHERE id=#{id}")
    int delete(@Param("id") Integer id);

    @Select("SELECT * FROM wvp_factory_group WHERE id=#{id}")
    FactoryGroup select(@Param("id") Integer id);

    @Select("SELECT * FROM wvp_factory_group WHERE app=#{app} ORDER BY sort_order ASC, id ASC")
    List<FactoryGroup> selectByApp(@Param("app") String app);

    @Select("SELECT * FROM wvp_factory_group WHERE app=#{app} AND name=#{name}")
    FactoryGroup selectByAppAndName(@Param("app") String app, @Param("name") String name);

    @Select("SELECT COUNT(*) FROM wvp_factory_group WHERE app=#{app}")
    int countByApp(@Param("app") String app);
}

