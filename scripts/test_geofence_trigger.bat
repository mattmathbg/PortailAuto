@echo off
REM ==============================================================================
REM Script de simulation ADB pour PortailAuto
REM Envoie un broadcast de test au GeofenceBroadcastReceiver de l'application.
REM ==============================================================================

echo.
echo =======================================================
echo   PortailAuto - Simulation de declenchement Geofence
echo =======================================================
echo.

adb devices
echo.

echo Envoi du Broadcast de transition Geofence (Release / Debug)...
REM Envoi cible sur la version Release
adb shell am broadcast -a com.crous.gateopener.ACTION_SIMULATE_TRIGGER -n com.crous.gateopener/.geofence.GeofenceBroadcastReceiver >nul 2>&1
REM Envoi cible sur la version Debug (suffixe .debug)
adb shell am broadcast -a com.crous.gateopener.ACTION_SIMULATE_TRIGGER -n com.crous.gateopener.debug/com.crous.gateopener.geofence.GeofenceBroadcastReceiver >nul 2>&1

echo.
echo Broadcast emis !
echo Verifiez l'ecran de l'application (journal d'evenements) ou executez :
echo   adb logcat -s GateGeofenceReceiver
echo.
pause
