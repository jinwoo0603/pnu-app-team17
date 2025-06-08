package com.example.pnu_app_team17

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.time.LocalDate

class ReceiptAddActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var selectedImageUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
            imageBitmap?.let {
                imageView.setImageBitmap(it)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val inputStream: InputStream? = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_add)

        imageView = findViewById(R.id.receiptImage)
        findViewById<TextView>(R.id.titleText).text = "소비 추가"

        findViewById<Button>(R.id.buttonCamera).setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        }

        findViewById<Button>(R.id.buttonGallery).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        // ✅ 임의 소비 추가 버튼 클릭 시 하드코딩 소비 추가
        // 나중에 삭제할거임
        findViewById<Button>(R.id.buttonDummyAdd).setOnClickListener {
            val dummySpendings = listOf(
                Triple(LocalDate.now(), "식비", 12000),
                Triple(LocalDate.now(), "카페", 4500),
                Triple(LocalDate.now(), "교통비", 3000),
                Triple(LocalDate.now(), "쇼핑", 29000),
                Triple(LocalDate.now(), "구독", 9900)
            )

            val prefs = getSharedPreferences("spending_prefs", MODE_PRIVATE)
            var currentTotal = prefs.getInt("total_spent", 0)

            dummySpendings.forEach { (date, category, amount) ->
                Sobi.add(this, date, category, amount)
                currentTotal += amount
            }

            prefs.edit().putInt("total_spent", currentTotal).apply()

            Toast.makeText(this, "${dummySpendings.size}건의 소비가 추가되었습니다.", Toast.LENGTH_SHORT).show()

            // 메인 화면으로 이동
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

    }
}
