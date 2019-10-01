package com.dandytek.sms_blocker.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collection;

public class FirebaseSpamTagService extends Service {


    public FirebaseSpamTagService() {
    }


    @Override
    public void onCreate() {
        // Code to execute when the service is first created
        firebaseOfflineDataCollection();

    }




    public void firebaseOfflineDataCollection(){

        // Access a Cloud Firestore instance from your Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("testing","test firestore");

        final String return_data;


        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);




// The default cache size threshold is 100 MB. Configure "setCacheSizeBytes"
// for a different threshold (minimum 1 MB) or set to "CACHE_SIZE_UNLIMITED"
// to disable clean-up.

        settings = new FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);





        db.collection("tag")
                .addSnapshotListener(MetadataChanges.INCLUDE, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("firestore L e", "Listen error", e);
                            return;
                        }

                        for (DocumentChange change : querySnapshot.getDocumentChanges()) {
                            if (change.getType() == DocumentChange.Type.ADDED) {
                                Log.d("firestore L C", "New tag:" + change.getDocument().getData().values());
                            }

                            String source = querySnapshot.getMetadata().isFromCache() ?
                                    "local cache" : "server";
                            String cache_data = querySnapshot.getDocuments().toString();

                            Log.d("firestore Cache", "Data fetched from " + source + cache_data);
                        }

                    }
                });



        final DocumentReference docRef = db.collection("tag").document("SpamList");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("firestore listen failed", "Listen failed.", e);
                    return;
                }

                String source = snapshot != null && snapshot.getMetadata().hasPendingWrites()
                        ? "Server" : "Local";

                if (snapshot != null && snapshot.exists()) {
                    Collection<Object> x = snapshot.getData().values();
                    String gg = String.valueOf(x);
                   // return_data = gg;
                    // firestore_data[0] = String.valueOf(x);
                    Log.d("firestore cache", source + " data: " + snapshot.getData().values());
                    Log.d("firestore cache data type", source + " data: " + snapshot.getData().values().getClass().getName());
                    Log.d("firestore coll",String.valueOf(x));
                    Log.d("firestore String",gg);

                } else {
                    Log.d("firestore cache null", source + " data: null");
                }
            }
        });



        String fromPath = db.collection("tag").document("dhN31FezLbNKXluS7hFB").getPath();

        String toPath = db.collection("tag").document("SpamList").getPath();

        DocumentReference fPath = db.collection("tag").document("dhN31FezLbNKXluS7hFB");

        DocumentReference tPath = db.collection("tag").document("SpamList");



        // function to update one document to another
        // moveFirestoreDocument(fPath,tPath);


    }


    // Function to move one document's data to another document on same collection

    public void moveFirestoreDocument(final DocumentReference fromPath, final DocumentReference toPath) {
        fromPath.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null) {
                        toPath.set(document.getData())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("firestore doc write success", "DocumentSnapshot successfully written!");
                                        fromPath.delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d("firestore doc write deleted", "DocumentSnapshot successfully deleted!");
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.w("firestore doc write error", "Error deleting document", e);
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("firestore doc error", "Error writing document", e);
                                    }
                                });
                    } else {
                        Log.d("firestore doc", "No such document");
                    }
                } else {
                    Log.d("firestore failed exception", "get failed with ", task.getException());
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
