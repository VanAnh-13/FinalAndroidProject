package com.example.healthylifehub.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.local.dao.ReminderHistoryDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.ReminderHistory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unified performance utilities for animations, caching, and lazy loading
 * Combines AnimationManager, DatabaseOptimizer, LazyLoadingManager, and LRUCache
 * Requirements: Performance optimization and smooth user experience
 */
public class PerformanceUtils {
    
    private static final String TAG = "PerformanceUtils";
    
    // Animation constants
    private static final long PROGRESS_ANIMATION_DURATION = 800;
    private static final long FADE_ANIMATION_DURATION = 300;
    private static final long SCALE_ANIMATION_DURATION = 200;
    
    // Haptic feedback patterns
    private static final long[] COMPLETION_PATTERN = {0, 50, 100, 50};
    private static final long[] ACTION_PATTERN = {0, 30};
    private static final long[] WARNING_PATTERN = {0, 100, 50, 100};
    
    // Cache constants
    private static final int BATCH_SIZE = 50;
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes
    private static final int PAGE_SIZE = 20;
    private static final int CACHE_SIZE = 100;
    
    private static PerformanceUtils instance;
    private final Context context;
    private final Vibrator vibrator;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    
    // Caches
    private final LRUCache<String, CachedData<List<Reminder>>> reminderCache = new LRUCache<>(20);
    private final LRUCache<String, CachedData<List<ReminderHistory>>> historyCache = new LRUCache<>(50);
    private final LRUCache<String, CachedData<Reminder>> singleReminderCache = new LRUCache<>(100);
    
