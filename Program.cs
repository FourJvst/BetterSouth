using System.Text;
using WindowsMediaController;
using Windows.Storage.Streams;

if (args.Length != 1)
{
    return 2;
}

string outputPath = Path.GetFullPath(args[0]);
Directory.CreateDirectory(Path.GetDirectoryName(outputPath)!);
string commandDirectory = Path.Combine(Path.GetDirectoryName(outputPath)!, "commands");
Directory.CreateDirectory(commandDirectory);
string coverPath = Path.Combine(Path.GetDirectoryName(outputPath)!, "cover.img");

using var mediaManager = new MediaManager();
await mediaManager.StartAsync();
var lastErrorLogged = DateTimeOffset.MinValue;
var lastStateRefresh = DateTimeOffset.MinValue;
string? lastThumbnailTrack = null;

while (true)
{
    try
    {
        await ProcessCommandsAsync(mediaManager, commandDirectory);
        if (DateTimeOffset.UtcNow - lastStateRefresh < TimeSpan.FromSeconds(1))
        {
            await Task.Delay(TimeSpan.FromMilliseconds(100));
            continue;
        }
        lastStateRefresh = DateTimeOffset.UtcNow;

        var sessions = mediaManager.CurrentMediaSessions.Values
            .Where(session => session.Id.Contains("spotify", StringComparison.OrdinalIgnoreCase))
            .ToArray();
        var focused = mediaManager.GetFocusedSession();
        var spotifySession = focused is not null &&
                             focused.Id.Contains("spotify", StringComparison.OrdinalIgnoreCase)
            ? focused
            : sessions.FirstOrDefault();

        if (spotifySession is null)
        {
            WriteState(outputPath, "", "", "");
            ClearThumbnail(coverPath);
            lastThumbnailTrack = null;
        }
        else
        {
            var properties = await spotifySession.ControlSession.TryGetMediaPropertiesAsync();
            var playback = spotifySession.ControlSession.GetPlaybackInfo().PlaybackStatus.ToString();
            WriteState(outputPath, properties.Title, properties.Artist, playback);
            string trackKey = properties.Title + "\0" + properties.Artist;
            if (!string.Equals(lastThumbnailTrack, trackKey, StringComparison.Ordinal))
            {
                await WriteThumbnailAsync(properties.Thumbnail, coverPath);
                lastThumbnailTrack = trackKey;
            }
        }
    }
    catch (Exception exception)
    {
        if (DateTimeOffset.UtcNow - lastErrorLogged >= TimeSpan.FromSeconds(30))
        {
            await File.AppendAllTextAsync(
                outputPath + ".log",
                $"{DateTimeOffset.UtcNow:O} {exception}{Environment.NewLine}");
            lastErrorLogged = DateTimeOffset.UtcNow;
        }
    }

    await Task.Delay(TimeSpan.FromMilliseconds(100));
}

static async Task ProcessCommandsAsync(MediaManager mediaManager, string commandDirectory)
{
    var files = Directory.GetFiles(commandDirectory, "*.cmd")
        .OrderBy(Path.GetFileName, StringComparer.Ordinal)
        .ToArray();
    foreach (string file in files)
    {
        string command = await File.ReadAllTextAsync(file);
        File.Delete(file);
        var spotifySession = GetSpotifySession(mediaManager);
        if (spotifySession is null)
        {
            continue;
        }

        switch (command)
        {
            case "previous":
                await spotifySession.ControlSession.TrySkipPreviousAsync();
                break;
            case "toggle":
                var controls = spotifySession.ControlSession.GetPlaybackInfo().Controls;
                if (controls.IsPauseEnabled == true)
                {
                    await spotifySession.ControlSession.TryPauseAsync();
                }
                else if (controls.IsPlayEnabled == true)
                {
                    await spotifySession.ControlSession.TryPlayAsync();
                }
                else
                {
                    await spotifySession.ControlSession.TryTogglePlayPauseAsync();
                }
                break;
            case "next":
                await spotifySession.ControlSession.TrySkipNextAsync();
                break;
        }
    }
}

static MediaManager.MediaSession? GetSpotifySession(MediaManager mediaManager)
{
    var focused = mediaManager.GetFocusedSession();
    if (focused is not null && focused.Id.Contains("spotify", StringComparison.OrdinalIgnoreCase))
    {
        return focused;
    }

    return mediaManager.CurrentMediaSessions.Values.FirstOrDefault(
        session => session.Id.Contains("spotify", StringComparison.OrdinalIgnoreCase));
}

static async Task WriteThumbnailAsync(IRandomAccessStreamReference? thumbnail, string coverPath)
{
    if (thumbnail is null)
    {
        ClearThumbnail(coverPath);
        return;
    }

    using var stream = await thumbnail.OpenReadAsync();
    if (stream.Size == 0 || stream.Size > 10 * 1024 * 1024)
    {
        ClearThumbnail(coverPath);
        return;
    }

    using var reader = new DataReader(stream.GetInputStreamAt(0));
    uint size = checked((uint)stream.Size);
    await reader.LoadAsync(size);
    byte[] bytes = new byte[checked((int)size)];
    reader.ReadBytes(bytes);

    string temporaryPath = coverPath + ".tmp";
    await File.WriteAllBytesAsync(temporaryPath, bytes);
    File.Move(temporaryPath, coverPath, true);
}

static void ClearThumbnail(string coverPath)
{
    if (File.Exists(coverPath))
    {
        File.Delete(coverPath);
    }
}

static void WriteState(string outputPath, string title, string artist, string playback)
{
    string[] lines =
    [
        Convert.ToBase64String(Encoding.UTF8.GetBytes(title)),
        Convert.ToBase64String(Encoding.UTF8.GetBytes(artist)),
        Convert.ToBase64String(Encoding.UTF8.GetBytes(playback))
    ];
    string temporaryPath = outputPath + ".tmp";
    File.WriteAllLines(temporaryPath, lines, new UTF8Encoding(false));
    File.Move(temporaryPath, outputPath, true);
}
