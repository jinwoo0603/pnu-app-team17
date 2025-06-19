package com.example.pnu_app_team17

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.InputStream

class ReceiptAddActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var capturedBitmap: Bitmap? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                capturedBitmap = it
                imageView.setImageBitmap(it)
                findViewById<Button>(R.id.buttonAddReceipt).isEnabled = true
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val inputStream: InputStream? = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            capturedBitmap = bitmap
            imageView.setImageBitmap(bitmap)
            findViewById<Button>(R.id.buttonAddReceipt).isEnabled = true
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

        findViewById<Button>(R.id.buttonAddReceipt).setOnClickListener {
            val bitmap = capturedBitmap
            if (bitmap == null) {
                Toast.makeText(this, "이미지를 먼저 선택하거나 촬영하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val resultMessage = GPT.ocr(this@ReceiptAddActivity, bitmap)
                Toast.makeText(this@ReceiptAddActivity, resultMessage, Toast.LENGTH_LONG).show()

                if (resultMessage.contains("저장")) {
                    val intent = Intent(this@ReceiptAddActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            }
        }

        findViewById<Button>(R.id.buttonExchangeInfo).setOnClickListener {
            startActivity(Intent(this, ExchangeActivity::class.java))
        }

        findViewById<Button>(R.id.buttonManualAdd).setOnClickListener {
            startActivity(Intent(this, ManualAddActivity::class.java))
        }
    }
}
