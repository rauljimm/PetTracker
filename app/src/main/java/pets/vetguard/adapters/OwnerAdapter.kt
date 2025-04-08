package pets.vetguard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pets.vetguard.R
import pets.vetguard.model.Pet

class OwnerAdapter(
    private val petList: List<Pet>?
) : RecyclerView.Adapter<OwnerAdapter.PetViewHolder>() {

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.petNameTextView)
        val imageView: ImageView = itemView.findViewById(R.id.petImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = petList?.get(position)
        holder.nameTextView.text = pet?.name ?: "Sin nombre"
        Glide.with(holder.itemView.context)
            .load(pet?.profilePic ?: "")
            .placeholder(R.drawable.placeholder_image)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = petList?.size ?: 0

}