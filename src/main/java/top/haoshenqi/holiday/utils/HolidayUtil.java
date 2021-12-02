package top.haoshenqi.holiday.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * holiday util
 * @author haosh
 */
@Slf4j
public class HolidayUtil {

    public static String getMonth(String year, String month){
        String result = "fail";
        String query;
        query = year+"年"+month+"月";
        long timeMillis = System.currentTimeMillis();
        String apiUrl = "https://sp1.baidu.com/8aQDcjqpAAV3otqbppnN2DJv/api.php?tn=wisetpl&format=json&query="+query+"&co=&resource_id=39043&t="+ timeMillis +"&cb=op_aladdin_callback"+timeMillis;
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse hResponse = null;
        try {
            HttpGet method = new HttpGet(apiUrl);
            httpclient = HttpClientBuilder.create().build();
            method.setHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
            hResponse = httpclient.execute(method);
            HttpEntity repEntity = hResponse.getEntity();
            int statusCode = hResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                method.abort();
            }
            String content = EntityUtils.toString(repEntity, "UTF-8");
            result = reParseJson(content);
            return  result;
        } catch (Exception e) {
            log.info(" SocketTimeoutException " + System.currentTimeMillis());
            log.error(e.getMessage(),e);
        }finally {
            if (hResponse != null) {
                try {
                    hResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException e) {
                }
            }
            return result;
        }
    }

    /**
     * 解析json
     */
    public static String  reParseJson(String  old){
        int start = old.indexOf("{");
        int end = old.lastIndexOf("}");
        return old.substring(start,end+1);
    }
}
