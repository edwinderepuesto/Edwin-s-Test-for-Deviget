package com.deviget.edwinstest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import com.deviget.edwinstest.databinding.FragmentItemDetailBinding

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListFragment]
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
class ItemDetailFragment : Fragment() {
    private var postTitle: String? = null
    private var postSubTitle: String? = null
    private var postUrl: String? = null
    private var postThumbnailUrl: String? = null

    private var _binding: FragmentItemDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            postTitle = it.getString(ARG_POST_TITLE)
            postSubTitle = it.getString(ARG_POST_SUB_TITLE)
            postUrl = it.getString(ARG_POST_URL)
            postThumbnailUrl = it.getString(ARG_POST_THUMBNAIL_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root

        updateContent()

        return rootView
    }

    private fun updateContent() {
        binding.toolbarLayout?.title = getString(R.string.details)
        binding.detailTitleTextView?.text = postTitle
        binding.detailSubTitleTextView?.text = postSubTitle

        val validatedImageSource =
            if (postThumbnailUrl == "default")
                R.drawable.reddit_logo
            else
                postThumbnailUrl

        binding.detailImageView?.apply {
            load(validatedImageSource) { crossfade(true) }

            setOnClickListener {
                val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(postUrl))
                it.context.startActivity(urlIntent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_POST_TITLE = "ARG_POST_TITLE"
        const val ARG_POST_SUB_TITLE = "ARG_POST_SUB_TITLE"
        const val ARG_POST_URL = "ARG_POST_URL"
        const val ARG_POST_THUMBNAIL_URL = "ARG_POST_THUMBNAIL_URL"
    }
}