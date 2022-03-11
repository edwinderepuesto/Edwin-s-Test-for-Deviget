package com.deviget.edwinstest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.deviget.edwinstest.api.PostData
import com.deviget.edwinstest.api.PostWrapper
import com.deviget.edwinstest.databinding.FragmentItemListBinding
import com.deviget.edwinstest.databinding.ItemListContentBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi


/**
 * This fragment has different presentations for handset and larger screen devices. On handsets, the
 * fragment presents a list of items, which when touched, lead to a {@link ItemDetailFragment}
 * representing item details. On larger screens, the Navigation controller presents the list of
 * items and item details side-by-side using two vertical panes.
 */

@ExperimentalSerializationApi
class ItemListFragment : Fragment() {
    private lateinit var viewModelFactory: RedditPostsViewModelFactory

    private lateinit var viewModel: RedditPostsViewModel

    private var _binding: FragmentItemListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModelFactory = RedditPostsViewModelFactory(requireActivity())

        viewModel = ViewModelProvider(this, viewModelFactory).get(RedditPostsViewModel::class.java)

        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = binding.itemList

        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        val itemDetailFragmentContainer: View? = view.findViewById(R.id.item_detail_nav_container)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            recyclerView.adapter = SimpleItemRecyclerViewAdapter(
                                result.data, itemDetailFragmentContainer, ::savePostAsRead
                            )
                            binding.statusTextView.text = getString(R.string.done)
                        }
                        is Result.Loading -> {
                            binding.statusTextView.text =
                                getString(
                                    if (result.loading) R.string.fetching else R.string.idle
                                )
                        }
                        is Result.Error -> {
                            binding.statusTextView.text = result.errorMessage
                        }
                    }
                }
            }
        }
    }

    private fun savePostAsRead(postData: PostData) {
        context?.let {
            viewModel.savePostAsRead(postData)
        }
    }

    class SimpleItemRecyclerViewAdapter(
        private val values: List<PostWrapper>,
        private val itemDetailFragmentContainer: View?,
        private val onClickCallback: (PostData) -> Unit
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.PostItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostItemViewHolder {
            val binding =
                ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PostItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PostItemViewHolder, position: Int) {
            val item = values[position]
            holder.titleTextView.text = item.data.title
            holder.unreadIndicatorView.visibility =
                if (item.data.isRead) View.GONE else View.VISIBLE

            val displayRelativeTime = item.data.getDisplayRelativeCreationTime()
            val displayCommentCount = item.data.getDisplayCommentCount()

            holder.subTitleTextView.text = holder.subTitleTextView.context.getString(
                R.string.post_sub_title,
                item.data.authorName,
                displayRelativeTime,
                displayCommentCount
            )

            val validatedImageSource =
                if (item.data.thumbnailUrl == "default")
                    R.drawable.reddit_logo
                else
                    item.data.thumbnailUrl

            holder.thumbnailImageView.load(validatedImageSource) {
                crossfade(true)
                fallback(R.drawable.reddit_logo)
            }

            holder.thumbnailImageView.setOnClickListener {
                val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.data.url))
                it.context.startActivity(urlIntent)
            }

            holder.itemView.setOnClickListener { itemView ->
                onClickCallback(item.data)

                val bundle = Bundle()
                bundle.putString(
                    ItemDetailFragment.ARG_POST_TITLE,
                    item.data.title
                )
                bundle.putString(
                    ItemDetailFragment.ARG_POST_SELF_TEXT,
                    item.data.selfText
                )
                if (itemDetailFragmentContainer != null) {
                    itemDetailFragmentContainer.findNavController()
                        .navigate(R.id.fragment_item_detail, bundle)
                } else {
                    itemView.findNavController().navigate(R.id.show_item_detail, bundle)
                }
            }
        }

        override fun getItemCount() = values.size

        inner class PostItemViewHolder(binding: ItemListContentBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val titleTextView: TextView = binding.titleTextView
            val subTitleTextView: TextView = binding.subTitleTextView
            val thumbnailImageView: ImageView = binding.thumbnailImageView
            val unreadIndicatorView: TextView = binding.unreadIndicatorView
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}