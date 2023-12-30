package com.example.springdemo.client;

import com.example.springdemo.soap.model.SaveStorageRecordRequest;
import com.example.springdemo.soap.model.SaveStorageRecordResponse;
import com.example.springdemo.soap.model.StorageRecord;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

// import com.example.springdemo.wsclient.SaveStorageRecordRequest;
// import com.example.springdemo.wsclient.SaveStorageRecordResponse;
// import com.example.springdemo.wsclient.StorageRecord;

public class RecordStorageClient extends WebServiceGatewaySupport {

    public RecordStorageClient() {
        super();
    }

    public RecordStorageClient(WebServiceMessageFactory webServiceMessageFactory) {
        super(webServiceMessageFactory);
    }

    public SaveStorageRecordResponse saveRecord(String fileName, String contentType) {

        SaveStorageRecordRequest request = new SaveStorageRecordRequest();
        StorageRecord record = new StorageRecord();
        record.setName("test");
        record.setContentType(contentType);

        DataHandler dh = new DataHandler(new FileDataSource(fileName));
        record.setContent(dh);
        request.setStorageRecord(record);

        SaveStorageRecordResponse response = (SaveStorageRecordResponse) getWebServiceTemplate()
                .marshalSendAndReceive("http://amoremb1.home:8080/ws", request,
                        new SoapActionCallback(""));

        return response;
    }

}