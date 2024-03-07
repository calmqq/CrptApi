import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    // огранические на кол-во запросов
    private final int requestLimit;
    // семафор для обеспечения ↑
    private final Semaphore semaphore;
    // HTTP-клиент
    private final CloseableHttpClient httpClient;
    // gson для его работы (О_О)
    private final Gson gson;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClients.createDefault();
        this.gson = new Gson();

        // иниц таймера для сброса семафора
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(1));
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void createDocument(String document, String signature) {
        try {
            semaphore.acquire();
            // формирую gson-запрос
            String json = "{\"document\": " + document + ", \"signature\": \"" + signature + "\"}";

            HttpPost request = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(json));

            // выполняю этот запрос
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            // полученный ответ обрабатываю
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                System.out.println("API response: " + result);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }
}