package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        // detect download folder
        String downloadFolder = "music/";
        createDownloadFolder(downloadFolder);

        var singleSong = readSingleSongFile(downloadFolder);
        var playlistSong = readPlaylistSongFile(downloadFolder);

        List<ProcessBuilder> processBuilderList = new ArrayList<>();
        processBuilderList.addAll(singleSong);
        processBuilderList.addAll(playlistSong);

        if (processBuilderList.isEmpty()) {
            throw new RuntimeException("Your list is empty");
        }

        System.out.println("Start downloading...");


        // multi-threading tasks
        CountDownLatch countDownLatch = new CountDownLatch(processBuilderList.size());

        ExecutorService service = Executors.newCachedThreadPool();

        List<Runnable> tasks = createRunnable(processBuilderList, countDownLatch);

        for (var task : tasks) {
            service.execute(task);
        }

        countDownLatch.await();

        service.shutdown();
    }

    private static void createDownloadFolder(String downloadFolder) {
        Path path = Path.of(downloadFolder);

        if (Files.notExists(path)) {
            File file = path.toFile();
            file.mkdir();
        }

    }

    /**
     * Reads playlist song file
     */
    private static List<ProcessBuilder> readPlaylistSongFile(String downloadFolder) throws IOException {
        try (var playListInputStream = Files.lines(Path.of("playlist.txt"))) {
            Set<String> linkSet = playListInputStream.collect(Collectors.toSet());

            return createPlayListProcessBuilders(linkSet, downloadFolder);
        }
    }

    /**
     * Creates playlist processbuilder
     */
    private static List<ProcessBuilder> createPlayListProcessBuilders(Set<String> linkSet, String downloadFolder) {
        return linkSet.stream()
                .map(link -> {
                            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-x", "--yes-playlist", link);
                            pb.directory(new File(downloadFolder));
                            return pb;
                        }
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Reads single song file by line
     */
    private static List<ProcessBuilder> readSingleSongFile(String downloadFolder) throws IOException {
        try (var singleSongInputStream = Files.lines(Path.of("list.txt"))) {
            Set<String> linkSet = singleSongInputStream.collect(Collectors.toSet());

            return createSingleSongProcessBuilders(linkSet, downloadFolder);
        }
    }

    private static List<ProcessBuilder> createSingleSongProcessBuilders(Set<String> linkSet, String downloadFolder) {
        return linkSet.stream()
                .map(link -> {
                            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-x", link);
                            pb.directory(new File(downloadFolder));
                            return pb;
                        }
                )
                .toList();
    }

    private static List<Runnable> createRunnable(List<ProcessBuilder> processBuilderList,
                                                 CountDownLatch countDownLatch) {

        List<Runnable> list = new ArrayList<>();

        for (ProcessBuilder e : processBuilderList) {
            Runnable task = () -> {
                try {
                    e.start();
                    // print to console
                    InputStream inputStream = e.start().getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    var line = reader.readLine();

                    while (line != null) {
                        System.out.println(line);
                        line = reader.readLine();
                    }
                    System.out.println("Finished");
                    countDownLatch.countDown();

                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            };
            list.add(task);
        }
        return list;
    }

}