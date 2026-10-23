package com.example.data.repository

import com.example.data.model.Song

/**
 * Built-in song repertoire for initial library setup.
 */
object SampleSongCatalogue {

    fun getDefaultSongs(): List<Song> = listOf(
        Song(
            title = "Hotel California",
            artist = "Eagles",
            bpm = 74,
            keySignature = "Bm",
            originalTuning = "Standard (E A D G B e)",
            capo = 0,
            isFavorite = true,
            category = "Gigs",
            scrollSpeedPxPerSec = 22f,
            rawLyrics = """
[Intro]
[Bm] [F#] [A] [E] [G] [D] [Em] [F#]

[Verse 1]
On a [Bm]dark desert highway, [F#]cool wind in my hair
[A]Warm smell of colitas, [E]rising up through the air
[G]Up ahead in the distance, [D]I saw a shimmering light
[Em]My head grew heavy and my sight grew dim, [F#]I had to stop for the night

[Chorus]
[G]Welcome to the Hotel Cali[D]fornia
Such a [Em]lovely place (such a lovely place), such a [Bm]lovely face
[G]Plenty of room at the Hotel Cali[D]fornia
Any [Em]time of year (any time of year), you can [F#]find it here
            """.trimIndent()
        ),
        Song(
            title = "Wonderwall",
            artist = "Oasis",
            bpm = 88,
            keySignature = "G",
            originalTuning = "Standard (E A D G B e)",
            capo = 2,
            isFavorite = true,
            category = "Gigs",
            scrollSpeedPxPerSec = 25f,
            rawLyrics = """
[Intro]
[Em7] [G] [Dsus4] [A7sus4]

[Verse 1]
[Em7]Today is [G]gonna be the day that they're [Dsus4]gonna throw it back to [A7sus4]you
[Em7]By now you [G]should've somehow rea[Dsus4]lized what you gotta [A7sus4]do
[Em7]I don't believe that [G]anybody [Dsus4]feels the way I [A7sus4]do about you [Cadd9]now [Dsus4] [A7sus4]

[Chorus]
Because [Cadd9]maybe, [Em7] [G]
You're gonna [Em7]be the one that [Cadd9]saves me [Em7] [G]
And [Em7]after [Cadd9]all, [Em7] [G]
You're my [Em7]wonder[Cadd9]wall [Em7] [G] [Em7]
            """.trimIndent()
        ),
        Song(
            title = "Hallelujah",
            artist = "Leonard Cohen",
            bpm = 56,
            keySignature = "C",
            originalTuning = "Standard (E A D G B e)",
            capo = 0,
            isFavorite = false,
            category = "Rehearsals",
            scrollSpeedPxPerSec = 20f,
            rawLyrics = """
[Intro]
[C] [Am] [C] [Am]

[Verse 1]
I've [C]heard there was a [Am]secret chord
That [C]David played, and it [Am]pleased the Lord
But [F]you don't really [G]care for music, [C]do you? [G]
It [C]goes like this: the [F]fourth, the [G]fifth
The [Am]minor fall and the [F]major lift
The [G]baffled king com[E7]posing Halle[Am]lujah

[Chorus]
Halle[F]lujah, Halle[Am]lujah
Halle[F]lujah, Halle[C]lu[G]--[C]jah
            """.trimIndent()
        ),
        Song(
            title = "Let It Be",
            artist = "The Beatles",
            bpm = 72,
            keySignature = "C",
            originalTuning = "Standard (E A D G B e)",
            capo = 0,
            isFavorite = false,
            category = "Rehearsals",
            scrollSpeedPxPerSec = 24f,
            rawLyrics = """
[Verse 1]
When I [C]find myself in [G]times of trouble, [Am]Mother Mary [F]comes to me
[C]Speaking words of [G]wisdom, let it [F]be [C]
And [C]in my hour of [G]darkness she is [Am]standing right in [F]front of me
[C]Speaking words of [G]wisdom, let it [F]be [C]

[Chorus]
Let it [Am]be, let it [G]be, let it [F]be, let it [C]be
[C]Whisper words of [G]wisdom, let it [F]be [C]
            """.trimIndent()
        ),
        Song(
            title = "Knockin' on Heaven's Door",
            artist = "Bob Dylan",
            bpm = 68,
            keySignature = "G",
            originalTuning = "Standard (E A D G B e)",
            capo = 0,
            isFavorite = false,
            category = "Gigs",
            scrollSpeedPxPerSec = 20f,
            rawLyrics = """
[Verse 1]
[G]Mama, take this [D]badge off of [Am]me
[G]I can't [D]use it any[C]more
[G]It's gettin' [D]dark, too dark to [Am]see
[G]I feel like I'm [D]knockin' on heaven's [C]door

[Chorus]
[G]Knock, knock, [D]knockin' on heaven's [Am]door
[G]Knock, knock, [D]knockin' on heaven's [C]door
[G]Knock, knock, [D]knockin' on heaven's [Am]door
[G]Knock, knock, [D]knockin' on heaven's [C]door
            """.trimIndent()
        ),
        Song(
            title = "Sweet Child O' Mine",
            artist = "Guns N' Roses",
            bpm = 126,
            keySignature = "D",
            originalTuning = "Half-Step Down (Eb Ab Db Gb Bb eb)",
            capo = 0,
            isFavorite = false,
            category = "Band Setlists",
            scrollSpeedPxPerSec = 30f,
            rawLyrics = """
[Verse 1]
[D]She's got a smile that it seems to me
Re[C]minds me of childhood memories
Where [G]everything was as fresh as the bright blue [D]sky

[Chorus]
[A]Whoa, [B]sweet [C]child o' mine [D]
[A]Whoa, [B]oh, oh, oh, [C]sweet love of mine [D]
            """.trimIndent()
        ),
        Song(
            title = "Creep",
            artist = "Radiohead",
            bpm = 92,
            keySignature = "G",
            originalTuning = "Standard (E A D G B e)",
            capo = 0,
            isFavorite = false,
            category = "Gigs",
            scrollSpeedPxPerSec = 25f,
            rawLyrics = """
[Verse 1]
When you were here be[G]fore, couldn't look you in the [B]eye
You're just like an [C]angel, your skin makes me [Cm]cry
You float like a [G]feather in a beautiful [B]world
I wish I was [C]special, you're so fuckin' [Cm]special

[Chorus]
But I'm a [G]creep, I'm a [B]weirdo
What the hell am I [C]doin' here?
I don't be[Cm]long here
            """.trimIndent()
        ),
        Song(
            title = "Riptide",
            artist = "Vance Joy",
            bpm = 102,
            keySignature = "Am",
            originalTuning = "Standard (E A D G B e)",
            capo = 1,
            isFavorite = false,
            category = "Studio Drafts",
            scrollSpeedPxPerSec = 28f,
            rawLyrics = """
[Verse 1]
[Am]I was scared of [G]dentists and the [C]dark
[Am]I was scared of [G]pretty girls and [C]starting conversations
[Am]Oh, all my [G]friends are turning [C]green
You're the magician's [Am]assistant in [G]their dream [C]

[Chorus]
[Am]Lady, [G]running down to the [C]riptide
Taken away to the [Am]dark side
[G]I wanna be your [C]left hand man
            """.trimIndent()
        )
    )
}
