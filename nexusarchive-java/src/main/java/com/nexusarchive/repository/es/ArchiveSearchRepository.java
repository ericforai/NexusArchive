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
