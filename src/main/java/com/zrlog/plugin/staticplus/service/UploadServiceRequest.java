package com.zrlog.plugin.staticplus.service;

import java.util.ArrayList;
import java.util.List;

public class UploadServiceRequest {

    private Object fileInfo;
    private UploadServiceRequest payload;

    public Object getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(Object fileInfo) {
        this.fileInfo = fileInfo;
    }

    public UploadServiceRequest getPayload() {
        return payload;
    }

    public void setPayload(UploadServiceRequest payload) {
        this.payload = payload;
    }

    public List<String> fileInfoList() {
        List<String> values = new ArrayList<String>();
        appendFileInfo(values, fileInfo);
        return values;
    }

    private void appendFileInfo(List<String> values, Object value) {
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                appendFileInfo(values, item);
            }
            return;
        }
        if (value != null) {
            values.add(value.toString());
        }
    }
}
