package com.nexusarchive.tools;

// Input: Entity类 (com.nexusarchive.entity.*)
// Output: Schema列名文件 (target/*.columns.txt)
// Pos: 开发工具 - 数据库一致性检查
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.base.CaseFormat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity-Schema 验证工具
 * 用于扫描 Entity 类并生成预期的数据库列名列表
 */
public class SchemaValidator {

    public static void main(String[] args) throws Exception {
        System.out.println("[INFO] Starting Schema Validator...");
        
        String packageName = "com.nexusarchive.entity";
        List<Class<?>> entityClasses = getClasses(packageName);
        System.out.println("[INFO] Found " + entityClasses.size() + " classes in " + packageName);

        Path targetDir = Paths.get("target");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        int count = 0;
        for (Class<?> clazz : entityClasses) {
            // Skip ES entities
            if (clazz.getName().contains(".entity.es.")) {
                continue;
            }

            TableName tableNameAnnotation = clazz.getAnnotation(TableName.class);
            if (tableNameAnnotation != null) {
                String tableName = tableNameAnnotation.value();
                if (tableName == null || tableName.isEmpty()) {
                   tableName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clazz.getSimpleName());
                }
                
                Set<String> columns = extractColumns(clazz);
                writeColumnsToFile(tableName, columns);
                count++;
            }
        }
        System.out.println("[INFO] Generated column lists for " + count + " tables.");
    }

    private static Set<String> extractColumns(Class<?> clazz) {
        Set<String> columns = new TreeSet<>(); // Sorted for consistency
        
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null) {
                    if (!tableField.exist()) {
                        continue; 
                    }
                    if (!tableField.value().isEmpty()) {
                        columns.add(tableField.value());
                        continue;
                    }
                }
                
                // Default: camelCase -> snake_case
                String columnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
                columns.add(columnName);
            }
            current = current.getSuperclass();
        }
        return columns;
    }

    private static void writeColumnsToFile(String tableName, Set<String> columns) throws IOException {
        Path file = Paths.get("target", tableName + ".columns.txt");
        List<String> lines = new ArrayList<>(columns);
        Files.write(file, lines);
    }
    
    private static List<Class<?>> getClasses(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            // Handle space in path
            dirs.add(Paths.get(resource.toURI()).toFile());
        }
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
        }
        return classes;
    }
}
