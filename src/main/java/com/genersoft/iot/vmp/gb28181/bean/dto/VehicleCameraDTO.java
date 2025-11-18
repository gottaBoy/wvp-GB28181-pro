package com.genersoft.iot.vmp.gb28181.bean.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车辆相机DTO
 * @author auto-generated
 */
@Data
@Schema(description = "车辆相机信息")
public class VehicleCameraDTO {

    @Schema(description = "相机ID", required = true)
    @JsonProperty("cameraId")
    private String cameraId;
    
    // 支持不同的字段名映射
    @JsonProperty("camera_id")
    public void setCameraIdAlt(String cameraId) {
        this.cameraId = cameraId;
    }
    
    @JsonProperty("id")
    public void setIdAlt(String cameraId) {
        this.cameraId = cameraId;
    }

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
}

