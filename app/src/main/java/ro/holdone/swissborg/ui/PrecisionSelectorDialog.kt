package ro.holdone.swissborg.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ro.holdone.swissborg.R
import ro.holdone.swissborg.databinding.DialogPrecisionBinding
import ro.holdone.swissborg.server.model.Precision

class PrecisionSelectorDialog : BottomSheetDialogFragment() {

    lateinit var binding: DialogPrecisionBinding

    private val precisionAdapter = PrecisionAdapter()

    private val tickerViewModel: TickerViewModel by viewModels({ requireParentFragment() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.SwissborgBottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPrecisionBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.precisionRecyclerView.layoutManager = object: LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        binding.precisionRecyclerView.adapter = precisionAdapter

        tickerViewModel.precision.observe(viewLifecycleOwner) {
            precisionAdapter.currentPrecision = it
            precisionAdapter.notifyDataSetChanged()
        }
    }

    class PrecisionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(precision: Precision, isSelected: Boolean) {
            itemView.findViewById<TextView>(R.id.precision_value).text = precision.precisionDecimals
            itemView.findViewById<View>(R.id.check_mark).isVisible = isSelected
        }
    }

    inner class PrecisionAdapter : RecyclerView.Adapter<PrecisionViewHolder>() {

        private lateinit var layoutInflater: LayoutInflater

        var currentPrecision: Precision? = null

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            layoutInflater = LayoutInflater.from(recyclerView.context)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrecisionViewHolder {
            return PrecisionViewHolder(
                layoutInflater.inflate(
                    R.layout.precision_item,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: PrecisionViewHolder, position: Int) {
            val precision = Precision.values()[position]
            holder.bind(precision, precision == currentPrecision)
            holder.itemView.setOnClickListener {
                tickerViewModel.setPrecision(precision)
                dismiss()
            }
        }

        override fun getItemCount(): Int = Precision.values().size

    }
}
