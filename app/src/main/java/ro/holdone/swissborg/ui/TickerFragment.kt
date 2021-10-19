package ro.holdone.swissborg.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ro.holdone.swissborg.databinding.FragmentTickerBinding
import ro.holdone.swissborg.server.model.CoinsPair

@AndroidEntryPoint
class TickerFragment : Fragment() {

    lateinit var binding: FragmentTickerBinding

    private val tickerViewModel: TickerViewModel by viewModels()

    private val askAdapter = BookAdapter()
    private val bidAdapter = BookAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tickerViewModel.trackPair(CoinsPair.BTCUSD)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTickerBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTicker()
        setupBooks()
    }

    private fun setupTicker() {
        tickerViewModel.tickerSnapshot.observe(viewLifecycleOwner) { snapshot ->
            binding.volume.text = snapshot.volume.toString()
            binding.price.text = snapshot.lastPrice.toString()
            binding.lowPrice.text = snapshot.low.toString()
            binding.highPrice.text = snapshot.high.toString()
            binding.dailyChange.text = "${snapshot.dailyChangePerc}%"
        }
    }

    private fun setupBooks() {
        binding.askRecyclerView.layoutManager = object: LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean = false
        }
        binding.bidRecyclerView.layoutManager = object: LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean = false
        }
        binding.askRecyclerView.adapter = askAdapter
        binding.bidRecyclerView.adapter = bidAdapter

        binding.askRecyclerView.itemAnimator = null
        binding.bidRecyclerView.itemAnimator = null

        tickerViewModel.askOrders.observe(viewLifecycleOwner) { orders ->
            askAdapter.submitList(orders.map { BookItem.Ask(it) })
        }
        tickerViewModel.bidOrders.observe(viewLifecycleOwner) { orders ->
            bidAdapter.submitList(orders.map { BookItem.Bid(it) })
        }
    }
}