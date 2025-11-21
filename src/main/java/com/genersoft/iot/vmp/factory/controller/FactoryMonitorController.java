package com.genersoft.iot.vmp.factory.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 厂区监控接口
 */
@Tag(name = "厂区监控", description = "")
@RestController
@Slf4j
@RequestMapping(value = "/api/factory")
public class FactoryMonitorController {

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private UserSetting userSetting;

    // 流ID到名称的映射
    private static final Map<String, String> STREAM_NAME_MAP = new HashMap<String, String>() {{
        put("102", "退洗2号门对22号门");
        put("202", "炼铁北路");
        put("302", "退洗北路");
        put("402", "炼铁西路2号杆北");
        put("502", "炼铁西路2号杆南");
        put("602", "退洗22号门对2号门");
        put("702", "炼铁西1号杆南");
        put("802", "炼铁西路1号杆北");
        put("902", "压延1#地磅2号杆北");
        put("1002", "压延1#地磅3号杆西");
        put("1102", "压延1#地磅2号杆西");
        put("1202", "压延1#地磅3号杆北");
        put("1302", "压延1#地磅1号杆西");
        put("1402", "压延1#地磅1号杆北");
        put("1502", "广青1路1号杆西");
        put("1602", "广青1路1号杆北");
        put("1702", "广青1路2号杆西");
        put("1802", "广青1路2号杆北");
        put("1902", "成品库R22门");
        put("2002", "成品库R23门");
        put("2102", "成品库R21门");
    }};

    /**
     * 根据流ID获取摄像头名称
     */
    private String getStreamName(String stream) {
        return STREAM_NAME_MAP.getOrDefault(stream, stream);
    }

    @Operation(summary = "获取厂区汇总信息列表", security = {
            @SecurityRequirement(name = JwtUtils.HEADER),
            @SecurityRequirement(name = JwtUtils.API_KEY_HEADER)})
    @GetMapping(value = "/list/summary")
    @ResponseBody
    public WVPResult<List<Map<String, Object>>> getFactoryList() {
        try {
            // 获取所有拉流代理（不分页，最多10000条）
            PageInfo<StreamProxy> pageInfo = streamProxyService.getAll(1, 10000, null, null, null);
            List<StreamProxy> allProxies = pageInfo.getList();

            // 按app分组统计
            Map<String, Long> factoryCountMap = allProxies.stream()
                .filter(proxy -> StringUtils.hasText(proxy.getApp()))
                .collect(Collectors.groupingBy(StreamProxy::getApp, Collectors.counting()));

            // 构建厂区列表
            List<Map<String, Object>> factories = factoryCountMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> factory = new HashMap<>();
                    factory.put("app", entry.getKey());
                    factory.put("name", getFactoryName(entry.getKey()));
                    factory.put("count", entry.getValue());
                    return factory;
                })
                .collect(Collectors.toList());

