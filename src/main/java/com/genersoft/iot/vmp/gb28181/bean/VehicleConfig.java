package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车辆配置信息
 * @author auto-generated
 */
@Data
@Schema(description = "车辆配置信息")
public class VehicleConfig {

    @Schema(description = "数据库自增ID")
    private Integer id;

    @Schema(description = "车辆ID", required = true)
    private String vehicleId;

    @Schema(description = "HTTP API端口")
    private Integer httpApiPort;

    @Schema(description = "HTTP API密钥")
    private String httpApiKey;

    @Schema(description = "是否启用自动推流管理")
    private Boolean autoStreamManagement;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;
}