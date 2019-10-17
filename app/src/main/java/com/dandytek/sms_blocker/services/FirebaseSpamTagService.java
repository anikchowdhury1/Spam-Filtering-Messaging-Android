package com.dandytek.sms_blocker.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dandytek.sms_blocker.utils.DatabaseAccessHelper;
import com.dandytek.sms_blocker.utils.DatabaseAccessHelper.Contact;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseSpamTagService extends Service {


    public FirebaseSpamTagService() {
    }


    @Override
    public void onCreate() {
        // Code to execute when the service is first created
        firebaseOfflineDataCollection();

    }









    public void firebaseOfflineDataCollection(){

        Context context = getApplicationContext();
        final DatabaseAccessHelper fs_db = DatabaseAccessHelper.getInstance(context);

        if (fs_db != null)
        {

            // Access a Cloud Firestore instance from your Activity
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Log.d("testing","test firestore");

            final String return_data;


            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();



// The default cache size threshold is 100 MB. Configure "setCacheSizeBytes"
// for a different threshold (minimum 1 MB) or set to "CACHE_SIZE_UNLIMITED"
// to disable clean-up.

            settings = new FirebaseFirestoreSettings.Builder()
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            db.setFirestoreSettings(settings);


            final CollectionReference spamRef = db.collection("spam_sms");





            db.collection("spam_sms").whereEqualTo("type", "blacklist")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot snapshots,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w("firestore listen error", "listen:error", e);
                                return;
                            }

                            assert snapshots != null;
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {

                                // Query query = spamRef.whereArrayContains("type", "blacklist");
                                Map<String, Object> test_data = new HashMap<>();
                                test_data = dc.getDocument().getData();
                                Log.d("firestore key val",test_data.get("sender").toString());
                                String person = String.valueOf(test_data.get("sender"));
                                String sms_number = person;

                                Log.d("firestore type: ", String.valueOf(Contact.TYPE_FS_BLACK_LIST));
                                Log.d("firestore person",person);
                                Log.d("firestore sms_number",sms_number);

                                //assert fs_db != null;



                                if (person != null)
                                    fs_db.addContact(Contact.TYPE_FS_BLACK_LIST, person,sms_number);





                                // Log.d("firestore query: ", query.toString());

                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d("firestore data added", "New data: " + test_data);
                                        // Log.d("firestore added class", "New class: " + test_data.getClass().getName());
                                        break;
                                    case MODIFIED:
                                        Log.d("firestore data mod", "Modified data: " + test_data);
                                        break;
                                    case REMOVED:
                                        Log.d("firestore data removed", "Removed data: " + test_data);
                                        break;
                                }
                            }

                        }
                    });



        }




    }


    // Function to move one document's data to another document on same collection

  /*  public void moveFirestoreDocument(final DocumentReference fromPath, final DocumentReference toPath) {
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
    } */

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
