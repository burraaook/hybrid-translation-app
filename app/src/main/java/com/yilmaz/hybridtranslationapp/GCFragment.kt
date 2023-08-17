package com.yilmaz.hybridtranslationapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.longrunning.OperationFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.v3.CreateGlossaryMetadata
import com.google.cloud.translate.v3.CreateGlossaryRequest
import com.google.cloud.translate.v3.DeleteGlossaryMetadata
import com.google.cloud.translate.v3.DeleteGlossaryRequest
import com.google.cloud.translate.v3.DeleteGlossaryResponse
import com.google.cloud.translate.v3.GcsSource
import com.google.cloud.translate.v3.GetSupportedLanguagesRequest
import com.google.cloud.translate.v3.Glossary
import com.google.cloud.translate.v3.GlossaryInputConfig
import com.google.cloud.translate.v3.GlossaryName
import com.google.cloud.translate.v3.LocationName
import com.google.cloud.translate.v3.TranslateTextGlossaryConfig
import com.google.cloud.translate.v3.TranslateTextRequest
import com.google.cloud.translate.v3.TranslateTextResponse
import com.google.cloud.translate.v3.TranslationServiceClient
import com.google.cloud.translate.v3.TranslationServiceSettings
import com.yilmaz.hybridtranslationapp.databinding.FragmentGCBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GCFragment : Fragment() {
    private var _binding: FragmentGCBinding? = null
    private val binding get() = _binding!!
    private var client : TranslationServiceClient? = null
    private val projectId = "quiet-dryad-394606"
    private var sourceLanguage = "en"
    private var targetLanguage = "tr"
    private val glossaryPairSet : HashSet<String> = HashSet<String>()
    // code, index map
    private val supportedLanguagesMap : HashMap<String, Int> = HashMap<String, Int>()
    private val definedInGlossarySet : HashSet<String> = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentGCBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inflate the layout for this fragment
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.visibility = View.INVISIBLE
        setGlossaryDefine()

        if (checkInternetConnection())
            setCredentials()
        else {
            println("no internet")
            Snackbar.make(view, "No internet connection", Snackbar.LENGTH_LONG).show()
        }
        val location = "us-central1"
        val parent: LocationName = LocationName.of(projectId, location)
        // add spinners

        // Gets a list of all supported languages

        val supportedLanguagesRequest = GetSupportedLanguagesRequest.newBuilder()
            .setParent(parent.toString())
            .build()
        val supportedLanguagesResponse = client!!.getSupportedLanguages(supportedLanguagesRequest)

        for (lan in supportedLanguagesResponse.languagesList) {
            supportedLanguagesMap.put(lan.languageCode, 0)
        }

        val codeArray = supportedLanguagesMap.keys.toTypedArray()
        codeArray.sort()
        for (i in codeArray.indices)
        {
            supportedLanguagesMap.put(codeArray[i], i)
            //System.out.printf(codeArray[i] + ",")
        }
        //println("")

        val spinnerTarget = view.findViewById<Spinner>(R.id.targetSpinner)
        val arrayAdp1 =
            ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, codeArray)
        binding.targetSpinner.adapter = arrayAdp1
        binding.targetSpinner.onItemSelectedListener = object
            : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                targetLanguage = codeArray.get(position)
                if (checkInternetConnection())
                    translateAdvanced()
                else {
                    println("no internet")
                    Snackbar.make(view!!, "No internet connection", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        val spinnerSource = view.findViewById<Spinner>(R.id.sourceSpinner)
        val arrayAdp2 =
            ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, codeArray)
        binding.sourceSpinner.adapter = arrayAdp2
        binding.sourceSpinner.onItemSelectedListener = object
            : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                sourceLanguage = codeArray[position]
                if (checkInternetConnection())
                    translateAdvanced()
                else {
                    println("no internet")
                    Snackbar.make(view!!, "No internet connection", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // select the spinners
        spinnerSource.setSelection(supportedLanguagesMap.get("en")!!)
        spinnerTarget.setSelection(supportedLanguagesMap.get("tr")!!)

        binding.translateButton.setOnClickListener {
            if (checkInternetConnection()) {
                println("internet ok")
                translateAdvanced()
            }
            else {
                println("no internet")
                Snackbar.make(view, "No internet connection", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.updateButton.setOnClickListener {
            if (checkInternetConnection())
                updateGlossary()
            else {
                println("no internet")
                Snackbar.make(view, "No internet connection", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.reverseImage.setOnClickListener {
            if (checkInternetConnection())
                reverseLanguages()
            else {
                println("no internet")
                Snackbar.make(view, "No internet connection", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.navigateButton1.setOnClickListener {
            val action = GCFragmentDirections
                .actionGCFragmentToTranslationListFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }

    private fun checkInternetConnection(): Boolean {
        val connectivityManager = view?.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun translateAdvanced() {

        val glossaryId = "glossary-en-tr"

        if (binding.translationInputText.text.toString().isEmpty()) {
            binding.translationOutputText.setText("")
            return
        }

        if (client != null) {

            val location = "us-central1"
            val parent: LocationName = LocationName.of(projectId, location)

            val glossaryName: GlossaryName = GlossaryName.of(projectId, location, glossaryId)
            val glossaryConfig: TranslateTextGlossaryConfig =
                TranslateTextGlossaryConfig.newBuilder().setGlossary(glossaryName.toString())
                    .build()
            listGlossaries()

            // check if element is inside glossary codes
            var isElementGlossary = glossaryPairSet.contains(sourceLanguage) && glossaryPairSet.contains(targetLanguage)
            //println(isElementGlossary)
            //println("set: " + glossaryPairSet)
            if (isElementGlossary) {
                val request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setSourceLanguageCode(sourceLanguage)
                    .setTargetLanguageCode(targetLanguage)
                    .addContents(binding.translationInputText.text.toString())
                    .setGlossaryConfig(glossaryConfig)
                    .build()
                val response: TranslateTextResponse = client!!.translateText(request)
                binding.translationOutputText.text = response.getGlossaryTranslations(0).translatedText
            }
            else {
                val request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setSourceLanguageCode(sourceLanguage)
                    .setTargetLanguageCode(targetLanguage)
                    .addContents(binding.translationInputText.text.toString())
                    .build()
                val response: TranslateTextResponse = client!!.translateText(request)
                binding.translationOutputText.text = response.getTranslations(0).translatedText
            }
        }
    }

    private fun createGlossary() {
        val glossaryId = "glossary-en-tr"
        val languageCodes: MutableList<String> = ArrayList()

        for (lan in supportedLanguagesMap.keys.toTypedArray())
        {
            languageCodes.add(lan)
        }
        val inputUri = "gs://burak-bucket2/glossary.csv"

        try {
            val location = "us-central1"
            val parent = LocationName.of(projectId, location)
            val glossaryName = GlossaryName.of(projectId, location, glossaryId)

            val languageCodesSet: Glossary.LanguageCodesSet =
                Glossary.LanguageCodesSet.newBuilder().addAllLanguageCodes(languageCodes).build()

            val gcsSource: GcsSource = GcsSource.newBuilder().setInputUri(inputUri).build()
            val inputConfig: GlossaryInputConfig =
                GlossaryInputConfig.newBuilder().setGcsSource(gcsSource).build()

            val glossary = Glossary.newBuilder()
                .setName(glossaryName.toString())
                .setLanguageCodesSet(languageCodesSet)
                .setInputConfig(inputConfig)
                .build()

            val request: CreateGlossaryRequest = CreateGlossaryRequest.newBuilder()
                .setParent(parent.toString())
                .setGlossary(glossary)
                .build()

            // Start an asynchronous request
            val future: OperationFuture<Glossary, CreateGlossaryMetadata> =
                client!!.createGlossaryAsync(request)

            println("Waiting for operation to complete...")
            val response = future.get()
            println("Created Glossary.")
            System.out.printf("Glossary name: %s\n", response.name)
            System.out.printf("Entry count: %s\n", response.entryCount)
            System.out.printf("Input URI: %s\n", response.inputConfig.gcsSource.inputUri)
        } catch (e : Exception) {
            println("exception thrown")
            e.printStackTrace()
        }
    }

    private fun updateGlossary() {
        val glossaryId = "glossary-en-tr"
        Toast.makeText(view?.context,"Adding Glossary ..", Toast.LENGTH_LONG).show()
        val glossaryName = GlossaryName.of(projectId, "us-central1", glossaryId)
        val request: DeleteGlossaryRequest =
            DeleteGlossaryRequest.newBuilder().setName(glossaryName.toString()).build()

        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        progressBar?.visibility = View.VISIBLE
        binding.updateButton.isEnabled = false
        binding.translateButton.isEnabled = false
        binding.sourceSpinner.isEnabled = false
        binding.targetSpinner.isEnabled = false
        binding.reverseImage.isEnabled = false
        binding.navigateButton1.isEnabled = false

        GlobalScope.launch {
            // first delete the glossary
            try {

                // Start an asynchronous request
                val future: OperationFuture<DeleteGlossaryResponse, DeleteGlossaryMetadata> =
                    client!!.deleteGlossaryAsync(request)

                println("Waiting for operation to complete...")
                val response: DeleteGlossaryResponse = future.get()
                System.out.format("Deleted Glossary: %s\n", response.getName())

                createGlossary()

                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    binding.updateButton.isEnabled = true
                    binding.translateButton.isEnabled = true
                    binding.sourceSpinner.isEnabled = true
                    binding.targetSpinner.isEnabled = true
                    binding.reverseImage.isEnabled = true
                    binding.navigateButton1.isEnabled = true
                }

            } catch (e: Exception) {
                e.printStackTrace()

                try {
                    println("creating")
                    createGlossary()
                    progressBar?.visibility = View.GONE
                    binding.updateButton.isEnabled = true
                    binding.translateButton.isEnabled = true
                    binding.sourceSpinner.isEnabled = true
                    binding.targetSpinner.isEnabled = true
                    binding.reverseImage.isEnabled = true
                    binding.navigateButton1.isEnabled = true
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }

    }

    private fun setCredentials () {
        val credentialsStream = view?.context?.resources?.openRawResource(R.raw.credentials)
        val credentials = GoogleCredentials.fromStream(credentialsStream)
        val credentialsProvider = FixedCredentialsProvider.create(credentials)

        client = TranslationServiceClient.create(
            TranslationServiceSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider).build())

        println(client)
    }

    private fun listGlossaries () {
        val parent = LocationName.of(projectId, "us-central1")

        val glossaryName = GlossaryName.of(projectId, "us-central1", "glossary-en-tr")
        val glossary = client!!.getGlossary(glossaryName)
        val pairs = glossary.languageCodesSet
        var counter = 0
        glossaryPairSet.clear()
        while (counter < pairs.languageCodesCount) {
            glossaryPairSet.add(pairs.getLanguageCodes(counter))
            counter++
        }
    }

    private fun reverseLanguages () {
        val temp = sourceLanguage
        sourceLanguage = targetLanguage
        targetLanguage = temp

        // swap languages
        binding.sourceSpinner.setSelection(supportedLanguagesMap[sourceLanguage]!!)
        binding.targetSpinner.setSelection(supportedLanguagesMap[targetLanguage]!!)

        // swap text
        binding.translationInputText.setText(binding.translationOutputText.text)

        translateAdvanced()
    }

    private fun setGlossaryDefine () {
        definedInGlossarySet.add("en")
        definedInGlossarySet.add("tr")
        definedInGlossarySet.add("ar")
    }
}