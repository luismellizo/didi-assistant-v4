package com.kepler.didioracle

import android.content.Context
import android.util.Log
import java.util.Locale

object OracleLogic {

    // Valores por defecto (configurables desde la UI)
    private const val DEFAULT_RENDIMIENTO = 45.0
    private const val DEFAULT_PRECIO_GALON = 15600.0

    data class TripData(
        var precioOriginal: String = "",
        var arrendamientosTexto: String = "",
        var distanciasTexto: MutableList<String> = mutableListOf(),
        
        var precioParsed: Double = 0.0,
        var origenKm: Double = 0.0,
        var destinoKm: Double = 0.0,
        var arrendamientosParsed: Int = -1,
        var tipoPago: String = "EFECTIVO",
        
        // Cálculos de rentabilidad
        var costoGasolina: Double = 0.0,   // Costo del combustible para este viaje
        var gananciaNeta: Double = 0.0,     // Precio - costo gasolina
        var valorPorkm: Double = 0.0,       // Ganancia NETA por km
        
        var isRentable: Boolean = false,
        var msgRechazo: String = "",
        var debugDistancia: Double = 0.0
    )

    fun evaluate(context: Context, rawTexts: List<String>): TripData {
        val trip = TripData()
        val prefs = context.getSharedPreferences("DidiOraclePrefs", Context.MODE_PRIVATE)
        
        val metaGananciaPerKm = prefs.getFloat("META_KM", 1350f).toDouble()
        val maxDistanciaOrigen = prefs.getFloat("MAX_ORIGEN", 1.0f).toDouble()
        val rechazarNuevos = prefs.getBoolean("RECHAZAR_NUEVOS", true)
        val precioGalon = prefs.getFloat("PRECIO_GALON", DEFAULT_PRECIO_GALON.toFloat()).toDouble()
        val rendimientoKmGal = prefs.getFloat("RENDIMIENTO_KM", DEFAULT_RENDIMIENTO.toFloat()).toDouble()

        Log.d("DidiOracle", "=== NUEVA EVALUACIÓN ===")
        Log.d("DidiOracle", "Config: Meta=$metaGananciaPerKm, MaxOrigen=$maxDistanciaOrigen, BloquearNuevos=$rechazarNuevos")
        Log.d("DidiOracle", "Textos OCR (${rawTexts.size} bloques):")
        rawTexts.forEachIndexed { i, t -> Log.d("DidiOracle", "  [$i] \"$t\"") }

        // ================================================================
        // FASE 1: RECOLECCIÓN DE DATOS DESDE BLOQUES OCR DISPERSOS
        // ================================================================
        
        // Lista de TODOS los precios candidatos que encontremos
        val preciosCandidatos = mutableListOf<Long>()
        
        for ((index, text) in rawTexts.withIndex()) {
            val lower = text.lowercase()
            
            // --- PRECIOS ---
            // Limpiamos el texto para buscar números de precio
            // Removemos $, puntos (separador de miles), comas, espacios
            // Un precio válido de DiDi en Colombia es entre 3.000 y 99.999 (3-5 dígitos limpios)
            if (text.contains("$") || text.matches(Regex(".*\\d{1,3}\\.\\d{3}.*"))) {
                // Puede ser un precio. Extraer TODOS los números con formato de precio
                val precioRegex = Regex("\\$?\\s?(\\d{1,3}(?:[.]\\d{3})*)(?!\\d)")
                precioRegex.findAll(text).forEach { match ->
                    val raw = match.groupValues[1].replace(".", "").trim()
                    val valor = raw.toLongOrNull()
                    if (valor != null && valor in 3000..99999) {
                        preciosCandidatos.add(valor)
                        Log.d("DidiOracle", "  Precio candidato: $valor (de \"${match.value}\")")
                    }
                }
            }
            
            // --- ARRENDAMIENTOS ---
            if (lower.contains("arrendamiento")) {
                trip.arrendamientosTexto = text
                // Si el bloque solo dice "arrendamientos" sin número, buscamos en bloques adyacentes
                val numInBlock = text.replace("[^0-9]".toRegex(), "")
                if (numInBlock.isEmpty() && index > 0) {
                    val prevText = rawTexts[index - 1]
                    if (prevText.matches(Regex(".*[0-9]+.*"))) {
                        trip.arrendamientosTexto = prevText
                    } else if (index > 1 && rawTexts[index - 2].matches(Regex(".*[0-9]+.*"))) {
                        trip.arrendamientosTexto = rawTexts[index - 2]
                    }
                }
            }
            
            // --- MÉTODO DE PAGO ---
            if (lower.contains("nequi")) {
                trip.tipoPago = "NEQUI"
            } else if (lower.contains("tarjeta")) {
                trip.tipoPago = "TARJETA"
            } else if (lower.contains("efectivo")) {
                trip.tipoPago = "EFECTIVO"
            }
            
            // --- DISTANCIAS ---
            // El formato de DiDi es: "4min (1,1km)" y "7min (3,1km)"
            // También puede ser "4min (883m)"
            // Capturamos CUALQUIER bloque que contenga "km" o "min" o patrones numéricos con paréntesis
            if (lower.contains("min") || lower.contains("km") || lower.matches(Regex(".*\\d+[,.]\\d+.*"))) {
                trip.distanciasTexto.add(text)
            }
        }
        
        // ================================================================
        // FASE 2: PARSEO INTELIGENTE DE CADA CAMPO
        // ================================================================
        
        // --- PRECIO: Tomamos el PRIMER precio válido (el más prominente en pantalla) ---
        // DiDi muestra el precio principal grande arriba y luego los botones de sugerencia abajo
        // El primer candidato que encontremos será el precio principal
        if (preciosCandidatos.isNotEmpty()) {
            trip.precioParsed = preciosCandidatos.first().toDouble()
            trip.precioOriginal = preciosCandidatos.first().toString()
            Log.d("DidiOracle", "Precio seleccionado: ${trip.precioParsed} (de ${preciosCandidatos.size} candidatos: $preciosCandidatos)")
        }
        
        // --- ARRENDAMIENTOS ---
        val arroStr = trip.arrendamientosTexto.replace("[^0-9]".toRegex(), "")
        trip.arrendamientosParsed = arroStr.toIntOrNull() ?: 0
        
        // --- DISTANCIAS: Parseo a prueba de balas ---
        // Unimos todos los bloques de distancia en un solo string para analizar
        val superString = trip.distanciasTexto.joinToString(" ")
        Log.d("DidiOracle", "SuperString distancias: \"$superString\"")
        
        val distancesExtracted = mutableListOf<Double>()
        
        // PATRÓN 1: Buscar "Xmin (Y,Zkm)" o "Xmin (Ykm)"
        // Captura el número dentro del paréntesis que va seguido de "km"
        val kmPattern = Regex("(\\d+[,.]\\d+)\\s*km|(\\d+)\\s*km", RegexOption.IGNORE_CASE)
        kmPattern.findAll(superString).forEach { match ->
            val numStr = (match.groupValues[1].ifEmpty { match.groupValues[2] })
                .replace(",", ".")
            val num = numStr.toDoubleOrNull()
            if (num != null && num < 100) { // Sanity check: una distancia no debería ser > 100km
                distancesExtracted.add(num)
                Log.d("DidiOracle", "  Distancia km detectada: ${num}km (de \"${match.value}\")")
            }
        }
        
        // PATRÓN 2: Buscar metros SOLO si va seguido de "m)" o "m " pero NO "min"
        // Usamos un negative lookahead para evitar capturar "min"
        val mPattern = Regex("(\\d{2,4})\\s*m(?!i)(?:[)\\s,]|$)", RegexOption.IGNORE_CASE)
        mPattern.findAll(superString).forEach { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null && num in 50.0..9999.0) { // Sanity check: metros razonables
                val km = num / 1000.0
                distancesExtracted.add(km)
                Log.d("DidiOracle", "  Distancia m detectada: ${num}m = ${km}km (de \"${match.value}\")")
            }
        }
        
