package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车辆相机信息
 * @author auto-generated
 */
@Data
@Schema(description = "车辆相机信息")
public class VehicleCamera {

    @Schema(description = "数据库自增ID")
    private Integer id;

    @Schema(description = "车辆ID", required = true)
    private String vehicleId;

    @Schema(description = "相机ID", required = true)
    private String cameraId;

    @Schema(description = "相机名称")
    private String name;

    @Schema(description = "相机描述")
    private String description;

    @Schema(description = "流Topic（原始流或压缩流）")
    private String topic;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "帧率")
    private Integer fps;

    @Schema(description = "码率")
    private Integer bitrate;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "质量：high/medium/low")
    private String quality;

    @Schema(description = "状态：active/inactive")
    private String status;

    @Schema(description = "关联的推流ID")
    private Integer streamPushId;

    @Schema(description = "是否正在推流")
    private Boolean pushing;

    @Schema(description = "推流开始时间")
    private String pushTime;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;
}

