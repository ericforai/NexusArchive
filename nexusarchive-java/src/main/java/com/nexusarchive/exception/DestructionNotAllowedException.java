// Input: RuntimeException
// Output: DestructionNotAllowedException 异常类
// Pos: 异常类
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.exception;

/**
 * 销毁不允许异常
 * 当档案存在在借记录或冻结状态时抛出
 */
public class DestructionNotAllowedException extends RuntimeException {
    
    public DestructionNotAllowedException(String message) {
        super(message);
    }
    
    public DestructionNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}

