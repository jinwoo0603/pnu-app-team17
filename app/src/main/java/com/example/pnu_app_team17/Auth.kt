package com.example.pnu_app_team17

import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.widget.Toast

//로그인 회원가입 기능
object Auth {
    //Auth.signup(this, "id", "pw")
    fun signup(context: Context, id: String, pw: String) {
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("id", id)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    Toast.makeText(context, "이미 존재하는 아이디입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val user = mapOf(
                        "id" to id,
                        "pw" to pw
                    )
                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener {
                            saveToPreferences(context, id)
                            Toast.makeText(context, "회원가입 완료", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "회원가입 실패", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
    }

    //Auth.signin(this, "id", "pw")
    fun signin(context: Context, id: String, pw: String) {
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("id", id)
            .whereEqualTo("pw", pw)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    saveToPreferences(context, id)
                    Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "계정이 존재하지 않거나 비밀번호가 틀립니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
    }

    //현재 로그인 상태인지 확인하는 메서드
    //Auth.isLoggedIn(this)
    //Auth.isLoggedIn(context.applicationContext)
    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE)
        return prefs.getString("currentUserId", null) != null
    }

    //현재 로그인된 아이디를 반환하는 메서드
    //Auth.currentId(this)
    //Auth.currentId(context.applicationContext)
    fun currentId(context: Context): String? {
        val prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE)
        return prefs.getString("currentUserId", null)
    }

    private fun saveToPreferences(context: Context, id: String) {
        val prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE)
        prefs.edit().putString("currentUserId", id).apply()
    }
}