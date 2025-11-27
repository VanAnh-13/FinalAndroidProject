package com.example.healthylifehub.patterns.command;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.repository.HealthMetricRepository;

import java.util.List;

/**
 * Command Pattern - Concrete Command
 * Command for updating a health metric (with undo support)
 */
public class UpdateHealthMetricCommand implements Command {
    private final HealthMetricRepository repository;
    private final HealthMetric newMetric;
    private HealthMetric oldMetric;

    public UpdateHealthMetricCommand(HealthMetricRepository repository, HealthMetric newMetric) {
        this.repository = repository;
        this.newMetric = newMetric;
    }

    @Override
    public void execute() {
        // Store old value for undo (search in cached metrics)
        List<HealthMetric> cached = repository.getCachedMetrics(null);
        for (HealthMetric m : cached) {
            if (newMetric.getId() != null && newMetric.getId().equals(m.getId())) {
                oldMetric = m;
                break;
            }
        }
        repository.saveHealthMetric(newMetric);
    }

    @Override
    public void undo() {
        if (oldMetric != null) {
            repository.saveHealthMetric(oldMetric);
        }
    }

    @Override
    public String getDescription() {
        return "Cập nhật chỉ số " + newMetric.getType();
    }
}
