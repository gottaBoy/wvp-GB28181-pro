package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.service.IVehicleHttpClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 车辆HTTP客户端服务实现类
 * @author auto-generated
 */
@Slf4j
@Service
public class VehicleHttpClientServiceImpl implements IVehicleHttpClientService {

    @Autowired
    private RestTemplate vehicleRestTemplate;

    private static final int DEFAULT_PORT = 8080;
    private static final int TIMEOUT_SECONDS = 10;

    @Override
    public boolean subscribeCameras(String vehicleIpAddress, Integer port, List<String> cameraIds, String apiKey) {
        log.info("[车辆API调用-订阅] 开始处理订阅请求: vehicleIp={}, port={}, cameraIds={}", 
                vehicleIpAddress, port, cameraIds);
        
        if (!StringUtils.hasText(vehicleIpAddress) || cameraIds == null || cameraIds.isEmpty()) {
            log.error("[车辆API调用-订阅] 参数验证失败: vehicleIpAddress={}, cameraIds={}", vehicleIpAddress, cameraIds);
            return false;
        }

        try {
            String url = buildApiUrl(vehicleIpAddress, port, "/api/camera/subscribe");
            log.info("[车辆API调用-订阅] 构建请求URL: {}", url);
            
            HttpHeaders headers = buildHeaders(apiKey);
            log.info("[车辆API调用-订阅] 构建请求头: {}", headers);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("camera_ids", cameraIds);
            log.info("[车辆API调用-订阅] 构建请求体: {}", requestBody);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("[车辆API调用-订阅] 发送HTTP请求:");
            log.info("  - Method: POST");
            log.info("  - URL: {}", url);
            log.info("  - Headers: {}", headers);
            log.info("  - Body: {}", requestBody);
            log.info("  - Timeout: {} seconds", TIMEOUT_SECONDS);
            
            ResponseEntity<Map> response = vehicleRestTemplate.exchange(
                url, HttpMethod.POST, request, Map.class
            );
            
            log.info("[车辆API调用-订阅] 收到HTTP响应:");
            log.info("  - Status Code: {}", response.getStatusCode());
            log.info("  - Headers: {}", response.getHeaders());
            log.info("  - Body: {}", response.getBody());
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("[车辆API调用-订阅] 订阅相机成功: vehicle={}:{}, cameras={}, response={}", 
                        vehicleIpAddress, port, cameraIds, response.getBody());
            } else {
                log.error("[车辆API调用-订阅] 订阅相机失败: vehicle={}:{}, status={}, response={}", 
                         vehicleIpAddress, port, response.getStatusCode(), response.getBody());
            }
            
            return success;
        } catch (Exception e) {
            log.error("[车辆API调用-订阅] HTTP请求异常: vehicle={}:{}, cameras={}", 
                     vehicleIpAddress, port, cameraIds, e);
            log.error("[车辆API调用-订阅] 异常详细信息: type={}, message={}", 
                     e.getClass().getSimpleName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("[车辆API调用-订阅] 根本原因: type={}, message={}", 
                         e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            return false;
        }
    }

    @Override
    public boolean unsubscribeCameras(String vehicleIpAddress, Integer port, List<String> cameraIds, String apiKey) {
        if (!StringUtils.hasText(vehicleIpAddress) || cameraIds == null || cameraIds.isEmpty()) {
            log.warn("[车辆API调用] 参数无效: vehicleIpAddress={}, cameraIds={}", vehicleIpAddress, cameraIds);
            return false;
        }

        try {
            String url = buildApiUrl(vehicleIpAddress, port, "/api/camera/unsubscribe");
            
            HttpHeaders headers = buildHeaders(apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("camera_ids", cameraIds);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = vehicleRestTemplate.exchange(
                url, HttpMethod.POST, request, Map.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("[车辆API调用] 取消订阅相机成功: vehicle={}, cameras={}", vehicleIpAddress, cameraIds);
            } else {
                log.warn("[车辆API调用] 取消订阅相机失败: vehicle={}, status={}", vehicleIpAddress, response.getStatusCode());
            }
            
            return success;
        } catch (Exception e) {
            log.error("[车辆API调用] 取消订阅相机异常: vehicle={}, cameras={}", vehicleIpAddress, cameraIds, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getSubscribedCameras(String vehicleIpAddress, Integer port, String apiKey) {
        if (!StringUtils.hasText(vehicleIpAddress)) {
            log.warn("[车辆API调用] 参数无效: vehicleIpAddress={}", vehicleIpAddress);
            return Collections.emptyList();
        }

        try {
            String url = buildApiUrl(vehicleIpAddress, port, "/api/camera/list");
            
            HttpHeaders headers = buildHeaders(apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = vehicleRestTemplate.exchange(
                url, HttpMethod.GET, request, Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object cameraIdsObj = responseBody.get("camera_ids");
                
                if (cameraIdsObj instanceof List) {
                    List<String> cameraIds = (List<String>) cameraIdsObj;
                    log.debug("[车辆API调用] 获取订阅列表成功: vehicle={}, cameras={}", vehicleIpAddress, cameraIds);
                    return cameraIds;
                }
            }
            
            log.warn("[车辆API调用] 获取订阅列表失败: vehicle={}, status={}", vehicleIpAddress, response.getStatusCode());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[车辆API调用] 获取订阅列表异常: vehicle={}", vehicleIpAddress, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean checkHealth(String vehicleIpAddress, Integer port, String apiKey) {
        if (!StringUtils.hasText(vehicleIpAddress)) {
            return false;
        }

        try {
            String url = buildApiUrl(vehicleIpAddress, port, "/health");
            
            HttpHeaders headers = buildHeaders(apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = vehicleRestTemplate.exchange(
                url, HttpMethod.GET, request, String.class
            );
            
            boolean healthy = response.getStatusCode().is2xxSuccessful();
            log.debug("[车辆API调用] 健康检查: vehicle={}, healthy={}", vehicleIpAddress, healthy);
            return healthy;
        } catch (Exception e) {
            log.debug("[车辆API调用] 健康检查异常: vehicle={}", vehicleIpAddress, e);
            return false;
        }
    }

    /**
     * 构建API URL
     */
    private String buildApiUrl(String vehicleIpAddress, Integer port, String path) {
        int actualPort = port != null ? port : DEFAULT_PORT;
        String url = String.format("http://%s:%d%s", vehicleIpAddress, actualPort, path);
        log.debug("[构建API URL] vehicleIp={}, port={}, path={}, actualPort={}, finalUrl={}", 
                 vehicleIpAddress, port, path, actualPort, url);
        return url;
    }

    /**
     * 构建HTTP请求头
     */
    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (StringUtils.hasText(apiKey)) {
            headers.set("access-token", apiKey);
            log.debug("[构建请求头] 已设置API密钥");
        } else {
            log.debug("[构建请求头] 未设置API密钥");
        }
        
        log.debug("[构建请求头] Content-Type={}, Headers={}", MediaType.APPLICATION_JSON, headers);
        return headers;
    }

    @Override
    public boolean directSubscribeCamera(String vehicleIpAddress, Integer port, String cameraId, String vehicleId, String apiKey) {
        log.info("[直接订阅相机] vehicleIpAddress={}, port={}, cameraId={}, vehicleId={}", 
                vehicleIpAddress, port, cameraId, vehicleId);
        
        // 复用subscribeCameras方法，将单个cameraId转为列表
        return subscribeCameras(vehicleIpAddress, port, Collections.singletonList(cameraId), apiKey);
    }

    @Override
    public boolean directUnsubscribeCamera(String vehicleIpAddress, Integer port, String cameraId, String vehicleId, String apiKey) {
        log.info("[直接取消订阅相机] vehicleIpAddress={}, port={}, cameraId={}, vehicleId={}", 
                vehicleIpAddress, port, cameraId, vehicleId);
        
        // 复用unsubscribeCameras方法，将单个cameraId转为列表
        return unsubscribeCameras(vehicleIpAddress, port, Collections.singletonList(cameraId), apiKey);
    }
}