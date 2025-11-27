package com.example.healthylifehub.patterns.command;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.repository.HealthMetricRepository;

import java.util.List;

/**
 * Command Pattern - Concrete Command
 * Command for deleting a health metric (with undo support)
 */
public class DeleteHealthMetricCommand implements Command {
    private final HealthMetricRepository repository;
    private final String metricId;
    private HealthMetric deletedMetric;

    public DeleteHealthMetricCommand(HealthMetricRepository repository, String metricId) {
        this.repository = repository;
        this.metricId = metricId;
    }

    @Override
    public void execute() {
        // Store the metric before deleting for undo (search in cached metrics)
        List<HealthMetric> cached = repository.getCachedMetrics(null);
        for (HealthMetric m : cached) {
            if (metricId.equals(m.getId())) {
                deletedMetric = m;
                break;
            }
        }
        repository.deleteHealthMetric(metricId);
    }

    @Override
    public void undo() {
        if (deletedMetric != null) {
            repository.saveHealthMetric(deletedMetric);
        }
    }

    @Override
    public String getDescription() {
        return "Xóa chỉ số " + (deletedMetric != null ? deletedMetric.getType() : metricId);
    }
}
