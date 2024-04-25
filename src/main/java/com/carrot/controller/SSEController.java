package com.carrot.controller;

import com.carrot.util.SseEmitterUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RequestMapping("/sse")
public class SSEController {

    private static final Map<String, SseEmitter> ssePool = new ConcurrentHashMap<>();

    @GetMapping("/test1")
    public String test() {
        return "success";
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        SseEmitter emitter = new SseEmitter();
        try {
            for (int i = 0; i < 10; i++) {
                emitter.send(SseEmitter.event().data("Event " + i));
                Thread.sleep(1000); // Simulate some delay
            }
            emitter.complete();
        } catch (IOException | InterruptedException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @GetMapping(path = "/subscribe/{id}")
//    @GetMapping(path = "/subscribe/{id}", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter subscribe(@PathVariable String id) {
        SseEmitter sseEmitter = null;
        if (ssePool.containsKey(id)) {
            sseEmitter = ssePool.get(id);
            System.out.println("subscribe already have connect:" + id);
        } else {
            sseEmitter = new SseEmitter(3 * 60 * 1000L);
            sseEmitter.onCompletion(completionCallBack(id));
            sseEmitter.onError(errorCallBack(id));
            sseEmitter.onTimeout(timeoutCallBack(id));

            ssePool.put(id, sseEmitter);
            System.out.println("subscribe create a connect:" + id);
        }
        return sseEmitter;
    }

    @GetMapping(path = "/push/{id}")
    public void push(@PathVariable String id, @RequestParam String content) {
        SseEmitter sseEmitter = ssePool.get(id);
        if (sseEmitter == null) {
            System.out.println("push do not find a connect:" + id);
            return;
        }
        try {
            sseEmitter.send(content);
//            sseEmitter.send(SseEmitter.event().id("test").data("666"));
//            sseEmitter.complete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping(path = "/close/{id}")
    public void close(@PathVariable String id) {
        SseEmitter sseEmitter = ssePool.get(id);
        if (sseEmitter == null) {
            System.out.println("close do not find a connect:" + id);
            return;
        }
        sseEmitter.complete();
//        try {
//            sseEmitter.send(SseEmitter.event().id("close"));
//            sseEmitter.complete();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private static Runnable completionCallBack(String key) {
        return () -> {
            System.out.println("结束连接：" + key);
            ssePool.remove(key);
        };
    }

    private static Runnable timeoutCallBack(String key) {
        return () -> {
            System.out.println("连接超时：" + key);
            ssePool.remove(key);
        };
    }

    private static Consumer<Throwable> errorCallBack(String key) {
        return throwable -> {
            System.out.println("连接异常：" + key);
            ssePool.remove(key);
        };
    }

}
