package com.yilmaz.hybridtranslationapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView

class ListTranslationAdapter(val translatorList : ArrayList<String>)
    : RecyclerView.Adapter<ListTranslationAdapter.TranslatorHolder>() {

        class TranslatorHolder (itemView : View) : RecyclerView.ViewHolder(itemView) {

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslatorHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.recycler_row, parent, false)
        return TranslatorHolder(view)
    }

    override fun getItemCount(): Int {
        return translatorList.size
    }

    override fun onBindViewHolder(holder: TranslatorHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.recyclerRowText)
            .text = translatorList[position]

        holder.itemView.setOnClickListener {

            if (translatorList[position].equals("ML-KIT Translator")) {
                val action = TranslationListFragmentDirections
                    .actionTranslationListFragmentToMlkitFragment()
                Navigation.findNavController(it).navigate(action)
            }
            else if (translatorList[position].equals("Google Cloud API Translator")) {
                val action = TranslationListFragmentDirections
                    .actionTranslationListFragmentToGCFragment()
                Navigation.findNavController(it).navigate(action)
            }
            else if (translatorList[position].equals("Advanced ML-KIT Translator")) {
                val action = TranslationListFragmentDirections
                    .actionTranslationListFragmentToAdvancedMLKITFragment()
                Navigation.findNavController(it).navigate(action)
            }

        }
    }


}