#!/usr/bin/env bash
# ==============================================================================
# Script de simulation ADB pour PortailAuto
# Envoie un broadcast de test au GeofenceBroadcastReceiver de l'application.
# ==============================================================================

echo "======================================================="
echo "  PortailAuto - Simulation de déclenchement Geofence"
echo "======================================================="
echo ""

adb devices
echo ""

echo "Envoi du Broadcast de transition Geofence (Release & Debug)..."
# Version Release
adb shell am broadcast -a com.crous.gateopener.ACTION_SIMULATE_TRIGGER -n com.crous.gateopener/.geofence.GeofenceBroadcastReceiver >/dev/null 2>&1
# Version Debug
adb shell am broadcast -a com.crous.gateopener.ACTION_SIMULATE_TRIGGER -n com.crous.gateopener.debug/com.crous.gateopener.geofence.GeofenceBroadcastReceiver >/dev/null 2>&1

echo ""
echo "Broadcast émis !"
echo "Vérifiez l'écran de l'application (journal d'événements) ou exécutez :"
echo "  adb logcat -s GateGeofenceReceiver"
echo ""
