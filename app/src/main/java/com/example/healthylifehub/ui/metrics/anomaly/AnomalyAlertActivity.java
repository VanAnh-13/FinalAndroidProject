package com.example.healthylifehub.ui.metrics.anomaly;

import android.content.Intent;
import android.widget.Toast;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityAnomalyAlertBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnomalyAlertActivity extends BaseActivity<ActivityAnomalyAlertBinding> {

    public static final String EXTRA_METRIC_TYPE = "metric_type";
    public static final String EXTRA_METRIC_VALUE = "metric_value";
    public static final String EXTRA_ALERT_TITLE = "alert_title";
    public static final String EXTRA_ALERT_DESC = "alert_desc";
    public static final String EXTRA_NORMAL_RANGE = "normal_range";
    
    private String metricType;
    private String metricValue;
    private String alertTitle;
    private String alertDesc;
    private String normalRange;

    public AnomalyAlertActivity() {
        super(ActivityAnomalyAlertBinding::inflate);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        metricType = intent.getStringExtra(EXTRA_METRIC_TYPE);
        metricValue = intent.getStringExtra(EXTRA_METRIC_VALUE);
        alertTitle = intent.getStringExtra(EXTRA_ALERT_TITLE);
        alertDesc = intent.getStringExtra(EXTRA_ALERT_DESC);
        normalRange = intent.getStringExtra(EXTRA_NORMAL_RANGE);

        if (metricType == null) metricType = "heart_rate";
        if (metricValue == null) metricValue = "145";
        if (alertTitle == null) alertTitle = getString(R.string.high_heart_rate_detected);
        if (alertDesc == null) alertDesc = getString(R.string.high_heart_rate_desc);
        if (normalRange == null) normalRange = "Normal: 60-100 BPM";
    }

    @Override
    public void bindData() {
        getBinding().tvAlertTitle.setText(alertTitle);
        getBinding().tvAlertDescription.setText(alertDesc);
        
        String[] valueParts = parseMetricValue(metricValue);
        getBinding().tvAlertValue.setText(valueParts[0]);
        getBinding().tvAlertUnit.setText(valueParts[1]);
        
        getBinding().tvNormalRange.setText(normalRange);
        
        SimpleDateFormat sdf = new SimpleDateFormat("'Today at' h:mm a", Locale.getDefault());
        getBinding().tvAlertTime.setText(sdf.format(new Date()));
    }

    @Override
    public void setOnClick() {
        getBinding().ivClose.setOnClickListener(v -> finish());

        getBinding().llActionRest.setOnClickListener(v -> {
            Toast.makeText(this, "Opening rest and monitor guide", Toast.LENGTH_SHORT).show();
        });

        getBinding().llActionContact.setOnClickListener(v -> {
            Toast.makeText(this, "Opening healthcare contact", Toast.LENGTH_SHORT).show();
        });

        getBinding().btnContactDoctor.setOnClickListener(v -> {
            Toast.makeText(this, "Contacting doctor", Toast.LENGTH_SHORT).show();
        });

        getBinding().btnLearnMore.setOnClickListener(v -> {
            Toast.makeText(this, "Opening health information", Toast.LENGTH_SHORT).show();
        });

        getBinding().btnDismiss.setOnClickListener(v -> {
            finish();
        });
    }

    private String[] parseMetricValue(String value) {
        String[] result = new String[2];
        
        if (value.contains("/")) {
            result[0] = value.split(" ")[0];
            result[1] = value.substring(value.indexOf(" ") + 1);
        } else if (value.contains(" ")) {
            String[] parts = value.split(" ");
            result[0] = parts[0];
            result[1] = parts.length > 1 ? parts[1] : "";
        } else {
            result[0] = value;
            result[1] = getUnitForMetricType(metricType);
        }
        
        return result;
    }

    private String getUnitForMetricType(String type) {
        switch (type) {
            case "heart_rate":
                return "BPM";
            case "blood_sugar":
                return "mg/dL";
            case "blood_pressure":
                return "mmHg";
            case "weight":
                return "kg";
            case "temperature":
                return "°C";
            default:
                return "";
        }
    }
}