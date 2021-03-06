package com.deviget.edwinstest.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.deviget.edwinstest.R
import com.deviget.edwinstest.presentation.viewmodel.RedditPostsViewModel
import com.deviget.edwinstest.presentation.viewmodel.RedditPostsViewModelFactory
import com.deviget.edwinstest.common.MyResult
import com.deviget.edwinstest.data.dto.PostData
import com.deviget.edwinstest.data.dto.PostWrapper
import com.deviget.edwinstest.databinding.FragmentPostListBinding
import com.deviget.edwinstest.databinding.ItemPostBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * This fragment has different presentations for handset and larger screen devices. On handsets, the
 * fragment presents a list of items, which when touched, lead to a {@link ItemDetailFragment}
 * representing item details. On larger screens, the Navigation controller presents the list of
 * items and item details side-by-side using two vertical panes.
 */
@ExperimentalSerializationApi
class PostListFragment : Fragment() {
    private lateinit var viewModelFactory: RedditPostsViewModelFactory

    private lateinit var viewModel: RedditPostsViewModel

    private var _binding: FragmentPostListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModelFactory = RedditPostsViewModelFactory(requireActivity())

        viewModel = ViewModelProvider(this, viewModelFactory)[RedditPostsViewModel::class.java]

        _binding = FragmentPostListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        val itemDetailFragmentContainer: View? = view.findViewById(R.id.item_detail_nav_container)

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchPostsPage(resetData = true)
        }

        binding.dismissLink.setOnClickListener {
            dismissAllPosts()
        }

        binding.fetchNewPageLink.setOnClickListener {
            viewModel.fetchPostsPage(resetData = false)
        }

        val adapter = SimpleItemRecyclerViewAdapter(
            emptyList(),
            itemDetailFragmentContainer,
            ::savePostAsRead,
            ::dismissPost
        )

        binding.itemList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { result ->
                    when (result) {
                        is MyResult.Success -> {
                            adapter.updateDataSet(result.data)
                            binding.statusTextView.text = getString(R.string.done)
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                        is MyResult.Loading -> {
                            binding.statusTextView.text =
                                getString(
                                    if (result.loading) R.string.fetching_new_page else R.string.idle
                                )
                            binding.swipeRefreshLayout.isRefreshing = result.loading
                        }
                        is MyResult.Error -> {
                            binding.statusTextView.text = result.errorMessage
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    private fun savePostAsRead(postData: PostData) {
        viewModel.savePostAsRead(postData)
    }

    private fun dismissPost(postIdToDelete: String, itemHolderView: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            itemHolderView.slideOutRightAndWaitUntilFinished()
            viewModel.removePostIdFromDataSet(postIdToDelete)
        }
    }

    private fun dismissAllPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.itemList.slideOutRightAndWaitUntilFinished()
            viewModel.clearDataSet()
        }
    }

    class SimpleItemRecyclerViewAdapter(
        private var values: List<PostWrapper>,
        private val itemDetailFragmentContainer: View?,
        private val onClickCallback: (PostData) -> Unit,
        private val onDismissPostCallback: (String, View) -> Unit
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.PostItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostItemViewHolder {
            val binding =
                ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PostItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PostItemViewHolder, position: Int) {
            val item = values[position]
            holder.titleTextView.text = item.data.title
            holder.unreadIndicatorView.visibility =
                if (item.data.isRead) View.GONE else View.VISIBLE

            val displayRelativeTime = item.data.getDisplayRelativeCreationTime()
            val displayCommentCount = item.data.getDisplayCommentCount()

            val subTitleText = holder.subTitleTextView.context.getString(
                R.string.post_sub_title,
                item.data.authorName,
                displayRelativeTime,
                displayCommentCount
            )

            holder.subTitleTextView.text = subTitleText

            holder.thumbnailImageView.apply {
                load(item.data.getSafeThumbnailUrl()) { crossfade(true) }

                setOnClickListener {
                    val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.data.url))
                    it.context.startActivity(urlIntent)
                }
            }

            holder.dismissPostButton.setOnClickListener {
                onDismissPostCallback(item.data.id, holder.itemView)
            }

            holder.itemView.setOnClickListener { itemView ->
                onClickCallback(item.data)

                val bundle = Bundle()
                bundle.putString(
                    PostDetailsFragment.ARG_POST_ID,
                    item.data.id
                )
                bundle.putString(
                    PostDetailsFragment.ARG_POST_TITLE,
                    item.data.title
                )
                bundle.putString(
                    PostDetailsFragment.ARG_POST_SUB_TITLE,
                    subTitleText
                )
                bundle.putString(
                    PostDetailsFragment.ARG_POST_URL,
                    item.data.url
                )
                bundle.putString(
                    PostDetailsFragment.ARG_POST_THUMBNAIL_URL,
                    item.data.getSafeThumbnailUrl()
                )
                if (itemDetailFragmentContainer != null) {
                    itemDetailFragmentContainer.findNavController()
                        .navigate(R.id.fragment_post_details, bundle)
                } else {
                    itemView.findNavController().navigate(R.id.show_post_details, bundle)
                }
            }
        }

        override fun getItemCount() = values.size

        @SuppressLint("NotifyDataSetChanged")
        fun updateDataSet(newData: List<PostWrapper>) {
            values = newData
            notifyDataSetChanged()
        }

        inner class PostItemViewHolder(binding: ItemPostBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val titleTextView: TextView = binding.titleTextView
            val subTitleTextView: TextView = binding.subTitleTextView
            val thumbnailImageView: ImageView = binding.thumbnailImageView
            val unreadIndicatorView: TextView = binding.unreadIndicatorView
            val dismissPostButton: TextView = binding.dismissPostButton
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun View.slideOutRightAndWaitUntilFinished() {
        val slideOutAnimation: Animation = AnimationUtils.loadAnimation(
            this.context,
            android.R.anim.slide_out_right
        )
        slideOutAnimation.duration = ANIMATION_DURATION_MILLIS
        this.startAnimation(slideOutAnimation)
        delay(ANIMATION_DURATION_MILLIS)
    }

    companion object {
        const val ANIMATION_DURATION_MILLIS = 300L
    }
}