        Log.d("DidiOracle", "Distancias extraídas: $distancesExtracted")
        
        if (distancesExtracted.size >= 1) trip.origenKm = distancesExtracted[0]
        if (distancesExtracted.size >= 2) trip.destinoKm = distancesExtracted[1]
        
        trip.debugDistancia = trip.origenKm + trip.destinoKm
        
        Log.d("DidiOracle", "RESULTADO PARSEO: Precio=${trip.precioParsed}, Origen=${trip.origenKm}km, Destino=${trip.destinoKm}km, Viajes=${trip.arrendamientosParsed}, Pago=${trip.tipoPago}")
        
        // ================================================================
        // FASE 3: REGLAS DE DECISIÓN
        // ================================================================
        
        if (trip.precioParsed <= 0) {
            trip.isRentable = false
            trip.msgRechazo = "Error leyendo precio"
            return trip
        }
        
        if (trip.origenKm == 0.0 || trip.destinoKm == 0.0) {
            trip.isRentable = false
            trip.msgRechazo = "No se observan KM"
            return trip
        }
        
        if (rechazarNuevos && trip.arrendamientosParsed == 0 && trip.arrendamientosTexto.isNotEmpty()) {
            trip.isRentable = false
            trip.msgRechazo = "Rechazado: Nvo Usuario"
            return trip
        }
        
        if (trip.origenKm > maxDistanciaOrigen) {
            trip.isRentable = false
            trip.msgRechazo = "Lejos: Origen a ${"%.1f".format(trip.origenKm)}km"
            return trip
        }
        
        val totalKm = trip.origenKm + trip.destinoKm
        
        // Cálculo de costo de combustible para este trayecto
        // Fórmula: (totalKm / rendimientoKmGal) * precioGalon
        val galonesConsumidos = totalKm / rendimientoKmGal
        trip.costoGasolina = galonesConsumidos * precioGalon
        trip.gananciaNeta = trip.precioParsed - trip.costoGasolina
        trip.valorPorkm = trip.gananciaNeta / totalKm
        
        if (trip.valorPorkm >= metaGananciaPerKm) {
            trip.isRentable = true
        } else {
            trip.isRentable = false
            trip.msgRechazo = "Pago bajo: \$${trip.valorPorkm.toInt()}/km"
        }
        
        Log.d("DidiOracle", "ECONOMÍA: Precio=${trip.precioParsed}, Gasolina=${trip.costoGasolina}, Neta=${trip.gananciaNeta}, $/km=${trip.valorPorkm}")
        Log.d("DidiOracle", "DECISIÓN: rentable=${trip.isRentable}, msg=${trip.msgRechazo}")
        
        return trip
    }

}