            return WVPResult.success(factories);
        } catch (Exception e) {
            log.error("获取厂区列表失败", e);
            return WVPResult.fail(-1, "获取厂区列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取指定厂区的摄像头列表", security = {
            @SecurityRequirement(name = JwtUtils.HEADER),
            @SecurityRequirement(name = JwtUtils.API_KEY_HEADER)})
    @GetMapping(value = "/cameras")
    @ResponseBody
    public WVPResult<List<Map<String, Object>>> getFactoryCameras(
            @Parameter(description = "厂区应用名") @RequestParam String app) {
        try {
            if (!StringUtils.hasText(app)) {
                return WVPResult.fail(-1, "厂区应用名不能为空");
            }

            // 获取所有拉流代理（不分页，最多10000条）
            PageInfo<StreamProxy> pageInfo = streamProxyService.getAll(1, 10000, null, null, null);
            List<StreamProxy> allProxies = pageInfo.getList();
            
            // 筛选指定app的拉流代理，并按ID排序
            List<Map<String, Object>> cameras = allProxies.stream()
                .filter(proxy -> app.equals(proxy.getApp()))
                .sorted(Comparator.comparingInt(StreamProxy::getId)) // 按ID从小到大排序
                .map(proxy -> {
                    Map<String, Object> camera = new HashMap<>();
                    camera.put("id", proxy.getId());
                    camera.put("stream", proxy.getStream());
                    camera.put("name", getStreamName(proxy.getStream()));
                    camera.put("app", proxy.getApp());
                    camera.put("pulling", proxy.getPulling() != null ? proxy.getPulling() : false);
                    camera.put("enable", proxy.isEnable());
                    
                    // 如果正在拉流，生成播放地址
                    if (proxy.getPulling() != null && proxy.getPulling() && StringUtils.hasText(proxy.getMediaServerId())) {
                        camera.put("mediaServerId", proxy.getMediaServerId());
                        
                        // 生成播放URL
                        MediaServer mediaServer = mediaServerService.getOne(proxy.getMediaServerId());
                        if (mediaServer != null) {
                            String streamIp = mediaServer.getStreamIp();
                            int httpPort = mediaServer.getHttpPort();
                            int httpSslPort = mediaServer.getHttpSSlPort();
                            int rtmpPort = mediaServer.getRtmpPort();
                            int rtspPort = mediaServer.getRtspPort();
                            
                            String appName = proxy.getApp();
                            String streamId = proxy.getStream();
                            
                            // 生成各种协议的播放地址
                            Map<String, String> playUrls = new HashMap<>();
                            
                            // WebRTC (推荐)
                            playUrls.put("rtc", String.format("http://%s:%d/index/api/webrtc?app=%s&stream=%s&type=play", 
                                streamIp, httpPort, appName, streamId));
                            
                            // HTTP-FLV
                            playUrls.put("flv", String.format("http://%s:%d/%s/%s.live.flv", 
                                streamIp, httpPort, appName, streamId));
                            
                            // WebSocket-FLV
                            playUrls.put("ws_flv", String.format("ws://%s:%d/%s/%s.live.flv", 
                                streamIp, httpPort, appName, streamId));
                            
                            // HLS
                            playUrls.put("hls", String.format("http://%s:%d/%s/%s/hls.m3u8", 
                                streamIp, httpPort, appName, streamId));
                            
                            // RTMP
                            playUrls.put("rtmp", String.format("rtmp://%s:%d/%s/%s", 
                                streamIp, rtmpPort, appName, streamId));
                            
                            // RTSP
                            playUrls.put("rtsp", String.format("rtsp://%s:%d/%s/%s", 
                                streamIp, rtspPort, appName, streamId));
                            
                            camera.put("playUrls", playUrls);
                            
                            log.debug("摄像头 {} 播放地址: {}", proxy.getId(), playUrls);
                        } else {
                            log.warn("摄像头 {} 的流媒体服务器 {} 不存在", proxy.getId(), proxy.getMediaServerId());
                        }
                    }
                    
                    return camera;
                })
                .collect(Collectors.toList());
            
            log.info("获取厂区 {} 的摄像头列表，共 {} 个，按ID排序", app, cameras.size());
            return WVPResult.success(cameras);
        } catch (Exception e) {
            log.error("获取厂区摄像头列表失败", e);
            return WVPResult.fail(-1, "获取摄像头列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取厂区名称
     */
    private String getFactoryName(String app) {
        Map<String, String> nameMap = new HashMap<String, String>() {{
            put("guangqing", "广青厂区");
            put("rtp", "RTP厂区");
            put("proxy", "代理厂区");
        }};
        return nameMap.getOrDefault(app, app);
    }

    @Operation(summary = "启动单个摄像头", security = {
            @SecurityRequirement(name = JwtUtils.HEADER),
            @SecurityRequirement(name = JwtUtils.API_KEY_HEADER)})
    @GetMapping(value = "/cameras/start")
    @ResponseBody
    public DeferredResult<WVPResult<StreamContent>> start(HttpServletRequest request, 
                                                           @Parameter(description = "摄像头ID", required = true) @RequestParam int id) {
        log.info("启动摄像头播放: {}", id);
        StreamProxy streamProxy = streamProxyService.getStreamProxy(id);
        Assert.notNull(streamProxy, "代理信息不存在");

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<StreamInfo> callback = (code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                WVPResult<StreamContent> wvpResult = WVPResult.success();
                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo = streamInfo.clone(); // 深拷贝
                        String host;
                        try {
                            URL url = new URL(request.getRequestURL().toString());
                            host = url.getHost();
                        } catch (MalformedURLException e) {
                            host = request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }

                    // 创建StreamContent但不包含mediaServer信息
                    streamInfo.setMediaInfo(null);
                    StreamContent streamContent = new StreamContent(streamInfo);
                    
                    wvpResult.setData(streamContent);
                } else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }

                result.setResult(wvpResult);
            } else {
                result.setResult(WVPResult.fail(code, msg));
            }
        };

        streamProxyPlayService.start(id, null, callback);
        return result;
    }

    @Operation(summary = "停止单个摄像头", security = {
            @SecurityRequirement(name = JwtUtils.HEADER),
            @SecurityRequirement(name = JwtUtils.API_KEY_HEADER)})
    @GetMapping(value = "/cameras/stop")
    @ResponseBody
    public WVPResult<String> stop(@Parameter(description = "摄像头ID", required = true) @RequestParam int id) {
        log.info("停止摄像头播放: {}", id);
        try {
            streamProxyPlayService.stop(id);
            return WVPResult.success("停止成功");
        } catch (Exception e) {
            log.error("停止摄像头播放失败，ID: {}", id, e);
            return WVPResult.fail(-1, "停止失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量启动摄像头", security = {
            @SecurityRequirement(name = JwtUtils.HEADER),
            @SecurityRequirement(name = JwtUtils.API_KEY_HEADER)})
    @PostMapping(value = "/cameras/batch/start")
    @ResponseBody
    public WVPResult<Map<String, Object>> batchStart(@RequestBody Map<String, List<Integer>> request) {
        List<Integer> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return WVPResult.fail(-1, "未提供摄像头ID列表");
        }

        int successCount = 0;
        int failCount = 0;

        for (Integer id : ids) {
            try {
                streamProxyPlayService.start(id, null, (code, msg, streamInfo) -> {
                    // 异步回调，这里不做处理
                });
                successCount++;
            } catch (Exception e) {
                log.error("批量启动摄像头失败，ID: {}", id, e);
                failCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", successCount);
        result.put("fail", failCount);

        return WVPResult.success(result);
    }

    @Operation(summary = "批量停止摄像头", security = {
            @SecurityRequirement(name = JwtUtils.HEADER),
            @SecurityRequirement(name = JwtUtils.API_KEY_HEADER)})
    @PostMapping(value = "/cameras/batch/stop")
    @ResponseBody
    public WVPResult<Map<String, Object>> batchStop(@RequestBody Map<String, List<Integer>> request) {
        List<Integer> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return WVPResult.fail(-1, "未提供摄像头ID列表");
        }

        int successCount = 0;
        int failCount = 0;

        for (Integer id : ids) {
            try {
                streamProxyPlayService.stop(id);
                successCount++;
            } catch (Exception e) {
                log.error("批量停止摄像头失败，ID: {}", id, e);
                failCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", successCount);
        result.put("fail", failCount);

        return WVPResult.success(result);
    }
}
