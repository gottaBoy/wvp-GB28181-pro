package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 车辆信息
 * @author auto-generated
 */
@Data
@Schema(description = "车辆信息")
public class Vehicle {

    @Schema(description = "数据库自增ID")
    private Integer id;

    @Schema(description = "车辆ID", required = true)
    private String vehicleId;

    @Schema(description = "车辆名称")
    private String vehicleName;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "状态：online/offline")
    private String status;

    @Schema(description = "备注")
    private String description;

    @Schema(description = "最后心跳时间")
    private String lastHeartbeat;

    @Schema(description = "注册时间")
    private String registerTime;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "相机列表")
    private List<VehicleCamera> cameras;
}

