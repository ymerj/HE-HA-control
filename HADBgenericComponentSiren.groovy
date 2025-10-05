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
        capability "Switch"
        capability "Refresh"
        capability "Actuator"
        capability "Health Check"

        command "playSound",  [[ name: "tone*", type: "NUMBER", description: "Tone number" ],[ name: "duration*", type: "NUMBER" ], [ name: "volume*", type: "NUMBER" ]]
        }
    preferences
        {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        }
    attribute "healthStatus", "enum", ["offline", "online"]
    attribute "tonesList", "string"
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
        if (it.name in ["soundName", "soundEffects", "healthStatus"])
            {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            }
        if (it.name in ["status"])
            {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            if (it.value == "playing") sendEvent(name: "switch", value: "on", descriptionText: "${this.device.label} was turn on")
            if (it.value == "stopped") sendEvent(name: "switch", value: "off", descriptionText: "${this.device.label} was turn off")
            }
        }
    }

void playSound(tone, duration=1, volume=50)
    {
    parent?.componentPlaySound(this.device, tone, duration, volume)
    }

void stop()
    {
    parent?.componentOff(this.device)
    }

void on()
    {
    parent?.componentOn(this.device)
    }

void off()
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
