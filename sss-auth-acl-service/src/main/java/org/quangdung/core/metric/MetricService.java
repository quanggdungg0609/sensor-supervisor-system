package org.quangdung.core.metric;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Enhanced MetricService using Micrometer
 * Provides metrics collection with pluggable registry support
 */
@ApplicationScoped
public class MetricService {

    private final Logger log;
    
    // Micrometer components
    private final MeterRegistry meterRegistry;

    @Inject
    public MetricService(MeterRegistry meterRegistry, Logger log) {
        this.meterRegistry = meterRegistry;
        this.log = log;
    }
    
    /**
     * Increments a counter metric by 1
     * @param name the name of the counter
     * @param tags optional tags for the metric
     */
    public void incrementCounter(String name, String... tags) {
        incrementCounter(name, 1.0, tags);
    }
    
    /**
     * Increments a counter metric by the specified amount
     * @param name the name of the counter
     * @param amount the amount to increment by
     * @param tags optional tags for the metric
     */
    public void incrementCounter(String name, double amount, String... tags) {

            try {
                Counter counter = Counter.builder(name)
                    .tags(parseTags(tags))
                    .register(meterRegistry);
                counter.increment(amount);
            } catch (Exception e) {
                log.warnf("Failed to increment counter %s: %s", name, e.getMessage());
            }
    }
    
    /**
     * Records a timer metric
     * @param name the name of the timer
     * @param duration the duration to record
     * @param unit the time unit of the duration
     * @param tags optional tags for the metric
     */
    public void recordTimer(String name, long duration, TimeUnit unit, String... tags) {

            try {
                Timer timer = Timer.builder(name)
                    .tags(parseTags(tags))
                    .register(meterRegistry);
                timer.record(duration, unit);
            } catch (Exception e) {
                log.warnf("Failed to record timer %s: %s", name, e.getMessage());
            }
    }
    
    /**
     * Times a runnable operation
     * @param name the name of the timer
     * @param runnable the operation to time
     * @param tags optional tags for the metric
     */
    public void timeOperation(String name, Runnable runnable, String... tags) {
            try {
                Timer timer = Timer.builder(name)
                    .tags(parseTags(tags))
                    .register(meterRegistry);
                timer.recordCallable(() -> {
                    runnable.run();
                    return null;
                });
            } catch (Exception e) {
                log.warnf("Failed to time operation %s: %s", name, e.getMessage());
                // Still execute the runnable even if timing fails
                runnable.run();
            }
    }
    
    /**
     * Times a supplier operation and returns its result
     * @param <T> the return type of the supplier
     * @param name the name of the timer
     * @param supplier the operation to time
     * @param tags optional tags for the metric
     * @return the result of the supplier
     */
    public <T> T timeOperation(String name, Supplier<T> supplier, String... tags) {
            try {
                Timer timer = Timer.builder(name)
                    .tags(parseTags(tags))
                    .register(meterRegistry);
                return timer.recordCallable(supplier::get);
            } catch (Exception e) {
                log.warnf("Failed to time operation %s: %s", name, e.getMessage());
                // Still execute the supplier even if timing fails
                return supplier.get();
            }
    }
    
    /**
     * Registers a gauge metric
     * @param name the name of the gauge
     * @param value the value supplier for the gauge
     * @param tags optional tags for the metric
     */
    public void registerGauge(String name, Supplier<Number> value, String... tags) {
            try {
                Gauge.builder(name, value)
                    .tags(parseTags(tags))
                    .register(meterRegistry);
            } catch (Exception e) {
                log.warnf("Failed to register gauge %s: %s", name, e.getMessage());
            }

    }
    
    /**
     * Registers a gauge metric for an object
     * @param <T> the type of the object to gauge
     * @param name the name of the gauge
     * @param obj the object to gauge
     * @param valueFunction function to extract numeric value from the object
     * @param tags optional tags for the metric
     */
    public <T> void registerGauge(String name, T obj, java.util.function.ToDoubleFunction<T> valueFunction, String... tags) {

            try {
                Gauge.builder(name, obj, valueFunction)
                    .tags(parseTags(tags))
                    .register(meterRegistry);
            } catch (Exception e) {
                log.warnf("Failed to register gauge %s: %s", name, e.getMessage());
            }
    }
    
    /**
     * Gets the MeterRegistry if available
     * @return Optional containing the MeterRegistry if present
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    /**
     * Parses tag strings into Micrometer Tags
     * @param tags array of tag strings in format "key=value"
     * @return Tags object
     */
    private Tags parseTags(String... tags) {
        if (tags == null || tags.length == 0) {
            return Tags.empty();
        }
        
        Tags result = Tags.empty();
        for (String tag : tags) {
            if (tag != null && tag.contains("=")) {
                String[] parts = tag.split("=", 2);
                if (parts.length == 2) {
                    result = result.and(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return result;
    }
}
