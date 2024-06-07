package com.example.learnmobilechating.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.learnmobilechating.databinding.ActivitySignInBinding;
import com.example.learnmobilechating.utilities.Constants;
import com.example.learnmobilechating.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        setListener();
    }

    private void setListener() {
        binding.textCreateNewAccount.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), SignUpActivity.class));});
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidInput()) {
                signIn();
            }
        });
    }

    private boolean isValidInput() {
        String emailOrPhone = binding.inputEmailOrPhone.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(emailOrPhone) || TextUtils.isEmpty(password)) {
            showToast("Please enter email/phone and password");
            return false;
        }
        return true;
    }

    private void signIn() {
        loading(true);
        String emailOrPhone = binding.inputEmailOrPhone.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL, emailOrPhone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        verifyPassword(task.getResult().getDocuments().get(0).getId(), password);
                    } else {
                        database.collection(Constants.KEY_COLLECTION_USER)
                                .whereEqualTo(Constants.KEY_PHONE, emailOrPhone)
                                .get()
                                .addOnCompleteListener(taskPhone -> {
                                    if (taskPhone.isSuccessful() && taskPhone.getResult() != null && !taskPhone.getResult().isEmpty()) {
                                        verifyPassword(taskPhone.getResult().getDocuments().get(0).getId(), password);
                                    } else {
                                        loading(false);
                                        showToast("Invalid email/phone or password");
                                    }
                                });
                    }
                });
    }

    private void verifyPassword(String userId, String password) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        String storedPassword = documentSnapshot.getString(Constants.KEY_PASSWORD);
                        if (TextUtils.equals(password, storedPassword)) {
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_USER_ID, userId);
                            preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                            preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            loading(false);
                            showToast("Invalid email/phone or password");
                        }
                    } else {
                        loading(false);
                        showToast("Unable to sign in");
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }
}