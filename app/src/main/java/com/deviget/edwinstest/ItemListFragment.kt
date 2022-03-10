package com.deviget.edwinstest

import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.deviget.edwinstest.api.PostWrapper
import com.deviget.edwinstest.databinding.FragmentItemListBinding
import com.deviget.edwinstest.databinding.ItemListContentBinding
import kotlinx.coroutines.flow.collect
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
                            binding.statusTextView.text = "Done"
                        }
                        is Result.Loading -> {
                            binding.statusTextView.text =
                                if (result.loading) "Fetching..." else "Idle"
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
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.data.id
            holder.contentView.text = item.data.title

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

        inner class ViewHolder(binding: ItemListContentBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val idView: TextView = binding.idText
            val contentView: TextView = binding.content
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}