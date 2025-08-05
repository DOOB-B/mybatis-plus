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
package com.baomidou.mybatisplus.core.toolkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Performance monitoring utility for MyBatis-Plus
 *
 * @author baomidou
 * @since 3.5.3
 */
public final class PerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    /**
     * Performance metrics storage
     */
    private static final ConcurrentHashMap<String, MetricData> METRICS = new ConcurrentHashMap<>();
    
    /**
     * Enable/disable performance monitoring
     */
    private static volatile boolean enabled = false;
    
    /**
     * Enable performance monitoring
     */
    public static void enable() {
        enabled = true;
        logger.info("Performance monitoring enabled");
    }
    
    /**
     * Disable performance monitoring
     */
    public static void disable() {
        enabled = false;
        logger.info("Performance monitoring disabled");
    }
    
    /**
     * Check if performance monitoring is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Start timing an operation
     *
     * @param operationName the name of the operation
     * @return a timer instance
     */
    public static Timer startTimer(String operationName) {
        if (!enabled) {
            return new NoOpTimer();
        }
        return new Timer(operationName);
    }
    
    /**
     * Record a metric
     *
     * @param name  metric name
     * @param value metric value
     */
    public static void recordMetric(String name, long value) {
        if (!enabled) {
            return;
        }
        METRICS.computeIfAbsent(name, k -> new MetricData()).record(value);
    }
    
    /**
     * Get performance report
     *
     * @return performance report as string
     */
    public static String getReport() {
        if (!enabled || METRICS.isEmpty()) {
            return "Performance monitoring is disabled or no metrics recorded";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("=== MyBatis-Plus Performance Report ===\n");
        
        METRICS.forEach((name, data) -> {
            report.append(String.format("%s:\n", name));
            report.append(String.format("  Count: %d\n", data.getCount()));
            report.append(String.format("  Total Time: %d ms\n", data.getTotalTime()));
            report.append(String.format("  Average Time: %.2f ms\n", data.getAverageTime()));
            report.append(String.format("  Min Time: %d ms\n", data.getMinTime()));
            report.append(String.format("  Max Time: %d ms\n", data.getMaxTime()));
            report.append("\n");
        });
        
        return report.toString();
    }
    
    /**
     * Clear all metrics
     */
    public static void clearMetrics() {
        METRICS.clear();
    }
    
    /**
     * Timer class for measuring operation duration
     */
    public static class Timer implements AutoCloseable {
        private final String operationName;
        private final long startTime;
        
        public Timer(String operationName) {
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
        }
        
        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            recordMetric(operationName, duration);
            
            if (duration > 100) { // Log slow operations
                logger.warn("Slow operation detected: {} took {} ms", operationName, duration);
            }
        }
    }
    
    /**
     * No-op timer for when monitoring is disabled
     */
    private static class NoOpTimer extends Timer {
        public NoOpTimer() {
            super("no-op");
        }
        
        @Override
        public void close() {
            // Do nothing
        }
    }
    
    /**
     * Metric data holder
     */
    private static class MetricData {
        private final LongAdder count = new LongAdder();
        private final AtomicLong totalTime = new AtomicLong();
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);
        
        public void record(long value) {
            count.increment();
            totalTime.addAndGet(value);
            
            long currentMin = minTime.get();
            while (value < currentMin && !minTime.compareAndSet(currentMin, value)) {
                currentMin = minTime.get();
            }
            
            long currentMax = maxTime.get();
            while (value > currentMax && !maxTime.compareAndSet(currentMax, value)) {
                currentMax = maxTime.get();
            }
        }
        
        public long getCount() {
            return count.sum();
        }
        
        public long getTotalTime() {
            return totalTime.get();
        }
        
        public double getAverageTime() {
            long count = this.count.sum();
            return count > 0 ? (double) totalTime.get() / count : 0.0;
        }
        
        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        public long getMaxTime() {
            return maxTime.get();
        }
    }
}