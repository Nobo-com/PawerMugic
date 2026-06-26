package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class ResourceType {
    VIDEO, PDF, IMAGE
}

data class DownloadableItem(
    val id: String,
    val title: String,
    val type: ResourceType,
    val remoteUrl: String,
    val localFileName: String,
    val description: String,
    val originalSize: String
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val speedKbps: Double) : DownloadState()
    object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

class DownloadViewModel : ViewModel() {

    // 3 Curated real online downloadable test resources (Video, PDF, Image)
    val downloadableItems = listOf(
        DownloadableItem(
            id = "video_class_1",
            title = "কোটলিন প্রোগ্রামিং পরিচিতি ক্লাস (ভিডিও)",
            type = ResourceType.VIDEO,
            remoteUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            localFileName = "kotlin_intro.mp4",
            description = "এই ভিডিও ক্লাসটিতে কোটলিন প্রোগ্রামিংয়ের বেসিক ও অ্যান্ড্রয়েড অ্যাপ ডেভেলপমেন্টে এর গুরুত্ব নিয়ে বিস্তারিত আলোচনা করা হয়েছে।",
            originalSize = "৩.৪ MB"
        ),
        DownloadableItem(
            id = "video_class_2",
            title = "Big Buck Bunny 360p 10s (ভিডিও)",
            type = ResourceType.VIDEO,
            remoteUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
            localFileName = "big_buck_bunny.mp4",
            description = "বিগ বাক বানি ভিডিও।",
            originalSize = "১ MB"
        ),
        DownloadableItem(
            id = "video_class_3",
            title = "Unknown File (ভিডিও)",
            type = ResourceType.VIDEO,
            remoteUrl = "http://file-to-link-stevebotz-02.koyeb.app/193678/Unknown_File?hash=AgADRS",
            localFileName = "stevebotz_video.mp4",
            description = "স্টিভবটজ কয়ব অ্যাপ থেকে ভিডিও।",
            originalSize = "40 MB"
        ),
        DownloadableItem(
            id = "pdf_notes_1",
            title = "শিক্ষালায় স্পেশাল বাংলা ব্যাকরণ গাইড (পিডিএফ)",
            type = ResourceType.PDF,
            remoteUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            localFileName = "bangla_grammar.pdf",
            description = "বিসিএস ও অন্যান্য প্রতিযোগিতামূলক পরীক্ষার জন্য উপযোগী শিক্ষালায়ের এই স্পেশাল বাংলা ব্যাকরণ পিডিএফ লেকচার শিট।",
            originalSize = "১০ KB"
        ),
        DownloadableItem(
            id = "pdf_notes_2",
            title = "বিষয়ভিত্তিক গাইডলাইন ১ (পিডিএফ)",
            type = ResourceType.PDF,
            remoteUrl = "https://plgb.koyeb.app/dl/6a1c715d2f229e4f4468b221/%E0%A6%AC%E0%A6%BF%E0%A6%B7%E0%A7%9F%E0%A6%AD%E0%A6%BF%E0%A6%A4%E0%A7%8D%E0%A6%A4%E0%A6%BF%E0%A6%95%20%E0%A6%97%E0%A6%BE%E0%A6%87%E0%A6%A1%E0%A6%B2%E0%A6%BE%E0%A6%87%E0%A6%A8_1.pdf",
            localFileName = "subject_guideline_1.pdf",
            description = "বিষয়ভিত্তিক গাইডলাইন ১ পিডিএফ।",
            originalSize = "Unknown"
        ),
        DownloadableItem(
            id = "chart_img_1",
            title = "ডিজিটাল লার্নিং ড্যাশবোর্ড মানচিত্র (ছবি)",
            type = ResourceType.IMAGE,
            remoteUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=800&auto=format&fit=crop",
            localFileName = "learning_chart.jpg",
            description = "মোবাইল ও ওয়েব অ্যাপ্লিকেশনের লার্নিং ম্যাটেরিয়াল গ্রাফিক্যাল চার্ট, যা অফলাইনে পড়ার জন্য অত্যন্ত সহায়ক।",
            originalSize = "২৫০ KB"
        )
    )

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    fun initDownloadedStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val states = mutableMapOf<String, DownloadState>()
            val dir = getDownloadsDir(context)
            downloadableItems.forEach { item ->
                val file = File(dir, item.localFileName)
                if (file.exists() && file.length() > 0) {
                    states[item.id] = DownloadState.Completed
                } else {
                    states[item.id] = DownloadState.Idle
                }
            }
            _downloadStates.value = states
        }
    }

    fun getDownloadsDir(context: Context): File {
        val dir = File(context.filesDir, "secure_downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getLocalFile(context: Context, item: DownloadableItem): File {
        return File(getDownloadsDir(context), item.localFileName)
    }

    fun downloadFile(context: Context, item: DownloadableItem) {
        // Prevent concurrent downloading of the same item
        val currentStates = _downloadStates.value
        val currentState = currentStates[item.id]
        if (currentState is DownloadState.Downloading) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateItemState(item.id, DownloadState.Downloading(0f, 0.0))
            
            val localFile = File(getDownloadsDir(context), item.localFileName)
            
            // If file already exists, delete it first to re-download fresh
            if (localFile.exists()) {
                localFile.delete()
            }

            try {
                val url = URL(item.remoteUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("সার্ভার থেকে ত্রুটিপূর্ণ কোড এসেছে: ${connection.responseCode}")
                }

                val fileLength = connection.contentLength
                val inputStream = BufferedInputStream(connection.inputStream)
                val outputStream = FileOutputStream(localFile)

                val data = ByteArray(4096)
                var totalBytesRead = 0L
                var bytesRead: Int
                val startTime = System.currentTimeMillis()

                while (inputStream.read(data).also { bytesRead = it } != -1) {
                    outputStream.write(data, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Update progress and speed
                    if (fileLength > 0) {
                        val progress = totalBytesRead.toFloat() / fileLength
                        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                        val speedKbps = if (elapsedTime > 0) {
                            (totalBytesRead / 1024.0) / elapsedTime
                        } else {
                            0.0
                        }
                        
                        // Limit UI state updates to prevent excessive rendering stress
                        if (totalBytesRead % (16 * 1024) == 0L || totalBytesRead == fileLength.toLong()) {
                            updateItemState(item.id, DownloadState.Downloading(progress, speedKbps))
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()

                Log.d("DownloadViewModel", "Downloaded file: ${localFile.absolutePath}, Size: ${localFile.length()} bytes")
                updateItemState(item.id, DownloadState.Completed)

            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error downloading item: ${item.id}", e)
                if (localFile.exists()) {
                    localFile.delete() // Cleanup broken file
                }
                updateItemState(item.id, DownloadState.Failed(e.localizedMessage ?: "অজানা নেটওয়ার্ক ত্রুটি।"))
            }
        }
    }

    fun deleteFile(context: Context, item: DownloadableItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val localFile = File(getDownloadsDir(context), item.localFileName)
            if (localFile.exists()) {
                val deleted = localFile.delete()
                if (deleted) {
                    Log.d("DownloadViewModel", "Deleted file: ${localFile.absolutePath}")
                    updateItemState(item.id, DownloadState.Idle)
                } else {
                    updateItemState(item.id, DownloadState.Failed("ফাইলটি ডিলেট করা সম্ভব হয়নি।"))
                }
            } else {
                updateItemState(item.id, DownloadState.Idle)
            }
        }
    }

    private fun updateItemState(itemId: String, newState: DownloadState) {
        val current = _downloadStates.value.toMutableMap()
        current[itemId] = newState
        _downloadStates.value = current
    }
}
