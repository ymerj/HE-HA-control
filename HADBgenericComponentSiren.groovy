/*
Copyright 2025
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

2.18- Yves Mercier - initial version
*/

metadata
    {
    definition(name: "HADB Generic Component Siren", namespace: "community", author: "Yves Mercier", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/HADBgenericComponentSiren.groovy")
        {
        capability "Chime"
        capability "Refresh"
        capability "Actuator"
        capability "Health Check"

        command "playSound",  [[ name: "tone*", type: "NUMBER", description: "Tone number" ],[ name: "duration", type: "NUMBER" ], [ name: "volume", type: "NUMBER" ]]
        command "setVolume",  [[ name: "volume*", type: "NUMBER", description: "Preset default volume" ]]
        command "setDuration",  [[ name: "duration*", type: "NUMBER", description: "Preset default duration" ]]
        }
    preferences
        {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        }
    attribute "healthStatus", "enum", ["offline", "online"]
    attribute "tonesList", "string"
    attribute "toneNumber", "number"
    attribute "volume", "number"
    attribute "duration", "number"
    }

void updated()
    {
    log.info "Updated..."
    log.warn "description logging is ${txtEnable == true}"
    }

void installed()
    {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
    }

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List description)
    {
    description.each
        {
        if (it.name in ["status", "soundName", "soundEffects", "toneList", "healthStatus"])
            {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            }
        }
    }

void playSound(tone)
    {
    def volume = this.device.currentValue("volume")
    def duration = this.device.currentValue("duration")
    parent?.componentPlaySound(this.device, tone, duration=1, volume=50)
    }

void playSound(tone, duration, volume)
    {
    parent?.componentPlaySound(this.device, tone, duration=1, volume=50)
    }

void setVolume(volume)
    {
    def description = "${this.device.label} volume was set to ${volume}"
    if (txtEnable) log.info description
    sendEvent(name: "volume", value: volume, descriptionText: description)
    }

void setDuration(duration)
    {
    def description = "${this.device.label} duration was set to ${duration}"
    if (txtEnable) log.info description 
    sendEvent(name: "duration", value: duration, descriptionText: description)
    }

void stop()
    {
    parent?.componentOff(this.device)
    }

void refresh()
    {
    parent?.componentRefresh(this.device)
    }

def ping()
    {
    refresh()
    }
