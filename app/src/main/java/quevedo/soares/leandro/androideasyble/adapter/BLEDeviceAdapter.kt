package quevedo.soares.leandro.androideasyble.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import quevedo.soares.leandro.androideasyble.R
import quevedo.soares.leandro.androideasyble.databinding.ItemBluetoothDeviceBinding
import quevedo.soares.leandro.androideasyble.models.BLEDevice

typealias BLEDeviceAdapterOnClickListener = (position: Int, device: BLEDevice) -> Unit

class BLEDeviceAdapter(private val recyclerView: RecyclerView, private val listener: BLEDeviceAdapterOnClickListener) : RecyclerView.Adapter<BLEDeviceAdapter.ViewHolder>() {

	private val inflater by lazy { LayoutInflater.from(this.recyclerView.context) }
	private var items: List<BLEDevice> = listOf()

	init {
		this.recyclerView.adapter = this
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(DataBindingUtil.inflate(this.inflater, R.layout.item_bluetooth_device, parent, false))

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.binding.device = this.items[position]
		holder.itemView.setOnClickListener {
			this.listener.invoke(holder.adapterPosition, this.items[holder.adapterPosition])
		}
	}

	override fun getItemCount(): Int = this.items.size

	@SuppressLint("NotifyDataSetChanged")
	fun setItems(newItems: List<BLEDevice>) {
		this.items = newItems.sortedBy { it.name }
		this.notifyDataSetChanged()
	}

	class ViewHolder(val binding: ItemBluetoothDeviceBinding) : RecyclerView.ViewHolder(binding.root)

}