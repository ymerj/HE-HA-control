/*

Copyright 2024

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------

Change history:

0.1  - ritchierich  - initial version
2.4  - Yves Mercier - Modified healthCheck handling
2.7  - Yves Mercier - Add support for presets
2.8  - mluck        - corrected typo
2.12 - Yves Mercier - Add presets by name
2.14 - Yves Mercier - Add support for humidity setting
2.16 - basilisk487  - Filter out setCoolingSetpoint/setHeatingSetpoint calls that are the opposite of current thermostat mode.
2.20 - Yves Mercier - Add option to translate HE auto thermostat mode to HA heat_cool

*/

metadata
{
    definition(name: 'HADB Generic Component Thermostat', namespace: 'community', author: 'community', importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/HADBgenericComponentThermostat.groovy")
    {
        capability 'Actuator'
        capability 'Sensor'
        capability 'TemperatureMeasurement'
        capability 'Thermostat'
        capability 'RelativeHumidityMeasurement'
        capability 'Refresh'
        capability "Health Check"
    }
    preferences
    {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
		input name: "translate", type: "bool", title: "Translate HE auto mode to HA heat_cool", defaultValue: false
    }

    command "setPreset", [[name: "preset", type: "STRING", description: "Preset"]]
    command "setHumidity", [[name: "humiditySetpoint", type: "NUMBER", description: "Humidity setpoint"]]
    command "setThermostatMode", [[ name: "thermostatMode*", type: "ENUM", constraints: [ "off", "heat", "cool", "heat_cool", "auto", "dry", "fan_only" ], description: "Thermostat mode to set" ]]

    attribute "healthStatus", "enum", ["offline", "online"]
    attribute "supportedThermostatFanModes", "JSON_OBJECT"
    attribute "supportedThermostatModes", "JSON_OBJECT"
    attribute "supportedPresets", "string"
    attribute "currentPreset", "string"
    attribute "maxHumidity", "number"
    attribute "minHumidity", "number"
    attribute "humiditySetpoint", "number"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void updated() {
    log.info "Updated..."
    if (logEnable) runIn(1800,logsOff)
}

void uninstalled() {
    log.info "${device} driver uninstalled"
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each
        {
        if (it.name in ["temperature", "thermostatOperatingState", "thermostatFanMode", "thermostatSetpoint", "coolingSetpoint", "heatingSetpoint", "supportedThermostatFanModes", "supportedPresets", "currentPreset", "healthStatus", "maxHumidity", "minHumidity", "humidity", "humiditySetpoint"])
            {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            }
        if (it.name in ["thermostatMode"])
            {
            if (translate && (it.value == "heat_cool")) it.value = "auto"
            sendEvent(it)
            }
        if (it.name in ["supportedThermostatModes"])
            {
            if (translate) it.value.replaceAll("heat_cool", "auto")
            sendEvent(it)
			}
        }
}

void off() {
    setThermostatMode("off")
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void setCoolingSetpoint(BigDecimal temperature) {
    if (this.device.currentValue("thermostatMode") == "heat") {
	log.warn("ignoring setCoolingSetpoint request from ${this.device.label} (mode is 'heat')")
        return
    }
    parent?.componentSetCoolingSetpoint(this.device, temperature)
}

void setHeatingSetpoint(BigDecimal temperature) {
    if (this.device.currentValue("thermostatMode") == "cool") {
	log.warn("ignoring setHeatingSetpoint request from ${this.device.label} (mode is 'cool')")
        return
    }   
    parent?.componentSetHeatingSetpoint(this.device, temperature)
}

void setThermostatMode(String thermostatMode) {
	if (translate && (thermostatMode == "auto")) thermostatMode = "heat_cool"
    parent?.componentSetThermostatMode(this.device, thermostatMode)
}

void setThermostatFanMode(String fanMode) {
    parent?.componentSetThermostatFanMode(this.device, fanMode)
}

def setHumidity(humiditySetpoint) {
    if ((humiditySetpoint > this.device.currentValue("maxHumidity")) || (humiditySetpoint < this.device.currentValue("minHumidity"))) log.warn "humidity setpoint out of range"
    else parent?.componentSetHumidity(this.device, humiditySetpoint)
}

void auto() {
    parent?.componentAuto(this.device)
}

void cool() {
    parent?.componentCool(this.device)
}

void emergencyHeat() {
    parent?.componentEmergencyHeat(this.device)
}

void heat() {
    parent?.componentHeat(this.device)
}

void fanAuto() {
    parent?.componentFanAuto(this.device)
}

void fanCirculate() {
    parent?.componentFanCirculate(this.device)
}

void fanOn() {
    parent?.componentFanOn(this.device)
}

def setPreset(preset){
    if (this.device.currentValue("supportedPresets") == "none") log.warn "no supported presets defined"
    else parent?.componentSetPreset(this.device, preset)
}

def logsOff(){
    log.warn("debug logging disabled...")
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def ping() {
    refresh()
}
