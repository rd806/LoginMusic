# Login Music

A simple Minecraft mod that can play music when players join in the world.

> This mod uses AI to assist in development.
>
> Inspired by TartaricAcid's [Net Music](https://modrinth.com/mod/net-music) !

| Milestone | Big changes                                                                                  |
|-----------|----------------------------------------------------------------------------------------------|
| 1.1.0     | javafx has given place to `com.googlecode.soundlibs:mp3spi`, which provides MP3 support now. |
| 1.2.0     | the music config has changed to JSON type.                                                   |
| 1.2.1     | Music can be played directly through the Internet without downloading; lyrics support!       |
| 1.2.2     | Audio backend loading, Players can receive audio from other players on the server.           | 
| 1.2.6     | Add cache system. Refactor the code.                                                         | 
| 2.0.0     | ModID changes to `loginmusic`; Using Minecraft Music Engine.                                 |

The logo comes from [here](https://www.flaticon.com/free-icon/music_9325026?term=music&page=1&position=87&origin=search&related_id=9325026), designed by juicy_fish.

## Functions

- [x] MP3 supported
- [x] Audio backend loading
- [x] Play different musics based on the player's name (uuid)
- [x] Show lyrics while playing music
- [ ] ...

## File Structure

> **Destructive changes**: 
> 
> Since mod version 1.2.5, the config file's storage has been changed! If you upgrade from previous version, please move them to the new place!

All music files are stored in the `/data/login_music` folder.

```
{version folder}
├── config
│   ├── loginmusic-music.json      # Specific musics
│   ├── loginmusic-client.toml     # Client config    
│   └── loginmusic-common.toml     # Common config   
│
├── data/login_music        # Store music files
│   ├── MusicsCache
│   │   ├── music_1.mp3
│   │   ├── music_2.wav
│   │   └── ...
│   └── LyricsCache   
│       ├── music_1.lrc
│       ├── music_2.lrc
│       └── ...
└── ...
```

## Configuration

**Since mod version 1.2.0, the music config has changed to JSON type, just as shown below.**

If you enter a server with this mod, the config file on the server has higher priority, which will cover your custom settings.

```json
{
  "defaultMusic": {
    "id": "Default",
    "musicName": "Default.mp3",
    "musicPath": "https://www.example.com/example.mp3",
    "lyricName": "Default.lrc",
    "lyricPath": "https://www.example.com/example.lrc"
  },  
  "musics": [
    {
      "id": "Steve",
      "musicName": "login_music_1.mp3",
      "musicPath": "https://www.example.com/login_music_1.mp3",
      "lyricName": "login_music_1.lrc",
      "lyricPath": "https://www.example.com/login_music_1.lrc"
    },
    {
      "id": "Alex",
      "musicName": "login_music_2.mp3",
      "musicPath": "https://www.example.com/login_music_2.mp3",
      "lyricName": "login_music_2.lrc",
      "lyricPath": "https://www.example.com/login_music_2.lrc"
    }
  ]
}
```

> The music will be downloaded if `InternetAccess` is set `true` (the default setting is `false`), otherwise, the music is played through network audio streams, which may cause a short pause when entering a world.

For example, When a player named Steve enters the world, the mod will try to find the music matched the name `Steve` (in this case it's `login_music_1.mp3` ) in the folder `data/login_music` first. Then try to download it from its related url if it has failed before.

> [!Note]
> If there is a file with the same name in the folder, it will skip the download even if the file does not match the URL.

If more entries need to be added, please follow the JSON's rule.

Since mod version 1.2.4, you can open Login Music Config from the main menu if `Cloth Config API` has been installed.

## Command

The Command system has been rebuilt since 1.2.6, all the commands need permission level 2. 

| Command                   | Function                              |
|---------------------------|---------------------------------------|
| `/loginmusic reload`      | reload music config                   |
| `/loginmusic list`        | show music config available currently |
| `/loginmusic stop`        | stop you current playing music        |
| `/loginmusic play`        | replay your login music               |
| `/loginmusic cache list`  | show the cached file                  |
| `/loginmusic cache clear` | clear the cached file                 |