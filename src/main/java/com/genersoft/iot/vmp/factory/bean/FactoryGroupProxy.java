package com.genersoft.iot.vmp.factory.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 厂区拉流代理分组关联实体类
 * @author system
 */
@Data
@Schema(description = "厂区拉流代理分组关联信息")
public class FactoryGroupProxy {

    @Schema(description = "数据库自增ID")
    private Integer id;

    @Schema(description = "分组ID")
    private Integer groupId;

    @Schema(description = "拉流代理应用名")
    private String app;

    @Schema(description = "拉流代理流ID")
    private String stream;

    @Schema(description = "创建时间")
    private String createTime;
}

