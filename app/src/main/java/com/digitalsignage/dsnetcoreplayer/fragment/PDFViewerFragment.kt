package com.digitalsignage.dsnetcoreplayer.fragment

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitalsignage.dsnetcoreplayer.R
import com.digitalsignage.dsnetcoreplayer.fragment.adapters.PdfPageAdapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.io.File

class PDFViewerFragment : Fragment() {
    private lateinit var pdfPagesRecyclerView: RecyclerView
    private var pdfPageAdapter: PdfPageAdapter? = null

    private val ARG_URL = "url"
    private val ARG_FILE_NAME = "docName"
    private val ARG_DOC_TYPE = "docType"

    private lateinit var docType: String
    private lateinit var officeFileName: String
    private lateinit var url: String

    // Coroutine scope for async operations
    private val fragmentScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            officeFileName = it.getString(ARG_FILE_NAME)!!
            url = it.getString(ARG_URL)!!
            docType = it.getString(ARG_DOC_TYPE)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfPagesRecyclerView = view.findViewById(R.id.pdfPagesRecyclerView)

        // Set up RecyclerView
        pdfPagesRecyclerView.layoutManager = LinearLayoutManager(context)

        // Load PDF file
        val filesDir = requireContext().getExternalFilesDir("DownloadedFiles")
        val pdfFile = File(filesDir, officeFileName)

        if (pdfFile.exists()) {
            openPdfFile(pdfFile)
        }
    }

    private fun openPdfFile(file: File) {
        // Clean up any existing adapter
        pdfPageAdapter?.cleanup()

        // Create new adapter
        pdfPageAdapter = PdfPageAdapter(file, fragmentScope)
        pdfPagesRecyclerView.adapter = pdfPageAdapter
    }

    override fun onDestroyView() {
        // Clean up resources
        pdfPageAdapter?.cleanup()
        super.onDestroyView()
    }

    override fun onDestroy() {
        // Cancel all coroutines when fragment is destroyed
        fragmentScope.cancel()
        super.onDestroy()
    }
}