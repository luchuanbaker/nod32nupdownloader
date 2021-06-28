package com.clu.tools;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.clu.tools.utils.HttpUtils;
import com.clu.tools.utils.JSON;
import com.clu.tools.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Hello world!
 */
public class App {

    private static void initLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        ch.qos.logback.classic.Logger root = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%level]%replace(%caller{1}){'\\t|Caller.{1}0|\\r\\n|\\n', ''} {%thread} %msg%n");
        encoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) root.getAppender("console");
        consoleAppender.setEncoder(encoder);
        // PatternLayoutEncoder没有初始化，调用此方法会触发其初始化ch.qos.logback.core.OutputStreamAppender.encoderInit()
        consoleAppender.start();

        loggerContext.getLogger(App.class.getPackage().getName()).setLevel(Level.DEBUG);
    }

    static {
        initLogger();
    }

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static boolean isInIDE = false;

    private static String serverRoot = "http://86.110.227.111/NOD/";
    // private static String serverRoot = "http://193.108.251.102/";

    private static String folder = "E:\\迅雷下载\\nup-20200803";

    private static void init(String[] args) {
        logger.info("初始化命令行参数：{}", JSON.stringify(args));
        if (args != null && args.length >= 1 && !StringUtils.isBlank(args[0])) {
            serverRoot = args[0];
        } else {
            String nupUrl = System.getProperty("nup-url");
            if (!StringUtils.isBlank(nupUrl)) {
                serverRoot = nupUrl;
            }
        }

        if (!serverRoot.startsWith("http")) {
            throw new RuntimeException(StringUtils.format("非法的url：{}", serverRoot));
        }

        ProtectionDomain protectionDomain = App.class.getProtectionDomain();
        if (protectionDomain != null) {
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource != null) {
                URL location = codeSource.getLocation();
                if (location != null && location.toString().startsWith("file:")) {
                    isInIDE = true;
                }
            }
        }

        if (!Constants.IS_WIN || !isInIDE) {
            folder = System.getProperty("user.dir");
        } else {
            File file = new File(folder);
            if (!file.exists() && !file.mkdirs()) {
                throw new RuntimeException("创建目录失败：" + file.getAbsolutePath());
            }
        }
        logger.info("当前路径：{}", folder);
    }

    private static final int timeoutMillis = 20 * 1000;

    public static void main(String[] args) {
        init(args);

        String updateVerFileName = "update.ver";

        Document document;
        logger.info("请求:{}", serverRoot);
        try {
            document = Jsoup.parse(new URL(serverRoot), timeoutMillis);
        } catch (IOException e) {
            logger.info(StringUtils.format("下载：{}失败", serverRoot), e);
            System.exit(-1);
            return;
        }

        Elements a = document.getElementsByTag("a");
        List<String> serverNupFiles = a.stream()
            .filter(element -> {
                String href = element.attr("href");
                return StringUtils.endsWith(href, ".nup");
            }).map(
                element -> {
                    String href = element.attr("href");
                    if (href.contains("/")) {
                        href = StringUtils.substringAfterLast(href, "/");
                    }
                    return href;
                }
            ).collect(Collectors.toList());

        String[] localNupFilesArray = new File(folder).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nup");
            }
        });
        if (!ArrayUtils.isEmpty(localNupFilesArray) && !CollectionUtils.isEmpty(serverNupFiles)) {
            List<String> localNupFiles = new ArrayList<>(Arrays.asList(localNupFilesArray));
            for (String nupFile : serverNupFiles) {
                localNupFiles.remove(nupFile);
            }
            if (!localNupFiles.isEmpty()) {
                File deprecatedFolder = new File(folder, "deprecated");
                if (!deprecatedFolder.exists()) {
                    if (!deprecatedFolder.mkdirs()) {
                        throw new RuntimeException(StringUtils.format("创建目录{}失败", deprecatedFolder.getAbsolutePath()));
                    }
                }
                for (String fileName : localNupFiles) {
                    logger.info("移除旧文件：{}", fileName);
                    try {
                        FileUtils.moveFile(new File(folder, fileName), new File(deprecatedFolder, fileName));
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                    String metaFileName = fileName + ".meta";
                    File metaFile = new File(folder, metaFileName);
                    if (metaFile.exists()) {
                        try {
                            FileUtils.moveFile(metaFile, new File(deprecatedFolder, metaFileName));
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }

        String updateVerUrl = StringUtils.format("{}{}", serverRoot, updateVerFileName);
        String newVerContent = HttpUtils.getText(updateVerUrl);

        String localContent = null;
        File localUpdateVerFile = new File(folder, updateVerFileName);
        if (localUpdateVerFile.exists()) {
            try {
                localContent = FileUtils.readFileToString(localUpdateVerFile, Charsets.UTF_8);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }
        if (!newVerContent.equals(localContent)) {
            boolean success = downloadNups(serverRoot, serverNupFiles, folder);
            if (success) {
                try {
                    if (!localUpdateVerFile.exists()) {
                        if (!localUpdateVerFile.createNewFile()) {
                            throw new RuntimeException("创建本地文件失败：" + localUpdateVerFile.getAbsolutePath());
                        }
                    }
                    FileUtils.write(localUpdateVerFile, newVerContent);
                    logger.info("更新完毕");
                } catch (IOException e) {
                    logger.error(StringUtils.format("写入{}文件失败", localUpdateVerFile.getAbsolutePath()), e);
                    return;
                }
            } else {
                logger.info("没有全部更新成功，请稍后重试");
            }
        } else {
            logger.info("不用更新");
        }
        if (Constants.IS_WIN && !isInIDE) {
            logger.info("按任意键退出");
            try {
                System.in.read();
            } catch (IOException e) {
                // ignore
            }
        }
        System.exit(0);
    }

    private static ThreadPoolExecutor executor = null;

    private static boolean downloadNups(String serverRoot, List<String> nupFiles, String folder) {
        CountDownLatch countDownLatch = new CountDownLatch(nupFiles.size());
        if (executor == null) {
            executor = new ThreadPoolExecutor(50, 50, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        }
        AtomicInteger successCount = new AtomicInteger();
        for (String fileName : nupFiles) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    Thread currentThread = Thread.currentThread();
                    String threadName = currentThread.getName();
                    try {
                        currentThread.setName(StringUtils.format("t-{}", fileName));
                        String url = StringUtils.format("{}{}", serverRoot, fileName);
                        File dstFile = new File(folder, fileName);
                        logger.info("开始下载:{}", url);
                        boolean success = HttpUtils.downloadIfNotExist(url, dstFile, 3);
                        if (!success && dstFile.exists()) {
                            logger.error("下载文件{}失败", url);
                            try {
                                FileUtils.forceDelete(dstFile);
                            } catch (IOException e) {
                                logger.error(e.getMessage(), e);
                            }
                        } else {
                            successCount.incrementAndGet();
                            logger.info("下载:{}文件成功，剩余{}/{}", fileName, countDownLatch.getCount() - 1, nupFiles.size());
                        }
                    } finally {
                        countDownLatch.countDown();
                        currentThread.setName(threadName);
                    }
                }
            });
        }

        try {
            // 等待所有的任务完毕
            countDownLatch.await();
            logger.info("处理完毕：一共{}个，成功{}个", nupFiles.size(), successCount.get());
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        return nupFiles.size() == successCount.get();
    }
}
