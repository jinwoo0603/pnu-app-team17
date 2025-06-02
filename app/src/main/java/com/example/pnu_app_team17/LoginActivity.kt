package com.example.pnu_app_team17

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val editId = findViewById<EditText>(R.id.editId)
        val editPw = findViewById<EditText>(R.id.editPw)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnSignIn.setOnClickListener {
            val id = editId.text.toString()
            val pw = editPw.text.toString()
            if (id.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Auth.signin(this, id, pw)
            if (Auth.isLoggedIn(this)) {
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish() // MainActivity로 돌아감
            }
        }

        btnSignUp.setOnClickListener {
            val id = editId.text.toString()
            val pw = editPw.text.toString()
            if (id.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Auth.signup(this, id, pw)
            if (Auth.isLoggedIn(this)) {
                Toast.makeText(this, "회원가입 완료", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
