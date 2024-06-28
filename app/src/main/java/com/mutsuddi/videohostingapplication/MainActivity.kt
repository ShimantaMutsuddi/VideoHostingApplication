package com.mutsuddi.videohostingapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    private val READ_WRITE_REQUEST_CODE = 42
    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var recyclerView: RecyclerView
    //private lateinit var adapter: VideoMetadataAdapter
    //private var videoMetadataList: MutableList<VideoMetadata> = mutableListOf()

    private  val TAG = "MainActivity2"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

       /* recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        //adapter = VideoMetadataAdapter(videoMetadataList)
        recyclerView.adapter = adapter*/


        pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleZipFile(uri)
                    Log.d(TAG, "onCreate: ${uri.toString()} ")
                    //unzip(uri, filesDir)
                    // unZip(uri, targetDirectory.path)
                    //unzipfile(uri.toString(),targetDirectory.path)
                }
                /* result.data?.data?.let { uri ->
                     Log.d(TAG, "URI: $uri")

                     val contentResolver = applicationContext.contentResolver
                     var inputStream: InputStream? = null
                     try {
                         inputStream = contentResolver.openInputStream(uri)
                         // Use inputStream as needed
                         // For example, pass it to your unzip function
                         unZip(uri, targetDirectory)
                     } catch (e: IOException) {
                         // Handle error opening InputStream
                         Log.e(TAG, "Error opening InputStream: ${e.message}")
                     } finally {
                         inputStream?.close()
                     }
                 }*/
            }
        }


        val btnPickFile = findViewById<Button>(R.id.btn_pick_file)
        btnPickFile.setOnClickListener {
            requestPermission()
        }

    }
    private fun requestPermission() {
        val readPermission = Manifest.permission.READ_EXTERNAL_STORAGE
        val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        val readPermissionGranted = ContextCompat.checkSelfPermission(this, readPermission) == PackageManager.PERMISSION_GRANTED
        val writePermissionGranted = ContextCompat.checkSelfPermission(this, writePermission) == PackageManager.PERMISSION_GRANTED

        if (!readPermissionGranted || !writePermissionGranted) {
            val permissionsToRequest = mutableListOf<String>()
            if (!readPermissionGranted) {
                permissionsToRequest.add(readPermission)
            }
            if (!writePermissionGranted) {
                permissionsToRequest.add(writePermission)
            }

            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), READ_WRITE_REQUEST_CODE)
        } else {
            pickZipFile()
        }
    }

    private fun pickZipFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        pickFileLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_WRITE_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    pickZipFile()
                } else {
                    Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show()
                }
                return
            }

        }

    }

    private fun handleZipFile(uri: Uri) {

        val targetDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "unzippedVideos")
        //val targetDirectory = File("/storage/emulated/0/unzippedVideos")

        // Create the target directory if it doesn't exist
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
            if (!targetDirectory.exists()) {
                Log.e("handleZipFile", "Failed to create target directory")
                return
            }
        }

        val outputDir = File(targetDirectory, "unzipped_files")

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                Log.e("handleZipFile", "Failed to create output directory")
                return
            }
        }
        Toast.makeText(this, "UnZipped at :  ${outputDir.path}", Toast.LENGTH_LONG).show()

        //unzipfile(uri.toString(), outputDir.path)
        //unzipfile(uri.toString(), targetDirectory.path)

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
                var entry: ZipEntry?
                val buffer = ByteArray(1024)



                while (zipInputStream.nextEntry.also { entry = it } != null) {
                    entry?.let {
                        val file = File(outputDir, it.name)
                        FileOutputStream(file).use { outputStream ->
                            var count: Int
                            while (zipInputStream.read(buffer).also { count = it } != -1) {
                                outputStream.write(buffer, 0, count)
                            }
                        }

                        zipInputStream.closeEntry()

                        if (file.extension == "xml") {
                           // parseXmlFile(file)
                        } else if (file.extension == "mp4") {
                           // extractVideoInfo(file)
                        }
                    }
                }
                zipInputStream.close()
            }
        } catch (e: FileNotFoundException) {
            Log.e("handleZipFile", "File not found: ${e.message}")
        } catch (e: IOException) {
            Log.e("handleZipFile", "IOException: ${e.message}")
        }
    }

    /*private fun parseXmlFile(file: File) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var eventType = parser.eventType
            var videoId = ""
            var title = ""
            var artist = ""
            var location = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (name) {
                            "VideoId" -> videoId = parser.nextText()
                            "Title" -> title = parser.nextText()
                            "Artist" -> artist = parser.nextText()
                            "Location" -> location = parser.nextText()
                        }
                    }
                }
                eventType = parser.next()
            }

            videoMetadataList.add(VideoMetadata(videoId, title, artist, location, "", "", file.name))
            adapter.notifyDataSetChanged()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractVideoInfo(file: File) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val resolution = "${width}x${height}"
        val fileSize = "${file.length() / (1024 * 1024)} MB"

        videoMetadataList.find { it.fileName == file.name }?.apply {
            this.resolution = resolution
            this.fileSize = fileSize
        }
        adapter.notifyDataSetChanged()
        retriever.release()
    }*/
}