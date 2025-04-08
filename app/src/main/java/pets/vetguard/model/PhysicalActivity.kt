package pets.vetguard.model

import java.util.Date

class PhysicalActivity(
    val pet: Pet,
    val duration: Long,
    val date: Date,
    val typeActivity: String)
{
    override fun toString(): String {
        return "PhysicalActivity(pet=$pet, duration=$duration, date=$date, typeActivity='$typeActivity')"
    }
}