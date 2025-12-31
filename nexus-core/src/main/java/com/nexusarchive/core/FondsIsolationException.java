// Input: RuntimeException
// Output: Fonds isolation error
// Pos: NexusCore isolation
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

public class FondsIsolationException extends RuntimeException {
    public FondsIsolationException(String message) {
        super(message);
    }
}
