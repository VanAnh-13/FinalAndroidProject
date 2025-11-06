package com.example.healthylifehub.ui.medicines.list.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.example.healthylifehub.base.BaseAdapter;
import com.example.healthylifehub.base.BaseViewHolder;
import com.example.healthylifehub.data.model.Medicine;
import com.example.healthylifehub.databinding.ItemMedicineBinding;

import java.util.ArrayList;
import java.util.List;

public class MedicinesAdapter extends BaseAdapter<Medicine, ItemMedicineBinding> {

    private List<Medicine> medicines = new ArrayList<>();
    private final OnMedicineClickListener listener;

    public interface OnMedicineClickListener {
        void onMedicineClick(Medicine medicine);
        void onCreateReminderClick(Medicine medicine);
    }

    public MedicinesAdapter(OnMedicineClickListener listener) {
        super(ItemMedicineBinding::inflate);
        this.listener = listener;
    }

    @Override
    public void bindData(ItemMedicineBinding binding, Medicine medicine, int position) {
        binding.tvMedicineName.setText(medicine.getName());
        binding.tvDosageFrequency.setText(medicine.getDosage() + " - " + medicine.getFrequency());
        binding.tvInstructions.setText(medicine.getInstructions() + ". Bắt đầu: " + medicine.getStartDate());
    }

    @Override
    public void onItemClick(ItemMedicineBinding binding, Medicine medicine, int position) {
        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onMedicineClick(medicine);
            }
        });

        binding.btnCreateReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCreateReminderClick(medicine);
            }
        });
    }

    public void setMedicines(List<Medicine> medicines) {
        this.medicines = medicines;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BaseViewHolder<ItemMedicineBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicineBinding binding = ItemMedicineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BaseViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<ItemMedicineBinding> holder, int position) {
        Medicine medicine = medicines.get(position);
        ItemMedicineBinding binding = holder.getBinding();

        binding.tvMedicineName.setText(medicine.getName());
        binding.tvDosageFrequency.setText(medicine.getDosage() + " - " + medicine.getFrequency());
        binding.tvInstructions.setText(medicine.getInstructions() + ". Bắt đầu: " + medicine.getStartDate());

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onMedicineClick(medicine);
            }
        });

        binding.btnCreateReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCreateReminderClick(medicine);
            }
        });
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }
}
