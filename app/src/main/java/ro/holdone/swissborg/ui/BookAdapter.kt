package ro.holdone.swissborg.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ro.holdone.swissborg.R
import ro.holdone.swissborg.extensions.roundTo
import ro.holdone.swissborg.server.model.BookEntry
import ro.holdone.swissborg.ui.views.VolumeIndicatorView
import kotlin.math.absoluteValue

sealed class BookItem(val entry: BookEntry) {
    class Ask(entry: BookEntry) : BookItem(entry)
    class Bid(entry: BookEntry) : BookItem(entry)

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<BookItem>() {
            override fun areItemsTheSame(oldItem: BookItem, newItem: BookItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: BookItem, newItem: BookItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class BookItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(bookEntry: BookEntry) {
        itemView.findViewById<TextView>(R.id.volume).text = "%4f".format(bookEntry.amount.roundTo(4).absoluteValue)
        itemView.findViewById<TextView>(R.id.price).text = "${bookEntry.price}"
        itemView.findViewById<VolumeIndicatorView>(R.id.volume_indicator).progress = bookEntry.count.toFloat() / bookEntry.maxCount
    }
}

class BookAdapter : ListAdapter<BookItem, BookItemViewHolder>(BookItem.diffUtil) {

    lateinit var layoutInflater: LayoutInflater

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutInflater = LayoutInflater.from(recyclerView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookItemViewHolder {
        return when (viewType) {
            VIEW_BID -> BookItemViewHolder(
                layoutInflater.inflate(
                    R.layout.bid_entry,
                    parent,
                    false
                )
            )
            VIEW_ASK -> BookItemViewHolder(
                layoutInflater.inflate(
                    R.layout.ask_entry,
                    parent,
                    false
                )
            )
            else -> throw Error("Unsupported viewType=$viewType")
        }
    }

    override fun onBindViewHolder(holder: BookItemViewHolder, position: Int) {
        holder.bind(getItem(position).entry)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is BookItem.Ask -> VIEW_ASK
            is BookItem.Bid -> VIEW_BID
        }
    }

    companion object {
        const val VIEW_ASK = 100
        const val VIEW_BID = 101
    }
}