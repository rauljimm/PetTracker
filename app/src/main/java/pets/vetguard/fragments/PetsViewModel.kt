package pets.vetguard.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pets.vetguard.model.Pet

class PetsViewModel : ViewModel() {
    private val _petList = MutableLiveData<MutableList<Pet>>(mutableListOf())
    val petList: LiveData<MutableList<Pet>> get() = _petList
    private val MAX_PETS = 10


    fun loadPets(pets: List<Pet>?) {
        _petList.value = pets?.toMutableList() ?: mutableListOf()
    }


    fun addPet(pet: Pet): Boolean {
        val currentList = _petList.value ?: mutableListOf()
        return if (currentList.size < MAX_PETS) {
            currentList.add(pet)
            _petList.value = currentList // Notificar cambios
            true
        } else {
            false
        }
    }


    fun updatePet(position: Int, pet: Pet): Boolean {
        val currentList = _petList.value ?: mutableListOf()
        return if (position >= 0 && position < currentList.size) {
            currentList[position] = pet
            _petList.value = currentList
            true
        } else {
            false
        }
    }
}
