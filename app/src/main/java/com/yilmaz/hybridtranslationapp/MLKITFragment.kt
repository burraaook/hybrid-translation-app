package com.yilmaz.hybridtranslationapp

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
import androidx.core.widget.addTextChangedListener
import androidx.navigation.Navigation
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.yilmaz.hybridtranslationapp.databinding.FragmentMLKITBinding
import java.util.Locale

class MLKITFragment : Fragment() {

    private var _binding: FragmentMLKITBinding? = null
    private val binding get() = _binding!!
    private var currentText : String? = null
    private var sourceLanguage : String? = "en"
    private var targetLanguage : String? = "tr"
    private var translator : Translator? = null
    private var isAuto = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMLKITBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inflate the layout for this fragment
        return view
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

        for (locale in allLanguages) {
            val languageCode = locale.language
            //println(languageCode)
            supportedLanguages.add(languageCode)
        }

        // target spinner
        val spinner = view.findViewById<Spinner>(R.id.targetLanSpinner)
        val languagesArray = supportedLanguages.toTypedArray()

        languagesArray.sort()

        val arrayAdp =
            ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, languagesArray)
        _binding?.targetLanSpinner?.adapter = arrayAdp
        _binding?.targetLanSpinner?.onItemSelectedListener = object
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
        val sourceSpinner = view.findViewById<Spinner>(R.id.sourceSpinner)
        val sourceOptionsArray = ArrayList<String>()

        sourceOptionsArray.add("Auto-Detect")
        for (elem in languagesArray)
            sourceOptionsArray.add(elem)

        val sourceArrayAdp =
            ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, sourceOptionsArray)

        _binding?.sourceSpinner?.adapter = sourceArrayAdp
        _binding?.sourceSpinner?.onItemSelectedListener = object
            : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                println("source elem: ${sourceOptionsArray[position]} position: ${position}")
                // or check position 0
                if (sourceOptionsArray[position].equals("Auto-Detect")) {
                    isAuto = true
                    _binding?.identifiedLanguageText?.setText("Identifying ..")
                    identifyLanguage()
                    makeTranslation()
                }
                else {
                    isAuto = false
                    sourceLanguage = sourceOptionsArray[position]
                    _binding?.identifiedLanguageText?.setText("Language: ${sourceLanguage}")
                    makeTranslation()

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }


        }

        _binding?.reverseImage?.setOnClickListener {
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

        _binding?.translationInputText?.addTextChangedListener {
            if (isAuto)
                identifyLanguage()
            else {
                currentText = _binding?.translationInputText?.text.toString()
            }
        }

        _binding?.translationButton?.setOnClickListener {
            println("on button click")
            if (isAuto)
                identifyLanguage()
            else {
                currentText = _binding?.translationInputText?.text.toString()
            }
            makeTranslation()
        }

        binding.navigateButton2.setOnClickListener {
            val action = MLKITFragmentDirections
                .actionMlkitFragmentToTranslationListFragment()
            Navigation.findNavController(it).navigate(action)
        }
    }

    fun identifyLanguage () {
        println("auto identify start")
        val untranslatedText = _binding?.translationInputText?.text.toString()
        println("untranslated text: ${untranslatedText}")
        currentText = untranslatedText
        if (currentText == null || currentText?.isEmpty()!!) {
            if (isAuto)
                binding.identifiedLanguageText.setText("Identifying...")

            return
        }
        // identify the language
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(untranslatedText)
            .addOnSuccessListener { languageCode ->

                currentText = untranslatedText
                if (!languageCode.equals("und")) {
                    _binding?.identifiedLanguageText?.setText("Language (identified): ${languageCode}")
                    sourceLanguage = languageCode

                } else {
                    binding?.identifiedLanguageText?.setText("UNIDENTIFIED")
                }
            }
            .addOnFailureListener {
                _binding?.identifiedLanguageText?.text = "Cannot identify"
            }
    }

    fun makeTranslation () {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage.toString())
            .setTargetLanguage(targetLanguage.toString())
            .build()
        translator = Translation.getClient(options)

        if (currentText == null || currentText?.isEmpty()!!)
            return

        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener { translatedText ->
                println("current text: ${currentText.toString()}")
                Toast.makeText(view?.context, "Translating ...", Toast.LENGTH_LONG).show()
                val result = translator?.translate(currentText.toString())
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

}