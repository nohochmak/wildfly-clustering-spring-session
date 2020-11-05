/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.spring.hotrod.annotation;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.spring.SessionMarshallerFactory;
import org.wildfly.clustering.web.spring.SessionPersistenceGranularity;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepository;
import org.wildfly.clustering.web.spring.hotrod.HotRodSessionRepositoryConfiguration;
import org.wildfly.common.Assert;

/**
 * @author Paul Ferraro
 */
@Configuration(proxyBeanMethods = false)
public class HotRodHttpSessionConfiguration extends SpringHttpSessionConfiguration implements HotRodSessionRepositoryConfiguration, ServletContextAware, ApplicationEventPublisherAware, ImportAware {

    private URI uri;
    private Properties properties = new Properties();
    private Integer maxActiveSessions = null;
    private SessionAttributePersistenceStrategy persistenceStrategy = SessionAttributePersistenceStrategy.COARSE;
    private Function<ClassLoader, ByteBufferMarshaller> marshallerFactory = SessionMarshallerFactory.JBOSS;
    private String templateName = DefaultTemplate.DIST_SYNC.getTemplateName();
    private Supplier<String> identifierFactory = () -> UUID.randomUUID().toString();
    private ApplicationEventPublisher publisher;
    private ServletContext context;

    @Bean
    public HotRodSessionRepository sessionRepository() {
        Assert.assertNotNull(this.properties);
        Assert.assertNotNull(this.persistenceStrategy);
        Assert.assertNotNull(this.publisher);
        Assert.assertNotNull(this.context);
        return new HotRodSessionRepository(this);
    }

    @Override
    public URI getUri() {
        return this.uri;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public String getTemplateName() {
        return this.templateName;
    }

    @Override
    public Integer getMaxActiveSessions() {
        return this.maxActiveSessions;
    }

    @Override
    public SessionAttributePersistenceStrategy getPersistenceStrategy() {
        return this.persistenceStrategy;
    }

    @Override
    public Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory() {
        return this.marshallerFactory;
    }

    @Override
    public Supplier<String> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public ApplicationEventPublisher getEventPublisher() {
        return this.publisher;
    }

    @Override
    public ServletContext getServletContext() {
        return this.context;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void setServletContext(ServletContext context) {
        super.setServletContext(context);
        this.context = context;
    }

    @Autowired(required = false)
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Autowired(required = false)
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Autowired(required = false)
    public void setGranularity(SessionPersistenceGranularity granularity) {
        this.persistenceStrategy = granularity.get();
    }

    @Autowired(required = false)
    public void setPersistenceStrategy(SessionAttributePersistenceStrategy persistenceStrategy) {
        this.persistenceStrategy = persistenceStrategy;
    }

    @Autowired(required = false)
    public void setMarshallerFactory(Function<ClassLoader, ByteBufferMarshaller> marshallerFactory) {
        this.marshallerFactory = marshallerFactory;
    }

    @Autowired(required = false)
    public void setMaxActiveSessions(Integer maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }

    @Autowired(required = false)
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @Autowired(required = false)
    public void setIdentifierFactory(Supplier<String> identifierFactory) {
        this.identifierFactory = identifierFactory;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata metadata) {
        Map<String, Object> attributeMap = metadata.getAnnotationAttributes(EnableHotRodHttpSession.class.getName());
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
        String uriValue = attributes.getString("uri");
        this.setUri(StringUtils.hasText(uriValue) ? URI.create(uriValue) : null);
        int maxActiveSessions = attributes.getNumber("maxActiveSessions").intValue();
        this.setMaxActiveSessions(maxActiveSessions < 0 ? null : maxActiveSessions);
        this.setMarshallerFactory(attributes.getEnum("marshallerFactory"));
        this.setGranularity(attributes.getEnum("granularity"));
        this.setTemplateName(attributes.getString("templateName"));
    }
}
