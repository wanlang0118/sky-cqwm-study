package com.sky.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的WebSocket服务，向管理端推送来单提醒/催单消息
 */
@ServerEndpoint("/ws/notify")
@Component
@Slf4j
public class WebSocketServer {

    /**
     * 存储session
     */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        SESSION_MAP.put(session.getId(), session);
        log.info("WebSocket连接建立, sessionId={}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        SESSION_MAP.remove(session.getId());
        log.info("WebSocket连接关闭, sessionId={}", session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 管理端如需心跳等，可在此处理
        log.info("收到客户端消息: {}", message);
    }

    /**
     * 向所有在线客户端广播JSON消息
     * @param type 消息类型 1来单提醒 2催单
     * @param orderId 订单ID
     * @param content 文本内容
     */
    public void broadcast(Integer type, Long orderId, String content) {
        JSONObject payload = new JSONObject();
        payload.put("type", type);
        payload.put("orderId", orderId);
        payload.put("content", content);
        String json = JSON.toJSONString(payload);
        SESSION_MAP.values().forEach(session -> {
            try {
                session.getBasicRemote().sendText(json);
            } catch (IOException e) {
                log.error("WebSocket发送消息失败, sessionId={}", session.getId(), e);
            }
        });
    }
}
