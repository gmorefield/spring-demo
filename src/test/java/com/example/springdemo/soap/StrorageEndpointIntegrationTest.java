package com.example.springdemo.soap;

import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.payload;

import java.util.Base64;

import javax.xml.transform.Source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

// @ExtendWith(SpringExtension.class)
// @ContextConfiguration(classes = { MtomServerConfig.class, StorageEndpoint.class })
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles({ "h2", "test" })
public class StrorageEndpointIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    MockWebServiceClient mockClient;

    @BeforeEach
    public void init() {
        mockClient = MockWebServiceClient.createClient(applicationContext);
    }

    @Test
    public void testWsEndPointTest() throws Exception {
        Source requestPayload = new StringSource(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<ns2:SaveStorageRecordRequest xmlns:ns2=\"https://springdemo.example.com/soap\">"
                        + "<ns2:StorageRecord><ns2:name>test</ns2:name><ns2:contentType>application/pdf</ns2:contentType>"
                        + "<ns2:content><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:3255c4ea-cbf6-4181-b4a4-255440704a35%40springdemo.example.com\"/>"
                        + Base64.getEncoder().encodeToString("test".getBytes())
                        + "</ns2:content>"
                        + "</ns2:StorageRecord>"
                        + "</ns2:SaveStorageRecordRequest>");

        Source responsePayload = new StringSource(
                "<ns2:SaveStorageRecordResponse xmlns:ns2=\"https://springdemo.example.com/soap\">"
                        + "<ns2:success>true</ns2:success>"
                        + "<ns2:sha1>a94a8fe5ccb19ba61c4c0873d391e987982fbbd3</ns2:sha1>"
                        + "</ns2:SaveStorageRecordResponse>");

        mockClient
                .sendRequest(withPayload(requestPayload))
                .andExpect(payload(responsePayload));
    }
}
