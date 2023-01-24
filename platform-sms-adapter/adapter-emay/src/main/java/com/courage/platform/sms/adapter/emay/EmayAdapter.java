package com.courage.platform.sms.adapter.emay;

import com.courage.platform.sms.adapter.core.OuterAdapter;
import com.courage.platform.sms.adapter.core.support.SPI;
import com.courage.platform.sms.adapter.core.support.SmsChannelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SPI("emay")
public class EmayAdapter implements OuterAdapter {

    private final static Logger logger = LoggerFactory.getLogger(EmayAdapter.class);

    @Override
    public void init(SmsChannelConfig smsChannelConfig) {
        logger.info("初始化亿美短信客户端 appKey:" + smsChannelConfig.getChannelAppKey());
    }

}
