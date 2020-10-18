package com.jzp.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * FileName:    WebServerConfiguration
 * Author:      jzp
 * Date:        2020/6/9 20:05
 * Description: Server 配置
 * <p>
 * 当 Spring 容器中没有 TomcatEmbeddedServletContainerFactory 这个 Bean 时
 * Spring 会将此类加载进 Spring 容器
 * 用于配置 application.yml 配置文件中所没有的其他 Tomcat 配置
 */
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        /* 使用工厂类定制化 Tomcat Connector */
        ((TomcatServletWebServerFactory) factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol http11NioProtocol = (Http11NioProtocol) connector.getProtocolHandler();
                /* 设置 30 秒没有请求，则服务端自动断开 keepAlive 连接 */
                http11NioProtocol.setKeepAliveTimeout(30000);
                /* 设置当同一个客户端发送超过 10000 个请求，则自动断开 keepAlive 连接 */
                http11NioProtocol.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
