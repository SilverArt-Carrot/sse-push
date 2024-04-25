package com.carrot.util;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SseEmitterUtil {

    // 当前连接数
    private static final AtomicInteger count = new AtomicInteger(0);
    // 存储 SseEmitter 信息
    private static final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();

    public static SseEmitter connect(String key) {
        if (sseEmitterMap.containsKey(key)) {
            return sseEmitterMap.get(key);
        }

        try {
            // 设置超时时间，0表示不过期。默认30秒
            SseEmitter sseEmitter = new SseEmitter(0L);
            // 注册回调
            sseEmitter.onCompletion(completionCallBack(key));
            sseEmitter.onError(errorCallBack(key));
            sseEmitter.onTimeout(timeoutCallBack(key));
            sseEmitterMap.put(key, sseEmitter);
            // 数量+1
            count.getAndIncrement();
            return sseEmitter;
        } catch (Exception e) {
            System.out.println("创建新的SSE连接异常，当前连接Key为：" + key);
        }
        return null;
    }

    public static void sendMessage(String key, String message) {
        if (sseEmitterMap.containsKey(key)) {
            try {
                sseEmitterMap.get(key).send(message);
            } catch (IOException e) {
                System.out.println("用户" + key +"推送异常:" + e.getMessage());
                remove(key);
            }
        }
    }

    public static void groupSendMessage(String groupId, String message) {
        if (sseEmitterMap.size() > 0) {
            sseEmitterMap.forEach((k, v) -> {
                try {
                    if (k.startsWith(groupId)) {
                        v.send(message, MediaType.APPLICATION_JSON);
                    }
                } catch (IOException e) {
                    System.out.println("用户" + k + "推送异常:" + e.getMessage());
                    remove(k);
                }
            });
        }
    }

    public static void batchSendMessage(String message) {
        sseEmitterMap.forEach((k, v) -> {
            try {
                v.send(message, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                System.out.println("用户" + k + "推送异常:" + e.getMessage());
                remove(k);
            }
        });
    }

    public static void batchSendMessage(String message, Set<String> ids) {
        ids.forEach(userId -> sendMessage(userId, message));
    }

    public static void remove(String key) {
        SseEmitter remove = sseEmitterMap.remove(key);
        if (remove != null) {
            count.getAndDecrement();
            System.out.println("移除连接：" + key);
        }
    }

    public static List<String> getIds() {
        return new ArrayList<>(sseEmitterMap.keySet());
    }

    public static int getCount() {
        return count.intValue();
    }

    private static Runnable completionCallBack(String key) {
        return () -> {
            System.out.println("结束连接：" + key);
            remove(key);
        };
    }

    private static Runnable timeoutCallBack(String key) {
        return () -> {
            System.out.println("连接超时：" + key);
            remove(key);
        };
    }

    private static Consumer<Throwable> errorCallBack(String key) {
        return throwable -> {
            System.out.println("连接异常：" + key);
            remove(key);
        };
    }

}

