/*

Copyright 2026

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

2.24 - Yves mercier - initial version

*/

metadata
    {
    definition(name: "HADB Generic Component Speech Synthesis", namespace: "community", author: "community", importUrl: "")
        {
        capability "Actuator"
        capability "Speech Synthesis"
        }
    preferences
        {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "engine", type:"string", title: "HA TTS engine", required: true, defaultValue: "tts.google_translate_en_com"
        }
    }

void updated()
    {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
    }

void installed()
    {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    }

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description)
    {
    log.info "nothing to parse"
    }

void speak(message, volume = null, voice = null)
    {
    parent?.componentSpeak(this.device, message, engine)
    }
