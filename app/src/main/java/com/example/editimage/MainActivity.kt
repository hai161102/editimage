package com.example.editimage

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.editimage.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    companion object {
        const val REQUEST_PERMISSION = 1234
        const val IMAGE_PATH = "image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.btnScreenshot.setOnClickListener {
            if (PermissionUtils.isStoragePermissionGranted(this)) {
                openEditActivity()
            } else {
                PermissionUtils.requestStoragePermission(this, REQUEST_PERMISSION)
            }
        }
    }

    private fun openEditActivity() {
        val intent = Intent(this@MainActivity, EditImageActivity::class.java)
        takeScreenshot()?.let {
//            intent.putExtra(IMAGE_PATH, it)
            EditImageActivity.bitmap = it
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestPermissionRespond(requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        requestPermissionRespond(requestCode)
    }

    private fun requestPermissionRespond(requestCode: Int) {
        if (requestCode == REQUEST_PERMISSION) {
            if (PermissionUtils.isStoragePermissionGranted(this)) {
                openEditActivity()
            } else {
                Toast.makeText(this@MainActivity, "Permission is not granted!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun takeScreenshot(): Bitmap? {
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val title: String = SimpleDateFormat("ddMMyyyy_HHmmss").format(Date())
            val mPath =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString() + "/" + title + ".jpg"


            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false
            return bitmap
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }
}