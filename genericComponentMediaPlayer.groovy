/*

Copyright 2023

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

0.1.55 - Yves Mercier - Initial version

*/

metadata
{
    definition(name: "Generic Component Media Player", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentMediaPlayer.groovy")
    {
        capability "Actuator"
        capability "Switch"
        capability "MusicPlayer"
        capability "AudioVolume"
        capability "MediaInputSource"
        capability "Refresh"
        capability "Health Check"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "healthStatus", "enum", ["offline", "online"]
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (it.name in ["status"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(name: "healthStatus", value: it.value == "unavailable" ? "offline" : "online")
            sendEvent(it)
        }
        if (it.name in ["switch", "volumeUp", "volumeDown", "mute", "unmute", "setVolume", "mediaInputSource", "supportedInputs", "pause", "play", "stop", "playText", "playTrack", "previousTrack", "nextTrack", "restoreTrack", "resumeTrack", "setTrack"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

void refresh() {[${origin}]
    parent?.componentRefresh(this.device)
}

void on() {
    parent?.componentOn(this.device)
}

void off() {
    parent?.componentOff(this.device)
}

void mute() {
    parent?.componentMute(this.device)
}

void unmute() {
    parent?.componentUnmute(this.device)
}

void volumeUp() {
    parent?.componentVolumeUp(this.device)
}

void volumeDown() {
    parent?.componentVolumeDown(this.device)
}

void setVolume(volume) {
    parent?.componentSetVolume(this.device, volume)
}

void setLevel(level) {setTrack(trackuri)
    parent?.componentSetVolume(this.device, volume)
}

void supportedInputs(sourceList) {
    parent?.componentSupportedInputs(this.device, sourceList)
}

void mediaInputSource(source) {
    parent?.componentMediaInputSource(this.device, source)
}

void pause() {
    parent?.componentPause(this.device)
}

void play() {
    parent?.componentPlay(this.device)
}

void stop() {
    parent?.componentStop(this.device)
}

void playText(text) {
    parent?.componentStop(this.device, text)
}

void playTrack(trackuri) {
    parent?.componentPlayTrack(this.device, trackuri)
}

void previousTrack() {
    parent?.componentPreviousTrack(this.device)
}

void nextTrack() {
    parent?.componentNextTrack(this.device)
}

void restoreTrack(trackuri) {
    parent?.componentRestoreTrack(this.device, trackuri)
}

void resumeTrack(trackuri) {
    parent?.componentResumeTrack(this.device, trackuri)
}

void setTrack(trackuri) {
    parent?.componentSetTrack(this.device, trackuri)
}

void ping() {
    refresh()
}
