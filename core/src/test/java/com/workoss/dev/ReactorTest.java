package com.workoss.dev;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;

public class ReactorTest {

    private static final Logger log = LoggerFactory.getLogger(ReactorTest.class);

    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        System.out.println("---");
    }

    @Test
    void test01() throws InterruptedException {


        // 创建 Sink 及共享流
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<String> sharedFlux = sink.asFlux().share();

        // 生产者线程

        for (int i = 0; i < 100; i++) {
            sink.tryEmitNext("Data-" + i);
        }
        sink.tryEmitComplete();


        Scheduler boundedElastic = Schedulers.newBoundedElastic(4, 100, "boundedElastic");
        // 消费者1（弹性线程池）
        sharedFlux
//                .publishOn(boundedElastic)
                .doOnError(e -> log.error("处理失败", e))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))  // 指数退避重试
                .subscribeOn(Schedulers.parallel())
                .map(data -> {
                    try {
                        HttpResponse<String> response = HttpClient.newHttpClient()
                                .send(HttpRequest.newBuilder().GET().uri(URI.create("https://www.baidu.com")).build(),
                                      HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        log.info("subscribe map:{} resp:{}", data, response.statusCode());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return data;
                })
                .subscribe(data -> {
//                    System.out.println(Thread.currentThread().getName() + " 处理: " + data);
                    log.info("subscribe map:{}", data);
                });


        // 消费者2（并行线程池）
//        sharedFlux.publishOn(Schedulers.newParallel("parallet",2))
//                .subscribe(data -> System.out.println(Thread.currentThread().getName() + "处理: " + data));


        Thread.sleep(10000);

    }

    @Test
    public void testPublishOn1() {
        // 生产者：每隔500ms生成一个数据
        Flux<Integer> producer = Flux.interval(Duration.ofMillis(100))
                .map(i -> i.intValue() + 1) // 转为整数
                .take(100) // 限制生产10个数据
                .doOnNext(data -> {
//                    log.info("Producer generated: {}", data);
                });

        // 分区消费：每个数据根据条件分配给一个消费者
        producer
                .groupBy(data -> data % 4) // 按数据的奇偶性分区
                .subscribe(groupFlux -> groupFlux
                        .publishOn(Schedulers.parallel()) // 每个分区并行消费
                        .doOnNext(data -> {
                            log.info("Consumer: {} processed:{}", groupFlux.key(), data);
                        })
                        .subscribe()
                );



        // 为了观察输出，主线程等待
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
