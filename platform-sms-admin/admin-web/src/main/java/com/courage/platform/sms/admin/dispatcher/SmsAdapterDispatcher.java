package com.courage.platform.sms.admin.dispatcher;

import com.courage.platform.sms.admin.common.utils.Pair;
import com.courage.platform.sms.admin.common.utils.ThreadFactoryImpl;
import com.courage.platform.sms.admin.dispatcher.processor.RequestEntity;
import com.courage.platform.sms.admin.dispatcher.processor.RequestCode;
import com.courage.platform.sms.admin.dispatcher.processor.ResponseEntity;
import com.courage.platform.sms.admin.dispatcher.processor.SmsAdatperProcessor;
import com.courage.platform.sms.admin.dispatcher.processor.impl.ApplyTemplateRequestProcessor;
import com.courage.platform.sms.admin.dispatcher.processor.impl.CreateRecordDetailRequestProcessor;
import com.courage.platform.sms.admin.dispatcher.processor.impl.SendMessageRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * 请求分发器
 */
@Component(value = "smsAdapterDispatcher")
public class SmsAdapterDispatcher {

    private final static Logger logger = LoggerFactory.getLogger(SmsAdapterDispatcher.class);

    //处理器命令映射
    protected final HashMap<Integer/* request code */, Pair<SmsAdatperProcessor, ExecutorService>> processorTable = new HashMap<Integer, Pair<SmsAdatperProcessor, ExecutorService>>(64);

    private volatile boolean running = false;

    @Autowired
    private SendMessageRequestProcessor sendMessageRequestProcessor;

    @Autowired
    private ApplyTemplateRequestProcessor applyTemplateRequestProcessor;

    @Autowired
    private CreateRecordDetailRequestProcessor createRecordDetailRequestProcessor;

    private ExecutorService createRecordDetailThread;

    @PostConstruct
    public synchronized void init() {
        if (running) {
            return;
        }
        logger.info("开始初始化短信适配器分发服务");
        long start = System.currentTimeMillis();
        // 映射处理器
        this.createRecordDetailThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("createRecordDetailThread-"));
        {
            registerProcessor(RequestCode.SEND_MESSAGE, sendMessageRequestProcessor);                                      // 发送短信
            registerProcessor(RequestCode.APPLY_TEMPLATE, applyTemplateRequestProcessor);                                  // 申请模版
            registerProcessor(RequestCode.CREATE_RECORD_DETAIL, createRecordDetailRequestProcessor, createRecordDetailThread); // 创建记录详情 (异步,使用单独的线程)
        }
        logger.info("结束初始化短信适配器分发服务, 耗时：" + (System.currentTimeMillis() - start));
    }

    // 分发处理请求
    public ResponseEntity dispatchSyncRequest(int requestCode, RequestEntity processorRequest) {
        Pair<SmsAdatperProcessor, ExecutorService> pair = processorTable.get(requestCode);
        SmsAdatperProcessor smsAdatperProcessor = pair.getObject1();
        ExecutorService executorService = pair.getObject2();
        if (executorService == null) {
            ResponseEntity responseCommand = smsAdatperProcessor.processRequest(processorRequest);
            return responseCommand;
        } else {
            Pair<CountDownLatch, ResponseEntity> responsePair = new Pair(new CountDownLatch(1), null);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    ResponseEntity response = null;
                    try {
                        response = smsAdatperProcessor.processRequest(processorRequest);
                        responsePair.setObject2(response);
                    } catch (Throwable e) {
                        logger.error("processRequest error:", e);
                    } finally {
                        responsePair.getObject1().countDown();
                    }
                }
            });
            try {
                responsePair.getObject1().await(5000, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            return responsePair.getObject2();
        }
    }

    public void dispatchAsyncRequest(int requestCode, RequestEntity processorRequest) {
        Pair<SmsAdatperProcessor, ExecutorService> pair = processorTable.get(requestCode);
        SmsAdatperProcessor smsAdatperProcessor = pair.getObject1();
        ExecutorService executorService = pair.getObject2();
        if (executorService == null) {
            smsAdatperProcessor.processRequest(processorRequest);
        }
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    smsAdatperProcessor.processRequest(processorRequest);
                } catch (Throwable e) {
                    logger.error("processRequest error:", e);
                }
            }
        });
    }

    public void registerProcessor(int requestCode, SmsAdatperProcessor smsAdatperProcessor) {
        registerProcessor(requestCode, smsAdatperProcessor, null);
    }

    public void registerProcessor(int requestCode, SmsAdatperProcessor smsAdatperProcessor, ExecutorService executorService) {
        Pair<SmsAdatperProcessor, ExecutorService> pair = new Pair<SmsAdatperProcessor, ExecutorService>(smsAdatperProcessor, executorService);
        this.processorTable.put(requestCode, pair);
    }

    @PreDestroy
    public synchronized void destroy() {
        if (running) {
            long start = System.currentTimeMillis();
            logger.info("开始销毁短信适配器分发服务");
            //2.关闭所有的命令处理器
            running = false;
            logger.info("结束销毁短信适配器分发服务, 耗时：" + (System.currentTimeMillis() - start));
        }
    }

}
