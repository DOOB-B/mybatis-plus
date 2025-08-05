/*
 * Copyright (c) 2011-2022, baomidou (jobob@qq.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.mybatisplus.test.performance;

import com.baomidou.mybatisplus.core.toolkit.PerformanceMonitor;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.test.h2.H2User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Performance test suite for MyBatis-Plus optimizations
 *
 * @author baomidou
 * @since 3.5.3
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceTestSuite {

    @BeforeAll
    void setup() {
        PerformanceMonitor.enable();
    }

    @Test
    void testReflectionPerformance() {
        System.out.println("=== Reflection Performance Test ===");
        
        // Test field access performance
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("field_access")) {
            for (int i = 0; i < 10000; i++) {
                ReflectionKit.getFieldList(H2User.class);
            }
        }
        
        // Test field value access
        H2User user = new H2User();
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("field_value_access")) {
            for (int i = 0; i < 10000; i++) {
                ReflectionKit.getFieldValue(user, "name");
            }
        }
        
        System.out.println(PerformanceMonitor.getReport());
        PerformanceMonitor.clearMetrics();
    }

    @Test
    void testStringOperationsPerformance() {
        System.out.println("=== String Operations Performance Test ===");
        
        String testString = "camelCaseString";
        
        // Test camel to underline conversion
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("camel_to_underline")) {
            for (int i = 0; i < 10000; i++) {
                StringUtils.camelToUnderline(testString);
            }
        }
        
        // Test underline to camel conversion
        String underlineString = "camel_case_string";
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("underline_to_camel")) {
            for (int i = 0; i < 10000; i++) {
                StringUtils.underlineToCamel(underlineString);
            }
        }
        
        // Test string matching
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("string_matching")) {
            for (int i = 0; i < 10000; i++) {
                StringUtils.matches("^[a-zA-Z]+$", testString);
            }
        }
        
        System.out.println(PerformanceMonitor.getReport());
        PerformanceMonitor.clearMetrics();
    }

    @Test
    void testTableInfoCachingPerformance() {
        System.out.println("=== Table Info Caching Performance Test ===");
        
        // Test table info initialization
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("table_info_init")) {
            for (int i = 0; i < 1000; i++) {
                TableInfoHelper.getTableInfo(H2User.class);
            }
        }
        
        // Test cached table info access
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("table_info_cached")) {
            for (int i = 0; i < 10000; i++) {
                TableInfoHelper.getTableInfo(H2User.class);
            }
        }
        
        System.out.println(PerformanceMonitor.getReport());
        PerformanceMonitor.clearMetrics();
    }

    @Test
    void testConcurrentPerformance() throws InterruptedException {
        System.out.println("=== Concurrent Performance Test ===");
        
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // Test concurrent reflection operations
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 1000; j++) {
                        ReflectionKit.getFieldList(H2User.class);
                        StringUtils.camelToUnderline("testString" + j);
                        TableInfoHelper.getTableInfo(H2User.class);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("concurrent_operations")) {
            latch.await(30, TimeUnit.SECONDS);
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println(PerformanceMonitor.getReport());
        PerformanceMonitor.clearMetrics();
    }

    @Test
    void testMemoryUsage() {
        System.out.println("=== Memory Usage Test ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection
        System.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform operations
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            objects.add(ReflectionKit.getFieldList(H2User.class));
            objects.add(StringUtils.camelToUnderline("testString" + i));
            objects.add(TableInfoHelper.getTableInfo(H2User.class));
        }
        
        // Force garbage collection again
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryUsed = finalMemory - initialMemory;
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + " MB");
        
        // Clear references
        objects.clear();
        System.gc();
    }

    @Test
    void testBatchOperationsPerformance() {
        System.out.println("=== Batch Operations Performance Test ===");
        
        List<H2User> users = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            H2User user = new H2User();
            user.setName("User" + i);
            users.add(user);
        }
        
        // Test batch field access
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("batch_field_access")) {
            for (H2User user : users) {
                ReflectionKit.getFieldValue(user, "name");
            }
        }
        
        // Test batch string operations
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("batch_string_operations")) {
            for (H2User user : users) {
                StringUtils.camelToUnderline(user.getName());
            }
        }
        
        System.out.println(PerformanceMonitor.getReport());
        PerformanceMonitor.clearMetrics();
    }
}