package kr.osam2020.sum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    EditText gunbunEdit, nameEdit, pwEdit, pwchkEdit;
    Button registerButton;

    // Firebase
    FirebaseAuth auth;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Init
        gunbunEdit = findViewById(R.id.gunbunEdit);
        nameEdit = findViewById(R.id.nameEdit);
        pwEdit = findViewById(R.id.pwEdit);
        pwchkEdit = findViewById(R.id.pwchkEdit);
        registerButton = findViewById(R.id.registerButton);

        // Auth
        auth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username_text = nameEdit.getText().toString();
                String gunbun_text = gunbunEdit.getText().toString();
                String pass_text = pwEdit.getText().toString();
                String pass_chk_text = pwchkEdit.getText().toString();

                if (TextUtils.isEmpty(username_text) || TextUtils.isEmpty(gunbun_text) ||
                        TextUtils.isEmpty(pass_text) || TextUtils.isEmpty(pass_chk_text)) {
                    Toast.makeText(RegisterActivity.this, "양식을 채워주세요.", Toast.LENGTH_LONG).show();
                } else if (!pass_chk_text.equals(pass_text)) {
                    Toast.makeText(RegisterActivity.this, "입력하신 비밀번호가 서로 다릅니다.", Toast.LENGTH_LONG).show();
                } else {
                    registerNow(username_text, gunbun_text, pass_text);
                }
            }
        });
    }

    private void registerNow(final String username, final String gunbun, String pw) {
        auth.createUserWithEmailAndPassword(gunbun + "@" + getString(R.string.email), pw)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            String userid = firebaseUser.getUid();

                            myRef = FirebaseDatabase.getInstance().getReference("MyUsers").child(userid);

                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("id", userid);
                            hashMap.put("username", username);
                            hashMap.put("introduction", "");
                            hashMap.put("imageURL", "default");

                            myRef.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Intent i = new Intent(RegisterActivity.this, MainActivity.class);
                                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(i);
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "회원등록에 실패했습니다.  (네트워크 문제)", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(RegisterActivity.this, "회원등록에 실패했습니다.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
