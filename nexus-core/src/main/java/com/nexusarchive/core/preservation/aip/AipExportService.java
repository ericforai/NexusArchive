// Input: Archive ID, OutputStream
// Output: void (streamed zip)
// Pos: NexusCore preservation/aip
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.aip;

import java.io.OutputStream;

/**
 * AIP (Archival Information Package) Export Service
 * Generates preservation-ready ZIP packages
 */
public interface AipExportService {
    
    /**
     * Export AIP package as ZIP stream
     * @param archiveId Archive ID to export
     * @param outputStream Output stream to write ZIP data to
     */
    void exportAip(String archiveId, OutputStream outputStream);
}
