package com.dandytek.sms_blocker.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dandytek.sms_blocker.utils.DatabaseAccessHelper;
import com.dandytek.sms_blocker.utils.DatabaseAccessHelper.Contact;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collection;
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

        // Access a Cloud Firestore instance from your Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("testing","test firestore");


        Context context = getApplicationContext();



        final String return_data;


       FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
//        db.setFirestoreSettings(settings);




// The default cache size threshold is 100 MB. Configure "setCacheSizeBytes"
// for a different threshold (minimum 1 MB) or set to "CACHE_SIZE_UNLIMITED"
// to disable clean-up.

        settings = new FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);


        final CollectionReference spamRef = db.collection("spam_sms");

        final DatabaseAccessHelper dtb = DatabaseAccessHelper.getInstance(context);



        db.collection("spam_sms").whereEqualTo("type", "blacklist")
               .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("firestore listen error", "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {

                           // Query query = spamRef.whereArrayContains("type", "blacklist");
                            Map<String, Object> test_data = new HashMap<>();
                            test_data = dc.getDocument().getData();
                            Log.d("firestore key val",test_data.get("sender").toString());
                            String person = String.valueOf(test_data.get("sender"));
                            String sms_number = person;

                            dtb.addContact(Contact.TYPE_FS_BLACK_LIST, person,sms_number);

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



       /* db.collection("spam_sms")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("firebase doc listen failed", "Listen failed.", e);
                            return;
                        }

                        List<String> cities = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            if (doc.get("name") != null) {
                                cities.add(doc.getString("name"));
                            }
                            Log.d("firestore tag name: ",doc.getData().values().toString());
                        }

                        List<DocumentSnapshot> change = value.getDocuments();
                        Log.d("firestore listen check", String.valueOf(change));
                    }
                }); */




      /*  db.collection("spam_sms")
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
                               // Log.d("firestore L C", "New tag:" + change.getDocument().getData().values());
                            }

                            String source = querySnapshot.getMetadata().isFromCache() ?
                                    "local cache" : "server";
                            String cache_data = querySnapshot.getDocuments().toString();

                          //  Log.d("firestore Cache", "Data fetched from " + source + cache_data);
                        }

                    }
                }); */



        final CollectionReference collRef = db.collection("spam_sms");
       // final DocumentReference docRef = db.collection("tag").document("SpamList");
        collRef.document().addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                  //  Log.w("firestore listen failed", "Listen failed.", e);
                    return;
                }

                String source = snapshot != null && snapshot.getMetadata().hasPendingWrites()
                        ? "Server" : "Local";

                if (snapshot != null && snapshot.exists()) {
                    Map<String, Object> docData = new HashMap<>();
                    Collection<Object> x = snapshot.getData().values();
                    docData = snapshot.getData();
                    String gg = String.valueOf(x);
                    Object value = snapshot.getData().values();
                   // ArrayList<String> listOfValues = new ArrayList<String>(x);

                    //  ArrayList hh = (ArrayList) value;
                  //  ArrayList<String> value_data = new ArrayList<>((HashMap<String, Object>) snapshot.getData()).value_data();


                    // return_data = gg;
                    // firestore_data[0] = String.valueOf(x);
                    Log.d("firestore cache", source + " data: " + snapshot.getData().values());
                 //   Log.d("firestore cache data type", source + " data: " + snapshot.getData().values().getClass().getName());
                 //   Log.d("firestore coll",String.valueOf(x));
                //    Log.d("firestore String",gg);
                 //   Log.d("firestore object:",value.toString());
                 //   Log.d("firestore array:",hh.toString());
                //    Log.d("firestore hashmap",docData.toString());


                 /*   if(value instanceof List) {
                        List<Object> values = (List<Object>) value;
                        // do your magic with values

                        for (int i = 0; i < values.size(); i++){
                            Log.d("firestore list: ",String.valueOf(i) + " "+ values.listIterator(i));
                        }
                    }
                    else {
                        // handle other possible types
                        Log.d("firestore list error","error");
                    } */

                  /*  for (int i = 0; i < listOfValues.size(); i++){
                        Log.d("firestore list: ",String.valueOf(i) + " "+ listOfValues.listIterator(i));
                    } */



                } else {
                   // Log.d("firestore cache null", source + " data: null");
                }
            }
        });



     //   String fromPath = db.collection("tag").document("dhN31FezLbNKXluS7hFB").getPath();

      //  String toPath = db.collection("tag").document("SpamList").getPath();

       // DocumentReference fPath = db.collection("tag").document("dhN31FezLbNKXluS7hFB");

       // DocumentReference tPath = db.collection("tag").document("SpamList");
       // CollectionReference tPath = db.collection("tag");



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
