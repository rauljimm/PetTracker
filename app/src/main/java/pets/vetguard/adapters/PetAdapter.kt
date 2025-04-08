package pets.vetguard.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pets.vetguard.R
import pets.vetguard.model.Pet

class PetAdapter(
    private var petList: MutableList<Pet>,
    private val onDoubleClick: (Pet, Int) -> Unit
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.petNameTextView)
        val imageView: ImageView = itemView.findViewById(R.id.petImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val pet = petList[position]
        holder.nameTextView.text = pet.name ?: "Sin nombre"
        Glide.with(holder.itemView.context)
            .load(pet.profilePic ?: "")
            .placeholder(R.drawable.placeholder_image)
            .into(holder.imageView)

        holder.itemView.setOnClickListener(object : View.OnClickListener {
            private val DOUBLE_CLICK_TIME_DELTA: Long = 300
            private var lastClickTime: Long = 0

            override fun onClick(v: View?) {
                val clickTime = System.currentTimeMillis()
                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                    onDoubleClick(pet, position)
                }
                lastClickTime = clickTime
            }
        })
    }

    override fun getItemCount(): Int = petList.size

    fun updatePets(newPets: List<Pet>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = petList.size
            override fun getNewListSize() = newPets.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                petList[oldPos].id == newPets[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                petList[oldPos] == newPets[newPos]
        })
        petList.clear()
        petList.addAll(newPets)
        diffResult.dispatchUpdatesTo(this)
    }
}