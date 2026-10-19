package com.digitalsignage.dsnetcoreplayer.fragment.adapters



import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.digitalsignage.dsnetcoreplayer.R
import com.github.chrisbanes.photoview.PhotoView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfPageAdapter(
    private val pdfFile: File,
    private val coroutineScope: CoroutineScope
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pageCount: Int = 0

    // Bitmap cache to avoid re-rendering pages
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    init {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                pageCount = pdfRenderer!!.pageCount

                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        // Check if we have this page in cache
        if (bitmapCache.containsKey(position)) {
            holder.pdfPageImageView.setImageBitmap(bitmapCache[position])
        } else {
            // Render the page
            renderPage(position, holder)
        }
    }

    private fun renderPage(position: Int, holder: PdfPageViewHolder) {
        if (pdfRenderer == null) return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Open the page
                val page = pdfRenderer!!.openPage(position)

                // Create a bitmap with appropriate dimensions
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                // Render the page onto the bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Close the page when done
                page.close()

                // Store in cache
                bitmapCache[position] = bitmap

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    holder.pdfPageImageView.setImageBitmap(bitmap)
//                    holder.pdfPageImageView.resetZoom()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = pageCount

    fun cleanup() {
        // Clear bitmap cache
        for (bitmap in bitmapCache.values) {
            bitmap.recycle()
        }
        bitmapCache.clear()

        // Close resources
        pdfRenderer?.close()
        fileDescriptor?.close()

        pdfRenderer = null
        fileDescriptor = null
    }

    class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pdfPageImageView: PhotoView = itemView.findViewById(R.id.pdfPageImageView)
    }
}