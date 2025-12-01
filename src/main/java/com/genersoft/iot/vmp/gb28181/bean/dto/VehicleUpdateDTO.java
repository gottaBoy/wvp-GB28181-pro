package com.genersoft.iot.vmp.gb28181.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车辆信息更新请求DTO
 * @author auto-generated
 */
@Data
@Schema(description = "车辆信息更新请求")
public class VehicleUpdateDTO {

    @Schema(description = "车辆ID")
    private String vehicleId;

    @Schema(description = "车辆名称")
    private String vehicleName;

    @Schema(description = "IP地址（只有非空时才会更新）")
    private String ipAddress;

    @Schema(description = "状态：online/offline")
    private String status;

    @Schema(description = "备注")
    private String description;
}