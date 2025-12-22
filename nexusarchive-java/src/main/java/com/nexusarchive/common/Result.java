// Input: Lombok、Java 标准库
// Output: Result 类
// Pos: 后端模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.common;

import lombok.Data;

/**
 * 统一返回结果
 */
@Data
public class Result<T> {
    
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    public static <T> Result<T> fail() {
        return new Result<>(500, "操作失败", null);
    }
    
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
    
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}