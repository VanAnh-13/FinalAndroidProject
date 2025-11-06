package com.example.healthylifehub.ui.records.fragment.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseAdapter;
import com.example.healthylifehub.base.BaseViewHolder;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.databinding.ItemRecordTimelineBinding;

import java.util.ArrayList;
import java.util.List;

public class RecordsAdapter extends BaseAdapter<MedicalRecord, ItemRecordTimelineBinding> {

    private List<MedicalRecord> records = new ArrayList<>();
    private final OnRecordClickListener listener;

    public interface OnRecordClickListener {
        void onRecordClick(MedicalRecord record);
    }

    public RecordsAdapter(OnRecordClickListener listener) {
        super(ItemRecordTimelineBinding::inflate);
        this.listener = listener;
    }

    @Override
    public void bindData(ItemRecordTimelineBinding binding, MedicalRecord record, int position) {
        binding.tvRecordDate.setText(record.getDate());
        binding.tvRecordTitle.setText(record.getTitle());
        binding.tvRecordDescription.setText(record.getDescription());

        // Set icon based on record type
        int iconRes = getIconForType(record.getType());
        binding.ivRecordIcon.setImageResource(iconRes);

        // Show/hide attachment
        if (record.hasAttachment()) {
            binding.llAttachment.setVisibility(View.VISIBLE);
            binding.tvAttachmentName.setText(record.getAttachment());
            
            // Set attachment icon based on file type
            if (record.getAttachment().endsWith(".pdf")) {
                binding.ivAttachmentIcon.setImageResource(R.drawable.ic_pdf);
            } else {
                binding.ivAttachmentIcon.setImageResource(R.drawable.ic_image);
            }
        } else {
            binding.llAttachment.setVisibility(View.GONE);
        }

        // Hide timeline line for last item
        if (position == records.size() - 1) {
            binding.viewTimelineLine.setVisibility(View.GONE);
        } else {
            binding.viewTimelineLine.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(ItemRecordTimelineBinding binding, MedicalRecord record, int position) {
        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(record);
            }
        });
    }

    public void setRecords(List<MedicalRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BaseViewHolder<ItemRecordTimelineBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecordTimelineBinding binding = ItemRecordTimelineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BaseViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<ItemRecordTimelineBinding> holder, int position) {
        MedicalRecord record = records.get(position);
        ItemRecordTimelineBinding binding = holder.getBinding();

        binding.tvRecordDate.setText(record.getDate());
        binding.tvRecordTitle.setText(record.getTitle());
        binding.tvRecordDescription.setText(record.getDescription());

        // Set icon based on record type
        int iconRes = getIconForType(record.getType());
        binding.ivRecordIcon.setImageResource(iconRes);

        // Show/hide attachment
        if (record.hasAttachment()) {
            binding.llAttachment.setVisibility(View.VISIBLE);
            binding.tvAttachmentName.setText(record.getAttachment());
            
            // Set attachment icon based on file type
            if (record.getAttachment().endsWith(".pdf")) {
                binding.ivAttachmentIcon.setImageResource(R.drawable.ic_pdf);
            } else {
                binding.ivAttachmentIcon.setImageResource(R.drawable.ic_image);
            }
        } else {
            binding.llAttachment.setVisibility(View.GONE);
        }

        // Hide timeline line for last item
        if (position == records.size() - 1) {
            binding.viewTimelineLine.setVisibility(View.GONE);
        } else {
            binding.viewTimelineLine.setVisibility(View.VISIBLE);
        }

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private int getIconForType(MedicalRecord.RecordType type) {
        switch (type) {
            case CARDIOLOGY:
                return R.drawable.ic_heart;
            case CHECKUP:
                return R.drawable.ic_stethoscope;
            case ALLERGY:
                return R.drawable.ic_science;
            case VACCINATION:
                return R.drawable.ic_vaccine;
            default:
                return R.drawable.ic_medication;
        }
    }
}
