package com.ele.crawler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FacebookPostCrawlerWithLogin {

    public static void main(String[] args) {
        FacebookPostCrawlerWithLogin example = new FacebookPostCrawlerWithLogin();
        String url = "https://www.facebook.com/JesseTang11/";
        example.start(url);
    }

    private void start(String url) {

        Document doc = getDocument(url);

        Elements postList = getPostList(doc);

        List<FbPostDto> fbPostDtoList = parse(postList);

        Gson gson = new GsonBuilder().serializeNulls().create();
        System.out.println(gson.toJson(fbPostDtoList));
    }

    public Document getDocument(String url) {
        Gson gson = new Gson();
        System.setProperty("webdriver.chrome.driver", "file/driver/chrome/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");                        // 啟動 Chrome 最大化模式
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-notifications");               // 禁用通知
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("excludeSwitches", Arrays.asList("disable-popup-blocking")); // 封鎖對話方塊視窗

        WebDriver driver = new ChromeDriver(options);
        try {
            login(driver);

            driver.get(url);
            Thread.sleep(5000);

            for (int i = 0; i < 1; i++) {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
                System.out.println("scroll");
                Thread.sleep(5000); // 暫停5秒
            }
            Thread.sleep(3000);
            String pageSource = driver.getPageSource();
            Files.write(Paths.get("file/html/facebook.html"), pageSource.getBytes());

            return Jsoup.parse(pageSource);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return null;
    }

    public void login(WebDriver driver) {

        String url = "https://www.facebook.com/login/";
        String account = "YOUR_ACCOUNT";
        String password = "YOUR_PASSWORD";

        try{
            // 開啟 Facebook 網頁
            driver.get(url);

            // 等待帳號欄位出現並定位
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(300));
            WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("email")));
            WebElement passInput = driver.findElement(By.name("pass"));

            // 輸入帳號和密碼
            emailInput.sendKeys(account);
            passInput.sendKeys(password);

            // 點擊登入按鈕
            WebElement loginButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='loginbutton']")));
            loginButton.click();

            Thread.sleep(5000);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private Elements getPostList(Document doc) {

        // 取得貼文的篩選條件的 span
        Element postQuerySpan = doc.select("span.x193iq5w.xeuugli.x13faqbe.x1vvkbs.x1xmvt09.x1lliihq.x1s928wv.xhkezso.x1gmr53x.x1cpjm7i.x1fgarty.x1943h6x.xtoi2st.x3x7a5m.x1603h9y.x1u7k74.x1xlr1w8.xzsf02u.x1yc453h").last();

        // 取得貼文的篩選條件的最外層的 <div>
        /**
         * 為什麼不直接找到最後一個 div.x1yztbdb 就好？ 因為在其他功能上也有使用到，因此可能會定位不到正確的元素
         */
        Element postQueryParent = postQuerySpan.closest("div.x1yztbdb");

        // 選取上述 <div> 的下一個兄弟元素 <div>，也就是貼文清單的最外層
        Element postListParent = postQueryParent.nextElementSibling();

        return postListParent.children();
    }

    private List<FbPostDto> parse(Elements postList) {

        List<FbPostDto> list = new ArrayList<>();

        for (Element post : postList) {

            FbPostDto fbPostDto = new FbPostDto();

            // 貼文內容
            Elements contentDiv = post.select("div[dir=auto][style='text-align: start;']");
            StringBuilder contentBuilder = new StringBuilder();
            for (Element e : contentDiv) {
                contentBuilder.append(e.text());
            }
            fbPostDto.setText(contentBuilder.toString());

            // 圖片
            Element imgParentDiv = post.select("img").first();
            String imgUrl = imgParentDiv == null ? "" : imgParentDiv.attr("src");
            fbPostDto.setImageUrl(imgUrl);

            // 讚數、留言數、分享數
            Element mood = post.select("div.x6s0dn4.xi81zsa.x78zum5.x6prxxf.x13a6bvl.xvq8zen.xdj266r.xat24cr.x1d52u69.xktsk01.x889kno.x1a8lsjc.xkhd6sd.x4uap5.x80vd3b.x1q0q8m5.xso031l").first();
            if (mood != null) {
                fbPostDto.setLikes(mood.select("span.xt0b8zv.x1e558r4").get(0).text());

                Elements commentsAndShares = post.select("span.html-span.xdj266r.x11i5rnm.xat24cr.x1mh8g0r.xexx8yu.x4uap5.x18d9i69.xkhd6sd.x1hl2dhg.x16tdsg8.x1vvkbs.x1sur9pj.xkrqix3");

                for(Element element : commentsAndShares){
                    String textContent = element.text();

                    //由於 留言數與分享數的 html 結構幾乎一致，因此暫時先用字串判斷
                    if (textContent.contains("留言")) {
                        fbPostDto.setComments(textContent);
                    } else if (textContent.contains("分享")) {
                        fbPostDto.setShares(textContent);
                    }
                }
            }

            list.add(fbPostDto);
        }

        return list;
    }
}
