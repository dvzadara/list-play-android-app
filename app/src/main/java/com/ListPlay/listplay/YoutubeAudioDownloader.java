package com.ListPlay.listplay;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class YoutubeAudioDownloader {
    public static final int START_PROGRESS = 1;
    public static final int SHOW_PROGRESS = 2;
    public static final int END_PROGRESS = 3;

    private static Handler mHandler;

    // Скачивает ютуб плейлист по ссылке и записывает его в папку с mp3 файлами
    public static boolean YoutubePlaylistToFiles(String youtubePlaylistUrl, String Name, File outputDir,
                                                 MainActivity mainActivity, PlaylistsViewModel playlistsViewModel) throws InterruptedException {
        Log.d("YoutubeAudioDownloader", "func called");
        if (!isYoutubePlaylistUrl(youtubePlaylistUrl) && !isYoutubeVideoUrl(youtubePlaylistUrl)) {
            Log.d("YoutubeAudioDownloader", "isNotYoutubePlaylistUrl " + youtubePlaylistUrl);
            return false;
        }
        Log.d("YoutubeAudioDownloader", "link is playlist");

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case START_PROGRESS:
                        mainActivity.startProgress(msg.arg1);
                        break;
                    case SHOW_PROGRESS:
                        mainActivity.showProgress(msg.arg1);
                        break;
                    case END_PROGRESS:
                        mainActivity.endProgress(msg.arg1);
                        break;
                }
            }
        };
        int videoCount;
        List<String> videosIdList;
        if (isYoutubePlaylistUrl(youtubePlaylistUrl)) {
            // Получение id всех видео в плейлисте
            String playlistId = getPlaylistIdFromUrl(youtubePlaylistUrl);
            YoutubeDownloader downloader = new YoutubeDownloader();
            Log.d("YoutubeAudioDownloader", "create downloader");
            RequestPlaylistInfo request = new RequestPlaylistInfo(playlistId);
            Log.d("YoutubeAudioDownloader", "create request");
            Response<PlaylistInfo> response = downloader.getPlaylistInfo(request);
            PlaylistInfo playlistInfo = response.data();
            videoCount = playlistInfo.details().videoCount();
            videosIdList = new ArrayList<>();
            for (PlaylistVideoDetails video : playlistInfo.videos()) {
                videosIdList.add(video.videoId());
            }
        } else {
            videosIdList = new ArrayList<>();
            videosIdList.add(getVideoIdFromUrl(youtubePlaylistUrl));
            videoCount = 1;
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        int maxPlaylistOrder = playlistsViewModel.getMaxPlaylistOrder();
        PlaylistDB playlist = new PlaylistDB(Name, maxPlaylistOrder + 1);

        int playlistDatabaseId = 0;
        try {
            playlistDatabaseId = (int)playlistsViewModel.insertPlaylist(playlist);
            playlist.setId(playlistDatabaseId);
        } catch (ExecutionException e) {
            return false;
        }
        mainActivity.setDownloadablePlaylistId(playlistDatabaseId);

        File playlistDir = new File(outputDir, "" + playlistDatabaseId);
        boolean success = playlistDir.mkdir();
        Log.d("YoutubeAudioDownloader", "create dir");

        mHandler.obtainMessage(START_PROGRESS, videoCount, 0).sendToTarget();
        Thread.sleep(100);

        // Извлечение из каждого видео mp3 файла
        int i = 0;
        for (String videoId : videosIdList) {
            Log.d("YoutubeAudioDownloader", Integer.toString(i));
            completionService.submit(() -> {
                try {
                    YoutubeVideoToFile(videoId, playlistDir, playlistsViewModel, playlist);
                } catch (IOException e) {
                    Log.d("YoutubeAudioDownloader.fileDownloadError", e.getMessage());
                } finally {
                    int completeTasks = (int)executor.getCompletedTaskCount() + 1;
                    mHandler.obtainMessage(SHOW_PROGRESS, completeTasks, 0).sendToTarget();
                    Thread.sleep(100);
                }
                Log.d("YoutubeAudioDownloader", videoId + " song downloaded");
                return null;
            });
            i++;
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        mHandler.obtainMessage(END_PROGRESS, videoCount, 0).sendToTarget();
        mainActivity.setDownloadablePlaylistId(-1);
        Thread.sleep(100);
        return true;
    }

    // Скачивает ютуб плейлист по ссылке и записывает его в папку с mp3 файлами
    public static boolean YoutubePlaylistToFiles(String youtubePlaylistUrl, int playlistContainerId, File playlistDir,
                                                 MainActivity mainActivity, PlaylistsViewModel playlistsViewModel) throws InterruptedException {
        Log.d("YoutubeAudioDownloader", "func called");
        if (!isYoutubePlaylistUrl(youtubePlaylistUrl) && !isYoutubeVideoUrl(youtubePlaylistUrl)) {
            Log.d("YoutubeAudioDownloader", "isNotYoutubePlaylistUrl " + youtubePlaylistUrl);
            return false;
        }
        Log.d("YoutubeAudioDownloader", "link is playlist");
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case START_PROGRESS:
                        mainActivity.startProgress(msg.arg1);
                        break;
                    case SHOW_PROGRESS:
                        mainActivity.showProgress(msg.arg1);
                        break;
                    case END_PROGRESS:
                        mainActivity.endProgress(msg.arg1);
                        break;
                }
            }
        };
        int videoCount;
        List<String> videosIdList;
        if (isYoutubePlaylistUrl(youtubePlaylistUrl)) {
            // Получение id всех видео в плейлисте
            String playlistId = getPlaylistIdFromUrl(youtubePlaylistUrl);
            YoutubeDownloader downloader = new YoutubeDownloader();
            Log.d("YoutubeAudioDownloader", "create downloader");
            RequestPlaylistInfo request = new RequestPlaylistInfo(playlistId);
            Log.d("YoutubeAudioDownloader", "create request");
            Response<PlaylistInfo> response = downloader.getPlaylistInfo(request);
            PlaylistInfo playlistInfo = response.data();
            videoCount = playlistInfo.details().videoCount();
            videosIdList = new ArrayList<>();
            for (PlaylistVideoDetails video : playlistInfo.videos()) {
                videosIdList.add(video.videoId());
            }
        } else {
            videosIdList = new ArrayList<>();
            videosIdList.add(getVideoIdFromUrl(youtubePlaylistUrl));
            videoCount = 1;
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        PlaylistDB playlist = playlistsViewModel.getPlaylistById(playlistContainerId);

        mainActivity.setDownloadablePlaylistId(playlist.getId());

        mHandler.obtainMessage(START_PROGRESS, videoCount, 0).sendToTarget();
        Thread.sleep(100);

        // Извлечение из каждого видео mp3 файла
        int i = 0;
        for (String videoId : videosIdList) {
            Log.d("YoutubeAudioDownloader", Integer.toString(i));
            completionService.submit(() -> {
                try {
                    YoutubeVideoToFile(videoId, playlistDir, playlistsViewModel, playlist);
                } catch (IOException e) {
                    Log.d("YoutubeAudioDownloader.fileDownloadError", e.getMessage());
                } finally {
                    int completeTasks = (int)executor.getCompletedTaskCount() + 1;
                    mHandler.obtainMessage(SHOW_PROGRESS, completeTasks, 0).sendToTarget();
                    Thread.sleep(100);
                }
                Log.d("YoutubeAudioDownloader", videoId + " song downloaded");
                return null;
            });
            i++;
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        mHandler.obtainMessage(END_PROGRESS, videoCount, 0).sendToTarget();
        mainActivity.setDownloadablePlaylistId(-1);
        Thread.sleep(100);
        return true;
    }

    // Проверка наличия плейлиста в ютуб ссылке
    public static boolean isYoutubePlaylistUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            String query = url.getQuery();
            return host.endsWith("youtube.com") && query != null && query.contains("list=");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    // Проверка наличия видео в ютуб ссылке
    public static boolean isYoutubeVideoUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            String query = url.getQuery();
            boolean isUrlType1 = host.endsWith("youtube.com") && query != null && query.contains("v=");
            boolean isUrlType2 = host.endsWith("youtu.be");
            return isUrlType1 || isUrlType2;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    // Достает из ссылки типа https://www.youtube.com/playlist?list=PLjpRURW6qyHPtDrXdo8tM9CdwvACczDw7
    // id плейлиста(здесь: PLjpRURW6qyHPtDrXdo8tM9CdwvACczDw7)
    public static String getPlaylistIdFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String query = url.getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equals("list")) {
                    return value;
                }
            }
            return null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    // Достает из ссылки типа https://www.youtube.com/playlist?v=PLjpRURW6qyHPtDrXdo8tM9CdwvACczDw7
    // id видео(здесь: PLjpRURW6qyHPtDrXdo8tM9CdwvACczDw7)
    public static String getVideoIdFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            if (url.getHost().endsWith("youtu.be")) {
                return urlString.substring(urlString.lastIndexOf("/") + 1, urlString.indexOf("?"));
            }

            String query = url.getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equals("v")) {
                    return value;
                }
            }
            return null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    // Скачивает аудио из ютуб плейлиста и конвертирует в mp3
    public static void YoutubeVideoToFile(String videoId, File outputDir, PlaylistsViewModel playlistsViewModel, PlaylistDB playlist) throws IOException {
        // Запрос информации о видео(название, видео или аудио формат)
        YoutubeDownloader downloader = new YoutubeDownloader();
        RequestVideoInfo request = new RequestVideoInfo(videoId);
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        Log.d("YoutubeAudioDownloader.videoId", videoId);
        Log.d("YoutubeAudioDownloader.fileDownloadError", response.status().toString());
        VideoInfo video = response.data();
        // Аудио формат
        Format format = video.bestAudioFormat();
        // Имя файла, совпадает с именем видео, но нормализируется(транслитерация и удаление недопустимых символов)
        String fileName = fileNameNormalize(video.details().title());
        RequestVideoFileDownload request_video_download;
        Response<File> response_video_download;
        if (format!=null) {
            // Скачивание аудиодорожки
            request_video_download = new RequestVideoFileDownload(format)
                    .saveTo(outputDir)
                    .renameTo(fileName)
                    .overwriteIfExists(true);
            response_video_download = downloader.downloadVideoFile(request_video_download);
        }
        // Если скачивание аудиодорожки прошло неуспешно то скачивается все видео вместе с аудиорядом
        else {
            format = video.bestVideoWithAudioFormat();
            request_video_download = new RequestVideoFileDownload(format)
                    .saveTo(outputDir)
                    .renameTo(fileName)
                    .overwriteIfExists(true);

            response_video_download = downloader.downloadVideoFile(request_video_download);
        }
        // После скачивания в папке outputDir будет находится файл с именем fileName и расширением m4a или mp4

        // Записываем путь к скачанному файлу в переменную mediaFilePath
        File mediaFile = response_video_download.data();
        String mediaFilePath = mediaFile.getAbsolutePath();

        //Записываем путь к будущему mp3 файлу в переменную mp3FilePath
        String mp3FilePath = mediaFilePath.substring(0, mediaFilePath.length() - 4) + ".mp3";

        // Конвертируем скачанный файл в mp3
        mediaFileToMp3(mediaFilePath, mp3FilePath);
        Log.d("YoutubeAudioDownloader.fileDownloadResult", mp3FilePath.toString());
        SongDB song = new SongDB(video.details().title(), mp3FilePath,
                playlistsViewModel.getMaxSongOrder(playlist.getId()) + 1, playlist.getId());
        playlistsViewModel.insertSong(song);
    }

    // Транслитерация и удаление всех не допустимых для имени файла символов
    public static String fileNameNormalize(String fileName){
        String newName;
        newName = fileName.toLowerCase(Locale.ROOT);
//        newName = newName.replaceAll(" ", "_");
        newName = newName.replaceAll("[^\\p{L}\\p{Nd}\\s]", "");
        newName = transliterate(newName);
        return newName;
    }

    // Транслитерация строки
    public static String transliterate(String message){
        char[] abcCyr =   {' ','а','б','в','г','д','е','ё', 'ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х', 'ц','ч', 'ш','щ','ъ','ы','ь','э', 'ю','я','А','Б','В','Г','Д','Е','Ё', 'Ж','З','И','Й','К','Л','М','Н','О','П','Р','С','Т','У','Ф','Х', 'Ц', 'Ч','Ш', 'Щ','Ъ','Ы','Ь','Э','Ю','Я','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
        String[] abcLat = {" ","a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch", "","i", "","e","ju","ja","A","B","V","G","D","E","E","Zh","Z","I","Y","K","L","M","N","O","P","R","S","T","U","F","H","Ts","Ch","Sh","Sch", "","I", "","E","Ju","Ja","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            for (int x = 0; x < abcCyr.length; x++ ) {
                if (message.charAt(i) == abcCyr[x]) {
                    builder.append(abcLat[x]);
                }
            }
            if ('0' <= message.charAt(i) && message.charAt(i) <= '9')
                builder.append(message.charAt(i));
        }
        return builder.toString();
    }

    // Конвертация файла с помощью FFmpeg и удаление старого файла
    public static void mediaFileToMp3(String mediaFilePath, String mp3FilePath){
//        String[] c = {"-i", mediaFilePath, mp3FilePath};
        String[] c = {"-i", mediaFilePath, "-vn", "-ar", "44100", "-ac", "2", "-b:a", "192k", mp3FilePath};
        FFmpeg.execute(c);
        Log.d("YoutubeAudioDownloader.mediaFileToMp3", mediaFilePath + " " + mp3FilePath);
        File inputFile = new File(mediaFilePath);
        if (inputFile.exists()) {
            inputFile.delete();
        }
    }
}
