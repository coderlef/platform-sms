package com.courage.platform.sms.admin.dispatcher.processor.requeset.body;

import com.courage.platform.sms.admin.dispatcher.processor.requeset.RequestBody;

public class SendMessageRequestBody extends RequestBody {

    private String mobile;

    private String templateId;

    private Integer appId;

    private String templateParam;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getTemplateParam() {
        return templateParam;
    }

    public void setTemplateParam(String templateParam) {
        this.templateParam = templateParam;
    }

}

