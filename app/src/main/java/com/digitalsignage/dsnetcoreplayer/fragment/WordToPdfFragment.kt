//package com.digitalsignage.dsnetcoreplayer.fragment
//
//
//import android.graphics.Color
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import com.digitalsignage.dsnetcoreplayer.R
//
//import java.io.File
//import java.io.FileInputStream
//
//
//
//class WordToPdfFragment : Fragment() {
//
//    private val fileName = "file.docx"
//    private val pdfFileName = "mypdf.pdf"
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_word_to_pdf, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val filesDir = requireContext().getExternalFilesDir("DownloadedFiles")
//        val docxFile = File(filesDir, fileName)
//        //val docxFile = File(requireContext().filesDir, fileName)
//        val pdfFile = File(requireContext().filesDir, pdfFileName)
//
//        if (docxFile.exists()) {
//            convertDocxToPdf(docxFile, pdfFile)
//            if (pdfFile.exists()) {
//                Log.e("WordToPdfFragment", "PDF file exists")
//                //showPdf(pdfFile)
//            } else {
//                Log.e("WordToPdfFragment", "PDF conversion failed!")
//            }
//        } else {
//            Log.e("WordToPdfFragment", "DOCX file not found!")
//        }
//    }
//
////    private fun convertDocxToPdf(inputFile: File, outputFile: File) {
////        try {
////            val document = XWPFDocument(FileInputStream(inputFile))
////            val pdfWriter = PdfWriter(outputFile)
////            val pdfDocument = PdfDocument(pdfWriter)
////            val documentPDF = Document(pdfDocument)
////
////            for (paragraph in document.paragraphs) {
////                val text = paragraph.text
////                val paragraphPDF = Paragraph(text)
////
////                // Apply styles if available
////                val runs = paragraph.runs
////                for (run in runs) {
////                    if (run.isBold) {
////                        paragraphPDF.setBold()
////                    }
////                    if (run.isItalic) {
////                        paragraphPDF.setItalic()
////                    }
////                    val color = run.color
////                    if (color != null) {
////                        try {
////                            // Convert color hex to RGB
////                            val rgbColor = Color.parseColor(color) // This will parse the color string
////                            val pdfColor = DeviceRgb(Color.red(rgbColor), Color.green(rgbColor), Color.blue(rgbColor))
////                            paragraphPDF.setFontColor(pdfColor)
////                        } catch (e: IllegalArgumentException) {
////                            Log.e("WordToPdfFragment", "Invalid color format: $color")
////                        }
////                    }
////                }
////
////                documentPDF.add(paragraphPDF)
////            }
////
////            documentPDF.close()
////            pdfDocument.close()
////
////            Log.d("WordToPdfFragment", "PDF saved successfully: ${outputFile.absolutePath}")
////        } catch (e: Exception) {
////            e.printStackTrace()
////            Log.e("WordToPdfFragment", "Error converting to PDF: ${e.message}")
////        }
////    }
//
//
//}