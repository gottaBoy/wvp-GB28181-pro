package com.genersoft.iot.vmp.factory.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 厂区分组实体类
 * @author system
 */
@Data
@Schema(description = "厂区分组信息")
public class FactoryGroup {

    @Schema(description = "数据库自增ID")
    private Integer id;

    @Schema(description = "分组名称，如：退洗库、黑皮库")
    private String name;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "所属厂区应用名")
    private String app;

    @Schema(description = "排序顺序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "分组下的摄像头数量（查询时使用）")
    private Integer cameraCount;
}

