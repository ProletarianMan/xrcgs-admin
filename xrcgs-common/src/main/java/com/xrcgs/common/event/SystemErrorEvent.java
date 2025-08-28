package com.xrcgs.common.event;

import org.springframework.context.ApplicationEvent;

/** 承载全局异常的可记录摘要信息
 * 增强全局异常处理
 **/
public class SystemErrorEvent extends ApplicationEvent {
    private final String title;
    private final String username;
    private final String httpMethod;
    private final String uri;
    private final String ip;
    private final String queryString;
    private final String exceptionMsg;

    public SystemErrorEvent(Object source, String title, String username,
                            String httpMethod, String uri, String ip,
                            String queryString, String exceptionMsg) {
        super(source);
        this.title = title;
        this.username = username;
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.ip = ip;
        this.queryString = queryString;
        this.exceptionMsg = exceptionMsg;
    }

    public String getTitle() { return title; }
    public String getUsername() { return username; }
    public String getHttpMethod() { return httpMethod; }
    public String getUri() { return uri; }
    public String getIp() { return ip; }
    public String getQueryString() { return queryString; }
    public String getExceptionMsg() { return exceptionMsg; }
}
