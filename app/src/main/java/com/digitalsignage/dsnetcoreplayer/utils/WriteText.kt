import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException

object WriteText {
    private const val FILENAME = "proof_of_play.txt"
    private const val TAG = "FileUtils"
//    fun writeToTextFile(fileName:String, context: Context, msg: String,updateLast:Boolean){
//        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
//
//
//        try {
//            FileWriter(file, true).use { writer ->
//                writer.append(msg+Utilities.getCurrentDateTime()+"\n")
//            }
//
//        } catch (e: IOException) {
//            Log.e(TAG, "Error writing proof of play data", e)
//        }
//    }
fun writeToTextFile(
    fileName: String,
    context: Context,
    msg: String,
    currentTime: String,
    updateLast: Boolean
) {
    if(PlayerManager.getPlayer()?.reportUptime==null || !PlayerManager.getPlayer()?.reportUptime!!){
        return
    }
    //val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
    val filesDir = context.getExternalFilesDir("ScreenTime")
    if (filesDir == null || (!filesDir.exists() && !filesDir.mkdirs())) {
        throw IOException("Failed to create directory for downloads")
    }
    val file = File(filesDir, fileName)
    if(!file.exists()){
        file.createNewFile()
    }
    val lines = file.readLines().toMutableList()
    try {
        if (updateLast) {
            // Read all lines
            if(lines.isEmpty()){
                file.writeText("ScreenStartTime:" + currentTime + "\n")
                return
            }
            if(lines[lines.lastIndex].contains("ScreenStartTime:")){
                FileWriter(file, true).use { writer ->
                    writer.append(msg + currentTime +"\n")
                }
                return
            }
            // If the file is not empty, replace the last line
            if (lines.isNotEmpty()) {
                lines[lines.lastIndex] = msg + currentTime

                // Write all lines back to the file
                file.writeText(lines.joinToString("\n") + "\n")
            }
        } else {
            if(msg.contains("ScreenEndTime:") && lines.isEmpty()) {
                FileWriter(file, true).use { writer ->
                    writer.append("ScreenStartTime:" + currentTime + "\n")
                }
            }else{
                FileWriter(file, true).use { writer ->
                    writer.append(msg + currentTime + "\n")
                }
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "Error writing proof of play data", e)
    }
}
    fun clearTextFile(fileName: String, context: Context) {
        //val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        val fileDir = context.getExternalFilesDir("ScreenTime")
        val file = File(fileDir, fileName)
        try {
            FileWriter(file, false).use { writer -> // Set `append` to `false` to overwrite the file
                writer.write("") // Write an empty string to clear the file
            }
            Log.d(TAG, "File cleared successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Error clearing the file", e)
        }
    }
    fun readFromTextFile(fileName: String, context: Context): List<String> {
        //val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        val fileDir = context.getExternalFilesDir("ScreenTime")
        val file = File(fileDir, fileName)
        val lines = mutableListOf<String>()

        try {
            if (!file.exists()) {
                Log.w(TAG, "File does not exist: ${file.absolutePath}")
                return lines
            }

            file.bufferedReader().use { reader ->
                lines.addAll(reader.readLines())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading proof of play data", e)
        }

        return lines
    }


    fun writeProofOfPlay(context: Context, playlistId: String, contentId: String, startTime: String, endTime: String, logType: Int, playerId: String) {
        val jObject = JSONObject().apply {
            put("PlaylistID", playlistId)
            put("ContentID", contentId)
            put("StartDate", startTime)
            put("EndDate", endTime)
            put("LogType", logType)
            put("PlayerID", playerId)
        }

        val file = File(context.getExternalFilesDir(null), FILENAME)
        try {
            FileWriter(file, true).use { writer ->
                writer.append(jObject.toString())
                writer.append("\n")
            }
            Log.e(TAG, "Data Wtitten"+ jObject.toString())
        } catch (e: IOException) {
            Log.e(TAG, "Error writing proof of play data", e)
        }
    }
    fun clearProofOfPlayFile(context: Context){
        val file = File(context.getExternalFilesDir(null),  FILENAME)
        if (file.exists()) {
            file.writeText("")
        }
    }
    fun readAndClearProofOfPlay(context: Context): JSONArray {
        val file = File(context.getExternalFilesDir(null),  FILENAME)
        val jsonArray = JSONArray()

        if (file.exists()) {
            try {
                file.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        jsonArray.put(JSONObject(line))
                    }
                }

            } catch (e: IOException) {
                Log.e(TAG, "Error reading proof of play data", e)
            }
        }

        return jsonArray
    }

}