package quevedo.soares.leandro.blemadeeasy.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import quevedo.soares.leandro.blemadeeasy.databinding.ItemBluetoothDeviceBinding
import quevedo.soares.leandro.blemadeeasy.models.BLEDevice

typealias BLEDeviceAdapterOnClickListener = (position: Int, device: BLEDevice) -> Unit

class BLEDeviceAdapter(private val recyclerView: RecyclerView, private val listener: BLEDeviceAdapterOnClickListener) : RecyclerView.Adapter<BLEDeviceAdapter.ViewHolder>() {

	private val inflater by lazy { LayoutInflater.from(this.recyclerView.context) }
	private var items: List<BLEDevice> = listOf()

	init {
		this.recyclerView.adapter = this
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemBluetoothDeviceBinding.inflate(inflater, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = this.items[position]
		holder.bind(item)
	}

	override fun getItemCount(): Int = this.items.size

	@SuppressLint("NotifyDataSetChanged")
	fun setItems(newItems: List<BLEDevice>) {
		this.items = newItems.sortedBy { it.name }
		this.notifyDataSetChanged()
	}

	inner class ViewHolder(private val binding: ItemBluetoothDeviceBinding) : RecyclerView.ViewHolder(binding.root) {

		@SuppressLint("SetTextI18n")
		fun bind(device: BLEDevice) {
			this.binding.apply {
				ibdTvName.text = device.name
				ibdTvAddress.text = device.macAddress
				ibdTvSignalStrength.text = "${device.rsii}dBm"

				root.setOnClickListener {
					listener.invoke(adapterPosition, device)
				}
			}
		}

	}

}