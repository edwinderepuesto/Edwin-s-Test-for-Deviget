package com.deviget.edwinstest

import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.deviget.edwinstest.api.PostWrapper
import com.deviget.edwinstest.databinding.FragmentItemListBinding
import com.deviget.edwinstest.databinding.ItemListContentBinding
import kotlinx.coroutines.launch


/**
 * This fragment has different presentations for handset and larger screen devices. On handsets, the
 * fragment presents a list of items, which when touched, lead to a {@link ItemDetailFragment}
 * representing item details. On larger screens, the Navigation controller presents the list of
 * items and item details side-by-side using two vertical panes.
 */

class ItemListFragment : Fragment() {
    private val viewModel: RedditPostsViewModel by viewModels()

    private var _binding: FragmentItemListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

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
                                result.data, itemDetailFragmentContainer
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

    class SimpleItemRecyclerViewAdapter(
        private val values: List<PostWrapper>,
        private val itemDetailFragmentContainer: View?
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

            val displayRelativeTime = item.data.getDisplayRelativeCreationTime()
            val displayCommentCount = item.data.getDisplayCommentCount()

            holder.subTitleTextView.text = holder.thumbnailImageView.context.getString(
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

            with(holder.itemView) {
                tag = item
                setOnClickListener { itemView ->
                    val clickedItem = itemView.tag as PostWrapper
                    val bundle = Bundle()
                    bundle.putString(
                        ItemDetailFragment.ARG_POST_TITLE,
                        clickedItem.data.title
                    )
                    bundle.putString(
                        ItemDetailFragment.ARG_POST_SELF_TEXT,
                        clickedItem.data.selfText
                    )
                    if (itemDetailFragmentContainer != null) {
                        itemDetailFragmentContainer.findNavController()
                            .navigate(R.id.fragment_item_detail, bundle)
                    } else {
                        itemView.findNavController().navigate(R.id.show_item_detail, bundle)
                    }
                }

                /**
                 * Context click listener to handle Right click events
                 * from mice and trackpad input to provide a more native
                 * experience on larger screen devices
                 */
                setOnContextClickListener { v ->
                    val contextClickedItem = v.tag as PostWrapper
                    Toast.makeText(
                        v.context,
                        "Context click of item " + contextClickedItem.data.id,
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }

                setOnLongClickListener { v ->
                    // Setting the item id as the clip data so that the drop target is able to
                    // identify the id of the content
                    val clipItem = ClipData.Item(item.data.id)
                    val dragData = ClipData(
                        v.tag as? CharSequence,
                        arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                        clipItem
                    )

                    v.startDragAndDrop(
                        dragData,
                        View.DragShadowBuilder(v),
                        null,
                        0
                    )
                }
            }
        }

        override fun getItemCount() = values.size

        inner class PostItemViewHolder(binding: ItemListContentBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val titleTextView: TextView = binding.titleTextView
            val subTitleTextView: TextView = binding.subTitleTextView
            val thumbnailImageView: ImageView = binding.thumbnailImageView
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}