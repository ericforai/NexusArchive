// Input: Lombok、Java 标准库
// Output: ArchiveDocument 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity.es;

import lombok.Data;
// import org.springframework.data.annotation.Id;
// import org.springframework.data.elasticsearch.annotations.Document;
// import org.springframework.data.elasticsearch.annotations.Field;
// import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
// @Document(indexName = "archive_v1")
public class ArchiveDocument {

    // @Id
    private String id;

    // @Field(type = FieldType.Keyword)
    private String archiveCode;

    // @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    // @Field(type = FieldType.Keyword)
    private String fondsCode;

    // @Field(type = FieldType.Keyword)
    private String categoryCode;

    // @Field(type = FieldType.Integer)
    private Integer fiscalYear;

    // @Field(type = FieldType.Keyword)
    private String retentionPeriod;

    // @Field(type = FieldType.Date)
    private LocalDateTime createdTime;

    // @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String ocrText;  // OCR content

    // @Field(type = FieldType.Keyword)
    private String status;
}
