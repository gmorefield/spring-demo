package com.example.springdemo.config;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.spring.http.CloudEventHttpUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class CloudEventHttpMessageConverter implements HttpMessageConverter<CloudEvent>{

    @Override
    public boolean canRead(@NotNull Class<?> clazz, @Nullable MediaType mediaType) {
        return CloudEvent.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(@NotNull Class<?> clazz, @Nullable MediaType mediaType) {
        return CloudEvent.class.isAssignableFrom(clazz);
    }

    @NotNull
    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(new MediaType("application","cloudevents"), MediaType.ALL);
    }

    @NotNull
    @Override
    public CloudEvent read(@NotNull Class<? extends CloudEvent> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
		byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
		return CloudEventHttpUtils.toReader(inputMessage.getHeaders(), () -> body).toEvent();
    }

    @Override
    public void write(@NotNull CloudEvent event, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
            throws HttpMessageNotWritableException {
		CloudEventUtils.toReader(event)
				.read(CloudEventHttpUtils.toWriter(outputMessage.getHeaders(), body -> copy(body, outputMessage)));
    }

	private void copy(byte[] body, HttpOutputMessage outputMessage) {
		try {
			StreamUtils.copy(body, outputMessage.getBody());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}    
}
