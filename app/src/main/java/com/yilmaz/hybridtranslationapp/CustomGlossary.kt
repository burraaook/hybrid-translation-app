package com.yilmaz.hybridtranslationapp

class CustomGlossary {

    // a map to represent simple glossary
    // <language code, set<translation>>
    private val glossaryMap : HashMap<String, HashMap<Int, String>> =
        HashMap()
    private var numOfWordID = 0

    private val findIDMap : HashMap<String, Int> =
        HashMap()

    fun addElement(sourceCode: String, targetCode: String, elemSource: String, elemTarget: String): Boolean {
        if (elemSource.isEmpty() || elemTarget.isEmpty())
            return false
        if (sourceCode == targetCode)
            return false

        var sourceMap = glossaryMap[sourceCode]
        var targetMap = glossaryMap[targetCode]

        if (targetMap == null) {
            // create the target language
            targetMap = HashMap<Int, String>()
            glossaryMap[targetCode] = targetMap
        }

        if (sourceMap == null) {
            // create the source language
            sourceMap = HashMap()
            glossaryMap[sourceCode] = sourceMap
        }

        // Check if the source word already exists
        var sourceId = findIDMap[elemSource]
        if (sourceId == null) {
            numOfWordID++
            sourceId = numOfWordID
            findIDMap[elemSource] = sourceId
        }

        // Check if the target word already exists
        var targetId = findIDMap[elemTarget]
        if (targetId == null) {
            numOfWordID++
            targetId = numOfWordID
            findIDMap[elemTarget] = targetId
        }

        // Check if the source and target words are already linked
        if (sourceId != targetId) {
            return false
        }

        // Map ID with words
        sourceMap[sourceId] = elemSource
        targetMap[targetId] = elemTarget

        // Update glossaryMap
        glossaryMap[sourceCode] = sourceMap
        glossaryMap[targetCode] = targetMap

        return true
    }

    fun getTranslation (sourceCode: String, targetCode: String,
                        elem: String): String? {

        println("glos: ")
        println("source code->" + sourceCode)
        println("elem->" + elem)
        println("target code->" + targetCode)

        println(findIDMap)

        // get the source and target maps
        val sourceMap = glossaryMap.get(sourceCode) ?: return null
        val targetMap = glossaryMap.get(targetCode) ?: return null

        // get the id of the element
        val id = findIDMap.get(elem) ?: return null

        println("translation: " + targetMap.get(id))

        // check if this translation equals
        if (!sourceMap.get(id).equals(elem))
            return null

        println("glos success")
        // find its translation from source map
        val translation = targetMap.get(id)
        return translation
    }
}