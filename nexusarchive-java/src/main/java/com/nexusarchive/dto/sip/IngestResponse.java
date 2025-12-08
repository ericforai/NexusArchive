package com.nexusarchive.dto.sip;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SIP 接收响应对象
 * Reference: DA/T 104-2024 接口反馈规范
 */
public class IngestResponse {
    
    private String requestId;
    
    private String status;
    
    private String archivalCode;
    
    private List<String> missingFields;
    
    private String timestamp;
    
    private String message;

    public IngestResponse() {}

    public IngestResponse(String requestId, String status, String archivalCode, List<String> missingFields, String timestamp, String message) {
        this.requestId = requestId;
        this.status = status;
        this.archivalCode = archivalCode;
        this.missingFields = missingFields;
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getArchivalCode() { return archivalCode; }
    public void setArchivalCode(String archivalCode) { this.archivalCode = archivalCode; }

    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static IngestResponseBuilder builder() {
        return new IngestResponseBuilder();
    }

    public static class IngestResponseBuilder {
        private IngestResponse response = new IngestResponse();

        public IngestResponseBuilder requestId(String requestId) { response.setRequestId(requestId); return this; }
        public IngestResponseBuilder status(String status) { response.setStatus(status); return this; }
        public IngestResponseBuilder archivalCode(String archivalCode) { response.setArchivalCode(archivalCode); return this; }
        public IngestResponseBuilder missingFields(List<String> missingFields) { response.setMissingFields(missingFields); return this; }
        public IngestResponseBuilder timestamp(String timestamp) { response.setTimestamp(timestamp); return this; }
        public IngestResponseBuilder message(String message) { response.setMessage(message); return this; }
        
        public IngestResponse build() { return response; }
    }
}
