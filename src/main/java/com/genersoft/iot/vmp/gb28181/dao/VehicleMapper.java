package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Vehicle;
import com.genersoft.iot.vmp.gb28181.bean.VehicleCamera;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 车辆信息数据访问层
 * @author auto-generated
 */
@Mapper
@Repository
public interface VehicleMapper {

    /**
     * 根据车辆ID查询车辆信息
     */
    @Select("SELECT id, vehicle_id, vehicle_name, ip_address, status, description, last_heartbeat, " +
            "register_time, create_time, update_time " +
            "FROM wvp_vehicle WHERE vehicle_id = #{vehicleId}")
    Vehicle getVehicleByVehicleId(@Param("vehicleId") String vehicleId);

    /**
     * 插入车辆信息
     */
    @Insert("INSERT INTO wvp_vehicle (vehicle_id, vehicle_name, ip_address, status, description, " +
            "last_heartbeat, register_time, create_time, update_time) " +
            "VALUES (#{vehicleId}, #{vehicleName}, #{ipAddress}, #{status}, #{description}, " +
            "#{lastHeartbeat}, #{registerTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertVehicle(Vehicle vehicle);

    /**
     * 更新车辆完整信息（包括IP地址）
     * ⚠️ 警告：此方法会更新IP地址，应谨慎使用！
     * 通常情况下应使用 updateVehicleBasicInfo() 或 updateVehicleHeartbeat()
     */
    @Update("UPDATE wvp_vehicle SET vehicle_name = #{vehicleName}, ip_address = #{ipAddress}, " +
            "status = #{status}, description = #{description}, last_heartbeat = #{lastHeartbeat}, " +
            "update_time = #{updateTime} WHERE vehicle_id = #{vehicleId}")
    int updateVehicle(Vehicle vehicle);

    /**
     * 更新车辆基本信息（不包含IP地址，IP地址只能通过心跳更新）
     */
    @Update("UPDATE wvp_vehicle SET vehicle_name = #{vehicleName}, status = #{status}, " +
            "last_heartbeat = #{lastHeartbeat}, description = #{description}, update_time = #{updateTime} WHERE vehicle_id = #{vehicleId}")
    int updateVehicleBasicInfo(@Param("vehicleId") String vehicleId,
                               @Param("vehicleName") String vehicleName,
                               @Param("status") String status,
                               @Param("lastHeartbeat") String lastHeartbeat,
                               @Param("description") String description,
                               @Param("updateTime") String updateTime);

    /**
     * 只更新车辆的更新时间，不更改IP地址等敏感信息
     */
    @Update("UPDATE wvp_vehicle SET update_time = #{updateTime} WHERE vehicle_id = #{vehicleId}")
    int updateVehicleUpdateTime(@Param("vehicleId") String vehicleId, @Param("updateTime") String updateTime);

    /**
     * 更新车辆心跳信息（只有心跳可以更新IP地址）
     */
    @Update("UPDATE wvp_vehicle SET status = #{status}, " +
            "last_heartbeat = #{lastHeartbeat}, update_time = #{updateTime} " +
            "WHERE vehicle_id = #{vehicleId}")
    int updateVehicleHeartbeat(@Param("vehicleId") String vehicleId,
                               @Param("status") String status,
                               @Param("lastHeartbeat") String lastHeartbeat,
                               @Param("updateTime") String updateTime);

    /**
     * 根据车辆ID查询相机列表
     */
    @Select("SELECT id, vehicle_id, camera_id, name, description, topic, " +
            "enabled, fps, bitrate, width, height, quality, status, stream_push_id, pushing, " +
            "push_time, create_time, update_time " +
            "FROM wvp_vehicle_camera WHERE vehicle_id = #{vehicleId}")
    List<VehicleCamera> getCamerasByVehicleId(@Param("vehicleId") String vehicleId);

    /**
     * 插入相机信息
     */
    @Insert("INSERT INTO wvp_vehicle_camera (vehicle_id, camera_id, name, description, " +
            "topic, enabled, fps, bitrate, width, height, quality, " +
            "status, stream_push_id, pushing, push_time, create_time, update_time) " +
            "VALUES (#{vehicleId}, #{cameraId}, #{name}, #{description}, " +
            "#{topic}, #{enabled}, #{fps}, #{bitrate}, " +
            "#{width}, #{height}, #{quality}, #{status}, #{streamPushId}, #{pushing}, " +
            "#{pushTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCamera(VehicleCamera camera);

    /**
     * 更新相机信息
     */
    @Update("UPDATE wvp_vehicle_camera SET name = #{name}, description = #{description}, " +
            "topic = #{topic}, " +
            "enabled = #{enabled}, fps = #{fps}, bitrate = #{bitrate}, " +
            "width = #{width}, height = #{height}, quality = #{quality}, " +
            "status = #{status}, stream_push_id = #{streamPushId}, pushing = #{pushing}, " +
            "push_time = #{pushTime}, update_time = #{updateTime} " +
            "WHERE vehicle_id = #{vehicleId} AND camera_id = #{cameraId}")
    int updateCamera(VehicleCamera camera);

    /**
     * 更新相机推流状态
     */
    @Update("UPDATE wvp_vehicle_camera SET pushing = #{pushing}, status = #{status}, " +
            "push_time = #{pushTime}, update_time = #{updateTime} " +
            "WHERE vehicle_id = #{vehicleId} AND camera_id = #{cameraId}")
    int updateCameraPushStatus(@Param("vehicleId") String vehicleId,
                               @Param("cameraId") String cameraId,
                               @Param("pushing") Boolean pushing,
                               @Param("status") String status,
                               @Param("pushTime") String pushTime,
                               @Param("updateTime") String updateTime);

    /**
     * 根据推流ID查询相机
     */
    @Select("SELECT id, vehicle_id, camera_id, name, description, topic, " +
            "enabled, fps, bitrate, width, height, quality, status, stream_push_id, pushing, " +
            "push_time, create_time, update_time " +
            "FROM wvp_vehicle_camera WHERE stream_push_id = #{streamPushId}")
    VehicleCamera getCameraByStreamPushId(@Param("streamPushId") Integer streamPushId);

    /**
     * 删除车辆的所有相机
     */
    @Delete("DELETE FROM wvp_vehicle_camera WHERE vehicle_id = #{vehicleId}")
    int deleteCamerasByVehicleId(@Param("vehicleId") String vehicleId);

    /**
     * 删除指定相机
     */
    @Delete("DELETE FROM wvp_vehicle_camera WHERE vehicle_id = #{vehicleId} AND camera_id = #{cameraId}")
    int deleteCamera(@Param("vehicleId") String vehicleId, @Param("cameraId") String cameraId);

    /**
     * 查询所有车辆列表
     */
    @Select("SELECT id, vehicle_id, vehicle_name, ip_address, status, description, last_heartbeat, " +
            "register_time, create_time, update_time FROM wvp_vehicle ORDER BY create_time DESC")
    List<Vehicle> getAllVehicles();

    /**
     * 查询心跳超时的车辆（状态为在线但1分钟内无心跳的车辆）
     */
    @Select("SELECT id, vehicle_id, vehicle_name, ip_address, status, description, last_heartbeat, " +
            "register_time, create_time, update_time FROM wvp_vehicle " +
            "WHERE status = 'online' AND (last_heartbeat IS NULL OR " +
            "TIMESTAMPDIFF(SECOND, STR_TO_DATE(last_heartbeat, '%Y-%m-%d %H:%i:%s'), NOW()) > #{timeoutSeconds})")
    List<Vehicle> getHeartbeatTimeoutVehicles(@Param("timeoutSeconds") int timeoutSeconds);

    /**
     * 批量更新车辆状态为离线
     */
    @Update("UPDATE wvp_vehicle SET status = 'offline', update_time = #{updateTime} " +
            "WHERE vehicle_id IN (${vehicleIds})")
    int batchUpdateVehicleStatusOffline(@Param("vehicleIds") String vehicleIds, 
                                       @Param("updateTime") String updateTime);

    /**
     * 更新单个车辆状态
     */
    @Update("UPDATE wvp_vehicle SET status = #{status}, update_time = #{updateTime} " +
            "WHERE vehicle_id = #{vehicleId}")
    int updateVehicleStatus(@Param("vehicleId") String vehicleId,
                           @Param("status") String status,
                           @Param("updateTime") String updateTime);
}

