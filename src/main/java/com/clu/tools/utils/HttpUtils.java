package com.clu.tools.utils;

import com.clu.tools.App;
import com.clu.tools.Constants;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static String getText(String uri) {
        InputStream inputStream = null;
        try {
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            inputStream = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, out);
            return new String(out.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(StringUtils.format("下载:{} 失败", uri), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static boolean downloadIfNotExist(String uri, File dstFile, int retryTimes) {
        int count = retryTimes;
        boolean getHeaderSuccess = false;
        JSONObject newMeta = null;
        HttpURLConnection connection = null;
        File metaFile = null;
        Long contentLengthLong = null;
        while (count > 0) {
            try {
                URL url = new URL(uri);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                String eTag = connection.getHeaderField("ETag");
                String lastModified = connection.getHeaderField("Last-Modified");
                String contentLength = connection.getHeaderField("Content-Length");
                if (StringUtils.isNumeric(contentLength)) {
                    contentLengthLong = Long.parseLong(contentLength);
                }
                newMeta = JSONObject.of(
                    "ETag", eTag,
                    "Last-Modified", lastModified,
                    "Content-Length", contentLength
                );
                metaFile = new File(dstFile.getParentFile(), dstFile.getName() + ".meta");
                if (!metaFile.exists()) {
                    if (!metaFile.createNewFile()) {
                        throw new IOException(StringUtils.format("创建{}失败", metaFile.getAbsolutePath()));
                    } else {
                        logger.info("成功创建meta文件：{}", metaFile.getAbsolutePath());
                    }
                } else {
                    String content = FileUtils.readFileToString(metaFile, Charsets.UTF_8);
                    JSONObject meta = JSON.parse(content, JSONObject.class);
                    if (newMeta.equals(meta)) {
                        // 检查文件大小
                        if (dstFile.exists() && StringUtils.equals(String.valueOf(dstFile.length()), contentLength)) {
                            logger.info("文件{}不需要更新", uri);
                            return true;
                        }
                    }
                }
                getHeaderSuccess = true;
                break;
            } catch (Exception e) {
                logger.warn("获取文件{}的header信息失败，重试({}/{}):{}:{}", uri, (retryTimes - count), retryTimes, e.getClass().getSimpleName(), e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            count--;
        }
        if (!getHeaderSuccess) {
            logger.error("获取文件{}的header信息失败", uri);
            return false;
        }

        count = retryTimes;
        boolean success = false;
        // 下载文件
        while (count > 0) {
            try {
                HttpUtils.downloadFile(uri, dstFile, App.isInIDE, contentLengthLong);
                success = true;
                break;
            } catch (Exception e) {
                if (count - 1 > 0) {
                    logger.warn("下载{}失败，重试({}/{}):{}:{}", uri, (retryTimes - count + 1), retryTimes, e.getClass().getSimpleName(), e.getMessage());
                }
            }
            count--;
        }
        if (success) {
            // 更新meta
            try {
                FileUtils.write(metaFile, newMeta.toJSONString());
            } catch (IOException e) {
                logger.error(StringUtils.format("更新meta文件{}失败", metaFile.getAbsolutePath()), e);
                success = false;
            }
        }
        return success;
    }

    public static void downloadFile(String uri, File dstFile, boolean useProxy, Long contentLength) throws IOException {
        if (!Constants.IS_WIN) {
            downloadByWGet(uri, dstFile);
            return;
        }
        URL url = new URL(uri);
        URLConnection connection;
        if (useProxy) {
            // Windows环境考虑用代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(1080));
            connection = url.openConnection(proxy);
        } else {
            connection = url.openConnection();
        }
        download0(connection, new FileOutputStream(dstFile), contentLength);
    }

    public static void downloadByWGet(String uri, File dstFile) throws IOException {
        String path = dstFile.getAbsolutePath();
        if (dstFile.exists()) {
            if (!dstFile.delete()) {
                throw new IOException(StringUtils.format("删除{}文件失败", path));
            }
        }
        ProcessBuilder processBuilder = new ProcessBuilder("wget", "--progress=dot", uri, "-O", path)
            .redirectErrorStream(true);
        logger.info(StringUtils.join(processBuilder.command(), " "));
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int code = process.waitFor();
            if (code != 0) {
                throw new IOException(StringUtils.format("下载失败，wget返回值为：{}", code));
            }
        } catch (InterruptedException e) {
            throw new IOException("wget等待被打断");
        }
    }

    public static void download0(URLConnection connection, OutputStream outputStream, Long contentLength) throws IOException {
        InputStream inputStream = null;
        long currentTime = System.currentTimeMillis();
        long downloadLength = 0;
        try {
            connection.setUseCaches(false);
            inputStream = connection.getInputStream();
            // buffer size 4k
            byte[] buffer = new byte[1024 * 4];
            int len = -1;
            while ((len = inputStream.read(buffer)) != -1) {
                downloadLength += len;
                outputStream.write(buffer, 0, len);
                if (System.currentTimeMillis() - currentTime > 5000/*超过5s，输出日志*/) {
                    logger.info("文件进度：{}% {}/{}", String.format("%.2f", downloadLength * 100.0 / contentLength), downloadLength, contentLength);
                    currentTime = System.currentTimeMillis();
                }
            }
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    public static void main(String[] args) {
        System.out.println(getText("http://86.110.227.111/NOD/update.ver"));
    }

}
