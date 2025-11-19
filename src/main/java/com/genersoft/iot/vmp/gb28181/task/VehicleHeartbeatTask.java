package com.genersoft.iot.vmp.gb28181.task;

import com.genersoft.iot.vmp.gb28181.service.IVehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 车辆心跳检测定时任务
 * 定期检查车辆心跳超时情况，自动更新离线状态
 * @author auto-generated
 */
@Slf4j
@Component
public class VehicleHeartbeatTask {

    @Autowired
    private IVehicleService vehicleService;

    /**
     * 心跳超时时间（秒）
     * 1分钟 = 60秒
     */
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 10;

    /**
     * 心跳检测任务
     * 每30秒执行一次，检查是否有车辆心跳超时
     */
    @Scheduled(fixedDelay = 5 * 1000) // 每30秒执行一次
    public void checkHeartbeatTimeout() {
        try {
            log.debug("[车辆心跳检测任务] 开始执行心跳超时检查");
            
            int processedCount = vehicleService.processHeartbeatTimeout(HEARTBEAT_TIMEOUT_SECONDS);
            
            if (processedCount > 0) {
                log.info("[车辆心跳检测任务] 发现并处理了 {} 个心跳超时车辆", processedCount);
            } else {
                log.debug("[车辆心跳检测任务] 没有发现心跳超时车辆");
            }
            
        } catch (Exception e) {
            log.error("[车辆心跳检测任务] 执行异常", e);
        }
    }

    /**
     * 获取当前心跳超时时间配置
     * @return 超时时间（秒）
     */
    public int getHeartbeatTimeoutSeconds() {
        return HEARTBEAT_TIMEOUT_SECONDS;
    }
}