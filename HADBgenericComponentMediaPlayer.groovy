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

2.11 - Yves Mercier - Initial version
2.12 - Yves Mercier - Add input source

*/

metadata
{
    definition(name: "HADB Generic Component Media Player", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/HADBgenericComponentMediaPlayer.groovy")
    {
        capability "Actuator"
        capability "Switch"
        capability "MusicPlayer"
        capability "AudioVolume"
        capability "MediaInputSource"
        capability "Refresh"
        capability "Health Check"

        command "playTrack", [[ name: "trackUri*", type: "STRING", description: "Media Content" ], [ name: "mediaType*", type: "ENUM", constraints: [ "music", "tvshow", "movie", "video", "episode", "channel", "playlist", "image", "url", "game", "app" ], description: "Media Type" ]]
        command "shuffle", [[ name: "shuffleValue*", type: "ENUM", defaultValue: "false", constraints: [ "true", "false" ], description: "Shuffle ?" ]]
        command "repeat", [[ name: "repeatValue*", type: "ENUM", defaultValue: "false", constraints: [ "true", "false" ], description: "Repeat ?" ]]
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
    attribute "healthStatus", "enum", ["offline", "online"]
    attribute "shuffle", "enum", ["true", "false"]
    attribute "repeat", "enum", ["true", "false"]
    attribute "mediaType", "string"
    attribute "duration", "number"
    attribute "position", "number"
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
        if (it.name in ["status", "healthStatus", "switch", "volume", "mute", "mediaInputSource", "supportedInputs", "trackData", "mediaType", "duration", "position", "trackDescription"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

void refresh() {
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

void setLevel(level) {
    parent?.componentSetVolume(this.device, level)
}

void setInputSource(source) {
    parent?.componentSetInputSource(this.device, source)
}

void pause() {
    parent?.componentPauseMedia(this.device)
}

void play() {
    parent?.componentPlay(this.device)
}

void stop() {
    parent?.componentStopMedia(this.device)
}

void playText(text) {
    // parent?.componentPlayText(this.device, text)
    log.warn "playText is not implemented"
}

void playTrack(trackUri) {
    parent?.componentPlayTrack(this.device, "music", trackUri)
    log.warn "media type set to music for default playTrack command"
}

void playTrack(trackUri, mediaType) {
    parent?.componentPlayTrack(this.device, mediaType, trackUri)
}

void previousTrack() {
    parent?.componentPreviousTrack(this.device)
}

void nextTrack() {
    parent?.componentNextTrack(this.device)
}

void restoreTrack(trackUri) {
    // parent?.componentRestoreTrack(this.device, trackUri)
    log.warn "restoreTrack is not implemented"
}

void resumeTrack(trackUri) {
    // parent?.componentResumeTrack(this.device, trackUri)
    log.warn "resumeTrack is not implemented"
}

void setTrack(trackUri) {
    // parent?.componentSetTrack(this.device, trackUri)
    log.warn "setTrack is not implemented"
}

void shuffle(shuffleValue) {
    parent?.componentShuffle(this.device, shuffleValue)
}

void repeat(repeatValue) {
    parent?.componentRepeat(this.device, repeatValue)
}

void ping() {
    refresh()
}
