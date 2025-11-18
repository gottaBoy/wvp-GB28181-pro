package com.genersoft.iot.vmp.gb28181.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 车辆相机列表更新请求DTO
 * @author auto-generated
 */
@Data
@Schema(description = "车辆相机列表更新请求")
public class VehicleCamerasUpdateDTO {

    @Schema(description = "车辆ID", required = true)
    private String vehicleId;

    @Schema(description = "相机列表", required = true)
    private List<VehicleCameraDTO> cameras;

    @Schema(description = "最后更新时间")
    private String lastUpdate;
}

