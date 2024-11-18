package com.ele.crawler;

import com.google.gson.Gson;
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

public class FacebookPostCrawlerWithoutLogin {

    public static void main(String[] args) {
        FacebookPostCrawlerWithoutLogin example = new FacebookPostCrawlerWithoutLogin();
        String url = "https://www.facebook.com/JesseTang11/";
//        String url = "https://www.facebook.com/adaymag/";
        example.start(url);
    }

    private void start(String url) {

        Document doc = getDocument(url);

        Elements postList = getPostList(doc);

        List<FbPostDto> fbPostList = parse(postList);

        System.out.println(new Gson().toJson(fbPostList));
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

            driver.get(url);

            // 點擊關閉按鈕
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(300));
            WebElement closeLoginFormButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@aria-label='關閉']")));
            closeLoginFormButton.click();

            /**
             * 最多只能下滾 1~2 次
             */
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            System.out.println("scroll");
            Thread.sleep(5000); // 暫停5秒

            clickSeeMoreButton(driver);

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

    private static void clickSeeMoreButton(WebDriver driver) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement seeMoreButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@role='button' and text()='查看更多']")));

        executor.executeScript("arguments[0].click();", seeMoreButton);
    }

    private Elements getPostList(Document doc) {

        if(doc == null) {
            throw new RuntimeException("抓取資料失敗");
        }

        Element postParentDiv = doc.select("div.x9f619.x1n2onr6.x1ja2u2z.xeuugli.x1iyjqo2.xs83m0k.xjl7jj.x1xmf6yo.x1emribx.x1e56ztr.x1i64zmx.x19h7ccj.x65f84u").first();

        Element innerDiv = postParentDiv.select("div").first();

        return innerDiv.children();
    }

    private List<FbPostDto> parse(Elements postList) {

        List<FbPostDto> fbPostDtoList = new ArrayList<>();

        for (Element post : postList) {
            FbPostDto fbPostDto = new FbPostDto();

            fbPostDto.setText(getText(post));
            fbPostDto.setImageUrl(getImageUrl(post));

            // 讚數、留言數、分享數
            Element mood = getMood(post);
            if (mood != null) {
                fbPostDto.setLikes(getLikes(fbPostDto, mood));

                Element commentsAndSharesParent = mood.select("div.x9f619.x1ja2u2z.x78zum5.x2lah0s.x1n2onr6.x1qughib.x1qjc9v5.xozqiw3.x1q0g3np.xykv574.xbmpl8g.x4cne27.xifccgj").first();

                Elements commentsAndShares = commentsAndSharesParent.select("div.x9f619.x1n2onr6.x1ja2u2z.x78zum5.xdt5ytf.x2lah0s.x193iq5w.xeuugli.xsyo7zv.x16hj40l.x10b6aqq.x1yrsyyn");

                for (Element e : commentsAndShares) {
                    Elements icon = e.select("i");
                    String iconStyle = icon.attr("style");
                    String count = e.select("span.html-span").text();

                    // 留言與分享數的 class、結構沒有差異，因此用 css 判斷
                    if (iconStyle.contains("background-position: 0px -1350px")) {
                        fbPostDto.setComments(count);
                    } else if (iconStyle.contains("background-position: 0px -1367px")) {
                        fbPostDto.setShares(count);
                    }
                }
            }

            fbPostDtoList.add(fbPostDto);
        }

        return fbPostDtoList;
    }

    private static String getText(Element post) {
        Elements contentDiv = post.select("div[dir=auto][style='text-align: start;']");
        StringBuilder contentBuilder = new StringBuilder();
        for (Element e : contentDiv) {
            contentBuilder.append(e.text());
        }
        return contentBuilder.toString();
    }

    private String getImageUrl(Element post) {
        Element imgParentDiv = post.select("img").first();
        String imgUrl = imgParentDiv == null ? "" : imgParentDiv.attr("src");
        return imgUrl;
    }

    private Element getMood(Element post) {
        Element mood = post.select("div.x6s0dn4.xi81zsa.x78zum5.x6prxxf.x13a6bvl.xvq8zen.xdj266r.xat24cr.x1d52u69.xktsk01.x889kno.x1a8lsjc.xkhd6sd.x4uap5.x80vd3b.x1q0q8m5.xso031l").first();
        return mood;
    }

    private String getLikes(FbPostDto fbPostDto, Element mood) {
        return mood.select("span.xt0b8zv, span.x1e558r4").text();
    }
}
