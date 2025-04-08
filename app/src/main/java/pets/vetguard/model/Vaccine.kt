package pets.vetguard.model

import java.util.Date

class Vaccine(
    var name: String,
    var date: Date,
    var pet: Pet
)
{
    override fun toString(): String {
        return "Vaccine(name='$name', date=$date, pet=$pet)"
    }
}