    private PerformanceUtils(Context context) {
        this.context = context.getApplicationContext();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    public static synchronized PerformanceUtils getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceUtils(context);
        }
        return instance;
    }
    
    // ==================== ANIMATION METHODS ====================
    
    /**
     * Animate progress bar with color transition
     */
    public void animateProgressWithColor(ProgressBar progressBar, int fromProgress, int toProgress, 
                                       int colorFrom, int colorTo) {
        if (progressBar == null) return;
        
        // Animate progress
        ValueAnimator progressAnimator = ValueAnimator.ofInt(fromProgress, toProgress);
        progressAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        
        progressAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            progressBar.setProgress(animatedValue);
        });
        
        // Animate color transition
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(
            context.getColor(colorFrom), 
            context.getColor(colorTo)
        );
        colorAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
        colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        colorAnimator.addUpdateListener(animation -> {
            int animatedColor = (int) animation.getAnimatedValue();
            progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(animatedColor));
        });
        
        progressAnimator.start();
        colorAnimator.start();
        
        // Add haptic feedback for significant progress changes
        int progressDiff = Math.abs(toProgress - fromProgress);
        if (progressDiff >= 10) {
            if (toProgress >= 100) {
                provideHapticFeedback(HapticType.COMPLETION);
            } else if (progressDiff >= 25) {
                provideHapticFeedback(HapticType.ACTION);
            }
        }
    }
    
    /**
     * Animate button press effect
     */
    public void animateButtonPress(View view, Runnable onComplete) {
        if (view == null) return;
        
        // Scale down
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f);
        
        scaleDownX.setDuration(SCALE_ANIMATION_DURATION / 2);
        scaleDownY.setDuration(SCALE_ANIMATION_DURATION / 2);
        
        scaleDownX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Scale back up
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f);
                
                scaleUpX.setDuration(SCALE_ANIMATION_DURATION / 2);
                scaleUpY.setDuration(SCALE_ANIMATION_DURATION / 2);
                
                if (onComplete != null) {
                    scaleUpX.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            onComplete.run();
                        }
                    });
                }
                
                scaleUpX.start();
                scaleUpY.start();
            }
        });
        
        scaleDownX.start();
        scaleDownY.start();
        
        provideHapticFeedback(HapticType.ACTION);
    }
    
    /**
     * Fade in animation
     */
    public void fadeIn(View view, Runnable onComplete) {
        if (view == null) return;
        
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeAnimator.setDuration(FADE_ANIMATION_DURATION);
        fadeAnimator.setInterpolator(new DecelerateInterpolator());
        
        if (onComplete != null) {
            fadeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onComplete.run();
                }
            });
        }
        
        fadeAnimator.start();
    }
    
    /**
     * Fade out animation
     */
    public void fadeOut(View view, boolean hideAfter, Runnable onComplete) {
        if (view == null) return;
        
        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeAnimator.setDuration(FADE_ANIMATION_DURATION);
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (hideAfter) {
                    view.setVisibility(View.GONE);
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        
        fadeAnimator.start();
    }
    
    /**
     * Provide haptic feedback
     */
    public void provideHapticFeedback(HapticType type) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        try {
            switch (type) {
                case COMPLETION:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createWaveform(COMPLETION_PATTERN, -1));
                    } else {
                        vibrator.vibrate(COMPLETION_PATTERN, -1);
                    }
                    break;
                    
                case ACTION:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createWaveform(ACTION_PATTERN, -1));
                    } else {
                        vibrator.vibrate(ACTION_PATTERN, -1);
                    }
                    break;
                    
                case WARNING:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createWaveform(WARNING_PATTERN, -1));
                    } else {
                        vibrator.vibrate(WARNING_PATTERN, -1);
                    }
                    break;
                    
                case LIGHT:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(25, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(25);
                    }
                    break;
            }
        } catch (Exception e) {
            // Ignore vibration errors
        }
    }
    
    // ==================== DATABASE OPTIMIZATION METHODS ====================
    
    /**
     * Optimize database configuration
     */
    public void optimizeDatabase(AppDatabase database) {
        Log.d(TAG, "Optimizing database configuration");
        
        executorService.execute(() -> {
            try {
                database.getOpenHelper().getWritableDatabase().execSQL("PRAGMA journal_mode=WAL");
                database.getOpenHelper().getWritableDatabase().execSQL("PRAGMA cache_size=10000");
                database.getOpenHelper().getWritableDatabase().execSQL("PRAGMA foreign_keys=ON");
                database.getOpenHelper().getWritableDatabase().execSQL("PRAGMA synchronous=NORMAL");
                database.getOpenHelper().getWritableDatabase().execSQL("PRAGMA temp_store=MEMORY");
                
                Log.d(TAG, "✅ Database optimization completed");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to optimize database", e);
            }
        });
    }
    
    /**
     * Get reminders with caching
     */
    public void getRemindersOptimized(String userId, ReminderDao dao, DataCallback<List<Reminder>> callback) {
        String cacheKey = "reminders_" + userId;
        
        CachedData<List<Reminder>> cached = reminderCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "📱 Returning cached reminders for user: " + userId);
            callback.onSuccess(cached.getData());
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<Reminder> reminders = dao.getAllRemindersSync(userId);
                reminderCache.put(cacheKey, new CachedData<>(reminders));
                
                Log.d(TAG, "💾 Loaded and cached " + reminders.size() + " reminders for user: " + userId);
                callback.onSuccess(reminders);
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to load reminders", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCache() {
        reminderCache.evictAll();
        historyCache.evictAll();
        singleReminderCache.evictAll();
        Log.d(TAG, "🧹 All caches cleared");
    }
    
    // ==================== LAZY LOADING METHODS ====================
    
    /**
     * Create lazy loading LiveData
     */
    public static <T> MediatorLiveData<List<T>> createLazyLiveData(LiveData<List<T>> sourceData) {
        MediatorLiveData<List<T>> lazyData = new MediatorLiveData<>();
        
        lazyData.addSource(sourceData, new Observer<List<T>>() {
            private boolean isFirstLoad = true;
            
            @Override
            public void onChanged(List<T> data) {
                if (data == null) return;
                
                if (isFirstLoad) {
                    int batchSize = Math.min(PAGE_SIZE, data.size());
                    lazyData.setValue(data.subList(0, batchSize));
                    isFirstLoad = false;
                } else {
                    lazyData.setValue(data);
                }
            }
        });
        
        return lazyData;
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Haptic feedback types
     */
    public enum HapticType {
        COMPLETION, ACTION, WARNING, LIGHT
    }
    
    /**
     * Data callback interface
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
    
    /**
     * LRU Cache implementation
     */
    public static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;
        
        public LRUCache(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
        
        public int maxSize() {
            return maxSize;
        }
        
        public void evictAll() {
            clear();
        }
    }
    
    /**
     * Cached data with expiry
     */
    private static class CachedData<T> {
        private final T data;
        private final long timestamp;
        
        public CachedData(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public T getData() {
            return data;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_TIME;
        }
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        Log.d(TAG, "Shutting down PerformanceUtils");
        executorService.shutdown();
        clearAllCache();
    }
}