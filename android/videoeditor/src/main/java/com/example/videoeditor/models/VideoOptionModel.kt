package com.example.videoeditor.models

class VideoOptionModel {
    var icon: Int = 0
    var title: String = ""

    constructor(title: String, icon: Int) {
        this.icon = icon
        this.title = title
    }
}