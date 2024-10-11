package com.example.practice4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practice4.databinding.ActivityMainBinding
import com.example.practice4.databinding.FragmentFilesBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.io.path.Path

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerBinding: FragmentFilesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        recyclerBinding = FragmentFilesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder = findViewById(R.id.camera_p)

        val folder = filesDir
        val f = File(folder, "photo")
        f.mkdir()
        val file = File(f,"dates.txt")
        if(!file.exists()) {
            file.createNewFile()
        }

        binding.photoButton.setOnClickListener{
            takePhoto(file)
            showPhotos(file)
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun takePhoto(file: File){
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        file.appendText(currentDate+"\n")
    }

    private fun showPhotos(file: File){
        val inputStream = FileInputStream(file)
        val bufferedReader =
            BufferedReader(InputStreamReader(inputStream))
        var line: String?
        val saved = ArrayList<String>()
        while (bufferedReader.readLine().also { line = it } != null) {
            saved.add(line.toString())
        }
        Log.d("path", saved.toString())
        bufferedReader.close()
        inputStream.close()
        val rv = recyclerBinding.savedFiles
        val adapter = MyAdapter(saved)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)
    }

    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider =
                cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this,
                    cameraSelector, preview)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions,
            grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
    }
}