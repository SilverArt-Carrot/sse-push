package com.carrot.controller;

import com.carrot.service.LongPollingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;

import javax.annotation.Resource;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/async")
public class AsyncTaskController {

    @Resource
    private LongPollingService longPollingService;

    @GetMapping("/asyncLongPolling")
    public String asyncLongPolling() {
        System.out.println("asyncLongPolling start...");
        String res = longPollingService.longPolling();   // Sync
        System.out.println("asyncLongPolling end...");
        return res;
    }

    @GetMapping("/webAsyncTask")
    public WebAsyncTask<String> webAsyncTask() {
        Callable<String> callable = () -> {
            TimeUnit.SECONDS.sleep(3);

            return "WebAsyncTask long polling";
        };

        return new WebAsyncTask<>(10000L, callable);
    }

    @GetMapping("/callable")
    public Callable<String> callable() {
        return () -> {
            TimeUnit.SECONDS.sleep(3);

            return "callable long polling";
        };
    }

    @GetMapping("/deferredResult")
    public DeferredResult<String> deferredResult() {
        DeferredResult<String> result = new DeferredResult<>();

        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            result.setResult("deferred result long polling");
        }).start();

        return result;
    }
}
