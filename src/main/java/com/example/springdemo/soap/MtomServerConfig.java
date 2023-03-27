package com.example.springdemo.soap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.ws.server.endpoint.adapter.DefaultMethodEndpointAdapter;
import org.springframework.ws.server.endpoint.adapter.method.MarshallingPayloadMethodProcessor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Configuration
public class MtomServerConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "storageRecord")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema documentsSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("StorageRecordPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("https://springdemo.example.com/soap");
        wsdl11Definition.setSchema(documentsSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema documentsSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/storageRecord.xsd"));
    }

    // ***********************
    // *** needed for MTOM ***
    // ***********************

//    @Bean
//    @ConfigurationProperties(prefix = "sample.multipart")
//    public CommonsMultipartResolver multipartResolver() {
//
//        return new CommonsMultipartResolver();
//    }
//
//    @Bean
//    public CommonsMultipartResolver filterMultipartResolver() {
//        final CommonsMultipartResolver resolver = new CommonsMultipartResolver();
//        return resolver;
//    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.example.springdemo.soap.model");
        marshaller.setMtomEnabled(true);
        return marshaller;
    }

    @Bean
    public DefaultMethodEndpointAdapter defaultMethodEndpointAdapter() {
        DefaultMethodEndpointAdapter adapter = new DefaultMethodEndpointAdapter();
        adapter.setMethodArgumentResolvers(Collections.singletonList(methodProcessor()));
        adapter.setCustomMethodReturnValueHandlers(Collections.singletonList(methodProcessor()));
        return adapter;
    }

    @Bean
    public MarshallingPayloadMethodProcessor methodProcessor() {
        return new MarshallingPayloadMethodProcessor(marshaller());
    }

    @Bean
    public WebMvcConfigurer multipartConverter(ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                ReadOnlyMultipartFormDataEndpointConverter converter = new ReadOnlyMultipartFormDataEndpointConverter(
                        objectMapper);
                List<MediaType> supportedMediaTypes = new ArrayList<>();
                supportedMediaTypes.addAll(converter.getSupportedMediaTypes());
                supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
                converter.setSupportedMediaTypes(supportedMediaTypes);

                converters.add(converter);
            }
        };
    }

    public static class ReadOnlyMultipartFormDataEndpointConverter extends MappingJackson2HttpMessageConverter {

        public ReadOnlyMultipartFormDataEndpointConverter(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            // When a rest client(e.g. RestTemplate#getForObject) reads a request, 'RequestAttributes' can be null.
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                return false;
            }
            HandlerMethod handlerMethod = (HandlerMethod) requestAttributes
                    .getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (handlerMethod == null) {
                return false;
            }
            RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
            if (requestMapping == null) {
                return false;
            }
            // This converter reads data only when the mapped controller method consumes just 'MediaType.MULTIPART_FORM_DATA_VALUE'.
            if (requestMapping.consumes().length != 1 || !MULTIPART_FORM_DATA_VALUE.equals(requestMapping.consumes()[0])) {
                return false;
            }
            return super.canRead(type, contextClass, mediaType);
        }

//      If you want to decide whether this converter can reads data depending on end point classes (i.e. classes with '@RestController'/'@Controller'),
//      you have to compare 'contextClass' to the type(s) of your end point class(es).
//      Use this 'canRead' method instead.
//      @Override
//      public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
//          return YourEndpointController.class == contextClass && super.canRead(type, contextClass, mediaType);
//      }

        @Override
        protected boolean canWrite(MediaType mediaType) {
            // This converter is only be used for requests.
            return false;
        }
    }
}