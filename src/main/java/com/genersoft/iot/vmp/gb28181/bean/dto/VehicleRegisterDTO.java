package com.genersoft.iot.vmp.gb28181.bean.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 车辆注册请求DTO
 * @author auto-generated
 */
@Data
@Schema(description = "车辆注册请求")
public class VehicleRegisterDTO {

    @Schema(description = "车辆ID", required = true)
    @JsonProperty("vehicleId")
    private String vehicleId;

    // 支持不同的字段名映射
    @JsonProperty("vehicle_id")
    public void setVehicleIdAlt(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    @JsonProperty("id")
    public void setIdAlt(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    @Schema(description = "车辆名称")
    @JsonProperty("vehicleName")
    private String vehicleName;

    // 支持不同的字段名映射
    @JsonProperty("vehicle_name")
    public void setVehicleNameAlt(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    @JsonProperty("name")
    public void setNameAlt(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    @Schema(description = "IP地址", required = true)
    @JsonProperty("ipAddress")
    private String ipAddress;

    // 支持不同的字段名映射
    @JsonProperty("ip_address")
    public void setIpAddressAlt(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @JsonProperty("ip")
    public void setIpAlt(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Schema(description = "状态：online/offline")
    @JsonProperty("status")
    private String status;

    @Schema(description = "备注")
    @JsonProperty("description")
    private String description;

    @Schema(description = "最后心跳时间")
    @JsonProperty("lastHeartbeat")
    private String lastHeartbeat;

    // 支持不同的字段名映射
    @JsonProperty("last_heartbeat")
    public void setLastHeartbeatAlt(String lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    @Schema(description = "相机列表")
    @JsonProperty("cameras")
    private List<VehicleCameraDTO> cameras;
}

