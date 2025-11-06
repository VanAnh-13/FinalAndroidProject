package com.example.healthylifehub.ui.metrics.list;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.MetricItem;
import com.example.healthylifehub.data.repository.MetricsRepository;
import java.util.List;

public class MetricsViewModel extends BaseViewModel {

    private final MediatorLiveData<List<MetricItem>> metrics = new MediatorLiveData<>();
    private final MetricsRepository metricsRepository;

    public MetricsViewModel(@NonNull Application application) {
        super(application);
        metricsRepository = new MetricsRepository();
        loadMetrics();
    }
    
    private void loadMetrics() {
        LiveData<List<MetricItem>> source = metricsRepository.loadMetrics();
        metrics.addSource(source, metrics::setValue);
    }
    
    public LiveData<List<MetricItem>> getMetrics() {
        return metrics;
    }
}
