// Input: Java 标准库、本地模块
// Output: ArchiveSearchRepository 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.repository.es;

// import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import com.nexusarchive.entity.es.ArchiveDocument;
import java.util.List;

// @Repository
public interface ArchiveSearchRepository { // extends ElasticsearchRepository<ArchiveDocument, String> {
    
    // List<ArchiveDocument> findByTitleOrOcrText(String title, String ocrText);
    
    // List<ArchiveDocument> findByArchiveCode(String archiveCode);
    
    // Mock methods if needed, or remove callers
    default void save(ArchiveDocument doc) {}
    default void deleteById(String id) {}
    default void deleteAll() {}
}
