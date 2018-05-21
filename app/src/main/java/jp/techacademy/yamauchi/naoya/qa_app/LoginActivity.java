package jp.techacademy.yamauchi.naoya.qa_app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    EditText mEmailEditText;
    EditText mPasswordEditText;
    EditText mNameEditText;
    ProgressDialog mProgress;

    FirebaseAuth mAuth;
    OnCompleteListener<AuthResult> mCreateAccountListener;
    OnCompleteListener<AuthResult> mLoginListener;
    DatabaseReference mDatabaseReference;

    //アカウント作成時のフラグ立て
    boolean mIsCreateAccount = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        //FirebaseAuthのオブジェクト取得
        mAuth = FirebaseAuth.getInstance();

        //アカウント作成処理のListener
        mCreateAccountListener = new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                Log.d("ERROR", task.getResult().toString());
                if (task.isSuccessful()) {
                    //成功時、ログインを行う
                    String email = mEmailEditText.getText().toString();
                    String password = mPasswordEditText.getText().toString();
                    login(email, password);
                } else {
                    //失敗時はエラーを表示
                    View view = findViewById(android.R.id.content);
                    Snackbar.make(view, "アカウント作成に失敗しました", Snackbar.LENGTH_LONG).show();

                    mProgress.dismiss();
                }
            }
        };

        //ログイン処理のListener
        mLoginListener = new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                Log.d("ERROR", task.getResult().toString());
                if (task.isSuccessful()) {
                    //成功した場合
                    FirebaseUser user = mAuth.getCurrentUser();
                    DatabaseReference userRef = mDatabaseReference.child(Const.UsersPATH).child(user.getUid());

                    if (mIsCreateAccount) {
                        //作成時、表示名をFirebaseに保存する
                        String name = mNameEditText.getText().toString();

                        Map<String, String> data = new HashMap<String, String>();
                        data.put("name", name);
                        userRef.setValue(data);
                        saveName(name);
                    } else {
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                Map data = snapshot.getValue(Map.class);
                                saveName((String) data.get("name"));
                            }

                            @Override
                            public void onCancelled(DatabaseError firebaseError) {

                            }
                        });
                    }
                    //ダイアログを非表示にする
                    mProgress.dismiss();
                    finish();
                } else {
                    //失敗した場合はエラー
                    View view = findViewById(android.R.id.content);
                    Snackbar.make(view, "ログインに失敗しました", Snackbar.LENGTH_LONG).show();

                    mProgress.dismiss();
                }
            }
        };

        //UIの準備
        setTitle("ログイン");

        mEmailEditText = (EditText) findViewById(R.id.emailText);
        mPasswordEditText = (EditText) findViewById(R.id.passwordText);
        mNameEditText = (EditText) findViewById(R.id.nameText);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("処理中…");

        Button createButton = (Button) findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //キーボードが出ていたら閉じる
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                String email = mEmailEditText.getText().toString();
                String password = mPasswordEditText.getText().toString();
                String name = mNameEditText.getText().toString();

                if (email.length() != 0 && password.length() >= 6 && name.length() != 0) {
                    mIsCreateAccount = true;
                    createAccount(email, password);
                } else {
                    Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show();

                }
            }
        });

        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                String email = mEmailEditText.getText().toString();
                String password = mPasswordEditText.getText().toString();

                if (email.length() != 0 && password.length() >= 6) {
                    //フラグを落とす
                    mIsCreateAccount = false;
                    login(email, password);
                } else {
                    Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show();

                }
            }
        });
    }

    private void createAccount(String email, String password) {
        //プログレスダイアログの表示
        mProgress.show();

        //アカウント作成
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener);
    }

    private void login(String email, String password) {
        mProgress.show();

        //ログイン
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener);
    }

    private void saveName(String name) {
        //Preferenceに保存
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Const.NameKEY, name);
        editor.commit();
    }
}
