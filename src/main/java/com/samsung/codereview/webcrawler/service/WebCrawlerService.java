package com.samsung.codereview.webcrawler.service;

import com.samsung.codereview.webcrawler.domain.TextContentCount;
import com.samsung.codereview.webcrawler.exception.InvalidUrlFormatException;
import com.samsung.codereview.webcrawler.exception.UrlSetSizeOverException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WebCrawlerService {
    private static final String TEXT_DELIMETER = " ";

    private TextContentService textContentService;
    private UrlAccessManageService urlAccessManageService;

    @Autowired
    public WebCrawlerService(TextContentService textContentService, UrlAccessManageService urlAccessManageService) {
        this.textContentService = textContentService;
        this.urlAccessManageService = urlAccessManageService;
    }

    public List<TextContentCount> getUsedTextContentList(String url) {
        getContentsDataFromWebDocument(url);
        return textContentService.getUsedTextContentList();
    }

    public void getContentsDataFromWebDocument(String url) {
        log.info(String.format("[ContentsDataFromWebDocument] url : %s", url));
        searchContentDataAndHyperLinkWithRetrieving(url);
    }

    public void searchContentDataAndHyperLinkWithRetrieving(String url) {
        try {
            Document document = getDocumentFromUrl(url);
            if (urlAccessManageService.isCoverUrlSet()) {
                saveAccessedUrl(url);
                searchContentData(url, document);
                retrieveHyperLink(searchHyperLink(document));
            }
        } catch (UrlSetSizeOverException e) {
            log.warn(e.getMessage());
        } catch (InvalidUrlFormatException e) {
            log.warn(e.getMessage());
        } catch (IOException e) {
            log.warn(e.getMessage());
        } catch (NullPointerException e) {
            log.warn(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage());
        }
    }

    public Document getDocumentFromUrl(String url) throws IOException {
        return Jsoup.connect(url).get();
    }

    public void searchContentData(String url, Document document) throws NullPointerException {
        String[] texts = document.body().text().split(TEXT_DELIMETER);
        for (String text : texts) {
            textContentService.addAfterCheckingValidation(url, text);
        }
    }

    public boolean saveAccessedUrl(String hyperLinkUrl) throws InvalidUrlFormatException, UrlSetSizeOverException {
        return urlAccessManageService.addUrl(hyperLinkUrl);
    }

    public List<String> searchHyperLink(Document document) {
        List<String> hyperLinks = new ArrayList<>();
        Elements elements = document.select("[href]");
        for (Element element : elements) {
            hyperLinks.add(element.attr("abs:href"));
        }
        return hyperLinks;
    }

    public void retrieveHyperLink(List<String> hyperLinkUrls) throws UrlSetSizeOverException {
        hyperLinkUrls.parallelStream()
                .filter(url -> urlAccessManageService.isUrlEmpty(url))
                .forEach(url -> {
                    if (!urlAccessManageService.isCoverUrlSet())
                        throw new UrlSetSizeOverException("[UrlSetSizeOverException] There are too many urls");
                    getContentsDataFromWebDocument(url);
                });
    }


}
