package com.example.healthylifehub.data.livedata;

import android.util.Log;
import androidx.lifecycle.LiveData;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import androidx.annotation.Nullable;

/**
 * A LiveData class that observes a Firestore Query and automatically manages the listener registration.
 * The listener is active only when there are active observers.
 *
 * @param <T> The type of data held by this LiveData
 */
public abstract class FirestoreQueryLiveData<T> extends LiveData<T> {

    private static final String TAG = "FirestoreQueryLiveData";
    private final Query query;
    private ListenerRegistration listenerRegistration;
    private final EventListener<QuerySnapshot> eventListener = new MyEventListener();

    public FirestoreQueryLiveData(Query query) {
        this.query = query;
    }

    @Override
    protected void onActive() {
        super.onActive();
        Log.d(TAG, "onActive: Registering Firestore listener");
        listenerRegistration = query.addSnapshotListener(eventListener);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        Log.d(TAG, "onInactive: Removing Firestore listener");
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private class MyEventListener implements EventListener<QuerySnapshot> {
        @Override
        public void onEvent(@Nullable QuerySnapshot snapshot, @Nullable FirebaseFirestoreException e) {
            if (e != null) {
                Log.e(TAG, "Firestore listener error", e);
                return;
            }
            setValue(parseSnapshot(snapshot));
        }
    }

    /**
     * Subclasses must implement this to parse the QuerySnapshot into the desired type T.
     *
     * @param snapshot The QuerySnapshot from Firestore
     * @return The parsed data
     */
    protected abstract T parseSnapshot(QuerySnapshot snapshot);
}
