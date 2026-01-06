// Input: Java 标准库
// Output: AuthTicketException 异常类
// Pos: 异常定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.exception;

/**
 * 授权票据异常
 */
public class AuthTicketException extends RuntimeException {
    
    public AuthTicketException(String message) {
        super(message);
    }
    
    public AuthTicketException(String message, Throwable cause) {
        super(message, cause);
    }
}





