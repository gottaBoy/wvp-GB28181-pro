package com.genersoft.iot.vmp.gb28181.bean.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车辆心跳请求DTO
 * @author auto-generated
 */
@Data
@Schema(description = "车辆心跳请求")
public class VehicleHeartbeatDTO {

    @Schema(description = "车辆ID", required = true)
    private String vehicleId;

    @Schema(description = "IP地址", required = true)
    @JsonProperty("ipAddress")
    private String ipAddress;

    @Schema(description = "状态：online/offline")
    private String status;

    @Schema(description = "最后心跳时间")
    private String lastHeartbeat;
}

