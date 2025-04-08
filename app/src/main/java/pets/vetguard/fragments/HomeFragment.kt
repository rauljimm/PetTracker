package pets.vetguard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import pets.vetguard.R
import pets.vetguard.model.Owner

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout del HomeFragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = arguments?.getParcelable("owner") as? Owner
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        owner?.let {
            titleTextView.text = "¡Bienvenido, ${it.ownerName}!"
        }
    }
}
