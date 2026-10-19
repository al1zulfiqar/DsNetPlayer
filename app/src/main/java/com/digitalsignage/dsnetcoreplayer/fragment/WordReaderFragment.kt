package com.digitalsignage.dsnetcoreplayer.fragment



import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.aspose.words.Document

import com.aspose.words.SaveFormat
import com.digitalsignage.dsnetcoreplayer.constants.MediaType
import com.digitalsignage.dsnetcoreplayer.constants.ZoneType

import com.digitalsignage.dsnetcoreplayer.databinding.FragmentWordReaderBinding
import com.digitalsignage.dsnetcoreplayer.services.OfficeToPdfConverter


import java.io.File

class WordReaderFragment : Fragment() {
    private val TAG = "WordReaderFragment"
    private var _binding: FragmentWordReaderBinding? = null
    private val binding get() = _binding!!
    private val ARG_URL = "url"
    private val ARG_FILE_NAME = "docName"
    private val ARG_DOC_TYPE = "docType"

    private lateinit var docType:String
    private lateinit var officeFileName: String
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            officeFileName = it.getString(ARG_FILE_NAME)!!
            url = it.getString(ARG_URL)!!
            docType = it.getString(ARG_DOC_TYPE)!!
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            if(docType.equals(ZoneType.ZONE_DOCX.toString()) || docType.equals(MediaType.DOCX)){
                val filesDir = requireContext().getExternalFilesDir("DownloadedFiles")
                val officeFile = File(filesDir, officeFileName)
                if(officeFile.exists()) {
                    checkPDFVersion(officeFile)
                }else{

                }
            }
    }

    private fun checkPDFVersion(wordFile: File) {
        var pdfVersionOfFile = officeFileName.substringBeforeLast(".")
        pdfVersionOfFile = pdfVersionOfFile+".pdf"
        val filesDir = requireContext().getExternalFilesDir("DownloadedFiles")
        val pdfFile = File(filesDir,pdfVersionOfFile)
        if(pdfFile.exists()){
            loadPDF(pdfFile)
        }else {
            try {
                val onlineConverter = OfficeToPdfConverter()
                    onlineConverter.convertDocxToPdf(
                        url= url,
                        officeFile = wordFile,
                        onSuccess = { pdfFile ->
                            context?.let {
                                loadPDF(pdfFile)
                            }
                        },
                        onError = { error ->
                            context?.let {
                            }
                        }
                    )

            } catch (e: Exception) {
                e.printStackTrace()
                context?.let {
                    Toast.makeText(it, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadPDF(pdfFile: File) {
        binding.pdfView.fromFile(pdfFile).load()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
