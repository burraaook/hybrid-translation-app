package com.yilmaz.hybridtranslationapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.get
import androidx.core.view.indices
import androidx.core.widget.addTextChangedListener
import androidx.navigation.Navigation
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.opencsv.CSVReaderBuilder
import com.yilmaz.hybridtranslationapp.databinding.FragmentAdvancedMLKITBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class AdvancedMLKITFragment : Fragment() {
    private var _binding: FragmentAdvancedMLKITBinding? = null
    private val binding get() = _binding!!
    private var currentText : String? = null
    private var sourceLanguage : String? = "en"
    private var targetLanguage : String? = "tr"
    private var translator : Translator? = null
    private var isAuto = true
    private val glossary = CustomGlossary()
    private lateinit var databaseHelper: GlossaryDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdvancedMLKITBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inflate the layout for this fragment
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        translator?.close()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // get set of all languages
        val allLanguages = Locale.getAvailableLocales()
        val supportedLanguages = HashSet<String>()
        databaseHelper = GlossaryDatabaseHelper(requireContext())

        for (locale in allLanguages) {
            val languageCode = locale.language
            //println(languageCode)
            supportedLanguages.add(languageCode)
        }

        // target spinner
        val spinner = binding.targetLanSpinnerAdvanced1
        val languagesArray = supportedLanguages.toTypedArray()

        languagesArray.sort()

        // initialize glossary
        //initializeGlossary()

        val arrayAdp =
            ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, languagesArray)
        binding.targetLanSpinnerAdvanced1?.adapter = arrayAdp
        binding.targetLanSpinnerAdvanced1?.onItemSelectedListener = object
            : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                println("selected language is: ${languagesArray[position]}")
                targetLanguage = languagesArray[position]
                if (isAuto)
                    identifyLanguage()
                else {
                    currentText = _binding?.translationInputText?.text.toString()
                }
                makeTranslation()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // source spinner
        val sourceSpinner = binding.sourceSpinnerAdvanced1
        val sourceOptionsArray = ArrayList<String>()

        sourceOptionsArray.add("Auto-Detect")
        for (elem in languagesArray)
            sourceOptionsArray.add(elem)

        val sourceArrayAdp =
            ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, sourceOptionsArray)

        binding.sourceSpinnerAdvanced1?.adapter = sourceArrayAdp
        binding.sourceSpinnerAdvanced1?.onItemSelectedListener = object
            : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                //println("source elem: ${sourceOptionsArray[position]} position: ${position}")
                // or check position 0
                if (sourceOptionsArray[position].equals("Auto-Detect")) {
                    isAuto = true
                    binding.identifiedLanguageTextAdvanced1.setText("Identifying ..")
                    identifyLanguage()
                    makeTranslation()
                }
                else {
                    isAuto = false
                    sourceLanguage = sourceOptionsArray[position]
                    binding.identifiedLanguageTextAdvanced1.setText("Language: ${sourceLanguage}")
                    makeTranslation()

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }


        }

        binding.reverseImageAdvanced1.setOnClickListener {
            val temp = sourceLanguage
            sourceLanguage = targetLanguage
            targetLanguage = temp

            _binding?.translationInputText?.setText(_binding
                ?.translationOutputText?.text.toString())

            // fix this (make constant time op)
            var pos : Int = 0
            for (lan in languagesArray)
            {
                if (lan.equals(targetLanguage))
                    spinner.setSelection(pos)
                ++pos
            }

            if (!sourceSpinner.selectedItemPosition.equals(0)) {

                pos = 0
                // find new source in spinner and set it
                for (lan in sourceOptionsArray)
                {
                    if (lan.equals(sourceLanguage))
                        sourceSpinner.setSelection(pos)
                    ++pos
                }
            }
            println("inside reverse")
            println("target = ${targetLanguage}")
            println("source = ${sourceLanguage}")

            if (isAuto)
                identifyLanguage()
            else {
                currentText = _binding?.translationInputText?.text.toString()
            }
            makeTranslation()
        }

        binding.translationInputText?.addTextChangedListener {
            if (isAuto)
                identifyLanguage()
            else {
                currentText = _binding?.translationInputText?.text.toString()
            }
        }

        binding.translationButtonAdvanced1.setOnClickListener {
            println("on button click")
            if (isAuto)
                identifyLanguage()
            else {
                currentText = _binding?.translationInputText?.text.toString()
            }
            makeTranslation()
        }

        binding.databaseButtonAdvanced.setOnClickListener {
            databaseHelper?.resetTranslations()
            Toast.makeText(context, "Updating..", Toast.LENGTH_LONG).show()
            storeInDatabase()
            //initializeGlossary()
        }

        binding.navigateButton3.setOnClickListener {
            val action = AdvancedMLKITFragmentDirections
                .actionAdvancedMLKITFragmentToTranslationListFragment()
            Navigation.findNavController(it).navigate(action)
        }

        // convert it to map like gc (supportedLanguages map) TODO
        // set spinners
        for (i in languagesArray.indices) {
            if (languagesArray.get(i).equals("tr")) {
                spinner.setSelection(i)
                targetLanguage = "tr"
                break
            }
        }
    }

    fun identifyLanguage () {
        println("auto identify start")
        val untranslatedText = _binding?.translationInputText?.text.toString()
        println("untranslated text: ${untranslatedText}")
        currentText = untranslatedText
        // identify the language
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(untranslatedText)
            .addOnSuccessListener { languageCode ->

                currentText = untranslatedText
                if (!languageCode.equals("und")) {
                    binding.identifiedLanguageTextAdvanced1.setText("Language (identified): ${languageCode}")
                    sourceLanguage = languageCode

                } else {
                    binding.identifiedLanguageTextAdvanced1.setText("Identifying..")
                }
            }
            .addOnFailureListener {
                binding.identifiedLanguageTextAdvanced1.text = "Cannot identify"
            }
    }

    fun makeTranslation () {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage.toString())
            .setTargetLanguage(targetLanguage.toString())
            .build()
        translator = Translation.getClient(options)
        println("trying translation")
        println("source: ${sourceLanguage}")
        println("target: ${targetLanguage}")

        if (currentText == null || currentText?.isEmpty()!!)
            return

        if (makeGlossaryTranslation())
            return

        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener { translatedText ->
                println("download successful")
                println("current text: ${currentText.toString()}")
                Toast.makeText(view?.context, "Translating ...", Toast.LENGTH_LONG).show()
                translator?.translate(currentText.toString())
                    ?.addOnSuccessListener {
                        println("success")
                        println(it)
                        _binding?.translationOutputText?.setText(it.toString())
                    }
                    ?.addOnFailureListener {
                        println("download fail")
                        it.printStackTrace()
                        Toast.makeText(context, "Translation Failed..", Toast.LENGTH_LONG).show()
                    }
            }
            ?.addOnFailureListener {
                println("download fail")
                it.printStackTrace()
                Toast.makeText(context, "Download Failed.. Check internet connection", Toast.LENGTH_LONG).show()
            }

        println("end translation")
        println(translator)
    }

    private fun makeGlossaryTranslation() : Boolean {

        // uses data structure

        /*val result = glossary.getTranslation(sourceLanguage!!,
          targetLanguage!!, currentText!!) ?: return false */

        // uses database
        val result = databaseHelper.getTranslation(sourceLanguage!!,
            targetLanguage!!, currentText!!) ?: return false
        println("result: " + result)

        binding.translationOutputText.setText(result)
        return true
    }

    private fun initializeGlossary() {
        /*        glossary.addElement("en", "tr",
                    "circle", "circleTR")
                glossary.addElement("en", "de",
                "circle", "circleDE")
                glossary.addElement("tr", "en",
                "daire", "daireEN")
                glossary.addElement("en", "ar",
                "daireEN", "daireAR")*/

        val inputStream = context?.resources?.openRawResource(R.raw.glossary)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val csvReader = CSVReaderBuilder(reader).build()
        var nextRecord: Array<String>?
        if (csvReader.readNext().also { nextRecord = it } != null) {
            val sourceLangCodes = nextRecord!!
            while (csvReader.readNext().also { nextRecord = it } != null) {
                for (i in sourceLangCodes.indices) {
                    val sourceLangCode = sourceLangCodes[i]
                    val sourceText = nextRecord?.get(i)

                    var targetLangCode = sourceLangCodes[(i + 1) % sourceLangCodes.size]
                    var targetText = nextRecord?.get((i + 1) % sourceLangCodes.size)

                    if (targetText == null || targetText!!.isEmpty()) {
                        // find the first non-empty target in a circular search
                        var searchIndex = (i + 2) % sourceLangCodes.size
                        while (searchIndex != i) {
                            val searchTargetText = nextRecord?.get(searchIndex)
                            if (!searchTargetText.isNullOrEmpty()) {
                                targetText = searchTargetText
                                targetLangCode = sourceLangCodes[searchIndex]
                                break
                            }
                            searchIndex = (searchIndex + 1) % sourceLangCodes.size
                        }
                    }

                    /*
                    println("source code: " + sourceLangCode)
                    println("source: " + sourceText)
                    println("target code: " + targetLangCode)
                    println("target: " + targetText) */
                    if (sourceLangCode.isNotEmpty() && targetLangCode.isNotEmpty() &&
                        sourceText != null && targetText != null && sourceText.isNotEmpty() &&
                        targetText.isNotEmpty()) {
                        //println("added to glossary\n")
                        glossary.addElement(sourceLangCode, targetLangCode, sourceText, targetText ?: "")
                    }
                }
            }
        }
        csvReader.close()
    }

    private fun storeInDatabase() {
        val inputStream = context?.resources?.openRawResource(R.raw.glossary)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val csvReader = CSVReaderBuilder(reader).build()
        var nextRecord: Array<String>?
        if (csvReader.readNext().also { nextRecord = it } != null) {
            val sourceLangCodes = nextRecord!!
            while (csvReader.readNext().also { nextRecord = it } != null) {
                for (i in sourceLangCodes.indices) {
                    val sourceLangCode = sourceLangCodes[i]
                    val sourceText = nextRecord?.get(i)

                    var targetLangCode = sourceLangCodes[(i + 1) % sourceLangCodes.size]
                    var targetText = nextRecord?.get((i + 1) % sourceLangCodes.size)

                    if (targetText == null || targetText!!.isEmpty()) {
                        // find the first non-empty target in a circular search
                        var searchIndex = (i + 2) % sourceLangCodes.size
                        while (searchIndex != i) {
                            val searchTargetText = nextRecord?.get(searchIndex)
                            if (!searchTargetText.isNullOrEmpty()) {
                                targetText = searchTargetText
                                targetLangCode = sourceLangCodes[searchIndex]
                                break
                            }
                            searchIndex = (searchIndex + 1) % sourceLangCodes.size
                        }
                    }


                    if (sourceLangCode.isNotEmpty() && targetLangCode.isNotEmpty() &&
                        sourceText != null && targetText != null && sourceText.isNotEmpty() &&
                        targetText.isNotEmpty()) {
                        // add it to database
                        databaseHelper.addElement(sourceLangCode, targetLangCode, sourceText, targetText)
                    }
                }
            }
        }
        csvReader.close()
    }
}