package com.example.healthylifehub.patterns.command;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.repository.HealthMetricRepository;

/**
 * Command Pattern - Concrete Command
 * Command for adding a health metric
 */
public class AddHealthMetricCommand implements Command {
    private final HealthMetricRepository repository;
    private final HealthMetric metric;
    private String savedMetricId;

    public AddHealthMetricCommand(HealthMetricRepository repository, HealthMetric metric) {
        this.repository = repository;
        this.metric = metric;
    }

    @Override
    public void execute() {
        repository.saveHealthMetric(metric);
        savedMetricId = metric.getId();
    }

    @Override
    public void undo() {
        if (savedMetricId != null) {
            repository.deleteHealthMetric(savedMetricId);
        }
    }

    @Override
    public String getDescription() {
        return "Thêm chỉ số " + metric.getType();
    }
}
