package top.haoshenqi.holiday.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * http util
 */
@Slf4j
public class HttpUtil {
    public static String get(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Headers headers = request.headers();
        headers.newBuilder().add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
        log.info("request url:{}", url);
        try (Response response = client.newCall(request).execute()) {
            String result = response.body().string();
            log.info("response:{}", result);
            return result;
        } catch (Exception e) {
            log.error("request error", e);
            return null;
        }
    }
}
