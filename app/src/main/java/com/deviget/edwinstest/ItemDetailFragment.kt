package com.deviget.edwinstest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.deviget.edwinstest.databinding.FragmentItemDetailBinding
import com.google.android.material.appbar.CollapsingToolbarLayout

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListFragment]
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
class ItemDetailFragment : Fragment() {
    private var postTitle: String? = null
    private var postSelfText: String? = null

    private lateinit var itemDetailTextView: TextView
    private var toolbarLayout: CollapsingToolbarLayout? = null

    private var _binding: FragmentItemDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            postTitle = it.getString(ARG_POST_TITLE)
            postSelfText = it.getString(ARG_POST_SELF_TEXT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root

        toolbarLayout = binding.toolbarLayout
        itemDetailTextView = binding.itemDetail

        updateContent()

        return rootView
    }

    private fun updateContent() {
        toolbarLayout?.title = postTitle
        itemDetailTextView.text =
            if (postSelfText.isNullOrBlank()) "(No post body)" else postSelfText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_POST_TITLE = "ARG_POST_TITLE"
        const val ARG_POST_SELF_TEXT = "ARG_POST_SELF_TEXT"
    }
}