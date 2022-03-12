package com.deviget.edwinstest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.deviget.edwinstest.databinding.FragmentItemDetailBinding
import kotlinx.coroutines.launch

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListFragment]
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
class ItemDetailFragment : Fragment() {
    private lateinit var viewModelFactory: RedditPostsViewModelFactory
    private lateinit var viewModel: RedditPostsViewModel

    private var postId: String = ""
    private var postTitle: String = ""
    private var postSubTitle: String = ""
    private var postUrl: String = ""
    private var postThumbnailUrl: String = ""

    private var _binding: FragmentItemDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            postId = it.getString(ARG_POST_ID).toString()
            postTitle = it.getString(ARG_POST_TITLE).toString()
            postSubTitle = it.getString(ARG_POST_SUB_TITLE).toString()
            postUrl = it.getString(ARG_POST_URL).toString()
            postThumbnailUrl = it.getString(ARG_POST_THUMBNAIL_URL).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModelFactory = RedditPostsViewModelFactory(requireActivity())
        viewModel = ViewModelProvider(this, viewModelFactory)[RedditPostsViewModel::class.java]

        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root

        updateContent()

        return rootView
    }

    private fun updateContent() {
        binding.detailTitleTextView.text = postTitle
        binding.detailSubTitleTextView.text = postSubTitle

        val validatedImageSource =
            if (postThumbnailUrl == "default")
                "https://www.redditinc.com/assets/images/site/reddit-logo.png"
            else
                postThumbnailUrl

        binding.detailImageView.apply {
            load(validatedImageSource) { crossfade(true) }

            setOnClickListener {
                val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(postUrl))
                it.context.startActivity(urlIntent)
            }
        }

        binding.downloadButton.apply {
            visibility = if (postThumbnailUrl.isNotEmpty()) View.VISIBLE else View.GONE

            setOnClickListener {
                downloadPostThumbnail()
            }
        }
    }

    private fun downloadPostThumbnail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadFile(postThumbnailUrl, postId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_POST_ID = "ARG_POST_ID"
        const val ARG_POST_TITLE = "ARG_POST_TITLE"
        const val ARG_POST_SUB_TITLE = "ARG_POST_SUB_TITLE"
        const val ARG_POST_URL = "ARG_POST_URL"
        const val ARG_POST_THUMBNAIL_URL = "ARG_POST_THUMBNAIL_URL"
    }
}