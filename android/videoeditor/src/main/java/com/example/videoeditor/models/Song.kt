package com.example.videoeditor.models

class Song {
    var title: String? = null
    var id = 0
    var artist: String? = null

    constructor(title: String?, id: Int, artist: String?) {
        this.title = title
        this.id = id
        this.artist = artist
    }
}