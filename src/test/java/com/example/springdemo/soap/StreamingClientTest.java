package com.example.springdemo.soap;

import com.example.springdemo.soap.model.ObjectFactory;
import com.example.springdemo.soap.model.ReadStorageRecordRequest;
import com.example.springdemo.soap.model.ReadStorageRecordResponse;
import com.sun.xml.ws.developer.StreamingAttachmentFeature;
import com.sun.xml.ws.developer.StreamingDataHandler;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.activation.DataHandler;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.jws.soap.SOAPBinding.ParameterStyle;
import jakarta.jws.soap.SOAPBinding.Style;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.MTOMFeature;

import java.net.MalformedURLException;
import java.net.URL;

@Disabled("Manual test")
public class StreamingClientTest {

    @WebService(targetNamespace = "https://springdemo.example.com/soap")
    @SOAPBinding(style = Style.DOCUMENT, parameterStyle = ParameterStyle.BARE)
    @XmlSeeAlso({ ObjectFactory.class })
    public static interface StorageRecordPortService {
        @WebMethod()
        @WebResult(partName = "ReadStorageRecordResponse")
        @Action(input = "https://springdemo.example.com/soap/StorageRecordPort/ReadStorageRecordRequest", output = "https://springdemo.example.com/soap/StorageRecordPort/ReadStorageRecordResponse")
        ReadStorageRecordResponse ReadStorageRecord(
                @WebParam(targetNamespace = "https://springdemo.example.com/soap", name = "ReadStorageRecordRequest", partName = "ReadStorageRecordRequest") ReadStorageRecordRequest request);
    }

    @Test
    public void ClientTest() throws MalformedURLException {
        MTOMFeature mtom = new MTOMFeature();
        // Configure such that whole MIME message is parsed eagerly,
        // Attachments under 4MB are kept in memory
        StreamingAttachmentFeature stf = new StreamingAttachmentFeature("target", true, 4000000L);

        URL url = new URL("http://localhost:8080/ws/storageRecord.wsdl");
        // 1st argument service URI, refer to wsdl document above
        // 2nd argument is service name, refer to wsdl document above
        QName qname = new QName("https://springdemo.example.com/soap", "StorageRecordPortService");
        QName qnamePort = new QName("https://springdemo.example.com/soap", "StorageRecordPortSoap11");

        Service service = Service.create(url, qname);
        StorageRecordPortService readStorage = service.getPort(qnamePort, StorageRecordPortService.class, mtom, stf);

        // BindingProvider bp = (BindingProvider)readStorage;
        // bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        // "http://localhost:8080/ws");

        ReadStorageRecordRequest request = new ReadStorageRecordRequest();
//        request.setName("ideaIC-2022.3-aarch64.dmg");
        request.setName("spring-demo-0.1.0.jar");
        ReadStorageRecordResponse response = readStorage.ReadStorageRecord(request);
        DataHandler dh = response.getStorageRecord().getContent();
        Assertions.assertThat(dh).isInstanceOf(StreamingDataHandler.class);
        StreamingDataHandler sdh = (StreamingDataHandler) dh;
        IOUtils.closeQuietly(sdh);
        // MyService service = new MyService();
        // MyProxy proxy = service.getProxyPort(feature, stf);
    }
}
