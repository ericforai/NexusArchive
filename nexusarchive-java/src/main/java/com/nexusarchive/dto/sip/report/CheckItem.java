// Input: Lombok、Java 标准库
// Output: CheckItem 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.sip.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单项检测结果
 */
public class CheckItem {
    
    private String name;
    private OverallStatus status;
    private String message;
    private java.time.LocalDateTime checkTime;
    private List<String> errors = new ArrayList<>();

    public CheckItem() {}

    public CheckItem(String name, OverallStatus status, String message, List<String> errors) {
        this.name = name;
        this.status = status;
        this.message = message;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.checkTime = java.time.LocalDateTime.now();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public java.time.LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(java.time.LocalDateTime checkTime) { this.checkTime = checkTime; }

    public OverallStatus getStatus() { return status; }
    public void setStatus(OverallStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.status = OverallStatus.FAIL;
    }
    
    public static CheckItem pass(String name, String message) {
        CheckItem item = new CheckItem();
        item.setName(name);
        item.setStatus(OverallStatus.PASS);
        item.setMessage(message);
        return item;
    }
    
    public static CheckItem fail(String name, String message) {
        CheckItem item = new CheckItem();
        item.setName(name);
        item.setStatus(OverallStatus.FAIL);
        item.setMessage(message);
        item.addError(message);
        return item;
    }

    // Mock Builder to minimize code changes in other files
    public static CheckItemBuilder builder() {
        return new CheckItemBuilder();
    }

    public static class CheckItemBuilder {
        private CheckItem item = new CheckItem();

        public CheckItemBuilder name(String name) { item.setName(name); return this; }
        public CheckItemBuilder status(OverallStatus status) { item.setStatus(status); return this; }
        public CheckItemBuilder message(String message) { item.setMessage(message); return this; }
        public CheckItemBuilder errors(List<String> errors) { item.setErrors(errors); return this; }
        
        public CheckItem build() { return item; }
    }
}
